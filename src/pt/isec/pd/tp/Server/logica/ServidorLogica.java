package pt.isec.pd.tp.Server.logica;

import pt.isec.pd.tp.Server.ServidorMain;
import pt.isec.pd.tp.Server.comunicacao.ClienteHandler;
import pt.isec.pd.tp.Server.dados.DBManager;
import pt.isec.pd.tp.Client.Client;
import pt.isec.pd.tp.Utils.Mensagem;

import java.util.*;

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

    public Mensagem processarLoginRegisto(Mensagem msg, ClienteHandler handler) {

        if (msg.getTipo() == Mensagem.Tipo.LOGIN) {
            Client clientToAuthenticate = (Client) msg.getPayload();
            String tipoUser = dbManager.login(clientToAuthenticate);

            if (tipoUser != null) {
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

        if (msg.getTipo() == Mensagem.Tipo.REGISTO_ESTUDANTE) {
            if (!(msg.getPayload() instanceof String[])) {
                return new Mensagem(Mensagem.Tipo.REGISTO_FALHOU, "Payload de registo inválido.");
            }
            if (dbManager.registarEstudante((String[]) msg.getPayload())) {
                // Notifica todos os outros clientes
                notificarClientes(new Mensagem(Mensagem.Tipo.NOTIFICACAO_ASSINCRONA, "Um novo estudante foi registado."), handler);
                return new Mensagem(Mensagem.Tipo.REGISTO_SUCESSO, null);
            } else {
                System.out.println("Registo de estudante falhou");
                return new Mensagem(Mensagem.Tipo.REGISTO_FALHOU, null);
            }
        }

        if (msg.getTipo() == Mensagem.Tipo.REGISTO_DOCENTE) {
            if (!(msg.getPayload() instanceof String[])) {
                return new Mensagem(Mensagem.Tipo.REGISTO_FALHOU, "Payload de registo inválido.");
            }
            if (dbManager.registarDocente((String[]) msg.getPayload())) {
                // Notifica todos os outros clientes
                notificarClientes(new Mensagem(Mensagem.Tipo.NOTIFICACAO_ASSINCRONA, "Um novo docente foi registado."), handler);
                return new Mensagem(Mensagem.Tipo.REGISTO_SUCESSO, null);
            } else {
                System.out.println("Registo de docente falhou");
                return new Mensagem(Mensagem.Tipo.REGISTO_FALHOU, null);
            }
        }

        return new Mensagem(Mensagem.Tipo.EXIT, null);
    }

    public Mensagem processarMensagem(Mensagem msg, ClienteHandler handler) {
        if (!isServidorPrincipal()) {
            return new Mensagem(Mensagem.Tipo.ERRO, "Servidor não é o principal");
        }

        switch (msg.getTipo()) {
            // Escrita DB
            case CRIAR_PERGUNTA:
                String[] novaPergunta = dbManager.criaPergunta((String[]) msg.getPayload());
                if (novaPergunta != null) {
                    notificarClientes(new Mensagem(Mensagem.Tipo.NOTIFICACAO_ASSINCRONA, "Nova pergunta foi criada."), handler);
                    return new Mensagem(Mensagem.Tipo.DETALHES_PERGUNTA, novaPergunta);
                }
                break;
            case VISUALIZAR_PERGUNTA:
                String[] dadospergunta = dbManager.visualizarPergunta((int) msg.getPayload());

                if(dadospergunta != null){
                    return new Mensagem(Mensagem.Tipo.DETALHES_PERGUNTA, dadospergunta);
                }
                break;
            case EDITAR_PERGUNTA:
                if(dbManager.editarPergunta((String[]) msg.getPayload())) {
                    notificarClientes(new Mensagem(Mensagem.Tipo.NOTIFICACAO_ASSINCRONA, "Uma pergunta foi editada."), handler);
                    return new Mensagem(Mensagem.Tipo.OPERACAO_SUCESSO, null);
                }else{
                    return new Mensagem(Mensagem.Tipo.OPERACAO_FALHOU, null);
                }
            case ELIMINAR_PERGUNTA:
                if(dbManager.eliminarPergunta((int)msg.getPayload())){
                    notificarClientes(new Mensagem(Mensagem.Tipo.NOTIFICACAO_ASSINCRONA, "Uma pergunta foi eliminada."), handler);
                    return new Mensagem(Mensagem.Tipo.OPERACAO_SUCESSO, null);
                }else{
                    return new Mensagem(Mensagem.Tipo.OPERACAO_FALHOU, null);
                }
            case EDITAR_PERFIL_DOCENTE:
                if (dbManager.editarPerfilDocente((String[]) msg.getPayload())) {
                    return new Mensagem(Mensagem.Tipo.OPERACAO_SUCESSO, null);
                }else{
                    return new Mensagem(Mensagem.Tipo.OPERACAO_FALHOU, null);
                }
            case OBTER_PERGUNTA:
                String[] perguntaAtiva = dbManager.obterPerguntaPorCodigo((String) msg.getPayload());
                if (perguntaAtiva != null) {
                    return new Mensagem(Mensagem.Tipo.DETALHES_PERGUNTA_ESTUDANTE, perguntaAtiva);
                }
                break;
            case SUBMETER_RESPOSTA:
                String[] dadosResposta = (String[]) msg.getPayload();
                int idEstudante = Integer.parseInt(dadosResposta[0]);
                int idPergunta = Integer.parseInt(dadosResposta[1]);
                int opcaoEscolhida = Integer.parseInt(dadosResposta[2]);
                if (dbManager.submeterResposta(idEstudante, idPergunta, opcaoEscolhida)) {
                    return new Mensagem(Mensagem.Tipo.OPERACAO_SUCESSO, null);
                }
                break;
            case EDITAR_PERFIL_ESTUDANTE:
                if (dbManager.editarPerfilEstudante((String[]) msg.getPayload())) {
                    return new Mensagem(Mensagem.Tipo.OPERACAO_SUCESSO, null);
                }
                break;
            // Leitura DB
            case LISTAR_PERGUNTAS_DOCENTE:
                String[] payload = (String[]) msg.getPayload();
                int idDocente = Integer.parseInt(payload[0]);
                String filtro = payload[1];
                List<String[]> perguntas = dbManager.listarPerguntasDocente(idDocente, filtro);
                if (perguntas != null) {
                    return new Mensagem(Mensagem.Tipo.LISTA_PERGUNTAS, perguntas);
                }
                break;
            case VER_ESTATISTICAS:
                List<String[]> estatisticas = dbManager.getEstatisticasPerguntaExpirada((int) msg.getPayload());
                if (estatisticas != null) {
                    return new Mensagem(Mensagem.Tipo.ESTATISTICAS_PERGUNTA, estatisticas);
                }
                break;
            case LISTAR_RESULTADOS_PERGUNTA:
                // TODO
                break;
            case LISTAR_PERGUNTAS_RESPONDIDAS:
                List<String[]> respostas = dbManager.listarPerguntasRespondidas((int) msg.getPayload());
                if (respostas != null) {
                    return new Mensagem(Mensagem.Tipo.LISTA_PERGUNTAS_RESPONDIDAS, respostas);
                }
                break;
        }

        // Se nenhuma das operações acima retornou, a operação falhou.
        return new Mensagem(Mensagem.Tipo.OPERACAO_FALHOU, null);
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