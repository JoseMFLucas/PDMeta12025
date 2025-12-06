package pt.isec.pd.tp.Client.logica;

import pt.isec.pd.tp.Client.Client;
import pt.isec.pd.tp.Utils.Mensagem;
import pt.isec.pd.tp.Utils.Pergunta;

import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientManager {
    public static final String PROP_STATE = "state";
    public static final String PROP_MSG = "message";
    public static final String PROP_CLIENTES = "clientes";
    public static final String PROP_PERGUNTAS = "perguntas";
    public static final String PROP_PERGUNTA_ENCONTRADA = "pergunta_encontrada";
    public static final String PROP_CLOSE_APP = "close_app"; // New property

    private ClientState state;
    private final PropertyChangeSupport pcs;
    private Client user;
    private List<String> clientes;
    private List<Pergunta> perguntas;
    private Pergunta perguntaAtiva;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread listenerThread;

    public ClientManager() {
        this.pcs = new PropertyChangeSupport(this);
        this.state = ClientState.LOGIN;
        this.clientes = new ArrayList<>();
        this.perguntas = new ArrayList<>();
    }

    public void start(String ip, int port) {
        try {
            this.socket = new Socket(ip, port);
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());

            listenerThread = new Thread(() -> {
                try {
                    while (!socket.isClosed()) {
                        Mensagem msg = (Mensagem) in.readObject();
                        processarMensagem(msg);
                    }
                } catch (Exception e) {
                    System.err.println("Ligação perdida: " + e.getMessage());
                    // Use the existing pcs object to fire the close event
                    pcs.firePropertyChange(PROP_CLOSE_APP, false, true);
                }
            });
            listenerThread.start();
        } catch (IOException e) {
            pcs.firePropertyChange(PROP_MSG, null, "Erro de conexão: " + e.getMessage());
            pcs.firePropertyChange(PROP_CLOSE_APP, false, true); // Also close if connection fails on start
        }
    }

    private void processarMensagem(Mensagem msg) {
        switch (msg.getTipo()) {
            case LOGIN_SUCESSO:
                this.user = (Client) msg.getPayload();
                setState(user.getTipo() == Client.Tipo.DOCENTE ? ClientState.DOCENTE_HOME : ClientState.ESTUDANTE_HOME);
                break;
            case REGISTO_SUCESSO:
                pcs.firePropertyChange(PROP_MSG, null, "Registo efetuado! Faça login.");
                setState(ClientState.LOGIN);
                break;
            case LISTA_CLIENTES:
                pcs.firePropertyChange(PROP_CLIENTES, this.clientes, msg.getPayload());
                this.clientes = (List<String>) msg.getPayload();
                break;
            case LISTA_PERGUNTAS:
                pcs.firePropertyChange(PROP_PERGUNTAS, this.perguntas, msg.getPayload());
                this.perguntas = (List<Pergunta>) msg.getPayload();
                break;
            case DETALHES_PERGUNTA:
                pcs.firePropertyChange(PROP_PERGUNTA_ENCONTRADA, this.perguntaAtiva, msg.getPayload());
                this.perguntaAtiva = (Pergunta) msg.getPayload();
                break;
            case OPERACAO_SUCESSO:
                pcs.firePropertyChange(PROP_MSG, null, "Operação realizada com sucesso!");
                break;
            case ERRO:
            case OPERACAO_FALHOU:
                pcs.firePropertyChange(PROP_MSG, null, "Erro: " + msg.getPayload());
                break;
        }
    }

    public void login(String email, String password) {
        Client client = new Client(email, password, null);
        enviarMensagem(new Mensagem(Mensagem.Tipo.LOGIN, client));
    }

    public void registar(String tipoC, String[] info) {
        Mensagem.Tipo tipo = tipoC.equalsIgnoreCase(Client.Tipo.DOCENTE.toString()) ? Mensagem.Tipo.REGISTO_DOCENTE : Mensagem.Tipo.REGISTO_ESTUDANTE;
        enviarMensagem(new Mensagem(tipo, info));
    }

    public void criarPergunta(String enunciado, List<String> opcoes, int opcaoCorreta) {
        Pergunta p = new Pergunta(enunciado, opcoes.toArray(new String[0]), opcaoCorreta);
        p.setIdDocente(user.getId());
        enviarMensagem(new Mensagem(Mensagem.Tipo.CRIAR_PERGUNTA, p));
    }

    public void getPerguntas() {
        if (user != null) {
            enviarMensagem(new Mensagem(Mensagem.Tipo.LISTAR_PERGUNTAS_DOCENTE, user.getId()));
        }
    }

    public void procurarPergunta(String codigo) {
        enviarMensagem(new Mensagem(Mensagem.Tipo.OBTER_PERGUNTA, codigo));
    }

    public void submeterResposta(int idPergunta, int idOpcao) {
        if (user != null) {
            int[] payload = {user.getId(), idPergunta, idOpcao};
            enviarMensagem(new Mensagem(Mensagem.Tipo.SUBMETER_RESPOSTA, payload));
        }
    }

    public void getClientes() {
        enviarMensagem(new Mensagem(Mensagem.Tipo.PEDIR_LISTA_CLIENTES, null));
    }

    public void logout() {
        // Fire the close event on logout
        pcs.firePropertyChange(PROP_CLOSE_APP, false, true);
    }

    private void enviarMensagem(Mensagem m) {
        try {
            out.writeObject(m);
            out.flush();
        } catch (IOException e) {
            System.out.println("Erro ao enviar mensagem: " + e.getMessage());
        }
    }

    public void setState(ClientState novoEstado) {
        ClientState old = this.state;
        this.state = novoEstado;
        pcs.firePropertyChange(PROP_STATE, old, this.state);
    }

    public ClientState getState() { return state; }
    public Client getUser() { return user; }
    public List<String> getClientesList() { return clientes; }
    public List<Pergunta> getPerguntasList() { return perguntas; }
    public Pergunta getPerguntaAtiva() { return perguntaAtiva; }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }
}
