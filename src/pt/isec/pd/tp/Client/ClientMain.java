package pt.isec.pd.tp.Client;

import pt.isec.pd.tp.Client.Comunicacao.ClientComunicacao;
import pt.isec.pd.tp.Client.Comunicacao.ClientListener;
import pt.isec.pd.tp.Client.Vista.ClientVista;
import pt.isec.pd.tp.Utils.Mensagem;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClientMain {

    private static final String Pasta_CSV = "../../../../../../csv";

    private ClientListener listener;
    private Thread listenerThread;

    private final ClientVista vista;
    private boolean autenticated = false;
    private boolean closing = false;
    private Socket principalSocket;

    private ObjectOutputStream out;
    private ObjectInputStream in;

    Client user = null;

    String email, password, nome, enunciado, codigo, opcaocorreta, datainicio, datafim;

    int numero;

    String[] info;

    // Server info for reconnection
    private final String dirIp;
    private final int dirPort;
    private String currentServerIp;
    private int currentServerPort;

    public ClientMain(ClientVista vista, Socket principalSocket, String dirIp, int dirPort, String serverIp, int serverTcpPort) {
        this.vista = vista;
        this.principalSocket = principalSocket;
        this.dirIp = dirIp;
        this.dirPort = dirPort;
        this.currentServerIp = serverIp;
        this.currentServerPort = serverTcpPort;

        try {
            this.out = new ObjectOutputStream(principalSocket.getOutputStream());
            this.in = new ObjectInputStream(principalSocket.getInputStream());
        } catch (IOException e) {
            vista.mostrarErro("Erro ao criar streams de comunicação: " + e.getMessage());
            shutdown();
        }
    }

    // Loop Inicial (Menu Principal)

    public void loopInicial() {
        listener = new ClientListener(in, vista, this);
        listenerThread = new Thread(listener);
        listenerThread.start();

        while (!closing) {
            try {
                if (!autenticated) {
                    vista.menuPrincipal();
                    ClientVista.ClientInput input = vista.lerInputGeral();

                    switch (input.inputInt) {
                        case 1: // Login
                            vista.mostrarInfo("Introduza o email e a password para iniciar o login.");
                            this.email = vista.lerEmailValido("Email: ");
                            this.password = vista.lerStringObrigatoria("Password: ");
                            vista.mostrarInfo("Iniciar o login com email: " + this.email + " e password: " + this.password);
                            try {
                                user = new Client(0, this.email, this.password, null);
                                Mensagem msg = new Mensagem(Mensagem.Tipo.LOGIN, user);
                                out.writeObject(msg);
                                out.flush();

                                Mensagem response = listener.getResponse(30);

                                if (response != null && response.getTipo() == Mensagem.Tipo.LOGIN_SUCESSO) {
                                    vista.mostrarInfo("Login bem-sucedido!");
                                    this.user = (Client) response.getPayload();
                                    autenticated = true;
                                } else {
                                    vista.mostrarErro("Login falhou. Credenciais inválidas ou tempo de resposta excedido.");
                                    this.email = null; // Clear credentials on failure
                                    this.password = null;
                                }
                            } catch (IOException | InterruptedException e) {
                                if (!closing) {
                                    vista.mostrarErro("Erro durante o login: " + e.getMessage());
                                }
                            }
                            break;
                        case 2: // Registar
                        vista.menuRegisto();
                        ClientVista.ClientInput inputregistar = vista.lerInputGeral();
                        switch (inputregistar.inputInt) {
                            case 1:
                                vista.mostrarInfo("Introduza nome, e-mail, password e o código único para completar o registo.");
                                nome = vista.lerStringObrigatoria("Nome: ");
                                email = vista.lerEmailValido("Email: ");
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
                                email = vista.lerEmailValido("Email: ");
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
                                            user.setId(numero);
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
                            datainicio = vista.lerStringObrigatoria("Data de início (YYYY/MM/DD HH:MM): ");
                            datafim = vista.lerStringObrigatoria("Data de fim (YYYY/MM/DD HH:MM): ");

                            info = new String[]{String.valueOf(user.getId()), enunciado, Arrays.toString(opcoes), datainicio, datafim, opcaocorreta};

                            try {
                                Mensagem msg = new Mensagem(Mensagem.Tipo.CRIAR_PERGUNTA, info);
                                out.writeObject(msg);
                                out.flush();

                                Mensagem response = listener.getResponse();
                                if (response != null && response.getTipo() == Mensagem.Tipo.DETALHES_PERGUNTA) {
                                    vista.mostrarInfo("Pergunta adicionada com sucesso!");
                                    vista.mostrarDetalhesPergunta((String[]) response.getPayload());
                                } else {
                                    vista.mostrarErro("Erro a adicionar pergunta.");
                                }
                            } catch (IOException | InterruptedException e) {
                                vista.mostrarErro("Erro durante o envio da pergunta: " + e.getMessage());
                            }
                            break;
                        case 2:
                            vista.mostrarInfo("Introduza o id da pergunta para editar");
                            int idPerguntaEditar = vista.lerIntObrigatoria("ID da pergunta: ");

                            try {
                                Mensagem msg = new Mensagem(Mensagem.Tipo.VISUALIZAR_PERGUNTA, idPerguntaEditar);
                                out.writeObject(msg);
                                out.flush();

                                Mensagem response = listener.getResponse();

                                if (response != null && response.getTipo() == Mensagem.Tipo.DETALHES_PERGUNTA) {
                                    String[] dadosEditados = (String[]) response.getPayload();
                                    boolean edicaoConcluida = false;

                                    while (!edicaoConcluida) {
                                        vista.mostrarDetalhesPergunta(dadosEditados);
                                        vista.mostrarInfo("O que deseja editar?");
                                        vista.mostrarInfo("1. Enunciado");
                                        vista.mostrarInfo("2. Opções");
                                        vista.mostrarInfo("3. Opção Correta");
                                        vista.mostrarInfo("4. Data de Início");
                                        vista.mostrarInfo("5. Data de Fim");
                                        vista.mostrarInfo("6. Guardar Alterações");
                                        vista.mostrarInfo("0. Cancelar");
                                        ClientVista.ClientInput campoEditar = vista.lerInputGeral();

                                        switch (campoEditar.inputInt) {
                                            case 1:
                                                dadosEditados[1] = vista.lerStringObrigatoria("Novo enunciado: ");
                                                break;
                                            case 2:
                                                numero = vista.lerIntObrigatoria("Novo número de opções: ");
                                                String[] novasOpcoes = new String[numero];
                                                for (int i = 0; i < numero; i++) {
                                                    novasOpcoes[i] = vista.lerStringObrigatoria("Opção " + (i + 1) + ": ");
                                                }
                                                dadosEditados[2] = Arrays.toString(novasOpcoes);
                                                break;
                                            case 3:
                                                dadosEditados[5] = String.valueOf(vista.lerIntObrigatoria("Novo índice da opção correta: "));
                                                break;
                                            case 4:
                                                dadosEditados[3] = vista.lerStringObrigatoria("Nova data de início (YYYY/MM/DD HH:MM): ");
                                                break;
                                            case 5:
                                                dadosEditados[4] = vista.lerStringObrigatoria("Nova data de fim (YYYY/MM/DD HH:MM): ");
                                                break;
                                            case 6:
                                                // Payload: {idPergunta, idDocente, enunciado, opcoes, dataInicio, dataFim, opcaoCorreta}
                                                String[] payloadFinal = new String[7];
                                                payloadFinal[0] = String.valueOf(idPerguntaEditar);
                                                System.arraycopy(dadosEditados, 0, payloadFinal, 1, dadosEditados.length);

                                                msg = new Mensagem(Mensagem.Tipo.EDITAR_PERGUNTA, payloadFinal);
                                                out.writeObject(msg);
                                                out.flush();

                                                response = listener.getResponse();
                                                if (response != null && response.getTipo() == Mensagem.Tipo.OPERACAO_SUCESSO) {
                                                    vista.mostrarInfo("Pergunta editada com sucesso!");
                                                } else {
                                                    vista.mostrarErro("Erro ao editar a pergunta. (Pode já ter respostas associadas)");
                                                }
                                                edicaoConcluida = true;
                                                break;
                                            case 0:
                                                vista.mostrarInfo("Edição cancelada.");
                                                edicaoConcluida = true;
                                                break;
                                            default:
                                                vista.mostrarAviso("Opção inválida.");
                                                break;
                                        }
                                    }
                                } else {
                                    vista.mostrarErro("Erro ao obter detalhes da pergunta. Verifique o ID.");
                                }
                            } catch (IOException | InterruptedException e) {
                                vista.mostrarErro("Erro durante a comunicação: " + e.getMessage());
                            }
                            break;
                        case 3:
                            vista.mostrarInfo("Introduza o ID da pergunta a eliminar:");
                            int idPerguntaEliminar = vista.lerIntObrigatoria("ID da pergunta: ");
                            try {
                                Mensagem msg = new Mensagem(Mensagem.Tipo.ELIMINAR_PERGUNTA, idPerguntaEliminar);
                                out.writeObject(msg);
                                out.flush();

                                Mensagem response = listener.getResponse();

                                if (response != null && response.getTipo() == Mensagem.Tipo.OPERACAO_SUCESSO) {
                                    vista.mostrarInfo("Pergunta eliminada com sucesso!");
                                } else {
                                    vista.mostrarErro("Erro ao eliminar a pergunta. (ID inválido ou a pergunta já tem respostas)");
                                }
                            } catch (IOException | InterruptedException e) {
                                vista.mostrarErro("Erro durante a comunicação: " + e.getMessage());
                            }
                            break;
                        case 4:
                            vista.mostrarInfo("Escolha um filtro para a pesquisa:");
                            vista.mostrarInfo("1. Ativas");
                            vista.mostrarInfo("2. Futuras");
                            vista.mostrarInfo("3. Expiradas");
                            vista.mostrarInfo("4. Todas");
                            ClientVista.ClientInput filtroInput = vista.lerInputGeral();
                            String filtro = "todas";
                            switch (filtroInput.inputInt) {
                                case 1: filtro = "ativas"; break;
                                case 2: filtro = "futuras"; break;
                                case 3: filtro = "expiradas"; break;
                                case 4: filtro = "todas"; break;
                                default: vista.mostrarAviso("Filtro inválido, a mostrar todas as perguntas."); break;
                            }

                            try {
                                String[] payload = {String.valueOf(user.getId()), filtro};
                                Mensagem msg = new Mensagem(Mensagem.Tipo.LISTAR_PERGUNTAS_DOCENTE, payload);
                                out.writeObject(msg);
                                out.flush();

                                Mensagem response = listener.getResponse();
                                if (response != null && response.getTipo() == Mensagem.Tipo.LISTA_PERGUNTAS) {
                                    if (response.getPayload() instanceof List) {
                                        List<?> rawList = (List<?>) response.getPayload();
                                        List<String[]> perguntas = new ArrayList<>();
                                        for (Object item : rawList) {
                                            if (item instanceof String[]) {
                                                perguntas.add((String[]) item);
                                            }
                                        }
                                        vista.mostrarListaPerguntas(perguntas, filtro);
                                    }
                                } else {
                                    vista.mostrarErro("Erro ao listar as perguntas.");
                                }
                            } catch (IOException | InterruptedException e) {
                                vista.mostrarErro("Erro durante a comunicação: " + e.getMessage());
                            }
                            break;
                        case 5:
                            vista.mostrarInfo("Introduza o ID da pergunta expirada para ver as estatísticas:");
                            int idPerguntaStats = vista.lerIntObrigatoria("ID da pergunta: ");
                            try {
                                Mensagem msg = new Mensagem(Mensagem.Tipo.VER_ESTATISTICAS, idPerguntaStats);
                                out.writeObject(msg);
                                out.flush();

                                Mensagem response = listener.getResponse();
                                if (response != null && response.getTipo() == Mensagem.Tipo.ESTATISTICAS_PERGUNTA) {
                                    if (response.getPayload() instanceof List) {
                                        List<?> rawList = (List<?>) response.getPayload();
                                        List<String[]> estatisticas = new ArrayList<>();
                                        for (Object item : rawList) {
                                            if (item instanceof String[]) {
                                                estatisticas.add((String[]) item);
                                            }
                                        }
                                        vista.mostrarEstatisticasPergunta(estatisticas);
                                    }
                                } else {
                                    vista.mostrarErro("Erro ao obter as estatísticas da pergunta.");
                                }
                            } catch (IOException | InterruptedException e) {
                                vista.mostrarErro("Erro durante a comunicação: " + e.getMessage());
                            }
                            break;
                        case 6:
                            vista.mostrarInfo("Introduza o ID da pergunta expirada para exportar os resultados para CSV:");
                            int idPerguntaExport = vista.lerIntObrigatoria("ID da pergunta: ");
                            try {
                                // Request 1: Get question details
                                Mensagem msgDetalhes = new Mensagem(Mensagem.Tipo.VISUALIZAR_PERGUNTA, idPerguntaExport);
                                out.writeObject(msgDetalhes);
                                out.flush();
                                Mensagem responseDetalhes = listener.getResponse();

                                if (responseDetalhes != null && responseDetalhes.getTipo() == Mensagem.Tipo.DETALHES_PERGUNTA) {
                                    String[] detalhesPergunta = (String[]) responseDetalhes.getPayload();

                                    // Request 2: Get statistics (student answers)
                                    Mensagem msgStats = new Mensagem(Mensagem.Tipo.VER_ESTATISTICAS, idPerguntaExport);
                                    out.writeObject(msgStats);
                                    out.flush();
                                    Mensagem responseStats = listener.getResponse();

                                    if (responseStats != null && responseStats.getTipo() == Mensagem.Tipo.ESTATISTICAS_PERGUNTA) {
                                        if (responseStats.getPayload() instanceof List) {
                                            List<?> rawList = (List<?>) responseStats.getPayload();
                                            List<String[]> estatisticas = new ArrayList<>();
                                            for (Object item : rawList) {
                                                if (item instanceof String[]) {
                                                    estatisticas.add((String[]) item);
                                                }
                                            }
                                            exportarResultadosParaCSV(detalhesPergunta, estatisticas);
                                        } else {
                                            vista.mostrarErro("Formato de dados de estatísticas inválido.");
                                        }
                                    } else {
                                        vista.mostrarErro("Não foi possível obter as respostas dos alunos para esta pergunta. A pergunta pode não ter expirado ou não ter respostas.");
                                    }
                                } else {
                                    vista.mostrarErro("Não foi possível obter os detalhes da pergunta. Verifique o ID.");
                                }
                            } catch (IOException | InterruptedException e) {
                                vista.mostrarErro("Erro durante a comunicação para exportação de CSV: " + e.getMessage());
                            }
                            break;
                        case 7:
                            vista.mostrarInfo("O que deseja editar no seu perfil?");
                            vista.mostrarInfo("1. Nome");
                            vista.mostrarInfo("2. Email");
                            vista.mostrarInfo("3. Password");
                            vista.mostrarInfo("0. Cancelar");
                            ClientVista.ClientInput campoEditar = vista.lerInputGeral();

                            String campo = null;
                            String novoValor = null;

                            switch (campoEditar.inputInt) {
                                case 1:
                                    campo = "nome";
                                    novoValor = vista.lerStringObrigatoria("Novo nome: ");
                                    break;
                                case 2:
                                    campo = "email";
                                    novoValor = vista.lerEmailValido("Novo email: ");
                                    break;
                                case 3:
                                    campo = "password";
                                    novoValor = vista.lerStringObrigatoria("Nova password: ");
                                    break;
                                case 0:
                                    vista.mostrarInfo("Edição cancelada.");
                                    break;
                                default:
                                    vista.mostrarAviso("Opção inválida.");
                                    break;
                            }

                            if (campo != null) {
                                String codigoRegisto = vista.lerStringObrigatoria("Para confirmar, introduza o código de registo de docente: ");
                                String[] dadosEdicao = {String.valueOf(user.getId()), campo, novoValor, codigoRegisto};

                                try {
                                    Mensagem msg = new Mensagem(Mensagem.Tipo.EDITAR_PERFIL_DOCENTE, dadosEdicao);
                                    out.writeObject(msg);
                                    out.flush();

                                    Mensagem response = listener.getResponse();
                                    if (response != null && response.getTipo() == Mensagem.Tipo.OPERACAO_SUCESSO) {
                                        vista.mostrarInfo("Perfil atualizado com sucesso!");
                                        if ("email".equals(campo)) {
                                            user.setEmail(novoValor);
                                        }else if("password".equals(campo)){
                                            user.setPassword(novoValor);
                                        }
                                    } else {
                                        vista.mostrarErro("Erro ao atualizar o perfil. Verifique o código de registo.");
                                    }
                                } catch (IOException | InterruptedException e) {
                                    vista.mostrarErro("Erro durante a comunicação: " + e.getMessage());
                                }
                            }
                            break;
                        case 0:
                            vista.mostrarInfo("Logout.");
                            try {
                                Mensagem msg = new Mensagem(Mensagem.Tipo.LOGOUT, null);
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
                } else if (user.getTipo() == Client.Tipo.ESTUDANTE) {
                    vista.menuEstudante();
                    ClientVista.ClientInput inputregistar = vista.lerInputGeral();
                    switch (inputregistar.inputInt) {
                        case 1:
                            vista.mostrarInfo("Introduza o código de acesso da pergunta:");
                            String codigoAcesso = vista.lerStringObrigatoria("Código: ");
                            try {
                                Mensagem msg = new Mensagem(Mensagem.Tipo.OBTER_PERGUNTA, codigoAcesso);
                                out.writeObject(msg);
                                out.flush();

                                Mensagem response = listener.getResponse();

                                if (response != null && response.getTipo() == Mensagem.Tipo.DETALHES_PERGUNTA_ESTUDANTE) {
                                    String[] detalhesPergunta = (String[]) response.getPayload();
                                    vista.mostrarPerguntaParaEstudante(detalhesPergunta);

                                    int idPergunta = Integer.parseInt(detalhesPergunta[0]);
                                    int respostaUtilizador = vista.lerIntObrigatoria("Escolha a sua resposta: ");

                                    String[] dadosResposta = {String.valueOf(user.getId()), String.valueOf(idPergunta), String.valueOf(respostaUtilizador)};
                                    msg = new Mensagem(Mensagem.Tipo.SUBMETER_RESPOSTA, dadosResposta);
                                    out.writeObject(msg);
                                    out.flush();
                                    response = listener.getResponse();
                                    if (response != null && response.getTipo() == Mensagem.Tipo.OPERACAO_SUCESSO) {
                                        vista.mostrarInfo("Resposta submetida com sucesso!");
                                    } else {
                                        vista.mostrarErro("Erro ao submeter a resposta.");
                                    }
                                } else {
                                    vista.mostrarErro("Código de acesso inválido ou a pergunta não está ativa.");
                                }
                            } catch (IOException | InterruptedException e) {
                                vista.mostrarErro("Erro durante a comunicação: " + e.getMessage());
                            }
                            break;
                        case 2:
                            try {
                                Mensagem msg = new Mensagem(Mensagem.Tipo.LISTAR_PERGUNTAS_RESPONDIDAS, user.getId());
                                out.writeObject(msg);
                                out.flush();
                                Mensagem response = listener.getResponse();
                                if (response != null && response.getTipo() == Mensagem.Tipo.LISTA_PERGUNTAS_RESPONDIDAS) {
                                    if (response.getPayload() instanceof List) {
                                        List<?> rawList = (List<?>) response.getPayload();
                                        List<String[]> respostas = new ArrayList<>();
                                        for (Object item : rawList) {
                                            if (item instanceof String[]) {
                                                respostas.add((String[]) item);
                                            }
                                        }
                                        vista.mostrarPerguntasRespondidas(respostas);
                                    }
                                } else {
                                    vista.mostrarErro("Erro ao listar o histórico de respostas.");
                                }
                            } catch (IOException | InterruptedException e) {
                                vista.mostrarErro("Erro durante a comunicação: " + e.getMessage());
                            }
                            break;
                        case 3:
                            vista.mostrarInfo("O que deseja editar no seu perfil?");
                            vista.mostrarInfo("1. Número De Estudante");
                            vista.mostrarInfo("2. Nome");
                            vista.mostrarInfo("3. Email");
                            vista.mostrarInfo("4. Password");
                            vista.mostrarInfo("0. Cancelar");
                            ClientVista.ClientInput campoEditar = vista.lerInputGeral();

                            String campo = null;
                            String novoValor = null;

                            switch (campoEditar.inputInt) {
                                case 1:
                                    campo = "numero_estudante";
                                    novoValor = vista.lerStringObrigatoria("Novo Número De Estudante: ");
                                    break;
                                case 2:
                                    campo = "nome";
                                    novoValor = vista.lerStringObrigatoria("Novo nome: ");
                                    break;
                                case 3:
                                    campo = "email";
                                    novoValor = vista.lerEmailValido("Novo email: ");
                                    break;
                                case 4:
                                    campo = "password";
                                    novoValor = vista.lerStringObrigatoria("Nova password: ");
                                    break;
                                case 0:
                                    vista.mostrarInfo("Edição cancelada.");
                                    break;
                                default:
                                    vista.mostrarAviso("Opção inválida.");
                                    break;
                            }

                            if (campo != null) {
                                String[] dadosEdicao = {String.valueOf(user.getId()), campo, novoValor};

                                try {
                                    Mensagem msg = new Mensagem(Mensagem.Tipo.EDITAR_PERFIL_ESTUDANTE, dadosEdicao);
                                    out.writeObject(msg);
                                    out.flush();

                                    Mensagem response = listener.getResponse();
                                    if (response != null && response.getTipo() == Mensagem.Tipo.OPERACAO_SUCESSO) {
                                        vista.mostrarInfo("Perfil atualizado com sucesso!");
                                        if ("email".equals(campo)) {
                                            user.setEmail(novoValor);
                                        }else if("password".equals(campo)){
                                            user.setPassword(novoValor);
                                        }
                                    } else {
                                        vista.mostrarErro("Erro ao editar o perfil.");
                                    }
                                } catch (IOException | InterruptedException e) {
                                    vista.mostrarErro("Erro durante a comunicação: " + e.getMessage());
                                }
                            }
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
            } catch (Exception e) {
                if (!closing) {
                    vista.mostrarErro("Ocorreu um erro inesperado no loop principal: " + e.getMessage());
                }
            }
        }
        shutdown();
    }

    public synchronized void reconectar() {
        // Fecha streams e socket antigos. Não chama shutdown() para evitar fechar a vista.
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (principalSocket != null && !principalSocket.isClosed()) principalSocket.close();
        } catch (IOException e) {
            // Ignora erros ao fechar, pois a ligação já pode ter caído.
        }

        try {
            ClientComunicacao clcom = new ClientComunicacao(InetAddress.getByName(dirIp), dirPort);
            String[] serverDetails = clcom.requestPrincipalServer();

            if (serverDetails != null) {
                String newServerIp = serverDetails[0];
                int newServerPort = Integer.parseInt(serverDetails[1]);

                if (!newServerIp.equals(currentServerIp) || newServerPort != currentServerPort) {
                    vista.mostrarAviso("Novo servidor principal encontrado. A ligar a " + newServerIp + ":" + newServerPort);
                    currentServerIp = newServerIp;
                    currentServerPort = newServerPort;
                    if (tentarConexaoEAutenticacao()) {
                        vista.mostrarInfo("Reconexão bem-sucedida!");
                        // A thread do listener antigo vai morrer, a nova continua no loop principal.
                    } else {
                        vista.mostrarErro("Falha na autenticação com o novo servidor. A aplicação vai encerrar.");
                        System.exit(1);
                    }
                } else {
                    vista.mostrarAviso("O servidor é o mesmo. A tentar novamente em 20 segundos...");
                    try {
                        Thread.sleep(20000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    if (tentarConexaoEAutenticacao()) {
                        vista.mostrarInfo("Reconexão bem-sucedida!");
                    } else {
                        vista.mostrarErro("Falha na segunda tentativa de reconexão. A aplicação vai encerrar.");
                        System.exit(1);
                    }
                }
            } else {
                vista.mostrarErro("Não foi possível obter um servidor principal. A aplicação vai encerrar.");
                System.exit(1);
            }
        } catch (IOException e) {
            vista.mostrarErro("Erro durante a reconexão: " + e.getMessage());
            System.exit(1);
        }
    }

    private boolean tentarConexaoEAutenticacao() {
        try {
            principalSocket = new Socket(currentServerIp, currentServerPort);
            out = new ObjectOutputStream(principalSocket.getOutputStream());
            in = new ObjectInputStream(principalSocket.getInputStream());

            // Reinicia o listener com as novas streams
            listener = new ClientListener(in, vista, this);
            listenerThread = new Thread(listener);
            listenerThread.start();

            // Se estava autenticado, tenta reautenticar com as credenciais guardadas
            if (autenticated && this.email != null && this.password != null) {
                Mensagem msg = new Mensagem(Mensagem.Tipo.LOGIN, new Client(0, this.email, this.password, null));
                out.writeObject(msg);
                out.flush();

                Mensagem response = listener.getResponse(30);
                if (response != null && response.getTipo() == Mensagem.Tipo.LOGIN_SUCESSO) {
                    this.user = (Client) response.getPayload();
                    return true; // Sucesso
                } else {
                    autenticated = false; // Falha na reautenticação
                    return false;
                }
            }
            // Se não estava autenticado, a reconexão já é um sucesso.
            return true;
        } catch (IOException | InterruptedException e) {
            vista.mostrarErro("Falha ao tentar conectar e autenticar: " + e.getMessage());
            return false;
        }
    }

    private void exportarResultadosParaCSV(String[] detalhes, List<String[]> estatisticas) {

        String filenameRaw = vista.lerStringObrigatoria("Introduza o nome do ficheiro CSV (ex: resultados.csv): ");
        String filename;

        if (!filenameRaw.toLowerCase().endsWith(".csv")) {
            filename = filenameRaw + ".csv";
        } else {
            filename = filenameRaw;
        }

        File csvDir = new File(Pasta_CSV);

        if (!csvDir.exists()) {
            if (csvDir.mkdirs()) {
                vista.mostrarInfo("Pasta 'csv' criada com sucesso.");
            } else {
                vista.mostrarErro("ERRO: Não foi possível criar a pasta 'csv'. A exportação não será efetuada no local desejado.");
                return;
            }
        }

        // 3. Criar o objeto File que aponta para o ficheiro dentro da pasta 'csv'
        File finalFile = new File(csvDir, filename);
        String fullPath = finalFile.getAbsolutePath();

        try (FileWriter writer = new FileWriter(finalFile)) {
            String enunciado = detalhes[2];
            String opcoesStr = detalhes[3];
            String dataInicio = detalhes[4];
            String dataFim = detalhes[5];
            int opcaoCorretaIdx = Integer.parseInt(detalhes[6]);

            String[] inicioParts = dataInicio.split(" ");
            String[] fimParts = dataFim.split(" ");
            String[] dataParts = inicioParts[0].split("/");
            String dia = dataParts[2] + "-" + dataParts[1] + "-" + dataParts[0];
            String horaInicial = inicioParts[1];
            String horaFinal = fimParts[1];
            char opcaoCorretaLetra = (char) ('a' + opcaoCorretaIdx);

            writer.append("\"dia\";\"hora inicial\";\"hora final\";\"enunciado da pergunta\";\"opção certa\"\n");
            writer.append(String.format("\"%s\";\"%s\";\"%s\";\"%s\";\"%c\"\n", dia, horaInicial, horaFinal, enunciado, opcaoCorretaLetra));
            writer.append("\"opção\";\"texto da opção\"\n");

            String[] opcoes = opcoesStr.substring(1, opcoesStr.length() - 1).split(", ");
            for (int i = 0; i < opcoes.length; i++) {
                char letraOpcao = (char) ('a' + i);
                writer.append(String.format("\"%c\";\"%s\"\n", letraOpcao, opcoes[i]));
            }

            writer.append("\"número de estudante\";\"nome\";\"e-mail\";\"resposta\"\n");

            for (int i = 2; i < estatisticas.size(); i++) {
                String[] resposta = estatisticas.get(i);
                String numEstudante = resposta[0];
                String nomeEstudante = resposta[1];
                String emailEstudante = resposta[2];
                String respostaLetra = resposta[3];
                writer.append(String.format("\"%s\";\"%s\";\"%s\";\"%s\"\n", numEstudante, nomeEstudante, emailEstudante, respostaLetra));
            }
            vista.mostrarInfo("Resultados exportados com sucesso para " + fullPath);
        } catch (IOException e) {
            vista.mostrarErro("Ocorreu um erro ao escrever o ficheiro CSV: " + e.getMessage());
        } catch (Exception e) {
            vista.mostrarErro("Ocorreu um erro inesperado durante a exportação para CSV: " + e.getMessage());
        }
    }

    private void shutdown() {
        closing = true;
        if (listener != null) {
            listener.stopRunning();
        }
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (principalSocket != null && !principalSocket.isClosed()) {
                principalSocket.close();
            }
        } catch (IOException e) {
            // Ignora erros durante o encerramento
        }
        vista.fecharScanner();
    }

    // Função principal do cliente

    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("Uso: java ClientMain <IP_Diretoria> <Porto_Diretoria>");
            return;
        }

        // Cria o Socket e a Vista primeiro

        String dirIpStr = args[0];
        String dirPortStr = args[1];
        ClientVista vista = new ClientVista();
        Socket principalSocket = null;
        ClientMain client = null;

        try {
            InetAddress dirIp = InetAddress.getByName(dirIpStr);
            int dirPort = Integer.parseInt(dirPortStr);

            ClientComunicacao clcom = new ClientComunicacao(dirIp, dirPort);

            // Pede o Servidor Principal ao Servidor Diretoria
            String[] serverDetails = clcom.requestPrincipalServer();

            if (serverDetails != null) {
                String serverIp = serverDetails[0];
                int serverTcpPort = Integer.parseInt(serverDetails[1]);

                System.out.println("Servidor Principal encontrado: " + serverIp + ":" + serverTcpPort);

                principalSocket = new Socket(serverIp, serverTcpPort);
                System.out.println("Ligação TCP estabelecida: " + serverIp + ":" + serverTcpPort);

                client = new ClientMain(vista, principalSocket, dirIpStr, dirPort, serverIp, serverTcpPort);
                client.loopInicial();

            } else {
                System.out.println("Não foi possível obter um servidor principal. O cliente vai encerrar.");
            }

        } catch (UnknownHostException e) {
            vista.mostrarErro("O endereço IP/Host '" + dirIpStr + "' é inválido ou desconhecido.");
        } catch (NumberFormatException e) {
            vista.mostrarErro("O porto fornecido ('" + dirPortStr + "') não é um número válido.");
        } catch (IOException e) {
            vista.mostrarErro("Erro de comunicação inicial: " + e.getMessage());
        } finally {
            if (client == null) { // Se o cliente nem chegou a ser criado, fecha os recursos manualmente
                if (principalSocket != null && !principalSocket.isClosed()) {
                    try {
                        principalSocket.close();
                    } catch (IOException ignored) {}
                }
                vista.fecharScanner();
            }
        }
    }
}
