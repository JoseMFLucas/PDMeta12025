package pt.isec.pd.tp.Client.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pt.isec.pd.tp.Client.Comunicacao.ClientComunicacao;
import pt.isec.pd.tp.Client.logica.ClientManager;

import java.net.InetAddress;

public class ClientJFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // 1. Obter IP/Porto da Diretoria (Argumentos ou hardcoded para teste)
        String dirIp = getParameters().getRaw().get(0);
        int dirPort = Integer.parseInt(getParameters().getRaw().get(1));


        // 2. Iniciar Comunicação UDP para achar servidor
        ClientComunicacao comunicacaoUdp = new ClientComunicacao(InetAddress.getByName(dirIp), dirPort);
        String[] serverInfo = comunicacaoUdp.requestPrincipalServer();

        if (serverInfo == null) {
            System.out.println("Servidor não encontrado.");
            // In a real GUI app, you'd show an alert here.
            javafx.application.Platform.exit();
            return;
        }

        // 3. Iniciar Manager e ligar TCP
        ClientManager manager = new ClientManager();
        manager.start(serverInfo[0], Integer.parseInt(serverInfo[1]));

        // 4. Iniciar GUI
        MainPane root = new MainPane(manager);
        Scene scene = new Scene(root, 800, 600);
        // scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

        stage.setTitle("Cliente PD - JavaFX");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
