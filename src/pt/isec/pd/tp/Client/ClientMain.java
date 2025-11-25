package pt.isec.pd.tp.Client;

import pt.isec.pd.tp.Client.Comunicacao.ClientComunicacao;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ClientMain {

    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("Uso: java ClienteMain <IP_Diretoria> <Porto_UDP>");
            return;
        }

        try {

            InetAddress dirIp = InetAddress.getByName(args[0]);
            int dirPort = Integer.parseInt(args[1]);

            ClientComunicacao clcom = new ClientComunicacao(dirIp, dirPort);

            String[] serverDetails = clcom.requestPrincipalServer();

            if (serverDetails != null) {
                String serverIp = serverDetails[0];
                int serverTcpPort = Integer.parseInt(serverDetails[1]);

                System.out.println("Servidor Principal encontrado: " + serverIp + ":" + serverTcpPort);

                // TODO: LIGAR AO SERVIDOR PRINCIPAL
            } else {
                System.out.println("Não foi possível obter um servidor principal.");
            }

        } catch (UnknownHostException e) {
            // 2. O código que lida com o erro (tratamento da exceção)
            System.err.println("Erro: O endereço IP/Host '" + args[0] + "' é inválido ou desconhecido.");
            System.exit(1); // Terminar o programa
        } catch (NumberFormatException e) {
            System.err.println("Erro: O porto fornecido ('" + args[1] + "') não é um número válido.");
            System.exit(1);
        }
    }

}
