package pt.isec.pd.tp.Server.logica;

import pt.isec.pd.tp.Server.ServidorMain;
import pt.isec.pd.tp.Server.comunicacao.ClienteHandler;
import pt.isec.pd.tp.Server.dados.DBManager;
import pt.isec.pd.tp.Client.Client;
import pt.isec.pd.tp.Utils.Mensagem;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// Responsável pela Lógica de Negócio
public class ServidorLogica {
    private final ServidorMain servidorMain;

    private final DBManager dbManager;

    // Lista de clientes conectados para enviar notificações
    private final Set<ClienteHandler> activeClients = Collections.synchronizedSet(new HashSet<>());

    public ServidorLogica(ServidorMain servidorMain, DBManager dbManager) {

        this.servidorMain = servidorMain;
        this.dbManager = dbManager;

    }

    public boolean isServidorPrincipal() {
        return servidorMain.isPrincipal();
    }

    // TODO: Implementar métodos para processar pedidos dos clientes
    // Estes métodos serão chamados pelo ClienteHandler

    public Mensagem processarLoginRegisto(Mensagem msg) {

        if (msg.getTipo() == Mensagem.Tipo.LOGIN) {

            Client clientToAuthenticate = (Client) msg.getPayload();

            String tipoUser = dbManager.login(clientToAuthenticate);

            if(tipoUser != null){
                int idUser = dbManager.getUserId(clientToAuthenticate.getEmail(), tipoUser);

                clientToAuthenticate.setTipo(Client.Tipo.valueOf(tipoUser));
                clientToAuthenticate.setId(idUser);

                System.out.println("Login bem-sucedido. ID: " + clientToAuthenticate.getId() + " Tipo: " + clientToAuthenticate.getTipo());

                return new Mensagem(Mensagem.Tipo.LOGIN_SUCESSO, clientToAuthenticate);
            } else {
                System.out.println("Login falhou");
                return new Mensagem(Mensagem.Tipo.LOGIN_FALHOU, null);
            }
        }
        if(msg.getTipo() == Mensagem.Tipo.REGISTO_ESTUDANTE){
            if (!(msg.getPayload() instanceof String[])) {
                return new Mensagem(Mensagem.Tipo.REGISTO_FALHOU, "Payload de registo inválido.");
            }
            if(dbManager.registarEstudante((String []) msg.getPayload())){
                return new Mensagem(Mensagem.Tipo.REGISTO_SUCESSO, null);
            } else{
                System.out.println("Registo falhou");
                return new Mensagem(Mensagem.Tipo.REGISTO_FALHOU, null);
            }
        }

        if(msg.getTipo() == Mensagem.Tipo.REGISTO_DOCENTE){
            if (!(msg.getPayload() instanceof String[])) {
                return new Mensagem(Mensagem.Tipo.REGISTO_FALHOU, "Payload de registo inválido.");
            }

            if(dbManager.registarDocente((String []) msg.getPayload())){
                return new Mensagem(Mensagem.Tipo.REGISTO_SUCESSO, null);
            } else{
                System.out.println("Registo falhou");
                return new Mensagem(Mensagem.Tipo.REGISTO_FALHOU, null);
            }
        }

        return new Mensagem(Mensagem.Tipo.EXIT, null);
    }

