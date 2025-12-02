package pt.isec.pd.tp.Client.ui.gui.vistas;

import javafx.fxml.FXML;
import pt.isec.pd.tp.Client.logica.ClientManager;
import pt.isec.pd.tp.Client.logica.ClientState;
import pt.isec.pd.tp.Client.ui.InitializableController;

public class DocenteHomePage implements InitializableController {
    private ClientManager clientManager;

    public DocenteHomePage() {}

    @Override
    public void init(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @FXML
    private void onCriarPergunta() {
        clientManager.setState(ClientState.CRIAR_PERGUNTA);
    }

    @FXML
    private void onListarPerguntas() {
        clientManager.setState(ClientState.LISTAR_PERGUNTAS);
    }

    @FXML
    private void onEditarPerfilDocente(){
        //TODO
    }
}
