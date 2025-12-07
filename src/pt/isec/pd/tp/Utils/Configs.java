package pt.isec.pd.tp.Utils;

public class Configs {

    public static final String Pasta_CSV = "../../../../../../csv";

    public static final String Pasta_CSV_Vista = "../../../../../../../../csv";

    // Endereço e porto Multicast para heartbeats dos servidores
    public static final int MULTICAST_PORT = 3030;

    // Intervalo de heartbeat dos servidores (5 segundos)
    public static final int HEARTBEAT_INTERVAL_MS = 5000;

    // Timeout para o serviço de diretoria remover servidores inativos (17 segundos)
    public static final int SERVER_TIMEOUT_MS = 17000;

    // Timeout para autenticação do cliente (30 segundos)
    public static final int AUTH_TIMEOUT_MS = 30000;

    // Timeout para tentativa de reconexão do cliente (20 segundos)
    public static final int RECONNECT_TIMEOUT_MS = 20000;
}
