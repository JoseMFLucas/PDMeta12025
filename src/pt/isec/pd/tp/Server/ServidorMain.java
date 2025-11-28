package pt.isec.pd.tp.Server;

import pt.isec.pd.tp.Server.comunicacao.HeartbeatManager;
import pt.isec.pd.tp.Server.comunicacao.MulticastListener;
import pt.isec.pd.tp.Server.comunicacao.MulticastSpeaker;
import pt.isec.pd.tp.Server.comunicacao.ServidorTCPListener;
import pt.isec.pd.tp.Server.dados.DBManager;
import pt.isec.pd.tp.Server.logica.ServidorLogica;
import pt.isec.pd.tp.Utils.Configs;
import pt.isec.pd.tp.Utils.Mensagem;
import pt.isec.pd.tp.Utils.MessageCodes;

import java.io.*;
import java.net.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class ServidorMain {

    private final String diretoriaIp;
    private final int diretoriaPort;
    private final String multicastIp;
    private ServidorTCPListener tcpListener;

    private String principalIp;
    private int principalPort;

    private ServerSocket socketComP;
    private Thread threadComP;
    private MulticastSocket multicastSocket;
    private Socket socket;
    private final DBManager dbManager;

    private Thread tcpThread;

    private boolean isPrincipal = false;
    private final ServidorLogica logica;

    private boolean running = true;

    private int portoTCPClientes;

    ScheduledExecutorService heartbeat;
    ScheduledExecutorService multicastSpeaker;

    public void start() {
        try {

            socketComP = new ServerSocket(0); // Porto aleatório para comunicação com o servidor principal

            socket = new Socket();

            tcpListener = new ServidorTCPListener(logica, socket.getPort());
            tcpThread =  new Thread(tcpListener);
            tcpThread.start();

            this.portoTCPClientes = tcpListener.getLocalPort();

            System.out.println("Servidor escutando clientes em TCP:" + portoTCPClientes);

            // Registar no Serviço de Diretoria via UDP
            try(DatagramSocket udpsocket = new DatagramSocket()) {
                udpsocket.setSoTimeout(Configs.SERVER_TIMEOUT_MS);


                String mensagem = "REGISTER;" + portoTCPClientes + ";" + socketComP.getLocalPort(); // DB port placeholder
                System.out.println("A registar no Serviço de Diretoria: " + mensagem);
                byte[] buffer = mensagem.getBytes();

                InetAddress diretoriaAddress = InetAddress.getByName(diretoriaIp);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, diretoriaAddress, diretoriaPort);
                udpsocket.send(packet);
                System.out.println("Registo enviado ao Serviço de Diretoria.");

                // Aguardar resposta
                byte[] responseBuffer = new byte[1024];

                DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);

                udpsocket.receive(responsePacket);

                String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
                if(response.startsWith("OK;")) {

                    if(response.split(";").length == 3) {
                        principalIp = response.split(";")[1];
                        principalPort = Integer.parseInt(response.split(";")[2]);
                        if (principalIp.equals(packet.getAddress().getHostAddress()) && principalPort == getPortoTCPClientes()) {
                            setPrincipal(true);
                            System.out.println("Servidor iniciado como principal.");
                        } else {
                            setPrincipal(false);
                            System.out.println("Servidor iniciado como secundário. A comunicar com o principal em " + principalIp + ":" + principalPort);
                        }
                    }
                    else
                        throw new Exception("Resposta do servidor com numero de argumentos invalida." + response);
                } else {
                    throw new Exception("Falha ao registrar o servidor: " + response);
                }
            }
            catch (Exception e) {
                System.err.println("Erro ao registar no Serviço de Diretoria: " + e.getMessage());
                System.exit(1);
            }

            heartbeat = Executors.newSingleThreadScheduledExecutor();
            heartbeat.scheduleAtFixedRate( new HeartbeatManager(this), Configs.HEARTBEAT_INTERVAL_MS, Configs.HEARTBEAT_INTERVAL_MS, java.util.concurrent.TimeUnit.MILLISECONDS);

            multicastSocket = new MulticastSocket(Configs.MULTICAST_PORT);
            MulticastListener multicastListener = new MulticastListener(logica, multicastIp, running, multicastSocket);
            new Thread(multicastListener).start();

            multicastSpeaker = Executors.newSingleThreadScheduledExecutor();
            multicastSpeaker.scheduleAtFixedRate( new MulticastSpeaker(logica, multicastIp, running), Configs.HEARTBEAT_INTERVAL_MS, Configs.HEARTBEAT_INTERVAL_MS, java.util.concurrent.TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            System.out.println("Erro ao iniciar Servidor: " + e.getMessage());
        }
    }

    public void processarRespostaDiretoria(DatagramPacket udpPacket) {

        String response = new String(udpPacket.getData(), 0, udpPacket.getLength());

        if (response.startsWith("HEARTBEAT")) {
            if(response.contains("PRINCIPAL")) {
                if(!isPrincipal){
                    System.out.println("Servidor agora é o principal.");
                }
                setPrincipal(true);


                return;
            }
            else
            {
                if(threadComP != null && !threadComP.isAlive()){
                    principalIp = response.split(";")[1];
                    principalPort = Integer.parseInt(response.split(";")[2]);
                    /*threadComP = new Thread(this::ComunicadorPrincipalTCP);
                    threadComP.start();*/
                    System.out.println("Servidor agora é secundário. A comunicar com o principal em " + principalIp + ":" + principalPort);
                    setPrincipal(false);
                    return;
                }

            }
        } else if( response.contains(MessageCodes.CLOSE_CONNECTION.toString())) {
            if (tcpListener != null) {
                tcpListener.close();
            }
            if(tcpThread != null && tcpThread.isAlive())
                tcpThread.interrupt();
            if(heartbeat != null && !heartbeat.isShutdown())
                heartbeat.shutdownNow();
            System.out.println("Servidor principal mudou. A encerrar ligações de clientes.");
            return;
        } else {
            System.err.println("Resposta desconhecida do Serviço de Diretoria: " + response);
        }
    }

    public ServidorMain(String diretoriaIp, int diretoriaPort, String dbPath, String multicastIp) {
        this.diretoriaIp = diretoriaIp;
        this.diretoriaPort = diretoriaPort;
        this.multicastIp = multicastIp;
        this.dbManager = new DBManager(dbPath);
        this.logica = new ServidorLogica(this, this.dbManager);
    }

    public boolean isRunning() { return running; }
    public boolean isPrincipal() { return isPrincipal; }
    public void setPrincipal(boolean principal) { isPrincipal = principal; }
    public int getPortoTCPClientes() { return portoTCPClientes; }
    public String getDiretoriaIp() { return diretoriaIp; }
    public int getDiretoriaPort() { return diretoriaPort; }

    public void stop() {
        System.out.println("A encerrar o servidor...");
        if(socketComP != null && !socketComP.isClosed()) {
            try {
                socketComP.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar socket de comunicação com o principal: " + e.getMessage());
            }
        }
        if(multicastSpeaker != null && !multicastSpeaker.isShutdown())
            multicastSpeaker.shutdownNow();
        if(multicastSocket != null && !multicastSocket.isClosed()) {
            multicastSocket.close();
        }
        if (tcpListener != null) {
            tcpListener.close();
        }
        if(tcpThread != null && tcpThread.isAlive())
            tcpThread.interrupt();
        if(heartbeat != null && !heartbeat.isShutdown())
            heartbeat.shutdownNow();
        setPrincipal(false);
        running = false;
    }


    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Sintaxe: java pt.isec.pd.tp.servidor.ServidorMain <ip_diretoria> <porto_diretoria> <db_path> <ip_multicast>");
            return;
        }
        try {
            String dirIp = args[0];
            int dirPort = Integer.parseInt(args[1]);
            String dbPath = args[2];
            String multiIp = args[3];

            new ServidorMain(dirIp, dirPort, dbPath, multiIp).start();

        } catch (Exception e) {
            System.out.println("Erro ao iniciar Servidor: " + e.getMessage());
        }
    }


}