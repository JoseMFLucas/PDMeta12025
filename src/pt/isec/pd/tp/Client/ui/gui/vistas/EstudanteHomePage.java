package pt.isec.pd.tp.Client.ui.gui.vistas;

import javafx.fxml.FXML;
import pt.isec.pd.tp.Client.logica.ClientManager;
import pt.isec.pd.tp.Client.logica.ClientState;
import pt.isec.pd.tp.Client.ui.InitializableController;

public class EstudanteHomePage implements InitializableController {
    private ClientManager clientManager;

    public EstudanteHomePage() {

    }

    @Override
    public void init(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @FXML
    private void onEditarPerfilEstudante(){
        clientManager.setState(ClientState.EDITAR_PERFIL_ESTUDANTE);
    }

    @FXML
    private void onResponderPergunta() {
        clientManager.setState(ClientState.RESPONDER_PERGUNTA);
    }

    @FXML
    private void onLogout() {
        clientManager.logout();
    }

    @FXML
    private void onListarPerguntasRespondidas() {
        clientManager.getRespostas();
    }
}
