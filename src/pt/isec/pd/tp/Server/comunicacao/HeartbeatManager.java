package pt.isec.pd.tp.Server.comunicacao;

import pt.isec.pd.tp.Server.ServidorMain;
import pt.isec.pd.tp.Utils.Configs;
import pt.isec.pd.tp.Server.ServidorMain;
import pt.isec.pd.tp.Utils.Configs;

import java.net.*;

public class HeartbeatManager implements Runnable {
    private final ServidorMain servidor;

    public HeartbeatManager(ServidorMain servidor) {
        this.servidor = servidor;
    }

    @Override
    public void run() {

        try (DatagramSocket udpSocket = new DatagramSocket()) {

            udpSocket.setSoTimeout(Configs.SERVER_TIMEOUT_MS);

            InetAddress diretoriaAddr = InetAddress.getByName(servidor.getDiretoriaIp());

            String payload = String.format("HEARTBEAT;%d;%d", servidor.getPortoTCPClientes(), servidor.getDBversion());

            byte[] data = payload.getBytes();

            // Enviar heartbeat UDP para o Serviço de Diretoria
            DatagramPacket udpPacket = new DatagramPacket(data, data.length, diretoriaAddr, servidor.getDiretoriaPort());
            udpSocket.send(udpPacket);

            udpPacket = new DatagramPacket(new byte[1024], 1024);
            udpSocket.receive(udpPacket);

            servidor.processarRespostaDiretoria(udpPacket);


        } catch (Exception e) {
            servidor.stop();
            if(servidor.isRunning())
                System.out.println("Erro de heartbeat: " + e.getMessage());
        }
    }
}