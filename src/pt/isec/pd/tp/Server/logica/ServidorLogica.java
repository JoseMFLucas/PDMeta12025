package pt.isec.pd.tp.Server.logica;

import pt.isec.pd.tp.Server.ServidorMain;
import pt.isec.pd.tp.Server.comunicacao.ClienteHandler;
import pt.isec.pd.tp.Server.dados.DBManager;
import pt.isec.pd.tp.Utils.Client;
import pt.isec.pd.tp.Utils.Mensagem;
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

        if(msg.getTipo() == Mensagem.Tipo.LOGIN) {
            String userType = dbManager.login((Client) msg.getPayload());

            if (userType != null) {
                return new Mensagem(Mensagem.Tipo.LOGIN_SUCESSO, userType);
            } else {
                return new Mensagem(Mensagem.Tipo.LOGIN_FALHOU, null);
            }

        }
        if(msg.getTipo() == Mensagem.Tipo.REGISTO_ESTUDANTE){
            System.out.println("Registar Estudante");
            String[] payload = (String[]) msg.getPayload();
           if(dbManager.registarEstudante((String[]) msg.getPayload())){
               notificarClientes(new Mensagem(Mensagem.Tipo.REGISTO_SUCESSO, "Novo Cliente foi registado.")); // Notificar atualização

               System.out.println("Registo efetuado com sucesso.");

               // Após o registo, tenta fazer o login automaticamente
               String email = payload[1];
               String password = payload[2];
               String userType = dbManager.login(new Client(email, password, null));
               System.out.println("Tipo de utilizador:" + userType);
               if (userType != null) {
                   return new Mensagem(Mensagem.Tipo.LOGIN_SUCESSO, userType);
               } else {
                   return new Mensagem(Mensagem.Tipo.LOGIN_FALHOU, "Login automático falhou após registo.");
               }
            } else{
                System.out.println("Registo falhou");
                return new Mensagem(Mensagem.Tipo.REGISTO_FALHOU, null);
            }
        }

        if(msg.getTipo() == Mensagem.Tipo.REGISTO_DOCENTE){
            System.out.println("Registar Docente");
            String[] payload = (String[]) msg.getPayload();
            if(dbManager.registarDocente((String []) msg.getPayload())){
                notificarClientes(new Mensagem(Mensagem.Tipo.REGISTO_SUCESSO, "Novo Docente foi registado.")); // Notificar atualização

                System.out.println("Registo efetuado com sucesso.");

                // Após o registo, tenta fazer o login automaticamente
                String email = payload[1];
                String password = payload[2];
                String userType = dbManager.login(new Client(email, password, null));
                System.out.println("Tipo de utilizador:" + userType);
                if (userType != null) {
                    return new Mensagem(Mensagem.Tipo.LOGIN_SUCESSO, userType);
                } else {
                    return new Mensagem(Mensagem.Tipo.LOGIN_FALHOU, "Login automático falhou após registo.");
                }
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
                // TODO: 1. Pedir iniciais dados da pergunta - pedir primeiro o enunciado, o número de opções, o período de disponibilidade (data/hora de início e de fim)
                // 2. Pedir cada uma das opções
                // 3. Pedir a opção correta
                // TODO: 2. Chamar dbManager.criarPergunta( (Pergunta) msg.getPayload() )
                // houveAlteracaoBD = dbManager.criarPergunta(...);
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