package pt.isec.pd.tp.Server;

import pt.isec.pd.tp.Server.comunicacao.*;
import pt.isec.pd.tp.Server.dados.DBManager;
import pt.isec.pd.tp.Server.logica.ServidorLogica;
import pt.isec.pd.tp.Utils.Configs;
import pt.isec.pd.tp.Utils.Mensagem;
import pt.isec.pd.tp.Utils.MessageCodes;

import java.io.*;
import java.net.*;
import java.util.Scanner;
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

    Scanner scanner;



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
                            System.out.println("servidor registado como principal.");
                            setPrincipal(true);
                        } else {
                            System.out.println("Servidor registado como secundário.");
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

            MulticastSpeaker multicastSpeaker = new MulticastSpeaker( logica, multicastIp, running);
            new Thread(multicastSpeaker).start();
            logica.setMulticastSpeaker(multicastSpeaker);

            scanner = new Scanner(System.in);
            while (running) {
                System.out.println("Servidor a correr pressione Enter para encerrar o servidor...");
                scanner.nextLine();
                stop();

            }

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
                    setPrincipal(true);
                }
            }
            else {
                if(!isPrincipal) {
                    String[] parts = response.split(";");
                    if (parts.length == 3) {
                        String newPrincipalIp = parts[1];
                        if (!newPrincipalIp.equals(principalIp)) {
                            System.out.println("O servidor principal mudou para: " + newPrincipalIp);
                            principalIp = newPrincipalIp;
                            principalPort = Integer.parseInt(parts[2]);
                        }
                    } else {
                        throw new IllegalArgumentException("Resposta de heartbeat inválida: " + response);
                    }
                    setPrincipal(false);
                }
                else {
                    System.out.println("Server marcado como principal, mas recebeu heartbeat de outro servidor.");

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

    private void obterBaseDadosDoPrincipal(String ip, int port) throws IOException {
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

    public ServidorMain(String diretoriaIp, int diretoriaPort, String dbPath, String multicastIp) throws IOException {
        this.diretoriaIp = diretoriaIp;
        this.diretoriaPort = diretoriaPort;
        this.multicastIp = multicastIp;
        this.dbManager = new DBManager(dbPath);
        this.logica = new ServidorLogica(this, this.dbManager);
        this.dbManager.setServidorLogica(this.logica);
    }

    public boolean isRunning() { return running; }
    public boolean isPrincipal() { return isPrincipal; }
    public void setPrincipal(boolean principal) { isPrincipal = principal; }
    public int getPortoTCPClientes() { return portoTCPClientes; }
    public String getDiretoriaIp() { return diretoriaIp; }
    public int getDiretoriaPort() { return diretoriaPort; }
    public String getPrincipalIp() { return principalIp; }
    public int getDBversion(){ return dbManager.getVersaoDB(); }
    public int getdbPort(){return dbSyncSender.getLocalPort();}

    public void stop() {
        System.out.println("A encerrar o servidor...");

        try(DatagramSocket udpsocket = new DatagramSocket()) {
            udpsocket.setSoTimeout(Configs.SERVER_TIMEOUT_MS);

            String mensagem = "UNREGISTER";
            byte[] buffer = mensagem.getBytes();


            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(diretoriaIp), diretoriaPort);
            udpsocket.send(packet);
        }
        catch (Exception e) {
            System.err.println("Erro ao notificar o Serviço de Diretoria sobre o encerramento: " + e.getMessage());
        }
        if (dbSyncSender != null) {
            dbSyncSender.stop();
        }

        if (tcpListener != null) {
            tcpListener.close();
        }
        if(tcpThread != null && tcpThread.isAlive())
            tcpThread.interrupt();

        if(multicastSocket != null)
            multicastSocket.close();

        if(heartbeat != null && !heartbeat.isShutdown())
            heartbeat.shutdownNow();
        setPrincipal(false);
        running = false;
        if(scanner != null)
            scanner.close();
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