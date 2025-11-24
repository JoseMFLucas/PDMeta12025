package pt.isec.pd.tp.ServerDiretoria;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;

public class ServerInfo {
    private final InetAddress ip;
    private final int tcpPortClientes;
    private final int tcpPortDb;
    private final long registrationTime;
    private long lastHeartbeatTime;
    private int port;

    public ServerInfo(InetAddress ip, int tcpPortClientes, int tcpPortDb) {
        this.ip = ip;
        this.tcpPortClientes = tcpPortClientes;
        this.tcpPortDb = tcpPortDb;
        this.registrationTime = System.currentTimeMillis();
        this.lastHeartbeatTime = System.currentTimeMillis();
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public InetAddress getIp() { return ip; }

    public Integer getTcpPortClientes() { return tcpPortClientes; }

    public int getTcpPortDb() { return tcpPortDb; }

    public long getRegistrationTime() { return registrationTime; }

    public long getLastHeartbeatTime() { return lastHeartbeatTime; }


    public boolean compareServer(DatagramPacket packet) {
        String request = new String(packet.getData(), 0, packet.getLength());
        String[] parts = request.split(";");
        int port = Integer.parseInt(parts[1]);

        return this.getIp().equals(packet.getAddress()) && this.getTcpPortClientes() == port;
    }

    public void updateLastHeartbeatTime() {
        this.lastHeartbeatTime = System.currentTimeMillis();
    }
}

