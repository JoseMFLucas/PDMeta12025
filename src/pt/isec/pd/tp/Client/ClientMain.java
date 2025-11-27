package pt.isec.pd.tp.Client;

import pt.isec.pd.tp.Client.Comunicacao.ClientComunicacao;
import pt.isec.pd.tp.Client.Vista.ClientVista;
import pt.isec.pd.tp.Utils.Mensagem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

public class ClientMain {

    private final ClientVista vista;
    private boolean autenticated = false;
    private boolean closing = false;
    private Socket principalSocket; // Armazena a ligação TCP principal

    private ObjectOutputStream out;
    private ObjectInputStream in;

    Client user;

    String email, password, numero, nome, codigo, opcaocorreta, datainicio, datafim;

    String[] info;

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

    public void loopInicial() throws IOException {

        while (!autenticated && !closing) {

            vista.menuPrincipal();
            ClientVista.ClientInput input = vista.lerInputGeral();

            // Switch Case dos Comandos Iniciais
            switch (input.inputInt) {
                case 1: // Login
                    vista.mostrarInfo("Introduza o email e a password para iniciar o login.");
                    email = vista.lerStringObrigatoria("Email: ");
                    password = vista.lerStringObrigatoria("Password: ");
                    vista.mostrarInfo("Iniciar o login com email: " + email + " e password: " + password);
                    try {
                        user = new Client(email, password, null);
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
                            closeResources();
                            closing = true;
                        }
                    } catch(IOException | ClassNotFoundException e) {
                        vista.mostrarErro("Erro durante o login: " + e.getMessage());
                    }
                    break;
                case 2: // Registar
                    vista.menuRegisto();
                    ClientVista.ClientInput inputregistar = vista.lerInputGeral();
                    switch (inputregistar.inputInt){
                        case 1:
                            vista.mostrarInfo("Introduza nome, e-mail, password e o código único para completar o registo.");
                            nome = vista.lerStringObrigatoria("Nome: ");
                            email = vista.lerStringObrigatoria("Email: ");
                            password = vista.lerStringObrigatoria("Password: ");
                            codigo = vista.lerStringObrigatoria("Código Único: ");

                            info = new String[]{nome, email, password, codigo};
                            try {
                                Mensagem msg = new Mensagem(Mensagem.Tipo.REGISTO_DOCENTE, info);
                                out.writeObject(msg);
                                out.flush();

                                Mensagem response = (Mensagem) in.readObject();
                                if (response.getTipo() == Mensagem.Tipo.REGISTO_SUCESSO) {
                                    vista.mostrarInfo("Registo bem-sucedido!");
                                    vista.mostrarInfo("Efetue o registo!");
                                } else if(response.getTipo() == Mensagem.Tipo.REGISTO_FALHOU) {
                                    vista.mostrarErro("Registo falhou. O número de estudante ou o email já estão registados.");
                                    closeResources();
                                    closing = true;
                                } else {
                                    vista.mostrarErro("Registo falhou. Introduziu mal os dados.");
                                    closeResources();
                                    closing = true;
                                }
                            } catch(IOException | ClassNotFoundException e) {
                                vista.mostrarErro("Erro durante o registo: " + e.getMessage());
                            }
                            break;
                        case 2:
                            vista.mostrarInfo("Introduza número de estudante, nome, e-mail, password para completar o registo.");
                            numero = vista.lerStringObrigatoria("Numero: ");
                            nome = vista.lerStringObrigatoria("Nome: ");
                            email = vista.lerStringObrigatoria("Email: ");
                            password = vista.lerStringObrigatoria("Password: ");

                            info = new String[]{numero, nome, email, password};

                            try {
                                Mensagem msg = new Mensagem(Mensagem.Tipo.REGISTO_ESTUDANTE, info);
                                out.writeObject(msg);
                                out.flush();

                                Mensagem response = (Mensagem) in.readObject();
                                if (response.getTipo() == Mensagem.Tipo.REGISTO_SUCESSO) {
                                    vista.mostrarInfo("Registo bem-sucedido!");

                                    response = (Mensagem) in.readObject();
                                    if (response.getTipo() == Mensagem.Tipo.LOGIN_SUCESSO) {

                                        user = new Client(email, password, Client.Tipo.valueOf((String) response.getPayload()));

                                        System.out.println("Tipo de utilizador:" + user.getTipo());
                                        autenticated = true;
                                    }
                                } else if(response.getTipo() == Mensagem.Tipo.REGISTO_FALHOU) {
                                    vista.mostrarErro("Registo falhou. O número de estudante ou o email já estão registados.");
                                    closeResources();
                                    closing = true;
                                } else {
                                    vista.mostrarErro("Registo falhou. Introduziu mal os dados.");
                                    closeResources();
                                    closing = true;
                                }
                            } catch(IOException | ClassNotFoundException e) {
                                vista.mostrarErro("Erro durante o registo: " + e.getMessage());
                            }
                            break;
                    }
                    break;
                case 0: // Sair do programa
                    vista.mostrarInfo("A encerrar Cliente.");
                    Mensagem msg = new Mensagem(Mensagem.Tipo.EXIT, null);
                    out.writeObject(msg);
                    out.flush();
                    closeResources();
                    closing = true;
                    break;
                default:
                    vista.mostrarAviso("Opção de menu inválida. Tente um número válido.");
                    break;
            }
        }

