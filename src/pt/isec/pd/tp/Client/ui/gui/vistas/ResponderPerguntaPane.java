package pt.isec.pd.tp.Client.ui.gui.vistas;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import pt.isec.pd.tp.Client.logica.ClientManager;
import pt.isec.pd.tp.Client.logica.ClientState;
import pt.isec.pd.tp.Client.ui.InitializableController;
import pt.isec.pd.tp.Utils.Pergunta;

public class ResponderPerguntaPane implements InitializableController {
    private ClientManager clientManager;
    private Pergunta perguntaAtiva;

    @FXML
    private TextField txtCodigo;
    @FXML
    private Button btnProcurar;
    @FXML
    private VBox questionBox;
    @FXML
    private Label lblEnunciado;
    @FXML
    private VBox optionsDisplayContainer;
    @FXML
    private ChoiceBox<String> choiceBoxRespostas;
    @FXML
    private Button btnSubmeter;

    public ResponderPerguntaPane() {}

    @Override
    public void init(ClientManager clientManager) {
        this.clientManager = clientManager;
        registerHandlers();
        updateView();
    }

    private void registerHandlers() {
        clientManager.addPropertyChangeListener(evt -> {
            String propertyName = evt.getPropertyName();
            if (propertyName.equals(ClientManager.PROP_STATE) && clientManager.getState() == ClientState.RESPONDER_PERGUNTA) {
                Platform.runLater(this::updateView);
            } else if (propertyName.equals(ClientManager.PROP_PERGUNTA_ENCONTRADA_ESTUDANTE)) {
                this.perguntaAtiva = (Pergunta) evt.getNewValue();
                Platform.runLater(this::displayPergunta);
            } else if (propertyName.equals(ClientManager.PROP_MSG_SUCESSO)) {
                Platform.runLater(this::updateView);
            }
        });
    }

    private void updateView() {
        txtCodigo.clear();
        questionBox.setVisible(false);
        questionBox.setManaged(false);
        btnSubmeter.setDisable(true);
        if (perguntaAtiva != null) {
            perguntaAtiva = null;
        }
    }

    private void displayPergunta() {
        if (perguntaAtiva != null) {
            questionBox.setVisible(true);
            questionBox.setManaged(true);
            lblEnunciado.setText(perguntaAtiva.getEnunciado());
            optionsDisplayContainer.getChildren().clear();
            choiceBoxRespostas.getItems().clear();

            String[] opcoes = perguntaAtiva.getOpcoes();
            if (opcoes != null) {
                for (int i = 0; i < opcoes.length; i++) {
                    Label lblOpcao = new Label((i + 1) + ": " + opcoes[i]);
                    lblOpcao.setStyle("-fx-font-weight: bold; -fx-text-fill: #28a745; -fx-font-size: 16px;");
                    optionsDisplayContainer.getChildren().add(lblOpcao);
                    choiceBoxRespostas.getItems().add("Opção " + (i + 1));
                }
            }
            btnSubmeter.setDisable(false);
        }
    }

    @FXML
    private void onProcurar() {
        String codigo = txtCodigo.getText();
        if (codigo != null && !codigo.trim().isEmpty()) {
            clientManager.procurarPerguntaEstudante(codigo);
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Por favor, insira um código de acesso.");
            alert.setHeaderText(null);
            alert.showAndWait();
        }
    }

    @FXML
    private void onSubmeter() {
        int selectedIndex = choiceBoxRespostas.getSelectionModel().getSelectedIndex();
        if (perguntaAtiva != null && selectedIndex != -1) {
            clientManager.submeterResposta(perguntaAtiva.getId(), selectedIndex);
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Não foi possível submeter a resposta. Verifique se selecionou uma opção.");
            alert.setHeaderText(null);
            alert.showAndWait();
        }
    }

    @FXML
    private void onBack() {
        clientManager.setState(ClientState.ESTUDANTE_HOME);
    }
}
