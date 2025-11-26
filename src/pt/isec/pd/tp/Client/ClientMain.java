package pt.isec.pd.tp.Client;

import pt.isec.pd.tp.Client.Comunicacao.ClientComunicacao;
import pt.isec.pd.tp.Client.Vista.ClientVista;
import pt.isec.pd.tp.Utils.Mensagem;
import pt.isec.pd.tp.Utils.Client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class ClientMain {

    private final ClientVista vista;
    private boolean autenticated = false;
    private boolean closing = false;
    private Socket principalSocket; // Armazena a ligação TCP principal

    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ClientMain(ClientVista vista, Socket principalSocket){
        this.vista = vista;
        this.principalSocket = principalSocket; // Recebe a socket do main

        try {
            this.out = new ObjectOutputStream(principalSocket.getOutputStream());
            this.in = new ObjectInputStream(principalSocket.getInputStream());
        } catch (IOException e) {
            vista.mostrarErro("Erro ao criar streams de comunicação: " + e.getMessage());
            closeResources();
        }

    }

    // Loop Inicial (Menu Principal)

    public void loopInicial() {

        while (!autenticated) {

            vista.menuPrincipal();
            ClientVista.ClientInput input = vista.lerInputGeral();

            // Switch Case dos Comandos Iniciais
            switch (input.inputInt) {
                case 1: // Login
                    vista.mostrarInfo("Introduza o email e a password para iniciar o login.");
                    String email = vista.lerStringObrigatoria("Email: ");
                    String password = vista.lerStringObrigatoria("Password: ");
                    vista.mostrarInfo("Iniciar o login com email: " + email + " e password: " + password);
                    try {
                        Client user = new Client(email, password, null);
                        Mensagem msg = new Mensagem(Mensagem.Tipo.LOGIN, user);
                        out.writeObject(msg);
                        out.flush();

                        Mensagem response = (Mensagem) in.readObject();
                        if (response.getTipo() == Mensagem.Tipo.LOGIN_SUCESSO) {
                            vista.mostrarInfo("Login bem-sucedido!");

                            user.setTipo(Client.Tipo.valueOf((String) response.getPayload()));
                            System.out.println("Tipo de utilizador:" + user.getTipo());
                            autenticated = true;
                        } else {
                            vista.mostrarErro("Login falhou. Credenciais inválidas.");
                        }
                    } catch(IOException | ClassNotFoundException e) {
                        vista.mostrarErro("Erro durante o login: " + e.getMessage());
                        closing = true;
                    }
                    autenticated = true;
                    break;
                case 2: // Registar
                    vista.mostrarInfo("Iniciando Registo. (Lógica a implementar)...");
                    break;
                case 0: // Sair do programa
                    vista.mostrarInfo("A encerrar Cliente por opção '0'.");
                    closing = true;
                    break;
                default:
                    vista.mostrarAviso("Opção de menu inválida. Tente 'exit' ou um número válido.");
                    break;
            }
        }

        // Finalização
        closeResources();
    }

    // Fecha o socket TCP e o scanner da Vista.

    private void closeResources() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (principalSocket != null && !principalSocket.isClosed()) {
                principalSocket.close();
                vista.mostrarInfo("Ligação TCP com o servidor fechada.");
            }
        } catch (IOException e) {
            vista.mostrarErro("Erro ao fechar a ligação TCP: " + e.getMessage());
        }
        vista.fecharScanner();
    }

    // Função principal do cliente

    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("Uso: java ClientMain <IP_Diretoria> <Porto_UDP>");
            return;
        }

        // Cria o Socket e a Vista primeiro

        Socket principalSocket = null;
        ClientVista vista = new ClientVista();

        try {
            InetAddress dirIp = InetAddress.getByName(args[0]);
            int dirPort = Integer.parseInt(args[1]);

            ClientComunicacao clcom = new ClientComunicacao(dirIp, dirPort);

            // Pede o Servidor Principal ao Servidor Diretoria
            String[] serverDetails = clcom.requestPrincipalServer();

            if (serverDetails != null) {
                String serverIp = serverDetails[0];
                int serverTcpPort = Integer.parseInt(serverDetails[1]);

                System.out.println("Servidor Principal encontrado: " + serverIp + ":" + serverTcpPort);

                // Ligação TCP ao Servidor Principal
                try {
                    principalSocket = new Socket(serverIp, serverTcpPort);
                    System.out.println("Ligação TCP estabelecida: " + serverIp + ":" + serverTcpPort);

                    // Inicia o Loop Principal do Cliente
                    ClientMain client = new ClientMain(vista, principalSocket);
                    client.loopInicial();

                } catch (Exception e) {
                    vista.mostrarErro("Erro ao ligar ao Servidor Principal via TCP: " + e.getMessage());
                }

            } else {
                System.out.println("Não foi possível obter um servidor principal. O cliente vai encerrar.");
            }

        } catch (UnknownHostException e) {
            vista.mostrarErro("O endereço IP/Host '" + args[0] + "' é inválido ou desconhecido.");
        } catch (NumberFormatException e) {
            vista.mostrarErro("O porto fornecido ('" + args[1] + "') não é um número válido.");
        } finally {
            // Fecha o Socket e o Scanner se estiverem abertos depois de haver um erro na função main
            if (principalSocket != null && !principalSocket.isClosed()) {
                try {
                    principalSocket.close();
                } catch (IOException ignored) {}
            }
            vista.fecharScanner();
        }
    }

}