package pt.isec.pd.tp.Server.comunicacao;

import pt.isec.pd.tp.Server.logica.ServidorLogica;
import pt.isec.pd.tp.Utils.Configs;

import java.net.*;

public class MulticastSpeaker implements Runnable {

    private final ServidorLogica logica;
    private final String multicastInterfaceIp; // IP da interface de rede
    private boolean running;
    private MulticastSocket socket;


    public MulticastSpeaker(ServidorLogica logica, String multicastInterfaceIp, boolean running) {
        this.logica = logica;
        this.multicastInterfaceIp = multicastInterfaceIp;
        this.running = running;
    }

    @Override
    public void run() {

        try {
            while (running) {
                socket = new MulticastSocket();
                if (multicastInterfaceIp == null || multicastInterfaceIp.isEmpty()) {
                    throw new Exception("IP da interface de rede para multicast não definido.");
                }
                InetSocketAddress group = new InetSocketAddress(multicastInterfaceIp, Configs.MULTICAST_PORT);

                // Definir a interface de rede para o multicast
                NetworkInterface netIf = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
                socket.setNetworkInterface(netIf);

                socket.joinGroup(group, netIf);

                String payload = logica.getVersaoBaseDados();
                byte[] buffer = payload.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group);
                socket.send(packet);
                Thread.sleep(Configs.HEARTBEAT_INTERVAL_MS);
            }

        } catch (Exception e) {
            if(running)
                System.out.println("Warning: " + e.getMessage());
            System.out.println("A encerrar o MulticastListener");
        }
    }

    public void sendDatabaseUpdate(int version, String sql) {
        try {
            InetSocketAddress group = new InetSocketAddress(multicastInterfaceIp, Configs.MULTICAST_PORT);
            if(socket == null || socket.isClosed()) {
                socket = new MulticastSocket();
                if (multicastInterfaceIp == null || multicastInterfaceIp.isEmpty()) {
                    throw new Exception("IP da interface de rede para multicast não definido.");
                }
                // Definir a interface de rede para o multicast
                NetworkInterface netIf = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
                socket.setNetworkInterface(netIf);

                socket.joinGroup(group, netIf);
            }
            String payload = version + ";" + sql;
            byte[] buffer = payload.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group);
            socket.send(packet);}
        catch (Exception e) {
            if(running)
                System.out.println("Warning: " + e.getMessage());
            System.out.println("A encerrar o MulticastListener");
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        if(running)
            running = false;
        if(socket != null && !socket.isClosed())
            socket.close();

    }


}