package pt.isec.pd.tp.Client;

import pt.isec.pd.tp.Client.Comunicacao.ClientComunicacao;
import pt.isec.pd.tp.Client.Comunicacao.ClientListener;
import pt.isec.pd.tp.Client.Vista.ClientVista;
import pt.isec.pd.tp.Utils.Mensagem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

public class ClientMain {

    private ClientListener listener;
    private Thread listenerThread;

    private final ClientVista vista;
    private boolean autenticated = false;
    private boolean closing = false;
    private Socket principalSocket; // Armazena a ligação TCP principal

    private ObjectOutputStream out;
    private ObjectInputStream in;

    Client user = null;

    String email, password, nome, enunciado, codigo, opcaocorreta, datainicio, datafim;

    int numero;

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
        // O listener é iniciado aqui para lidar com todas as respostas do servidor
        listener = new ClientListener(in, vista);
        listenerThread = new Thread(listener);
        listenerThread.start();

        while (!closing) {
            if (!autenticated) {
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

                            Mensagem response = listener.getResponse();

                            if (response != null && response.getTipo() == Mensagem.Tipo.LOGIN_SUCESSO) {
                                vista.mostrarInfo("Login bem-sucedido!");
                                Client autenticateduser = (Client) response.getPayload();
                                this.user = autenticateduser;
                                autenticated = true;
                            } else {
                                vista.mostrarErro("Login falhou. Credenciais inválidas ou tempo de resposta excedido.");
                            }
                        } catch (IOException | InterruptedException e) {
                            vista.mostrarErro("Erro durante o login: " + e.getMessage());
                        }
                        break;
                    case 2: // Registar
                        vista.menuRegisto();
                        ClientVista.ClientInput inputregistar = vista.lerInputGeral();
                        switch (inputregistar.inputInt) {
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

                                    Mensagem response = listener.getResponse();

                                    if (response != null && response.getTipo() == Mensagem.Tipo.REGISTO_SUCESSO) {
                                        vista.mostrarInfo("Registo bem-sucedido! A fazer login automático...");
                                        // Tenta fazer login após o registo
                                        user = new Client(email, password, null);
                                        msg = new Mensagem(Mensagem.Tipo.LOGIN, user);
                                        out.writeObject(msg);
                                        out.flush();

                                        response = listener.getResponse();

                                        System.out.println(response.getTipo());
                                        System.out.println(response.getPayload());

                                        if (response != null && response.getTipo() == Mensagem.Tipo.LOGIN_SUCESSO) {
                                            Client autenticateduser = (Client) response.getPayload();
                                            this.user = autenticateduser;
                                            autenticated = true;
                                        } else {
                                            vista.mostrarErro("Login automático falhou.");
                                        }
                                    } else {
                                        vista.mostrarErro("Registo falhou. Verifique os dados e tente novamente.");
                                    }
                                } catch (IOException | InterruptedException e) {
                                    vista.mostrarErro("Erro durante o registo: " + e.getMessage());
                                }
                                break;
                            case 2:
                                vista.mostrarInfo("Introduza número de estudante, nome, e-mail, password para completar o registo.");
                                numero = vista.lerIntObrigatoria("Numero: ");
                                nome = vista.lerStringObrigatoria("Nome: ");
                                email = vista.lerStringObrigatoria("Email: ");
                                password = vista.lerStringObrigatoria("Password: ");

                                info = new String[]{String.valueOf(numero), nome, email, password};

                                try {
                                    Mensagem msg = new Mensagem(Mensagem.Tipo.REGISTO_ESTUDANTE, info);
                                    out.writeObject(msg);
                                    out.flush();

                                    Mensagem response = listener.getResponse();

                                    if (response != null && response.getTipo() == Mensagem.Tipo.REGISTO_SUCESSO) {
                                        vista.mostrarInfo("Registo bem-sucedido! A fazer login automático...");
                                        user = new Client(email, password, null);
                                        msg = new Mensagem(Mensagem.Tipo.LOGIN, user);
                                        out.writeObject(msg);
                                        out.flush();

                                        response = listener.getResponse();
                                        if (response != null && response.getTipo() == Mensagem.Tipo.LOGIN_SUCESSO) {
                                            Client autenticateduser = (Client) response.getPayload();
                                            this.user = autenticateduser;
                                            autenticated = true;
                                        } else {
                                            vista.mostrarErro("Login automático falhou.");
                                        }
                                    } else {
                                        vista.mostrarErro("Registo falhou. Verifique os dados e tente novamente.");
                                    }
                                } catch (IOException | InterruptedException e) {
                                    vista.mostrarErro("Erro durante o registo: " + e.getMessage());
                                }
                                break;
                        }
                        break;
                    case 0: // Sair do programa
                        vista.mostrarInfo("A encerrar Cliente.");
                        try {
                            Mensagem msg = new Mensagem(Mensagem.Tipo.EXIT, null);
                            out.writeObject(msg);
                            out.flush();
                        } catch (IOException e) {
                            // Ignora o erro se o servidor já tiver fechado a ligação
                        }
                        closing = true;
                        break;
                    default:
                        vista.mostrarAviso("Opção de menu inválida. Tente um número válido.");
                        break;
                }
            } else {
                if (user.getTipo() == Client.Tipo.DOCENTE) {
                    vista.menuDocente();
                    ClientVista.ClientInput inputregistar = vista.lerInputGeral();
                    switch (inputregistar.inputInt) {
                        case 1:
                            vista.mostrarInfo("Introduza o enunciado, as opções, a opção correta e o período de disponibilidade (data/hora de início e de fim)");
                            enunciado = vista.lerStringObrigatoria("Enunciado: ");
                            numero = vista.lerIntObrigatoria("Numero de opções: ");
                            String[] opcoes = new String[numero];
                            for (int i = 1; i < numero + 1; i++) {
                                opcoes[i - 1] = vista.lerStringObrigatoria("Opção " + i + ": ");
                            }
                            opcaocorreta = vista.lerStringObrigatoria("Opção Correta: ");
                            datainicio = vista.lerStringObrigatoria("Período de disponibilidade (data/hora de início): ");
                            datafim = vista.lerStringObrigatoria("Período de disponibilidade (data/hora de fim): ");

                            info = new String[]{enunciado, Arrays.toString(opcoes), opcaocorreta, datainicio, datafim};

                            try {
                                Mensagem msg = new Mensagem(Mensagem.Tipo.CRIAR_PERGUNTA, info);
                                out.writeObject(msg);
                                out.flush();

                                Mensagem response = listener.getResponse();
                                if (response != null && response.getTipo() == Mensagem.Tipo.OPERACAO_SUCESSO) {
                                    vista.mostrarInfo("Pergunta adicionada com sucesso! (ID PERGUNTA: " + response.getPayload());
                                } else {
                                    vista.mostrarErro("Erro a adicionar pergunta.");
                                }
                            } catch (IOException | InterruptedException e) {
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
                                Mensagem msg = new Mensagem(Mensagem.Tipo.LOGOUT, null);
                                out.writeObject(msg);
                                out.flush();

                                Mensagem response = listener.getResponse();

                                System.out.println(response.getTipo() + " " + response.getPayload());

                                if (response != null && response.getTipo() == Mensagem.Tipo.OPERACAO_SUCESSO) {
                                    vista.mostrarInfo("O Cliente deu logout com sucesso!");
                                    autenticated = false; // Apenas muda o estado
                                } else {
                                    vista.mostrarErro("Erro a dar logout.");
                                }
                            } catch (IOException | InterruptedException e) {
                                vista.mostrarErro("Erro durante o logout: " + e.getMessage());
                            }
                            break;
                    }
                } else if (user.getTipo() == Client.Tipo.ESTUDANTE) {
                    vista.menuEstudante();
                    ClientVista.ClientInput inputregistar = vista.lerInputGeral();
                    switch (inputregistar.inputInt) {
                        case 1:
                            break;
                        case 2:
                            break;
                        case 0:
                            vista.mostrarInfo("Logout.");
                            try {
                                Mensagem msg = new Mensagem(Mensagem.Tipo.LOGOUT, user);
                                out.writeObject(msg);
                                out.flush();

                                Mensagem response = listener.getResponse();
                                if (response != null && response.getTipo() == Mensagem.Tipo.OPERACAO_SUCESSO) {
                                    vista.mostrarInfo("O Cliente deu logout com sucesso!");
                                    autenticated = false; // Apenas muda o estado
                                } else {
                                    vista.mostrarErro("Erro a dar logout.");
                                }
                            } catch (IOException | InterruptedException e) {
                                vista.mostrarErro("Erro durante o logout: " + e.getMessage());
                            }
                            break;
                    }
                } else {
                    closing = true;
                }
            }
        }

        // Finalização
        closeResources();
    }

    // Fecha o socket TCP e o scanner da Vista.

    private void closeResources() {

        if (listener != null) {
            listener.stopRunning();
            try {
                // Interrompe a thread para desbloquear o `poll` ou `put`
                if (listenerThread != null) {
                    listenerThread.interrupt();
                    listenerThread.join(100);
                }
            } catch (InterruptedException ignored) {}
        }

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
