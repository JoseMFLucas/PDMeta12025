package pt.isec.pd.tp.Client.Vista;

import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

public class ClientVista {

    private final Scanner scanner;
    // Regex para uma validação de email simples mas eficaz
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

    public ClientVista() {
        this.scanner = new Scanner(System.in);
    }

    public static class ClientInput {
        public final String inputString; // Input em minúsculas
        public final int inputInt;      // Valor inteiro, se for um número.

        public ClientInput(String inputString, int inputInt) {
            this.inputString = inputString;
            this.inputInt = inputInt;
        }

        // Verifica se o input pôde ser convertido num número.
        public boolean isNumeric() {
            return inputInt != Integer.MIN_VALUE;
        }
    }

    // Menus

    public void menuPrincipal() {
        System.out.println("\n--- BEM-VINDO ---");
        System.out.println("1. Login");
        System.out.println("2. Registar");
        System.out.println("0. Sair");
        System.out.print("Escolha uma opção: ");
    }

    public void menuRegisto() {
        System.out.println("\n--- REGISTAR ---");
        System.out.println("1. Docente");
        System.out.println("2. Estudante");
        System.out.println("0. Sair");
        System.out.print("Escolha uma opção: ");
    }

    public void menuDocente() {
        System.out.println("\n--- MENU DOCENTE ---");
        System.out.println("1. Criar pergunta");
        System.out.println("2. Editar dados da pergunta");
        System.out.println("3. Eliminar pergunta");
        System.out.println("4. Consultar perguntas");
        System.out.println("5. Ver estatísticas de pergunta expirada");
        System.out.println("6. Exportar resultados de pergunta expirada para CSV");
        System.out.println("7. Editar dados pessoais");
        System.out.println("0. Logout");
        System.out.print("Escolha uma opção: ");
    }

    public void menuEstudante() {
        System.out.println("\n--- MENU ESTUDANTE ---");
        System.out.println("1. Introduzir còdigo da pergunta");
        System.out.println("2. Consultar perguntas respondidas");
        System.out.println("3. Editar dados pessoais");
        System.out.println("0. Logout");
        System.out.print("Escolha uma opção: ");
    }


    // Metodo de Leitura (Inputs)


    public ClientInput lerInputGeral() {
        // Leitura do input do Cliente

        String input = scanner.nextLine().trim();
        String lowerCaseInput = input.toLowerCase();

        int numericValue = Integer.MIN_VALUE;

        try {
            // Tenta converter o ‘input’ para inteiro
            numericValue = Integer.parseInt(input);
            return new ClientInput(lowerCaseInput, numericValue);
        } catch (NumberFormatException e) {
            // É um comando de texto (ou um input inválido)
            return new ClientInput(lowerCaseInput, numericValue);
        }


    }

    // Função para verificar se o cliente introduziu alguma coisa

