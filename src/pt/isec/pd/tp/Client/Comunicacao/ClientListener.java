package pt.isec.pd.tp.Client.Comunicacao;

import pt.isec.pd.tp.Client.Vista.ClientVista;
import pt.isec.pd.tp.Utils.Mensagem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ClientListener implements Runnable {
    private final ObjectInputStream in;
    private final ClientVista vista;
    private volatile boolean running = true;
    private final BlockingQueue<Mensagem> responseQueue = new LinkedBlockingQueue<>();

    public ClientListener(ObjectInputStream in, ClientVista vista) {
        this.in = in;
        this.vista = vista;
    }

    public void stopRunning() {
        running = false;
    }

    public Mensagem getResponse() throws InterruptedException {
        // Usa um timeout para evitar que o cliente fique bloqueado indefinidamente
        return responseQueue.poll(15, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        while (running) {
            try {
                // O listener é o único que lê do stream
                Mensagem response = (Mensagem) in.readObject();
                processarMensagem(response);
            } catch (IOException e) {
                if (running) {
                    vista.mostrarErro("Erro de leitura na thread de escuta: " + e.getMessage());
                }
                running = false;
            } catch (ClassNotFoundException e) {
                if (running) {
                    vista.mostrarErro("Objeto recebido desconhecido: " + e.getMessage());
                }
            } catch (InterruptedException e) {
                running = false;
                Thread.currentThread().interrupt();
            }
        }
        vista.mostrarInfo("Thread de escuta terminada.");
    }

    private void processarMensagem(Mensagem msg) throws InterruptedException {
        if (msg == null) return;

        // Distingue entre respostas diretas e notificações assíncronas
        switch (msg.getTipo()) {
            case NOTIFICACAO_ASSINCRONA:
                // As notificações são mostradas diretamente na vista
                vista.mostrarAviso("\n[NOTIFICAÇÃO] " + msg.getPayload() + "\n");
                break;
            default:
                // Outras mensagens são respostas a pedidos e vão para a fila
                responseQueue.put(msg);
                break;
        }
    }
}
