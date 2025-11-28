package pt.isec.pd.tp.Server.comunicacao;

import pt.isec.pd.tp.Server.logica.ServidorLogica;
import pt.isec.pd.tp.Utils.Configs;

import java.net.*;

public class MulticastSpeaker implements Runnable {

    private final ServidorLogica logica;
    private final String multicastInterfaceIp; // IP da interface de rede
    private final boolean running;

    public MulticastSpeaker(ServidorLogica logica, String multicastInterfaceIp, boolean running) {
        this.logica = logica;
        this.multicastInterfaceIp = multicastInterfaceIp;
        this.running = running;
    }

    @Override
    public void run() {

        try(MulticastSocket socket = new MulticastSocket()) {
            if(multicastInterfaceIp == null || multicastInterfaceIp.isEmpty()) {
                throw new Exception("IP da interface de rede para multicast não definido.");
            }
            InetSocketAddress group = new InetSocketAddress(multicastInterfaceIp, Configs.MULTICAST_PORT);

            // Definir a interface de rede para o multicast
            NetworkInterface netIf = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            socket.setNetworkInterface(netIf);

            socket.joinGroup(group, netIf);

            String payload = logica.getVersaoBaseDados().toString() + ";0;0"; // add client handler tcp port and tcp port for db sync
            byte[] buffer = payload.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group);
            socket.send(packet);

        } catch (Exception e) {
            if(running)
                System.out.println("Warning: " + e.getMessage());
            System.out.println("A encerrar o MulticastListener");
        }
    }
}