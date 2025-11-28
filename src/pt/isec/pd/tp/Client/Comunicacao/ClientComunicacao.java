package pt.isec.pd.tp.Client.Comunicacao;

import pt.isec.pd.tp.Utils.Mensagem;
import java.io.*;
import java.net.*;

public class ClientComunicacao {

    private final InetAddress dirIp;
    private final int dirUDPPort;
    private final int TIMEOUT = 5000;

    // TCP Connection fields
    private Socket socketServidor;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean conectado = false;
    private Thread listenerThread;

    public ClientComunicacao(InetAddress dirIp, int dirUDPPort) {
        this.dirIp = dirIp;
        this.dirUDPPort = dirUDPPort;
    }

    /**
     * Contacta a Diretoria via UDP para obter o IP e Porto do Servidor Principal.
     * Retorna um array {IP, PORTA} ou null em caso de erro.
     */
    public String[] requestPrincipalServer() {
        String request = "REQUEST_SERVER";
        String responseStr;

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT);

            // Enviar o pedido para a Diretoria
            byte[] sendData = request.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, dirIp, dirUDPPort);
            socket.send(sendPacket);
            System.out.println("Enviado pedido para o Serviço de Diretoria em " + dirIp.getHostAddress() + ":" + dirUDPPort);

            // Aguardar resposta
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);

            responseStr = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Resposta da Diretoria: " + responseStr);

            // Tratamento da resposta (Esperado: OK;IP;PORTA)
            String[] parts = responseStr.split(";");

            if (parts.length >= 3 && parts[0].equals("OK")) {
                String serverIp = parts[1];
                String serverTcpPort = parts[2];
                return new String[]{serverIp, serverTcpPort};
            } else {
                System.err.println("Erro ou formato inválido na resposta da Diretoria: " + responseStr);
                return null;
            }

        } catch (SocketTimeoutException e) {
            System.err.println("Timeout: Serviço de diretoria não respondeu.");
            return null;
        } catch (Exception e) {
            System.err.println("Erro na comunicação UDP com Diretoria: " + e.getMessage());
            return null;
        }
    }

    /**
     * Estabelece a ligação TCP com o Servidor Principal e inicia a thread de escuta.
     */
    public boolean ligarAoServidor(String ip, int port) {
        try {
            System.out.println("A tentar ligar ao servidor " + ip + ":" + port + "...");
            socketServidor = new Socket(ip, port);

            // Ordem importante: Criar Output antes do Input para evitar deadlock de streams
            out = new ObjectOutputStream(socketServidor.getOutputStream());
            in = new ObjectInputStream(socketServidor.getInputStream());

            conectado = true;

            // Iniciar thread para ouvir notificações/respostas do servidor continuamente
            listenerThread = new Thread(this::escutarServidor);
            listenerThread.setDaemon(true); // Termina se a main thread terminar
            listenerThread.start();

            System.out.println("Ligação TCP estabelecida com sucesso.");
            return true;

        } catch (IOException e) {
            System.err.println("Falha ao ligar ao servidor TCP: " + e.getMessage());
            return false;
        }
    }

    /**
     * Método executado pela Thread de escuta.
     * Recebe mensagens do servidor e processa-as.
     */
    private void escutarServidor() {
        try {
            while (conectado) {
                // Bloqueia aqui até receber um objeto do servidor
                Mensagem msg = (Mensagem) in.readObject();

                // Encaminhar a mensagem para ser processada (ex: atualizar UI)
                processarMensagemRecebida(msg);
            }
        } catch (SocketException e) {
            // Socket fechado (desconexão normal ou forçada)
            System.out.println("Ligação ao servidor encerrada.");
        } catch (EOFException e) {
            System.out.println("Servidor fechou a ligação.");
        } catch (Exception e) {
            System.err.println("Erro na receção de dados do servidor: " + e.getMessage());
        } finally {
            desligar();
        }
    }

    /**
     * Envia uma mensagem (objeto) para o servidor.
     */
    public void enviarMensagem(Mensagem msg) {
        if (!conectado || out == null) {
            System.err.println("Erro: Não está ligado ao servidor.");
            return;
        }
        try {
            synchronized (out) { // Garantir que threads não escrevem ao mesmo tempo
                out.writeObject(msg);
                out.flush();
                out.reset(); // Evitar problemas de cache de objetos repetidos
            }
        } catch (IOException e) {
            System.err.println("Erro ao enviar mensagem: " + e.getMessage());
            desligar();
        }
    }

    /**
     * Trata as mensagens recebidas (Respostas a comandos ou Notificações).
     * Idealmente, isto deveria chamar métodos na 'ClientVista' ou usar um Observer.
     */
    private void processarMensagemRecebida(Mensagem msg) {
        // TODO: Ligar isto à interface gráfica/consola
        System.out.println("\n[MENSAGEM SERVIDOR] Tipo: " + msg.getTipo());
        if (msg.getPayload() != null) {
            System.out.println("Payload: " + msg.getPayload());
        }

        // Exemplo: Se for um erro de login, o utilizador deve saber
        if(msg.getTipo() == Mensagem.Tipo.LOGIN_FALHOU || msg.getTipo() == Mensagem.Tipo.LOGIN_SUCESSO) {
            // Notificar vista de login...
        }
    }

    /**
     * Fecha a ligação de forma segura.
     */
    public void desligar() {
        conectado = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socketServidor != null && !socketServidor.isClosed()) socketServidor.close();
        } catch (IOException e) {
            System.err.println("Erro ao fechar ligação: " + e.getMessage());
        }
    }

    public boolean isConectado() {
        return conectado;
    }
}