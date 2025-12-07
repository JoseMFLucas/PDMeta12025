package pt.isec.pd.tp.Client.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.isec.pd.tp.Client.Comunicacao.ClientComunicacao;
import pt.isec.pd.tp.Client.logica.ClientManager;

import java.net.InetAddress;

public class ClientJFX extends Application {

    private static final Logger log = LoggerFactory.getLogger(ClientJFX.class);
    private String dirIp;
    private int dirPort;

    @Override
    public void start(Stage stage) throws Exception {

        dirIp = getParameters().getRaw().get(0);
        dirPort = Integer.parseInt(getParameters().getRaw().get(1));

        ClientComunicacao comunicacaoUdp = new ClientComunicacao(InetAddress.getByName(dirIp), dirPort);
        String[] serverInfo = comunicacaoUdp.requestPrincipalServer();

        if (serverInfo == null) {
            new Alert( Alert.AlertType.ERROR, "Servidor não encontrado");
            javafx.application.Platform.exit();
            return;
        }

        ClientManager manager = new ClientManager();
        manager.start(serverInfo[0], Integer.parseInt(serverInfo[1]), this);


        MainPane root = new MainPane(manager, dirIp , dirPort);
        Scene scene = new Scene(root, 1000, 800);

        stage.setTitle("Cliente PD - JavaFX");
        stage.setScene(scene);
        stage.show();
    }

    public boolean getConection(){
        try{
            ClientComunicacao comunicacaoUdp = new ClientComunicacao(InetAddress.getByName(dirIp), dirPort);
            String[] serverInfo = comunicacaoUdp.requestPrincipalServer();
            if (serverInfo == null) {
                new Alert( Alert.AlertType.ERROR, "Servidor não encontrado");
                return false;
            }
            ClientManager manager = new ClientManager();
            System.out.println("Client Reconectado ao Servidor Principal");
            manager.start(serverInfo[0], Integer.parseInt(serverInfo[1]), this);
        }
        catch (Exception e){
            log.error("Error: ", e);
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
