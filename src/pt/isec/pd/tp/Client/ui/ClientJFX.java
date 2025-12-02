package pt.isec.pd.tp.Client.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import pt.isec.pd.tp.Client.Comunicacao.ClientComunicacao;
import pt.isec.pd.tp.Client.logica.ClientManager;

import java.net.InetAddress;

public class ClientJFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        String dirIp = getParameters().getRaw().get(0);
        int dirPort = Integer.parseInt(getParameters().getRaw().get(1));


        ClientComunicacao comunicacaoUdp = new ClientComunicacao(InetAddress.getByName(dirIp), dirPort);
        String[] serverInfo = comunicacaoUdp.requestPrincipalServer();

        if (serverInfo == null) {
            new Alert( Alert.AlertType.ERROR, "Servidor não encontrado");
            javafx.application.Platform.exit();
            return;
        }

        ClientManager manager = new ClientManager();
        manager.start(serverInfo[0], Integer.parseInt(serverInfo[1]));


        MainPane root = new MainPane(manager);
        Scene scene = new Scene(root, 800, 600);

        stage.setTitle("Cliente PD - JavaFX");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
