package pt.isec.pd.tp.ServerDiretoria;

import pt.isec.pd.tp.Utils.Configs;
import pt.isec.pd.tp.Utils.MessageCodes;

import javax.xml.crypto.Data;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

public class ServidorDiretoriaMain {

    private final List<ServerInfo> activeServers = Collections.synchronizedList(new ArrayList<>());
    private final int udpPort;
    private DatagramSocket socket;
    private ScheduledExecutorService scheduler;
    private Thread GeraTeclado, processaPacotes;
    private Boolean closing = false;
    private ServerSocket servidorSocket;
    private boolean tcpOpen = false;

    public ServidorDiretoriaMain(int udpPort) {
        this.udpPort = udpPort;
    }

    private void TrataTeclado() {
        System.out.println("Thread de teclado iniciada. Prima ENTER para listar servidores ativos.");
        Scanner sc = new Scanner(System.in);
        try {
            while (!closing) {
                switch(sc.nextLine().toLowerCase()) {
                    case "exit":
                        System.out.println("A sair...");
                        closing = true;
                        try (DatagramSocket socket = new DatagramSocket()) {
                            byte[] buffer = MessageCodes.CLOSE_CONNECTION.getBytes();

                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("localhost"), udpPort);
                            socket.send(packet);
                        } catch (Exception e) {
                            System.out.println("Erro ao notificar servidores sobre encerramento: " + e.getMessage());
                        }


                        break;
                    case "server":
                        System.out.println("Servidores ativos:");
                        synchronized (activeServers) {
                            for (ServerInfo server : activeServers) {
                                System.out.println(" - " + server.getIp() + " (Clientes TCP: " + server.getTcpPortClientes() + ", BD TCP: " + server.getTcpPortDb() + ")");
                            }
                        }
                        break;
                    default:
                        System.out.println("Comando desconhecido. Prima ENTER para listar servidores ativos ou 'exit' para sair.");
                        break;
                }

            }
        } catch (Exception e) {
            System.out.println("Erro ao listar servidores ativos ou fechar servers em segurança: " + e.getMessage());
        }

