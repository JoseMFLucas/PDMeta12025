package pt.isec.pd.tp.Client.Vista;

import java.util.InputMismatchException;
import java.util.Scanner;

public class ClientVista {

    private final Scanner scanner;

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

    public void menuDocente() {
        System.out.println("\n--- MENU DOCENTE ---");
        System.out.println("1. Criar pergunta");
        System.out.println("2. Editar dados da pergunta");
        System.out.println("3. Eliminar pergunta");
        System.out.println("4. Consultar perguntas");
        System.out.println("5. Editar dados pessoais");
        System.out.println("0. Logout");
        System.out.print("Escolha uma opção: ");
    }

    public void menuEstudante() {
        System.out.println("\n--- MENU ESTUDANTE ---");
        System.out.println("1. Introduzir codigo da pergunta");
        System.out.println("2. Consultar perguntas respondidas");
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

    // Mostrar informações (erros, infos, avisos)

    public void mostrarErro(String msg) {
        System.err.println("ERRO: " + msg);
    }

    public void mostrarInfo(String msg) {
        System.out.println("INFO: " + msg);
    }

    public void mostrarAviso(String msg) {
        System.out.println("AVISO: " + msg);
    }

    public void fecharScanner() {
        if (scanner != null) {
            scanner.close();
        }
    }

}