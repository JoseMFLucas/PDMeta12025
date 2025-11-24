package pt.isec.pd.tp.Client.Comunicacao;

import pt.isec.pd.tp.Client.Vista.ClientVista;
import pt.isec.pd.tp.ServerDiretoria.ServerInfo;
import pt.isec.pd.tp.Utils.MessageCodes;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class ClientComunicacao {

    private Boolean closing = false;
    private Socket socketLigacao;
    private final boolean logged = false;

    ClientVista view;

    private void TrataTeclado() {
        System.out.println("Thread de teclado iniciada. Prima ENTER para listar servidores ativos.");
        Scanner sc = new Scanner(System.in);

        try {
            while (!closing) {

                view.menuprincipal();

                /*
                switch(sc.nextLine().toLowerCase()) {
                    case "0":
                        System.out.println("A sair...");
                        closing = true;
                        try (DatagramSocket socket = new DatagramSocket()) {
                            byte[] buffer = MessageCodes.CLOSE_CONNECTION.getBytes();

                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("localhost"), udpPort);
                            socket.send(packet);
                        } catch (Exception e) {
                            System.out.println("Erro ao notificar servidores sobre encerramento: " + e.getMessage());
                        }
                        if(socketLigacao != null && !socketLigacao.isClosed())
                            socketLigacao.close();

                        break;
                    case "1":
                        System.out.println("Login:");

                        break;
                    case "2":
                        System.out.println("Servidores ativos:");
                        synchronized (activeServers) {
                            for (ServerInfo server : activeServers) {
                                System.out.println(" - " + server.getIp() + " (Clientes TCP: " + server.getTcpPortClientes() + ", BD TCP: " + server.getTcpPortDb() + ")");
                            }
                        }
                        break;
                    default:
                        System.out.println("Comando desconhecido. Prima ENTER para listar servidores ativos ou 'exit' para sair.");
                        break;
                }


                 */
            }
        } catch (Exception e) {
            System.out.println("Erro ao listar servidores ativos ou fechar servers em segurança: " + e.getMessage());
        }

        System.out.println("Thread de teclado terminada.");
    }
}
