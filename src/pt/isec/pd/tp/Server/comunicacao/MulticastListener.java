package pt.isec.pd.tp.Server.comunicacao;

import pt.isec.pd.tp.Server.logica.ServidorLogica;
import pt.isec.pd.tp.Utils.Configs;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

public class MulticastListener implements Runnable {
    private final ServidorLogica logica;
    private final String multicastInterfaceIp; // IP da interface de rede
    private final boolean running;

    public MulticastListener(ServidorLogica logica, String multicastInterfaceIp, boolean running) {
        this.logica = logica;
        this.multicastInterfaceIp = multicastInterfaceIp;
        this.running = running;
    }

    @Override
    public void run() {
        try (MulticastSocket socket = new MulticastSocket(Configs.MULTICAST_PORT)) {
            InetAddress group = InetAddress.getByName(Configs.MULTICAST_ADDRESS);

            // Definir a interface de rede para o multicast
            NetworkInterface netIf = NetworkInterface.getByInetAddress(InetAddress.getByName(multicastInterfaceIp));
            socket.setNetworkInterface(netIf);

            //socket.joinGroup(group);

            while (running) {
                byte[] buffer = new byte[4096]; // Buffer maior para queries SQL
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                socket.receive(packet);

                while(netIf.getInetAddresses().hasMoreElements()) {
                    InetAddress addr = netIf.getInetAddresses().nextElement();
                    if (addr.equals(packet.getAddress())) {
                        throw new Exception("Ignorando pacote multicast próprio.");
                    }
                }

                // Ignorar se for o principal (só backups ouvem)
                if (logica.isServidorPrincipal()) {
                    throw new Exception("Ignorando pacote multicast próprio.");
                }

                String payload = new String(packet.getData(), 0, packet.getLength());
                //logica.processarMensagemMulticast(payload);
                System.out.println("Recebido multicast: " + payload);
            }
        } catch (Exception e) {
            System.out.println("Warning: " + e.getMessage());
        }
    }
}