    public String lerStringObrigatoria(String prompt) {
        String input;
        do {
            System.out.print(prompt);
            input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                mostrarErro("Este campo é obrigatório e não pode ser vazio.");
            }
        } while (input.isEmpty());
        return input;
    }

    public String lerEmailValido(String prompt) {
        String email;
        do {
            email = lerStringObrigatoria(prompt);
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                mostrarErro("Formato de e-mail inválido. Tente novamente.");
                email = "";
            }
        } while (email.isEmpty());
        return email;
    }

    public int lerIntObrigatoria(String prompt) {
        String input;
        int numero = 0;
        boolean inputValido = false;

        do {
            System.out.print(prompt);
            input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                mostrarErro("Este campo é obrigatório e não pode ser vazio.");
            } else {
                try {
                    numero = Integer.parseInt(input);
                    inputValido = true;
                } catch (NumberFormatException e) {
                    mostrarErro("Introduza um número inteiro válido.");
                    inputValido = false;
                }
            }
        } while (!inputValido);

        return numero;
    }

    // Mostrar informações (erros, infos, avisos)

    public void mostrarDetalhesPergunta(String[] detalhes) {
        if (detalhes == null || detalhes.length < 8) {
            mostrarErro("Não foi possível obter os detalhes da pergunta.");
            return;
        }
        // Formato: {idpergunta, idDocente, enunciado, Arrays.toString(opcoes), dataInicio, dataFim, opcaoCorreta, codigoAcesso}
        String id = detalhes[0];
        String enunciado = detalhes[2];
        String opcoesString = detalhes[3];
        String dataInicio = detalhes[4];
        String dataFim = detalhes[5];
        String opcaoCorreta = detalhes[6];
        String codigoAcesso = detalhes[7];


        System.out.println("\n--- DETALHES DA PERGUNTA (Código: " + codigoAcesso + ") ---");
        System.out.println("ID Pergunta: " + id);
        System.out.println("Enunciado: " + enunciado);

        // Formata e exibe as opções
        String[] opcoes = opcoesString.substring(1, opcoesString.length() - 1).split(", ");
        System.out.println("Opções:");
        for (int i = 0; i < opcoes.length; i++) {
            System.out.println("  " + (i + 1) + ". " + opcoes[i]);
        }

        System.out.println("Opção Correta (índice): " + opcaoCorreta);
        System.out.println("Disponível de: " + dataInicio);
        System.out.println("Disponível até: " + dataFim);
        System.out.println("-------------------------------------\n");
    }

    public void mostrarPerguntaParaEstudante(String[] detalhes) {
        if (detalhes == null || detalhes.length < 3) {
            mostrarErro("Não foi possível obter os detalhes da pergunta.");
            return;
        }
        // Formato: {idPergunta, enunciado, opcoesString}
        String enunciado = detalhes[1];
        String opcoesString = detalhes[2];

        System.out.println("\n--- RESPONDA À PERGUNTA ---");
        System.out.println("Enunciado: " + enunciado);

        String[] opcoes = opcoesString.substring(1, opcoesString.length() - 1).split(", ");
        System.out.println("Opções:");
        for (int i = 0; i < opcoes.length; i++) {
            System.out.println("  " + (i + 1) + ". " + opcoes[i]);
        }
        System.out.println("---------------------------\n");
    }

    public void mostrarListaPerguntas(List<String[]> perguntas, String filtro) {
        if (perguntas == null || perguntas.isEmpty()) {
            mostrarInfo("Nenhuma pergunta encontrada para o filtro selecionado.");
            return;
        }

        boolean isExpiradas = "expiradas".equalsIgnoreCase(filtro);

        System.out.println("\n--- LISTA DE PERGUNTAS ---");
        if (isExpiradas) {
            System.out.printf("%-5s | %-40s | %-10s | %-19s | %-19s | %-10s | %-10s%n", "ID", "Enunciado", "Código", "Início", "Fim", "Respostas", "% Certas");
            System.out.println("--------------------------------------------------------------------------------------------------------------------------");
        } else {
            System.out.printf("%-5s | %-40s | %-10s | %-19s | %-19s%n", "ID", "Enunciado", "Código", "Início", "Fim");
            System.out.println("-------------------------------------------------------------------------------------------------");
        }

        for (String[] pergunta : perguntas) {
            if (isExpiradas) {
                // Formato: {idpergunta, enunciado, codigo_acesso, data_hora_inicio, data_hora_fim, total_respostas, percentagem_certas}
                System.out.printf("%-5s | %-40.40s | %-10s | %-19s | %-19s | %-10s | %-10s%n",
                        pergunta[0], pergunta[1], pergunta[2], pergunta[3], pergunta[4], pergunta[5], pergunta[6]);
            } else {
                // Formato: {idpergunta, enunciado, codigo_acesso, data_hora_inicio, data_hora_fim}
                System.out.printf("%-5s | %-40.40s | %-10s | %-19s | %-19s%n",
                        pergunta[0], pergunta[1], pergunta[2], pergunta[3], pergunta[4]);
            }
        }

        if (isExpiradas) {
            System.out.println("--------------------------------------------------------------------------------------------------------------------------\n");
        } else {
            System.out.println("-------------------------------------------------------------------------------------------------\n");
        }
    }

    public void mostrarEstatisticasPergunta(List<String[]> estatisticas) {
        if (estatisticas == null || estatisticas.size() < 2) {
            mostrarErro("Não foi possível obter as estatísticas da pergunta.");
            return;
        }

        String[] detalhesPergunta = estatisticas.get(0);
        String percentagemCertas = estatisticas.get(1)[0];

        System.out.println("\n--- ESTATÍSTICAS DA PERGUNTA ---");
        System.out.println("Enunciado: " + detalhesPergunta[0]);
        System.out.println("Data de Fim: " + detalhesPergunta[1]);
        System.out.println("Opções: " + detalhesPergunta[2]);
        System.out.println("Opção Correta: " + detalhesPergunta[3]);
        System.out.println("Percentagem de Respostas Certas: " + percentagemCertas);

        System.out.println("\n--- RESPOSTAS DOS ALUNOS ---");
        System.out.printf("%-15s | %-25s | %-25s | %-40s | %-19s%n", "Nº Estudante", "Nome", "Email", "Resposta Escolhida", "Data/Hora");
        System.out.println("---------------------------------------------------------------------------------------------------------------------------------");

        for (int i = 2; i < estatisticas.size(); i++) {
            String[] resposta = estatisticas.get(i);
            System.out.printf("%-15s | %-25s | %-25s | %-40s | %-19s%n",
                    resposta[0], resposta[1], resposta[2], resposta[3], resposta[4]);
        }
        System.out.println("---------------------------------------------------------------------------------------------------------------------------------\n");
    }


    public void mostrarPerguntasRespondidas(List<String[]> respostas) {
        if (respostas == null || respostas.isEmpty()) {
            mostrarInfo("Ainda não respondeu a nenhuma pergunta.");
            return;
        }
        System.out.println("\n--- HISTÓRICO DE RESPOSTAS ---");
        System.out.printf("%-40s | %-19s | %-10s | %-10s%n", "Enunciado da Pergunta", "Data da Resposta", "Sua Opção", "Resultado");
        System.out.println("-------------------------------------------------------------------------------------------");
        for (String[] resposta : respostas) {
            // Formato: {enunciado, data_hora_realizacao, opcao_escolhida_indice, esta_certa}
            System.out.printf("%-40.40s | %-19s | %-10s | %-10s%n",
                    resposta[0], resposta[1], resposta[2], resposta[3]);
        }
        System.out.println("-------------------------------------------------------------------------------------------\n");
    }

    public void mostrarErro(String msg) {
        System.err.println("ERRO: " + msg);
    }

    public void mostrarInfo(String msg) {
        System.out.println("INFO: " + msg);
    }

    public void mostrarAviso(String msg) {
        System.out.println(msg);
    }

    public void fecharScanner() {
        if (scanner != null) {
            scanner.close();
        }
    }

}