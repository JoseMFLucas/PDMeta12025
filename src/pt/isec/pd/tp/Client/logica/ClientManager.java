package pt.isec.pd.tp.Client.logica;

import pt.isec.pd.tp.Client.Client;
import pt.isec.pd.tp.Utils.Mensagem;
import pt.isec.pd.tp.Utils.Pergunta;
import pt.isec.pd.tp.Utils.Resposta;

import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClientManager {
    public static final String PROP_STATE = "state";
    public static final String PROP_MSG = "message";
    public static final String PROP_CLIENTES = "clientes";
    public static final String PROP_PERGUNTAS = "perguntas";
    public static final String PROP_RESPOSTAS = "respostas";
    public static final String PROP_PERGUNTA_ENCONTRADA = "pergunta_encontrada";
    public static final String PROP_PERGUNTA_ENCONTRADA_ESTUDANTE = "pergunta_encontrada_estudante";
    public static final String PROP_PERGUNTA_PARA_EDITAR = "pergunta_para_editar";
    public static final String PROP_ESTATISTICAS_PERGUNTA = "estatisticas_pergunta";
    public static final String PROP_CLOSE_APP = "close_app";
    public static final String PROP_MSG_SUCESSO = "operacao_sucesso";
    public static final String PROP_MSG_ERRO = "operacao_erro";

    private ClientState state;
    private final PropertyChangeSupport pcs;
    private Client user;
    private List<String> clientes;
    private List<Pergunta> perguntas;
    private List<Resposta> respostas;
    private Pergunta perguntaAtiva;
    private Pergunta perguntaParaEditar;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread listenerThread;
    private String serverIp;
    private int serverPort;
    private volatile boolean loggingOut = false;

    public ClientManager() {
        this.pcs = new PropertyChangeSupport(this);
        this.state = ClientState.LOGIN;
        this.clientes = new ArrayList<>();
        this.perguntas = new ArrayList<>();
        this.respostas = new ArrayList<>();
    }

    public void start(String ip, int port) {
        this.serverIp = ip;
        this.serverPort = port;
        this.loggingOut = false;
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
                    if (loggingOut) {
                        System.out.println("Conexão fechada para logout.");
                    } else {
                        System.err.println("Ligação perdida: " + e.getMessage());
                        pcs.firePropertyChange(PROP_CLOSE_APP, false, true);
                    }
                }
            });
            listenerThread.start();
        } catch (IOException e) {
            pcs.firePropertyChange(PROP_MSG, null, "Erro de conexão: " + e.getMessage());
            pcs.firePropertyChange(PROP_CLOSE_APP, false, true);
        }
    }

    private void processarMensagem(Mensagem msg) {
        switch (msg.getTipo()) {
            case LOGIN_SUCESSO:
                this.user = (Client) msg.getPayload();
                setState(user.getTipo() == Client.Tipo.DOCENTE ? ClientState.DOCENTE_HOME : ClientState.ESTUDANTE_HOME);
                break;
            case REGISTO_SUCESSO:
                login(user.getEmail(), user.getPassword());
                break;
            case LISTA_PERGUNTAS:
                if (msg.getPayload() instanceof List) {
                    List<?> rawList = (List<?>) msg.getPayload();
                    List<Pergunta> newPerguntas = new ArrayList<>();
                    for (Object item : rawList) {
                        if (item instanceof String[]) {
                            String[] data = (String[]) item;
                            Pergunta p = new Pergunta();
                            try {
                                p.setId(Integer.parseInt(data[0]));
                                p.setEnunciado(data[1]);
                                p.setCodigo(data[2]);
                                p.setDataInicio(data[3]);
                                p.setDataFim(data[4]);

                                if (data.length >= 7) {
                                    p.setTotalRespostas(Integer.parseInt(data[5]));

                                    String percentString = data[6];
                                    percentString = percentString.replace("%", "");
                                    percentString = percentString.replace(",", ".");

                                    p.setPercentagemCertas(Double.parseDouble(percentString));
                                }
                                newPerguntas.add(p);
                            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                                System.err.println("Error parsing pergunta data from server: " + e.getMessage());
                            }
                        }
                    }
                    List<Pergunta> oldPerguntas = this.perguntas;
                    this.perguntas = newPerguntas;
                    pcs.firePropertyChange(PROP_PERGUNTAS, oldPerguntas, this.perguntas);
                }
                break;
            case LISTA_PERGUNTAS_RESPONDIDAS:
                if (msg.getPayload() instanceof List) {
                    List<?> rawList = (List<?>) msg.getPayload();
                    List<Resposta> newRespostas = new ArrayList<>();
                    for (Object item : rawList) {
                        if (item instanceof String[]) {
                            String[] data = (String[]) item;
                            try {
                                newRespostas.add(new Resposta(data[0], data[1], data[2], data[3], data[4]));
                            } catch (ArrayIndexOutOfBoundsException e) {
                                System.err.println("Erro a adicionar respostas: " + e.getMessage());
                            }
                        }
                    }
                    List<Resposta> oldRespostas = this.respostas;
                    this.respostas = newRespostas;
                    setState(ClientState.LISTA_PERGUNTAS_RESPONDIDAS);
                    pcs.firePropertyChange(PROP_RESPOSTAS, oldRespostas, this.respostas);
                }
                break;
            case DETALHES_PERGUNTA:
                String[] detalhes = (String[]) msg.getPayload();

                String opcoesString = detalhes[3].replaceAll("[\\[\\]]", "").trim();
                String[] opcoes = opcoesString.isEmpty() ? new String[0] : opcoesString.split(",\\s*");

                Pergunta pergunta = new Pergunta(detalhes[2], opcoes, Integer.parseInt(detalhes[6]));
                pergunta.setId(Integer.parseInt(detalhes[0]));
                pergunta.setIdDocente(Integer.parseInt(detalhes[1]));
                pergunta.setDataInicio(detalhes[4]);
                pergunta.setDataFim(detalhes[5]);
                pergunta.setCodigo(detalhes[7]);

                Pergunta oldPergunta = this.perguntaAtiva;
                this.perguntaAtiva = pergunta;

                setState(ClientState.DETALHES_PERGUNTA);
                pcs.firePropertyChange(PROP_PERGUNTA_ENCONTRADA, oldPergunta, this.perguntaAtiva);
                break;
            case DETALHES_PERGUNTA_ESTUDANTE:
                String[] perguntaEstudante = (String[]) msg.getPayload();

                String opcoesEstudanteString = perguntaEstudante[2].replaceAll("[\\[\\]]", "").trim();
                String[] opcoesEstudante = opcoesEstudanteString.isEmpty() ? new String[0] : opcoesEstudanteString.split(",\\s*");

                // The student does not receive the correct answer, so we pass -1
                Pergunta perguntaEstudanteObj = new Pergunta(perguntaEstudante[1], opcoesEstudante, -1);
                perguntaEstudanteObj.setId(Integer.parseInt(perguntaEstudante[0]));

                Pergunta oldPerguntaEstudante = this.perguntaAtiva;
                this.perguntaAtiva = perguntaEstudanteObj;

                pcs.firePropertyChange(PROP_PERGUNTA_ENCONTRADA_ESTUDANTE, oldPerguntaEstudante, this.perguntaAtiva);
                break;
            case ESTATISTICAS_PERGUNTA:
                List<String[]> estatisticas = (List<String[]>) msg.getPayload();
                pcs.firePropertyChange(PROP_ESTATISTICAS_PERGUNTA, null, estatisticas);
                break;
            case OPERACAO_SUCESSO:
                pcs.firePropertyChange(PROP_MSG_SUCESSO, null, "Operação realizada com sucesso!");
                setState(user.getTipo() == Client.Tipo.DOCENTE ? ClientState.DOCENTE_HOME : ClientState.ESTUDANTE_HOME);
                break;
            case ERRO:
            case OPERACAO_FALHOU:
                pcs.firePropertyChange(PROP_MSG_ERRO, null, "Erro: " + msg.getPayload());
                break;
        }
    }

    public void login(String email, String password) {
        if (socket == null || socket.isClosed()) {
            if (serverIp != null && serverPort > 0) {
                start(serverIp, serverPort);
            } else {
                pcs.firePropertyChange(PROP_MSG_ERRO, null, "Dados de conexão não encontrados. Reinicie a aplicação.");
                return;
            }
        }
        Client client = new Client(email, password, null);
        enviarMensagem(new Mensagem(Mensagem.Tipo.LOGIN, client));
    }

    public void registar(String tipoC, String[] info) {
        Mensagem.Tipo tipo = tipoC.equalsIgnoreCase(Client.Tipo.DOCENTE.toString()) ? Mensagem.Tipo.REGISTO_DOCENTE : Mensagem.Tipo.REGISTO_ESTUDANTE;
        System.out.println(Arrays.toString(info));
        if(tipo == Mensagem.Tipo.REGISTO_DOCENTE)
            user = new Client(info[1], info[2], null);
        else
            user = new Client(info[2], info[3], null);

        enviarMensagem(new Mensagem(tipo, info));
    }

    public void criarPergunta(String enunciado, List<String> opcoes, int opcaoCorreta, String datainicio, String datafim) {
        String[] info = new String[]{String.valueOf(user.getId()), enunciado, opcoes.toString(), datainicio, datafim, String.valueOf(opcaoCorreta)};
        enviarMensagem(new Mensagem(Mensagem.Tipo.CRIAR_PERGUNTA, info));
    }

    public void editarPergunta(int id, String enunciado, List<String> opcoes, int opcaoCorreta, String datainicio, String datafim) {
        String[] info = new String[]{String.valueOf(id), String.valueOf(user.getId()), enunciado, opcoes.toString(), datainicio, datafim, String.valueOf(opcaoCorreta)};
        enviarMensagem(new Mensagem(Mensagem.Tipo.EDITAR_PERGUNTA, info));
    }

    public void eliminarPergunta(int id){
        enviarMensagem(new Mensagem(Mensagem.Tipo.ELIMINAR_PERGUNTA, id));
    }

    public void verEstatisticas(int idPergunta) {
        enviarMensagem(new Mensagem(Mensagem.Tipo.VER_ESTATISTICAS, idPergunta));
    }

    public void getPerguntas() {
        if (user != null) {
            String[] info = new String[]{String.valueOf(user.getId()), "todas"};
            enviarMensagem(new Mensagem(Mensagem.Tipo.LISTAR_PERGUNTAS_DOCENTE, info));
        }
    }

    public void getRespostas() {
        if (user != null) {
            enviarMensagem(new Mensagem(Mensagem.Tipo.LISTAR_PERGUNTAS_RESPONDIDAS, user.getId()));
        }
    }

    public void getEstatisticasPerguntas() {
        if (user != null) {
            String[] info = new String[]{String.valueOf(user.getId()), "expiradas"};
            enviarMensagem(new Mensagem(Mensagem.Tipo.LISTAR_PERGUNTAS_DOCENTE, info));
        }
    }

    public void procurarPergunta(String codigo) {
        enviarMensagem(new Mensagem(Mensagem.Tipo.VISUALIZAR_PERGUNTA, Integer.parseInt(codigo)));
    }

    public void procurarPerguntaEstudante(String codigo) {
        enviarMensagem(new Mensagem(Mensagem.Tipo.OBTER_PERGUNTA, codigo));
    }

    public void submeterResposta(int idPergunta, int idOpcao) {
        if (user != null) {
            String[] payload = {String.valueOf(user.getId()), String.valueOf(idPergunta), String.valueOf(idOpcao)};
            enviarMensagem(new Mensagem(Mensagem.Tipo.SUBMETER_RESPOSTA, payload));
        }
    }

    public void editarPerfilDocente(String campo, String novoValor, String codigo){
        String[] info = new String[]{String.valueOf(user.getId()), campo, novoValor, codigo};

        if(campo.equals("nome"))
            user.setNome(novoValor);
        else if(campo.equals("email"))
            user.setEmail(novoValor);
        else if(campo.equals("password"))
            user.setPassword(novoValor);

        enviarMensagem(new Mensagem(Mensagem.Tipo.EDITAR_PERFIL_DOCENTE, info));
    }

    public void editarPerfilEstudante(String campo, String novoValor){
        String[] info = new String[]{String.valueOf(user.getId()), campo, novoValor};

        if(campo.equals("numero_estudante")){
            user.setId(Integer.parseInt(novoValor));
        }else if(campo.equals("nome"))
            user.setNome(novoValor);
        else if(campo.equals("email"))
            user.setEmail(novoValor);
        else if(campo.equals("password"))
            user.setPassword(novoValor);

        enviarMensagem(new Mensagem(Mensagem.Tipo.EDITAR_PERFIL_ESTUDANTE, info));
    }

    public void logout() {
        loggingOut = true;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Erro ao fechar a conexão durante o logout: " + e.getMessage());
        } finally {
            user = null;
            socket = null;
            in = null;
            out = null;
            listenerThread = null;
            setState(ClientState.LOGIN);
        }
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
    public List<Resposta> getRespostasList() { return respostas; }
    public Pergunta getPerguntaAtiva() { return perguntaAtiva; }
    public Pergunta getPerguntaParaEditar() { return perguntaParaEditar; }

    public void setPerguntaParaEditar(Pergunta pergunta) {
        Pergunta old = this.perguntaParaEditar;
        this.perguntaParaEditar = pergunta;
        pcs.firePropertyChange(PROP_PERGUNTA_PARA_EDITAR, old, this.perguntaParaEditar);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }
}
