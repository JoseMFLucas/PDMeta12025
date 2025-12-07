package pt.isec.pd.tp.Client.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import pt.isec.pd.tp.Client.logica.ClientManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainPane extends StackPane {
    private final ClientManager clientManager;
    private final List<Node> views = new ArrayList<>();
    private final String dirip;
    private final int dirport;


    public MainPane(ClientManager clientManager, String dirIp, int dirPort) {
        this.dirip = dirIp;
        this.dirport = dirPort;
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
        views.add(loadFXML("../ui/gui/fxml/VisualizarPerguntaPane.fxml"));
        views.add(loadFXML("../ui/gui/fxml/ConsultarPerguntaPane.fxml"));
        views.add(loadFXML("../ui/gui/fxml/EditarPerguntaPane.fxml"));
        views.add(loadFXML("../ui/gui/fxml/EditarPerfilDocente.fxml"));
        views.add(loadFXML("../ui/gui/fxml/EditarPerfilEstudante.fxml"));
        views.add(loadFXML("../ui/gui/fxml/ListarPerguntasEstudantePane.fxml"));


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
            System.err.println("Failed to load FXML file: " + fxmlPath);
            e.printStackTrace();
            throw new RuntimeException("Failed to load FXML file: " + fxmlPath, e);
        }
    }

    private void registerHandlers() {
        clientManager.addPropertyChangeListener(evt -> {
            Platform.runLater(() -> {
                if (evt.getPropertyName().equals(ClientManager.PROP_STATE)) {
                    update();
                } else if (evt.getPropertyName().equals(ClientManager.PROP_MSG_SUCESSO) || evt.getPropertyName().equals(ClientManager.PROP_MSG_ERRO)) {
                    showAlert(evt.getPropertyName(), (String) evt.getNewValue());
                } else if (evt.getPropertyName().equals(ClientManager.PROP_CLOSE_APP)) {

                    Stage stage = (Stage) this.getScene().getWindow();
                    stage.close();
                }
            });
        });
    }

    private void showAlert(String propertyName, String message) {
        Alert.AlertType alertType = propertyName.equals(ClientManager.PROP_MSG_SUCESSO) ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR;
        Alert alert = new Alert(alertType);
        alert.setTitle(alertType == Alert.AlertType.INFORMATION ? "Sucesso" : "Erro");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    private void update() {
        this.getChildren().forEach(node -> node.setVisible(false));
        switch (clientManager.getState()) {
            case LOGIN -> views.get(0).setVisible(true);
            case REGISTER -> views.get(1).setVisible(true);
            case DOCENTE_HOME -> views.get(2).setVisible(true);
            case ESTUDANTE_HOME -> views.get(3).setVisible(true);
            case CRIAR_PERGUNTA -> views.get(4).setVisible(true);
            case LISTAR_PERGUNTAS -> {
                if (clientManager.getUser() != null) {
                    clientManager.getPerguntas();
                }
                views.get(5).setVisible(true);
            }
            case RESPONDER_PERGUNTA -> views.get(6).setVisible(true);
            case DETALHES_PERGUNTA -> views.get(7).setVisible(true);
            case CONSULTAR_PERGUNTA -> views.get(8).setVisible(true);
            case EDITAR_PERGUNTA -> views.get(9).setVisible(true);
            case EDITAR_PERFIL_DOCENTE -> views.get(10).setVisible(true);
            case EDITAR_PERFIL_ESTUDANTE -> views.get(11).setVisible(true);
            case LISTA_PERGUNTAS_RESPONDIDAS -> views.get(12).setVisible(true);
        }
    }
}