        System.out.println("Thread de teclado terminada.");
    }

    public void start() {
        try {
            GeraTeclado = new Thread( this::TrataTeclado );
            GeraTeclado.start();

            socket = new DatagramSocket(udpPort);
            System.out.println("Serviço de Diretoria iniciado no porto UDP " + udpPort);

            // Thread para verificar timeouts dos servidores
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(this::checkServerTimeouts,
                    Configs.SERVER_TIMEOUT_MS, Configs.SERVER_TIMEOUT_MS / 2, TimeUnit.MILLISECONDS);

            servidorSocket = new ServerSocket();
            servidorSocket.bind(new InetSocketAddress("0.0.0.0", 8081));

            while (!closing) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                if(closing)
                    break;
                processaPacotes =  new Thread(() -> {
                    processPacket(packet);
                });
                processaPacotes.start();
            }
        } catch (Exception e) {
            System.out.println("Erro ao listar servidores ativos: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                scheduler.shutdownNow();
                try { // Force close to release Server should be changed
                    if (servidorSocket != null && !servidorSocket.isClosed())
                        servidorSocket.close();
                } catch (Exception e) {
                    System.out.println("Erro ao fechar o servidor socket: " + e.getMessage());
                }
            }
            System.out.println("A fechar o serviço de diretoria...");
        }
        System.out.println("Thread de principal terminada.");
    }

    private boolean manageHeartbeat(DatagramPacket packet, ServerInfo server, String responseStr) {

        if (server.compareServer(packet)) {

            server.updateLastHeartbeatTime();
            try {
                String [] args = responseStr.split(";");
                activeServers.sort(Comparator.comparingLong(ServerInfo::getRegistrationTime));

                if(!activeServers.isEmpty() && activeServers.get(0).equals(server))
                    responseStr = "HEARTBEAT;PRINCIPAL";
                else
                    responseStr = "HEARTBEAT;" + server.getIp().getHostAddress() + ";" + server.getPort();


                try(DatagramSocket socket = new DatagramSocket()){

                    byte[] buffer = responseStr.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length, packet.getAddress(), packet.getPort());
                    socket.send(responsePacket);
                }
                catch (Exception e) {
                    System.out.println("Erro ao enviar heartbeat response: " + e.getMessage());
                }
            }
            catch (Exception e) {
                System.out.println("Erro ao enviar heartbeat response: " + e.getMessage());
            }
        }
        return responseStr.equals("HEARTBEAT;PRINCIPAL");
    }

    private void processPacket(DatagramPacket packet) {
        try {
            String request = new String(packet.getData(), 0, packet.getLength());

            StringTokenizer tokenizer = new StringTokenizer(request, ";");
            String responseStr = MessageCodes.ERROR.toString();


            if (request.startsWith("REGISTER")) {
                String[] args = request.split(";");
                if(args.length != 3) {
                    byte[] responseData = responseStr.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, packet.getAddress(), packet.getPort());
                    socket.send(responsePacket);
                    return;
                }

                int portCli = Integer.parseInt(args[1]);
                int portDb = Integer.parseInt(args[2]);

                ServerInfo novoServer = new ServerInfo(packet.getAddress(), portCli, portDb);
                activeServers.add(novoServer);
                activeServers.sort(Comparator.comparingLong(ServerInfo::getRegistrationTime));

                ServerInfo principal = activeServers.get(0);

                responseStr = "OK;" + principal.getIp().getHostAddress() + ";"
                        + principal.getTcpPortClientes() + ";"
                        + principal.getTcpPortDb();
                try(DatagramSocket socket = new DatagramSocket()){
                    byte[] buffer = responseStr.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length, packet.getAddress(), packet.getPort());
                    socket.send(responsePacket);
                }
                catch (Exception e) {
                    System.out.println("Erro ao registrar servidor: " + e.getMessage());
                }
                return;

            }
            if (request.startsWith("HEARTBEAT")) {

                for (ServerInfo server : activeServers) {

                    if(manageHeartbeat(packet, server, responseStr)) {
                        break;
                    }

                }
                return;

            }
            if (request.startsWith("UNREGISTER")) {
                for (ServerInfo server : activeServers) {
                    if(activeServers.remove(server))
                    {
                        System.out.println("Servidor: " + packet.getAddress() + " removido com sucesso.");
                    }

                    return;
                }
            }
            if (request.startsWith("REQUEST_SERVER")) {
                // Pedido do Cliente para obter o Servidor Principal

                if (activeServers.isEmpty()) {
                    responseStr = "ERROR;Nenhum servidor ativo.";
                } else {
                    ServerInfo principal = activeServers.get(0);

                    responseStr = "OK;" + principal.getIp().getHostAddress() + ";" + principal.getTcpPortClientes();
                }

                try(DatagramSocket socket = new DatagramSocket()){
                    byte[] buffer = responseStr.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length, packet.getAddress(), packet.getPort());
                    socket.send(responsePacket);
                }
                catch (Exception e) {
                    System.out.println("Erro ao enviar request server response: " + e.getMessage());
                }

                return;
            }

            // Enviar resposta
            byte[] responseData = responseStr.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, packet.getAddress(), packet.getPort());
            socket.send(responsePacket);

        } catch (Exception e) {
            System.out.println("Erro ao enviar servidor: " + e.getMessage());
        }
    }


    private void checkServerTimeouts() {
        long now = System.currentTimeMillis();
        synchronized (activeServers) {
            Iterator<ServerInfo> iterator = activeServers.iterator();
            while (iterator.hasNext()) {
                ServerInfo server = iterator.next();
                if ((now - server.getLastHeartbeatTime()) > Configs.SERVER_TIMEOUT_MS) {
                    System.out.println("Servidor " + server.getIp() + " expirou. Removendo...");
                    iterator.remove();
                }
            }
        }
    }

    public static void main(String[] args) {
        if(args.length < 1){
            System.out.println("Uso: java ServidorDiretoriaMain");
            System.exit(1);
        }

        int udpPort = Integer.parseInt(args[0]);
        ServidorDiretoriaMain diretoria = new ServidorDiretoriaMain(udpPort);
        diretoria.start();
    }
}