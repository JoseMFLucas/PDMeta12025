package pt.isec.pd.tp.Client.ui.gui.vistas;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import pt.isec.pd.tp.Client.logica.ClientManager;
import pt.isec.pd.tp.Client.logica.ClientState;
import pt.isec.pd.tp.Client.ui.InitializableController;
import pt.isec.pd.tp.Utils.Resposta;

import java.util.List;

public class ListarPerguntasEstudantePane implements InitializableController {
    private ClientManager clientManager;

    @FXML
    private TableView<Resposta> tableViewPerguntas;
    @FXML
    private TableColumn<Resposta, String> colEnunciado;
    @FXML
    private TableColumn<Resposta, String> colData;
    @FXML
    private TableColumn<Resposta, String> colOpcao;
    @FXML
    private TableColumn<Resposta, String> colResultado;

    @Override
    public void init(ClientManager clientManager) {
        this.clientManager = clientManager;
        initializeTableView();
        registerHandlers();
        clientManager.getRespostas();
    }

    private void initializeTableView() {
        colEnunciado.setCellValueFactory(new PropertyValueFactory<>("enunciado"));
        colData.setCellValueFactory(new PropertyValueFactory<>("data"));
        colOpcao.setCellValueFactory(new PropertyValueFactory<>("opcao"));
        colResultado.setCellValueFactory(new PropertyValueFactory<>("resultado"));
    }

    private void registerHandlers() {
        clientManager.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equals(ClientManager.PROP_RESPOSTAS)) {
                Platform.runLater(this::updateTableView);
            } else if (evt.getPropertyName().equals(ClientManager.PROP_STATE) && clientManager.getState() == ClientState.LISTA_PERGUNTAS_RESPONDIDAS) {
                Platform.runLater(this::updateTableView);
            }
        });
    }

    private void updateTableView() {
        List<Resposta> respostas = clientManager.getRespostasList();
        if (respostas == null) return;
        tableViewPerguntas.setItems(FXCollections.observableArrayList(respostas));
    }

    @FXML
    private void onBack() {
        clientManager.setState(ClientState.ESTUDANTE_HOME);
    }
}
