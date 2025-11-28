package pt.isec.pd.tp.Server.comunicacao;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class DBSyncSender implements Runnable {
    private final int port;
    private final String dbPath;
    private boolean running = true;
    private final ServerSocket serverSocket;

    public DBSyncSender(String dbPath) throws IOException {
        this.dbPath = dbPath;
        this.serverSocket = new ServerSocket(0); // 0 = Porto automático
        this.port = this.serverSocket.getLocalPort();
    }

    public int getLocalPort() {
        return port;
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) { /* Ignorar */ }
    }

    @Override
    public void run() {
        while (running) {
            try (Socket socket = serverSocket.accept();
                 FileInputStream fis = new FileInputStream(dbPath);
                 OutputStream os = socket.getOutputStream()) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
                System.out.println("Cópia da BD enviada para um servidor secundário.");

            } catch (IOException e) {
                if (running) System.out.println("Erro no DBSyncSender: " + e.getMessage());
            }
        }
    }
}