        while (autenticated && !closing) {
            if(user.getTipo() == Client.Tipo.DOCENTE){
                vista.menuDocente();
                ClientVista.ClientInput inputregistar = vista.lerInputGeral();
                switch (inputregistar.inputInt){
                    case 1:
                        vista.mostrarInfo("Introduza o número de opções, as opções, a opção correta e o período de disponibilidade (data/hora de início e de fim)");
                        numero = vista.lerStringObrigatoria("Numero de opções: ");
                        String[] opcoes = new String[Integer.parseInt(numero)];
                        for(int i = 1; i < Integer.parseInt(numero) + 1; i++){
                            opcoes[i-1] = vista.lerStringObrigatoria("Opção " + i + ": ");
                        }
                        opcaocorreta = vista.lerStringObrigatoria("Opção Correta: ");
                        datainicio = vista.lerStringObrigatoria("Período de disponibilidade (data/hora de início): ");
                        datafim = vista.lerStringObrigatoria("Período de disponibilidade (data/hora de fim): ");

                        info = new String[]{numero, Arrays.toString(opcoes), opcaocorreta, datainicio, datafim};

                        try {
                            Mensagem msg = new Mensagem(Mensagem.Tipo.CRIAR_PERGUNTA, info);
                            out.writeObject(msg);
                            out.flush();

                            Mensagem response = (Mensagem) in.readObject();
                            if (response.getTipo() == Mensagem.Tipo.OPERACAO_SUCESSO) {
                                vista.mostrarInfo("Pergunta adicionada com sucesso! (ID PERGUNTA: " + response.getPayload());
                            } else {
                                vista.mostrarErro("Erro a adicionar pergunta.");
                            }
                        } catch(IOException | ClassNotFoundException e) {
                            vista.mostrarErro("Erro durante o envio da pergunta: " + e.getMessage());
                        }
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                    case 4:
                        break;
                    case 5:
                        break;
                    case 0:
                        vista.mostrarInfo("Logout.");
                        try {
                            Mensagem msg = new Mensagem(Mensagem.Tipo.LOGOUT, info);
                            out.writeObject(msg);
                            out.flush();

                            Mensagem response = (Mensagem) in.readObject();
                            if (response.getTipo() == Mensagem.Tipo.OPERACAO_SUCESSO) {
                                vista.mostrarInfo("O Cliente deu logout com sucesso!");
                            } else {
                                vista.mostrarErro("Erro a dar logout.");
                            }
                        } catch(IOException | ClassNotFoundException e) {
                            vista.mostrarErro("Erro durante o envio da mensagem: " + e.getMessage());
                        }
                        autenticated = false;
                        loopInicial();
                        break;
                }
            }else if(user.getTipo() == Client.Tipo.ESTUDANTE){
                vista.menuEstudante();
                ClientVista.ClientInput inputregistar = vista.lerInputGeral();
                switch (inputregistar.inputInt){
                    case 1:
                        break;
                    case 2:
                        break;
                    case 0:
                        vista.mostrarInfo("Logout.");
                        try {
                            Mensagem msg = new Mensagem(Mensagem.Tipo.LOGOUT, info);
                            out.writeObject(msg);
                            out.flush();

                            Mensagem response = (Mensagem) in.readObject();
                            if (response.getTipo() == Mensagem.Tipo.OPERACAO_SUCESSO) {
                                vista.mostrarInfo("O Cliente deu logout com sucesso!");
                            } else {
                                vista.mostrarErro("Erro a dar logout.");
                            }
                        } catch(IOException | ClassNotFoundException e) {
                            vista.mostrarErro("Erro durante o envio da mensagem: " + e.getMessage());
                        }
                        autenticated = false;
                        loopInicial();
                        break;
                }
            }else{
                closeResources();
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