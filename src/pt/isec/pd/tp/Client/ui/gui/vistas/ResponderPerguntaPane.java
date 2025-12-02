package pt.isec.pd.tp.Client.ui.gui.vistas;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import pt.isec.pd.tp.Client.logica.ClientManager;
import pt.isec.pd.tp.Client.logica.ClientState;
import pt.isec.pd.tp.Client.ui.InitializableController;
import pt.isec.pd.tp.Utils.Pergunta;

public class ResponderPerguntaPane implements InitializableController {
    private ClientManager clientManager;

    @FXML
    private TextField tfCodigo;
    @FXML
    private VBox perguntaContainer;
    @FXML
    private Label lblEnunciado;
    @FXML
    private VBox opcoesRespostaContainer;

    private ToggleGroup answerToggleGroup;
    private Pergunta perguntaAtual;

    public ResponderPerguntaPane() {}

    @Override
    public void init(ClientManager clientManager) {
        this.clientManager = clientManager;
        registerHandlers();
    }

    private void registerHandlers() {
        clientManager.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equals(ClientManager.PROP_PERGUNTA_ENCONTRADA) && evt.getNewValue() != null) {
                Platform.runLater(() -> showPergunta((Pergunta) evt.getNewValue()));
            }
        });
    }

    @FXML
    private void onProcurar() {
        String codigo = tfCodigo.getText();
        if (!codigo.isBlank()) {
            clientManager.procurarPergunta(codigo);
        }
    }

    public void showPergunta(Pergunta pergunta) {
        this.perguntaAtual = pergunta;
        lblEnunciado.setText(pergunta.getEnunciado());
        opcoesRespostaContainer.getChildren().clear();
        answerToggleGroup = new ToggleGroup();

        for (String opcao : pergunta.getOpcoes()) {
            RadioButton rb = new RadioButton(opcao);
            rb.setToggleGroup(answerToggleGroup);
            opcoesRespostaContainer.getChildren().add(rb);
        }

        perguntaContainer.setVisible(true);
        perguntaContainer.setManaged(true);
    }

    @FXML
    private void onSubmit() {
        if (answerToggleGroup.getSelectedToggle() != null) {
            int selectedIndex = answerToggleGroup.getToggles().indexOf(answerToggleGroup.getSelectedToggle());
            clientManager.submeterResposta(perguntaAtual.getId(), selectedIndex);
            resetView();
        } else {
            System.out.println("Nenhuma resposta selecionada.");
        }
    }

    @FXML
    private void onBack() {
        resetView();
        clientManager.setState(ClientState.ESTUDANTE_HOME);
    }

    private void resetView() {
        tfCodigo.clear();
        perguntaContainer.setVisible(false);
        perguntaContainer.setManaged(false);
        opcoesRespostaContainer.getChildren().clear();
        perguntaAtual = null;
    }
}
