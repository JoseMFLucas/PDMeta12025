package pt.isec.pd.tp.Client.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import pt.isec.pd.tp.Client.logica.ClientManager;
import pt.isec.pd.tp.Client.logica.ClientState;
import pt.isec.pd.tp.Client.ui.gui.vistas.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainPane extends StackPane {
    private final ClientManager clientManager;
    private final List<Node> views = new ArrayList<>();

    public MainPane(ClientManager clientManager) {
        this.clientManager = clientManager;
        createViews();
        registerHandlers();
        update();
    }

    private void createViews() {
        views.add(loadFXML("../ui/gui/fxml/LoginPage.fxml"));
        views.add(loadFXML("../ui/gui/fxml/RegisterPage.fxml"));
        views.add(loadFXML("../ui/gui/fxml/DocenteHomePage.fxml"));
        views.add(loadFXML("../ui/gui/fxml/EstudanteHomePage.fxml"));
        views.add(loadFXML("../ui/gui/fxml/CriarPerguntaPane.fxml"));
        views.add(loadFXML("../ui/gui/fxml/ListarPerguntasPane.fxml"));
        views.add(loadFXML("../ui/gui/fxml/ResponderPerguntaPane.fxml"));

        this.getChildren().addAll(views);
    }

    private Node loadFXML(String fxmlPath) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlPath));
            Pane view = fxmlLoader.load();
            Object controller = fxmlLoader.getController();

            if (controller instanceof InitializableController) {
                ((InitializableController) controller).init(clientManager);
            }
            if (controller instanceof RootAwareController) {
                ((RootAwareController) controller).setRootNode(view);
            }
            return view;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML file: " + fxmlPath, e);
        }
    }

    private void registerHandlers() {
        clientManager.addPropertyChangeListener(evt -> {
            Platform.runLater(() -> {
                if (evt.getPropertyName().equals(ClientManager.PROP_STATE)) {
                    update();
                } else if (evt.getPropertyName().equals(ClientManager.PROP_CLOSE_APP)) {
                    Stage stage = (Stage) this.getScene().getWindow();
                    stage.close();
                }
            });
        });
    }

    private void update() {
        this.getChildren().forEach(node -> node.setVisible(false));
        switch (clientManager.getState()) {
            case LOGIN -> views.get(0).setVisible(true);
            case REGISTER -> views.get(1).setVisible(true);
            case DOCENTE_HOME -> views.get(2).setVisible(true);
            case ESTUDANTE_HOME -> views.get(3).setVisible(true);
            case CRIAR_PERGUNTA -> views.get(4).setVisible(true);
            case LISTAR_PERGUNTAS -> views.get(5).setVisible(true);
            case RESPONDER_PERGUNTA -> views.get(6).setVisible(true);
        }
    }
}
