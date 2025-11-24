package pt.isec.pd.tp.Client.Vista;

import pt.isec.pd.tp.Client.Comunicacao.ClientComunicacao;
import pt.isec.pd.tp.Utils.Mensagem;
import java.util.Scanner;

public class ClientVista {

    public ClientVista() {

    }

    public void menuprincipal(){
        System.out.println("\n--- BEM-VINDO ---");
        System.out.println("1. Login");
        System.out.println("2. Registar");
        System.out.println("0. Sair");
        System.out.print("Escolha uma opção: ");
    }

    public void menudocente(){
        System.out.println("\n--- MENU DOCENTE ---");
        System.out.println("1. Criar pergunta");
        System.out.println("2. Editar dados da pergunta");
        System.out.println("3. Eliminar pergunta");
        System.out.println("4. Consultar perguntas");
        System.out.println("5. Editar dados pessoais");
        System.out.println("0. Sair");
        System.out.print("Escolha uma opção: ");
    }

    public void menualuno(){
        System.out.println("\n--- MENU ESTUDANTE ---");
        System.out.println("1. Login");
        System.out.println("2. Introduzir codigo da pergunta");
        System.out.println("3. Consultar perguntas respondidas");
        System.out.println("0. Sair");
        System.out.print("Escolha uma opção: ");
    }
}
