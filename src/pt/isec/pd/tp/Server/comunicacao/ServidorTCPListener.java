package pt.isec.pd.tp.Server.comunicacao;

import pt.isec.pd.tp.Server.logica.ServidorLogica;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

// Escuta pedidos de ligação TCP de Clientes
public class ServidorTCPListener implements Runnable {
    private final ServidorLogica logica;
    private final ServerSocket serverSocket;

    public ServidorTCPListener(ServidorLogica logica, int port) throws Exception {
        this.logica = logica;
        this.serverSocket = new ServerSocket(port);
    }

    public int getLocalPort() {
        return serverSocket.getLocalPort();
    }

    @Override
    public void run() {
        try {

            while (!serverSocket.isClosed()) {

                Socket clientSocket = serverSocket.accept();

                System.out.println("Novo cliente conectado: " + clientSocket.getRemoteSocketAddress());
                ClienteHandler handler = new ClienteHandler(clientSocket, logica);
                logica.addClienteHandler(handler); // Adiciona para futuras notificações
                new Thread(handler).start();
            }
        } catch (Exception e) {
            System.err.println("Erro no ServidorTCPListener: " + e.getMessage());
        }
    }
}