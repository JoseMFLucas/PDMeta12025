package pt.isec.pd.tp.Server.logica;

import pt.isec.pd.tp.Server.ServidorMain;
import pt.isec.pd.tp.Server.comunicacao.ClienteHandler;
import pt.isec.pd.tp.Utils.Mensagem;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// Responsável pela Lógica de Negócio
public class ServidorLogica {
    private final ServidorMain servidorMain;

    // Lista de clientes conectados para enviar notificações
    private final Set<ClienteHandler> activeClients = Collections.synchronizedSet(new HashSet<>());

    public ServidorLogica(ServidorMain servidorMain) {
        this.servidorMain = servidorMain;
    }

    public boolean isServidorPrincipal() {
        return servidorMain.isPrincipal();
    }

    // TODO: Implementar métodos para processar pedidos dos clientes
    // Estes métodos serão chamados pelo ClienteHandler

    public Mensagem processarLoginRegisto(Mensagem msg) {
        // TODO: Chamar dbManager.login() ou dbManager.registar()
        // ATENÇÃO: registo é uma escrita na BD.
        // if (dbManager.registarEstudante(...)) {
        //    notificarClientes(new Mensagem(...)); // Notificar atualização
        // }
        return new Mensagem(Mensagem.Tipo.LOGIN_SUCESSO, null); // Exemplo
    }

    public Mensagem processarMensagem(Mensagem msg) {
        if (!isServidorPrincipal()) {
            return new Mensagem(Mensagem.Tipo.ERRO, "Servidor não é o principal");
        }

        boolean houveAlteracaoBD = false;

        switch (msg.getTipo()) {
            case CRIAR_PERGUNTA:
                // TODO: 1. Validar dados da pergunta
                // TODO: 2. Chamar dbManager.criarPergunta( (Pergunta) msg.getPayload() )
                // houveAlteracaoBD = dbManager.criarPergunta(...);
                break;
            // ... outros casos (EDITAR, ELIMINAR, SUBMETER_RESPOSTA)

            // Casos de Leitura (não alteram BD)
            case LISTAR_PERGUNTAS_DOCENTE:
                // TODO: Chamar dbManager.getPerguntasDocente(...)
                // return new Mensagem(Mensagem.Tipo.LISTA_PERGUNTAS, lista);
                break;
            // ... Outros casos de leitura
        }


        return new Mensagem(Mensagem.Tipo.OPERACAO_SUCESSO, null); // Exemplo
    }

    public void addClienteHandler(ClienteHandler handler) {
        activeClients.add(handler);
    }

    public void removeClienteHandler(ClienteHandler handler) {
        activeClients.remove(handler);
    }

    // Notifica todos os clientes conectados
    private void notificarClientes(Mensagem msg) {
        synchronized (activeClients) {
            for (ClienteHandler handler : activeClients) {
                handler.enviarNotificacao(msg);
            }
        }
    }
}