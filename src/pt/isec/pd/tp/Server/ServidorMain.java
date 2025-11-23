package pt.isec.pd.tp.Server;

import pt.isec.pd.tp.Server.comunicacao.HeartbeatManager;
import pt.isec.pd.tp.Server.comunicacao.MulticastListener;
import pt.isec.pd.tp.Server.comunicacao.ServidorTCPListener;
import pt.isec.pd.tp.Server.logica.ServidorLogica;
import pt.isec.pd.tp.Utils.Configs;
import pt.isec.pd.tp.Utils.Mensagem;
import pt.isec.pd.tp.Utils.MessageCodes;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class ServidorMain {
    private final String diretoriaIp;
    private final int diretoriaPort;
    private final String multicastIp;
    private ServidorTCPListener tcpListener;

    private Thread tcpThread;

    private boolean isPrincipal = false;
    private final ServidorLogica logica = new ServidorLogica(this);

    private boolean running = true;

    private int portoTCPClientes;

    ScheduledExecutorService heartbeat;

    public void start() {
        try {

            tcpListener = new ServidorTCPListener(logica, 0);
            tcpThread =  new Thread(tcpListener);
            tcpThread.start();

            this.portoTCPClientes = tcpListener.getLocalPort();

            System.out.println("Servidor escutando clientes em TCP:" + portoTCPClientes);

            // Registar no Serviço de Diretoria via UDP
            try(DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(Configs.SERVER_TIMEOUT_MS);

                String mensagem = "REGISTER;" + portoTCPClientes + ";" + "0"; // DB port placeholder
                byte[] buffer = mensagem.getBytes();

                InetAddress diretoriaAddress = InetAddress.getByName(diretoriaIp);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, diretoriaAddress, diretoriaPort);
                socket.send(packet);
                System.out.println("Registo enviado ao Serviço de Diretoria.");

                // Aguardar resposta
                byte[] responseBuffer = new byte[1024];
                DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
                socket.receive(responsePacket);
                String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
                if(response.startsWith("OK;")) {
                    System.out.println("Registo no Serviço de Diretoria bem sucedido.");
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

            //MulticastListener multicastListener = new MulticastListener(logica, multicastIp);
            //multicastListener.run();

            System.out.println("Servidor iniciado e operacional.");

        } catch (Exception e) {
            System.out.println("Erro ao iniciar Servidor: " + e.getMessage());
        }
    }

    public int processarRespostaDiretoria(DatagramPacket udpPacket) {

        String response = new String(udpPacket.getData(), 0, udpPacket.getLength());
        System.out.println("Servidor escutando resposta diretoria: " + response);

        if (response.startsWith("HEARTBEAT")) {
            if(!isPrincipal()) {
                String ip = response.split(";")[1];
                int porto = Integer.parseInt(response.split(";")[2]);
                if (ip.equals(udpPacket.getAddress().getHostAddress()) && porto == getPortoTCPClientes()) {
                    setPrincipal(true);
                    System.out.println("Servidor é agora o PRINCIPAL.");
                    return 1;
                } else {
                    setPrincipal(false);
                    return 0;
                }
            }
        } else if( response.contains(MessageCodes.CLOSE_CONNECTION.toString())) {
            tcpThread.interrupt();
            heartbeat.close();
            System.out.println("Servidor principal mudou. A encerrar ligações de clientes.");
            return -1;
        } else {
            System.err.println("Resposta desconhecida do Serviço de Diretoria: " + response);
        }
        return 0;
    }

    public ServidorMain(String diretoriaIp, int diretoriaPort, String dbPath, String multicastIp) {
        this.diretoriaIp = diretoriaIp;
        this.diretoriaPort = diretoriaPort;
        this.multicastIp = multicastIp;
    }

    public boolean isPrincipal() { return isPrincipal; }
    public void setPrincipal(boolean principal) { isPrincipal = principal; }
    public int getPortoTCPClientes() { return portoTCPClientes; }
    public String getDiretoriaIp() { return diretoriaIp; }
    public int getDiretoriaPort() { return diretoriaPort; }
    public void stop() { this.running = false; }

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