package pt.isec.pd.tp.Server.comunicacao;

import pt.isec.pd.tp.Server.ServidorMain;
import pt.isec.pd.tp.Utils.Configs;
import pt.isec.pd.tp.Server.ServidorMain;
import pt.isec.pd.tp.Utils.Configs;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class HeartbeatManager implements Runnable {
    private final ServidorMain servidor;
    private boolean running = true;

    public HeartbeatManager(ServidorMain servidor) {
        this.servidor = servidor;
    }

    @Override
    public void run() {
        try (DatagramSocket udpSocket = new DatagramSocket();
             MulticastSocket multicastSocket = new MulticastSocket()) {
            
            udpSocket.setSoTimeout(5000);

            InetAddress diretoriaAddr = InetAddress.getByName(servidor.getDiretoriaIp());
            InetAddress multicastAddr = InetAddress.getByName(Configs.MULTICAST_ADDRESS);

            while (running) {

                String payload = String.format("HEARTBEAT;%d", servidor.getPortoTCPClientes());

                byte[] data = payload.getBytes();

                // Enviar heartbeat UDP para o Serviço de Diretoria
                DatagramPacket udpPacket = new DatagramPacket(data, data.length, diretoriaAddr, servidor.getDiretoriaPort());
                udpSocket.send(udpPacket);

                udpPacket = new DatagramPacket(data, data.length);
                udpSocket.receive(udpPacket);
                String response = new String(udpPacket.getData(), 0, udpPacket.getLength());

                if(servidor.processarRespostaDiretoria(response) == 1) {

                    DatagramPacket multicastPacket = new DatagramPacket(data, data.length, multicastAddr, Configs.MULTICAST_PORT);
                    multicastSocket.send(multicastPacket);
                }

            }
        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
        }
    }

    public void stop() {
        this.running = false;
    }
}