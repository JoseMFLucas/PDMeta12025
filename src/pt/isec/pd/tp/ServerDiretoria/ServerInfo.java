package pt.isec.pd.tp.ServerDiretoria;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class ServerInfo {
    private final InetAddress ip;
    private final int tcpPortClientes;
    private final int tcpPortDb;
    private final long registrationTime;
    private long lastHeartbeatTime;

    public ServerInfo(InetAddress ip, int tcpPortClientes, int tcpPortDb) {
        this.ip = ip;
        this.tcpPortClientes = tcpPortClientes;
        this.tcpPortDb = tcpPortDb;
        this.registrationTime = System.currentTimeMillis();
        this.lastHeartbeatTime = System.currentTimeMillis();
    }

    public InetAddress getIp() { return ip; }

    public Integer getPort() { return tcpPortClientes; }

    public int getTcpPortClientes() { return tcpPortClientes; }

    public int getTcpPortDb() { return tcpPortDb; }

    public long getRegistrationTime() { return registrationTime; }

    public long getLastHeartbeatTime() { return lastHeartbeatTime; }

    public void setLastHeartbeatTime(long lastHeartbeatTime) { this.lastHeartbeatTime = lastHeartbeatTime; }

    @Override
    public boolean equals(Object obj) {
        if(this.getIp().equals(((DatagramPacket)obj).getAddress())) {
            return true;
        } else {
            return this.getPort().equals(((DatagramPacket)obj).getPort());
        }
    }

    public void updateLastHeartbeatTime() {
        this.lastHeartbeatTime = System.currentTimeMillis();
    }
}

