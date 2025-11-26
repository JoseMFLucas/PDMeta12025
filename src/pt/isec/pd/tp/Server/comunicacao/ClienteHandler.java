package pt.isec.pd.tp.Server.comunicacao;

import pt.isec.pd.tp.Server.logica.ServidorLogica;
import pt.isec.pd.tp.Utils.Configs;
import pt.isec.pd.tp.Server.logica.ServidorLogica;
import pt.isec.pd.tp.Utils.Configs;
import pt.isec.pd.tp.Utils.Mensagem;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

// Gere a ligação TCP com um cliente específico
public class ClienteHandler implements Runnable {
    private Socket socket;
    private ServidorLogica logica;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private boolean autenticado = false;
    // TODO: Associar o User (Docente/Estudante) a este handler após login

    public ClienteHandler(Socket socket, ServidorLogica logica) {
        this.socket = socket;
        this.logica = logica;
    }

    @Override
    public void run() {
        try {
            // Definir timeout de autenticação
            socket.setSoTimeout(Configs.AUTH_TIMEOUT_MS);

            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());

            while (!socket.isClosed()) {
                Mensagem msg = (Mensagem) ois.readObject();

                // Se não for o servidor principal, rejeita a operação
                if (!logica.isServidorPrincipal()) {
                    System.err.println("Pedido recebido, mas não sou o principal. Rejeitando.");
                    oos.writeObject(new Mensagem(Mensagem.Tipo.ERRO, "Servidor não é o principal. Tente mais tarde."));
                    continue; // Idealmente o cliente seria redirecionado, mas o enunciado manda fechar
                }

                Mensagem resposta;

                if (!autenticado) {
                    if (msg.getTipo() == Mensagem.Tipo.LOGIN || msg.getTipo() == Mensagem.Tipo.REGISTO_DOCENTE
                            || msg.getTipo() == Mensagem.Tipo.REGISTO_ESTUDANTE) {

                        resposta = logica.processarLoginRegisto(msg);

                        if (resposta.getTipo() == Mensagem.Tipo.LOGIN_SUCESSO
                                || resposta.getTipo() == Mensagem.Tipo.REGISTO_SUCESSO) {
                            this.autenticado = true;
                            socket.setSoTimeout(0); // Remover timeout após autenticação
                            // TODO: Associar 'user' ao handler
                        }
                        oos.writeObject(resposta);
                    } else {
                        // Não autenticado e a tentar fazer outra coisa
                        oos.writeObject(new Mensagem(Mensagem.Tipo.ERRO, "Não autenticado"));
                        socket.close(); //
                        break;
                    }
                } else {
                    // Utilizador autenticado
                    if (msg.getTipo() == Mensagem.Tipo.LOGOUT) {
                        this.autenticado = false;
                        // TODO: Limpar dados do user
                        oos.writeObject(new Mensagem(Mensagem.Tipo.OPERACAO_SUCESSO, "Logout com sucesso"));
                        socket.setSoTimeout(Configs.AUTH_TIMEOUT_MS); // Repor timeout
                    } else {
                        // Processar outras mensagens
                        resposta = logica.processarMensagem(msg); // TODO: Implementar em ServidorLogica
                        oos.writeObject(resposta);
                    }
                }
            }
        } catch (SocketTimeoutException e) {
            System.err.println("Cliente falhou autenticação (timeout 30s) ou ficou inativo.");
        } catch (Exception e) {
            // Cliente desconectou-se
            System.err.println("ClienteHandler: " + e.getMessage());
        } finally {
            logica.removeClienteHandler(this); // Remove da lista de notificações
            try {
                if (socket != null) socket.close();
            } catch (Exception e) {}
        }
    }

    // Método para o ServidorLogica usar para enviar notificações
    public void enviarNotificacao(Mensagem msg) {
        try {
            if (oos != null && !socket.isClosed()) {
                // Enviar de forma síncrona para garantir a ordem
                synchronized (oos) {
                    oos.writeObject(msg);
                    oos.flush();
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao enviar notificação assíncrona: " + e.getMessage());
        }
    }
}