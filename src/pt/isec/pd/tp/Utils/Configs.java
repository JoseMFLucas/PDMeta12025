package pt.isec.pd.tp.Utils;

public class Configs {

    // Intervalo de heartbeat dos servidores (5 segundos)
    public static final int HEARTBEAT_INTERVAL_MS = 5000;

    // Timeout para o serviço de diretoria remover servidores inativos (17 segundos)
    public static final int SERVER_TIMEOUT_MS = 17000;

    // Endereço e porto Multicast para heartbeats dos servidores
    public static final String MULTICAST_ADDRESS = "230.30.30.30";
    public static final int MULTICAST_PORT = 3030;
}
