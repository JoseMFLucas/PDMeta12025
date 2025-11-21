package pt.isec.pd.tp.Utils;

public enum MessageCodes {
    REGISTER_SERVER,
    HEARTBEAT,
    REQUEST_SERVER_LIST,
    SERVER_LIST,
    AUTHENTICATE_CLIENT,
    AUTH_SUCCESS,
    AUTH_FAILURE,
    REQUEST_DB_TRANSFER,
    DB_TRANSFER_INIT,
    DB_TRANSFER_COMPLETE,
    ERROR,
    CLOSE_CONNECTION;

    public byte[] getBytes() {
        return this.name().getBytes();
    }
}
