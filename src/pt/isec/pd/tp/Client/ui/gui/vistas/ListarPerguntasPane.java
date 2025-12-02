package pt.isec.pd.tp.Client.ui.gui.vistas;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import pt.isec.pd.tp.Client.logica.ClientManager;
import pt.isec.pd.tp.Client.logica.ClientState;
import pt.isec.pd.tp.Client.ui.InitializableController;
import pt.isec.pd.tp.Client.ui.RootAwareController;
import pt.isec.pd.tp.Utils.Pergunta;

public class ListarPerguntasPane implements InitializableController, RootAwareController {
    private ClientManager clientManager;

    @FXML
    private ListView<Pergunta> listViewPerguntas;

    public ListarPerguntasPane() {

    }

    @Override
    public void init(ClientManager clientManager) {
        this.clientManager = clientManager;
        registerHandlers();
    }

    @Override
    public void setRootNode(Pane rootNode) {
        rootNode.visibleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && clientManager.getUser() != null) { // Check if user is logged in
                clientManager.getPerguntas(); // Request data from manager
            }
        });
    }

    private void registerHandlers() {
        clientManager.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equals(ClientManager.PROP_PERGUNTAS)) {
                Platform.runLater(this::updateListView);
            }
        });
    }

    private void updateListView() {
        listViewPerguntas.getItems().setAll(clientManager.getPerguntasList());
    }

    @FXML
    private void onBack() {
        clientManager.setState(ClientState.DOCENTE_HOME);
    }
}
