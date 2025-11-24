package pt.isec.pd.tp.ServerDiretoria;

import pt.isec.pd.tp.Utils.Configs;
import pt.isec.pd.tp.Utils.MessageCodes;

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
    private Thread GeraTeclado, processaPacotes, tcpToServerPrincipal;
    private Boolean closing = false;
    private ServerSocket servidorSocket;
    private Socket socketLigacao;
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
                        if(socketLigacao != null && !socketLigacao.isClosed())
                            socketLigacao.close();

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
            }
            System.out.println("A fechar o serviço de diretoria...");
        }
        System.out.println("Thread de principal terminada.");
    }

    private void processPacket(DatagramPacket packet) {
        try {
            String request = new String(packet.getData(), 0, packet.getLength());

            StringTokenizer tokenizer = new StringTokenizer(request, ";");
            String responseStr = "ERROR;Comando desconhecido";

            if (request.startsWith("REGISTER")) {
                if(tokenizer.countTokens()<3)
                {
                    System.out.println("ERROR;Parâmetros insuficientes para registo");
                    return;
                }
                activeServers.add(new ServerInfo(packet.getAddress(), Integer.parseInt(request.split(";")[1]),  Integer.parseInt(request.split(";")[2])));

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

                for (ServerInfo server : activeServers) {

                    if (server.compareServer(packet)) {

                        server.updateLastHeartbeatTime();
                        try {
                            
                            if(activeServers.getFirst().compareServer(packet)) {
                                responseStr = "HEARTBEAT;PRINCIPAL";

                                if(!tcpOpen) {

                                    try {
                                        tcpToServerPrincipal = new Thread(this::TcpToServerPrincipal);
                                        tcpToServerPrincipal.start();
                                        tcpOpen = true;
                                    } catch (Exception e) {
                                        System.out.println("Erro ao abrir porto TCP para comunicar com servidor principal: " + e.getMessage());
                                    }
                                }
                            }
                            else {
                                responseStr = "HEARTBEAT;" + activeServers.getFirst().getIp().getHostAddress() + ";" + activeServers.getFirst().getTcpPortClientes();
                            }
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
                }

           /* } else if (request.startsWith("GET_SERVER")) {
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
                }*/
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

    private void TcpToServerPrincipal() {

        try {

            Socket s;
            PrintStream out;
            BufferedReader in;

            if (servidorSocket != null) {
                s = servidorSocket.accept();

                out = new PrintStream(s.getOutputStream());
                in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                socketLigacao = s;
            }
            else
                return;


            while (!closing) {

                out.println(in.readLine());

            }
            out.println(MessageCodes.CLOSE_CONNECTION);

        } catch (Exception e) {
            System.out.println("Erro na comunicação TCP com o servidor principal: " + e.getMessage());
        }
        finally {
            System.out.println("Fechando comunicação TCP com o servidor principal.");
            tcpOpen = false;
            socket.close();
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