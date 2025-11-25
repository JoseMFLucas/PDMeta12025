package pt.isec.pd.tp.Client.Comunicacao;

import pt.isec.pd.tp.Client.Vista.ClientVista;
import pt.isec.pd.tp.Utils.MessageCodes;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.net.Socket;
import java.util.Scanner;

public class ClientComunicacao {

    private final InetAddress dirIp;
    private final int dirUDPPort;
    private final int TIMEOUT = 5000;


    public ClientComunicacao(InetAddress dirIp, int dirUDPPort) {
        this.dirIp = dirIp;
        this.dirUDPPort = dirUDPPort;
    }

    public String[] requestPrincipalServer() {
        String request = "REQUEST_SERVER"; // Comando a ser enviado para o servidor de diretoria
        String responseStr = null;

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT);

            // Enviar o pedido para a Diretoria (via UDP)
            byte[] sendData = request.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, dirIp, dirUDPPort);
            socket.send(sendPacket);
            System.out.println("Enviado pedido para o Serviço de Diretoria em " + dirIp.getHostAddress() + ":" + dirUDPPort);

            // Fica à espera da resposta
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);

            responseStr = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Resposta da Diretoria: " + responseStr);

            // Tratamento da resposta
            String[] parts = responseStr.split(";");

            if (parts.length >= 3 && parts[0].equals("OK")) {
                String serverIp = parts[1];
                String serverTcpPort = parts[2];
                return new String[]{serverIp, serverTcpPort};
            } else {
                System.err.println("Erro na resposta da Diretoria: " + responseStr);
                return null;
            }

        } catch (SocketTimeoutException e) {
            System.err.println("Timeout atingido. Servidor de diretoria não respondeu a tempo.");
            return null;
        } catch (Exception e) {
            System.err.println("Erro na comunicação com o Serviço de Diretoria: " + e.getMessage());
            return null;
        }
    }

}
