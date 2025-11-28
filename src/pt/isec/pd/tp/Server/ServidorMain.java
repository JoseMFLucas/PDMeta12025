package pt.isec.pd.tp.Server;

import pt.isec.pd.tp.Server.comunicacao.*;
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

    private MulticastSocket multicastSocket;
    private Socket socket;
    private final DBManager dbManager;

    private Thread tcpThread;

    private boolean isPrincipal = false;
    private final ServidorLogica logica;

    private boolean running = true;

    private int portoTCPClientes;
    private DBSyncSender dbSyncSender;

    ScheduledExecutorService heartbeat;
    ScheduledExecutorService multicastSpeaker;

    public void start() {
        try {
            socket = new Socket();

            tcpListener = new ServidorTCPListener(logica, socket.getPort());
            tcpThread =  new Thread(tcpListener);
            tcpThread.start();

            this.portoTCPClientes = tcpListener.getLocalPort();

            System.out.println("Servidor escutando clientes em TCP:" + portoTCPClientes);

            dbSyncSender = new DBSyncSender(dbManager.getDbPath());
            new Thread(dbSyncSender).start();

            // Registar no Serviço de Diretoria via UDP
            try(DatagramSocket udpsocket = new DatagramSocket()) {
                udpsocket.setSoTimeout(Configs.SERVER_TIMEOUT_MS);


                String mensagem = "REGISTER;" + portoTCPClientes + ";" + dbSyncSender.getLocalPort();
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
                    String[] parts = response.split(";");
                    if (parts.length >= 4) {
                        principalIp = parts[1];
                        int principalDbPort = Integer.parseInt(parts[3]);

                        if (principalIp.equals(packet.getAddress().getHostAddress()) && principalDbPort == dbSyncSender.getLocalPort()) {
                            setPrincipal(true);
                        } else {
                            setPrincipal(false);
                            obterBaseDadosDoPrincipal(principalIp, principalDbPort);
                        }
                    }
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
            /*
            else
            {
                if(threadComP != null && !threadComP.isAlive()){
                    principalIp = response.split(";")[1];
                    principalPort = Integer.parseInt(response.split(";")[2]);

                    System.out.println("Servidor agora é secundário. A comunicar com o principal em " + principalIp + ":" + principalPort);
                    setPrincipal(false);
                    return;
                }

            }
            */
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

    private void obterBaseDadosDoPrincipal(String ip, int port) throws IOException {//TODO comfirma se está de acordo com o protocolo NUNO
        System.out.println("A obter cópia da BD do principal (" + ip + ":" + port + ")...");
        try (Socket socket = new Socket(ip, port);
             InputStream is = socket.getInputStream();
             FileOutputStream fos = new FileOutputStream(dbManager.getDbPath())) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            System.out.println("Base de dados sincronizada com sucesso.");
        } catch (IOException e) {
            System.err.println("Falha crítica ao sincronizar BD: " + e.getMessage());
            throw e;
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