    public Mensagem processarMensagem(Mensagem msg) {
        if (!isServidorPrincipal()) {
            return new Mensagem(Mensagem.Tipo.ERRO, "Servidor não é o principal");
        }

        // TODO: VERIFICAR O TIPO DE UTILIZADOR
        // SEPARAR O TIPO DE MENSAGENS PARA CADA TIPO DE UTILIZADOR

        boolean houveAlteracaoBD = false;

        switch (msg.getTipo()) {
            // Casos de Escrita (alteram BD)
            case CRIAR_PERGUNTA:
                System.out.println("Criar pergunta");
                //houveAlteracaoBD = dbManager.criaPergunta(user, (String []) msg.getPayload());
                break;
            case EDITAR_PERGUNTA:
                // TODO: 1. Validar dados da pergunta
                // TODO: 2. Chamar dbManager.editarPergunta( (Pergunta) msg.getPayload() )
                // houveAlteracaoBD = dbManager.editarPergunta(...);

                /*if(dbManager.editarPergunta(msg)){
                    notificarClientes(new Mensagem(Mensagem.Tipo.OPERACAO_SUCESSO, null));
                }else{
                    return new Mensagem(Mensagem.Tipo.OPERACAO_FALHOU, null);
                }*/
                break;
            case ELIMINAR_PERGUNTA:
                // TODO: 1. Validar dados da pergunta
                // TODO: 2. Chamar dbManager.eliminarPergunta( (Pergunta) msg.getPayload() )
                // houveAlteracaoBD = dbManager.eliminarPergunta(...);

                if(dbManager.eliminarPergunta((String)msg.getPayload())){
                    notificarClientes(new Mensagem(Mensagem.Tipo.OPERACAO_SUCESSO, null));
                }else{
                    return new Mensagem(Mensagem.Tipo.OPERACAO_FALHOU, null);
                }
                break;
            case SUBMETER_RESPOSTA:
                // TODO: 1. Validar dados da pergunta
                // TODO: 2. Chamar dbManager.submeterResposta( (Pergunta) msg.getPayload() )
                // houveAlteracaoBD = dbManager.submeterResposta(...);
                break;

            // Casos de Leitura (não alteram BD)
            case LISTAR_PERGUNTAS_DOCENTE:
                // TODO: Chamar dbManager.listarPerguntasDocente(...)
                // return new Mensagem(Mensagem.Tipo.LISTA_PERGUNTAS, lista);
                break;
            case LISTAR_RESULTADOS_PERGUNTA:
                // TODO: Chamar dbManager.listarResultadosPergunta(...)
                // return new Mensagem(Mensagem.Tipo.LISTA_PERGUNTAS, lista);
                break;
            case LISTAR_PERGUNTAS_RESPONDIDAS:
                // TODO: Chamar dbManager.listarPerguntasRespondidas(...)
                // return new Mensagem(Mensagem.Tipo.LISTA_PERGUNTAS, lista);
                break;

           //Logout

            case LOGOUT:
                // TODO: LOGOUT CLIENTE
                break;
        }


        return new Mensagem(Mensagem.Tipo.OPERACAO_SUCESSO, null); // Exemplo
    }

    public Integer getVersaoBaseDados() {
        return dbManager.getVersaoDB();
    }

    public void addClienteHandler(ClienteHandler handler) {
        activeClients.add(handler);
    }

    public void removeClienteHandler(ClienteHandler handler) {
        activeClients.remove(handler);
    }

    public void processarMensagemMulticast(String mensagem) {
        if (mensagem == null)
            return;
        if(mensagem.split(";").length == 3){
            Integer versaoBD = Integer.parseInt(mensagem.split(";")[0]);
            Integer tcpPortClientes = Integer.parseInt(mensagem.split(";")[1]);
            Integer tcpPortDBSync = Integer.parseInt(mensagem.split(";")[2]);

            //servidorMain.atualizarInfoServidorPrincipal(versaoBD, tcpPortClientes, tcpPortDBSync);
        }
    }

    // Notifica todos os clientes conectados
    public void notificarClientes(Mensagem msg) {
        synchronized (activeClients) {
            for (ClienteHandler handler : activeClients) {
                handler.enviarNotificacao(msg);
            }
        }
    }

    // Notifica todos os clientes, exceto um
    public void notificarClientes(Mensagem msg, ClienteHandler excluir) {
        synchronized (activeClients) {
            for (ClienteHandler handler : activeClients) {
                if (handler != excluir) {
                    handler.enviarNotificacao(msg);
                }
            }
        }
    }
}