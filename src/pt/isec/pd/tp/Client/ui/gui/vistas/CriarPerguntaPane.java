package pt.isec.pd.tp.Client.ui.gui.vistas;

import javafx.fxml.FXML;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import pt.isec.pd.tp.Client.logica.ClientManager;
import pt.isec.pd.tp.Client.logica.ClientState;
import pt.isec.pd.tp.Client.ui.InitializableController;

import java.util.List;
import java.util.stream.Collectors;

public class CriarPerguntaPane implements InitializableController {
    private ClientManager clientManager;

    @FXML
    private TextArea taEnunciado;
    @FXML
    private VBox optionsContainer;
    @FXML
    private Spinner<Integer> spinnerOpcaoCorreta;

    public CriarPerguntaPane() {}

    @Override
    public void init(ClientManager clientManager) {
        this.clientManager = clientManager;
        // Add two initial options for convenience
        onAddOption();
        onAddOption();
    }

    @FXML
    private void onAddOption() {
        optionsContainer.getChildren().add(new TextField("Opção " + (optionsContainer.getChildren().size() + 1)));
        updateSpinner();
    }

    @FXML
    private void onRemoveOption() {
        if (optionsContainer.getChildren().size() > 2) { // Keep at least 2 options
            optionsContainer.getChildren().remove(optionsContainer.getChildren().size() - 1);
            updateSpinner();
        }
    }

    private void updateSpinner() {
        int numOptions = optionsContainer.getChildren().size();
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Math.max(0, numOptions - 1), 0);
        spinnerOpcaoCorreta.setValueFactory(valueFactory);
    }

    @FXML
    private void onCreate() {
        String enunciado = taEnunciado.getText();
        List<String> opcoes = optionsContainer.getChildren().stream()
                .filter(node -> node instanceof TextField)
                .map(node -> ((TextField) node).getText())
                .collect(Collectors.toList());
        int opcaoCorreta = spinnerOpcaoCorreta.getValue();

        if (!enunciado.isBlank() && opcoes.size() >= 2 && !opcoes.stream().anyMatch(String::isBlank)) {
            clientManager.criarPergunta(enunciado, opcoes, opcaoCorreta);
            clientManager.setState(ClientState.DOCENTE_HOME);
        } else {
            System.out.println("Por favor, preencha todos os campos.");
        }
    }

    @FXML
    private void onBack() {
        clientManager.setState(ClientState.DOCENTE_HOME);
    }
}
