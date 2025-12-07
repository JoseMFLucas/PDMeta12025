package pt.isec.pd.tp.Client.ui.gui.vistas;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import pt.isec.pd.tp.Client.logica.ClientManager;
import pt.isec.pd.tp.Client.logica.ClientState;
import pt.isec.pd.tp.Client.ui.InitializableController;

public class ConsultarPerguntaPane implements InitializableController {
    @FXML
    private TextField txtIdPergunta;

    private ClientManager clientManager;

    public ConsultarPerguntaPane() {}

    @Override
    public void init(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @FXML
    private void onConsultar() {
        String idText = txtIdPergunta.getText();
        if (idText != null && !idText.trim().isEmpty()) {
            clientManager.procurarPergunta(idText.trim());
        }
    }

    @FXML
    private void onBack() {
        clientManager.setState(ClientState.DOCENTE_HOME);
    }
}
