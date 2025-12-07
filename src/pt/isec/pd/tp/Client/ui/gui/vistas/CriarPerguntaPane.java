package pt.isec.pd.tp.Client.ui.gui.vistas;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import pt.isec.pd.tp.Client.logica.ClientManager;
import pt.isec.pd.tp.Client.logica.ClientState;
import pt.isec.pd.tp.Client.ui.InitializableController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
    @FXML
    private TextField txtDataInicio;
    @FXML
    private TextField txtDataFim;


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
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, numOptions, 1);
        spinnerOpcaoCorreta.setValueFactory(valueFactory);
    }

    @FXML
    private void onCreate() {
        String enunciado = taEnunciado.getText();
        List<String> opcoes = optionsContainer.getChildren().stream()
                .filter(node -> node instanceof TextField)
                .map(node -> ((TextField) node).getText())
                .collect(Collectors.toList());
        if (enunciado.isBlank() || opcoes.size() < 2 || opcoes.stream().anyMatch(String::isBlank)) {
            System.out.println("Por favor, preencha o enunciado e forneça pelo menos duas opções válidas.");
            return;
        }

        int opcaoCorreta = spinnerOpcaoCorreta.getValue() - 1;

        String dataHoraInicio;
        String dataHoraFim;

        try {
            dataHoraInicio = validardata(txtDataInicio, "Data de Início");
            dataHoraFim = validardata(txtDataFim, "Data de Fim");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
            LocalDateTime inicio = LocalDateTime.parse(dataHoraInicio, formatter);
            LocalDateTime fim = LocalDateTime.parse(dataHoraFim, formatter);

            if (inicio.isAfter(fim)) {
                System.out.println("A Data/Hora de Início não pode ser posterior à Data/Hora de Fim.");
                return;
            }

        } catch (IllegalArgumentException e) {
            System.out.println("Campos Incompletos");
            return;
        } catch (DateTimeParseException e) {
            System.out.println("O formato da data e hora deve ser 'YYYY/MM/DD HH:MM'.");
            return;
        }

        clientManager.criarPergunta(enunciado, opcoes, opcaoCorreta, dataHoraInicio, dataHoraFim);
        clientManager.setState(ClientState.DOCENTE_HOME);
    }

    private String validardata(TextField textField, String fieldName)
            throws IllegalArgumentException, DateTimeParseException {

        String dateTimeStr = textField.getText();

        if (dateTimeStr.isBlank()) {
            throw new IllegalArgumentException("O campo " + fieldName + " não pode estar vazio.");
        }

        DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

        LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, formato);

        return dateTime.format(formato);
    }

    @FXML
    private void onBack() {
        clientManager.setState(ClientState.DOCENTE_HOME);
    }
}
