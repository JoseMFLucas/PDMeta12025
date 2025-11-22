package pt.isec.pd.tp.ServerDiretoria;

import pt.isec.pd.tp.Utils.Configs;
import pt.isec.pd.tp.Utils.MessageCodes;

import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServidorDiretoriaMain {

    private final List<ServerInfo> activeServers = Collections.synchronizedList(new ArrayList<>());
    private final int udpPort;
    private DatagramSocket socket;
    private ScheduledExecutorService scheduler;
    private Thread GeraTeclado;

    public ServidorDiretoriaMain(int udpPort) {
        this.udpPort = udpPort;
    }

    private void TrataTeclado() {
        System.out.println("Thread de teclado iniciada. Prima ENTER para listar servidores ativos.");
        Scanner sc = new Scanner(System.in);
        try {
            while (true) {
                switch(sc.nextLine().toLowerCase()) {
                    case "exit":
                        System.out.println("A sair...");
                        for (Iterator<ServerInfo> iter = activeServers.iterator(); iter.hasNext();) {
                            try{
                                DatagramSocket socket1 = new DatagramSocket();
                                MessageCodes aviso = MessageCodes.CLOSE_CONNECTION;
                                byte[] buffer = aviso.getBytes();
                                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, iter.next().getIp(), Configs.MULTICAST_PORT);
                                socket1.send(packet);
                                socket1.close();
                            }
                            catch(Exception _){}
                            ServerInfo server = iter.next();
                            System.out.println("A notificar servidor " + server.getIp() + " sobre encerramento.");
                        }
                        System.exit(0);
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

            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Processar o pacote numa nova thread para não bloquear o loop
                new Thread(() -> processPacket(packet)).start();
            }
        } catch (Exception e) {
            System.out.println("Erro ao listar servidores ativos: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    private void processPacket(DatagramPacket packet) {
        try {
            String request = new String(packet.getData(), 0, packet.getLength());
            StringTokenizer tokenizer = new StringTokenizer(request, ";");
            String responseStr = "ERROR;Pedido inválido";

            if(tokenizer.countTokens() < 3) {


                byte[] responseData = responseStr.getBytes();
                DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, packet.getAddress(), packet.getPort());
                socket.send(responsePacket);
            }
            if (request.startsWith("REGISTER")) {

                // Formato: REGISTER;<porto_tcp_clientes>;<porto_tcp_bd> , Adicionar à lista 'activeServers'
                activeServers.add(new ServerInfo(packet.getAddress(), packet.getPort(),  Integer.parseInt(request.split(";")[2])));

                //    Formato: OK;<ip_principal>;<porto_bd_principal>
                activeServers.sort(Comparator.comparingLong(ServerInfo::getRegistrationTime));
                ServerInfo principal = activeServers.getFirst();
                responseStr = "OK;"+ principal.getIp().getHostAddress() + ";" + principal.getTcpPortDb();
                try(DatagramSocket socket = new DatagramSocket()){
                    byte[] buffer = responseStr.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length, packet.getAddress(), packet.getPort());
                    socket.send(responsePacket);
                }
                catch (Exception e) {
                    System.out.println("Erro ao registrar servidor: " + e.getMessage());
                }



            } else if (request.startsWith("HEARTBEAT")) {
                // Encontrar o servidor na lista (pelo IP e portos?) Se existir, atualizar o seu 'lastHeartbeatTime'

                for (ServerInfo server : activeServers) {
                    if (server.equals(packet)) { // usa o equals implementado em ServerInfo que compara IP e porto com o packet
                        server.updateLastHeartbeatTime();
                        try {
                            responseStr = "OK;" + activeServers.getFirst().getIp().getHostAddress() + ";" + activeServers.getFirst().getTcpPortDb();
                            try(DatagramSocket socket = new DatagramSocket()){
                                byte[] buffer = responseStr.getBytes();
                                DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length, packet.getAddress(), packet.getPort());
                                socket.send(responsePacket);
                            }
                        }
                        catch (Exception e) {
                            System.out.println("Erro ao enviar heartbeat response: " + e.getMessage());
                        }
                    }
                }

            } else if (request.startsWith("GET_SERVER")) {
                // Verificar se a lista 'activeServers' não está vazia Obter o primeiro da lista (servidor principal)
                activeServers.sort(Comparator.comparingLong(ServerInfo::getRegistrationTime));
                if (!activeServers.isEmpty())
                    try(DatagramSocket socket = new DatagramSocket()){
                    DatagramPacket responsePacket;

                    ServerInfo principal = activeServers.getFirst();
                    // Formato: SERVER;<ip_principal>;<porto_clientes_principal>
                    responseStr = "SERVER;" + principal.getIp().getHostAddress() + ";" + principal.getTcpPortClientes();
                    byte[] buffer = responseStr.getBytes();
                    responsePacket = new DatagramPacket(buffer, buffer.length, packet.getAddress(), packet.getPort());
                    socket.send(responsePacket);
                }
                catch (Exception e) {
                    System.out.println("Erro ao enviar servidor principal: " + e.getMessage());
                }

            } else if (request.startsWith("UNREGISTER")) {
                // Encontrar e remover o servidor da lista
                if(activeServers.removeIf(server -> server.equals(packet)))
                {
                    responseStr = "OK;Servidor removido";
                    try(DatagramSocket socket = new DatagramSocket()){
                        byte[] buffer = responseStr.getBytes();
                        DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length, packet.getAddress(), packet.getPort());
                        socket.send(responsePacket);
                    }
                    catch (Exception e) {
                        System.out.println("Erro ao enviar unregister response: " + e.getMessage());
                    }
                }
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