package pt.isec.pd.tp.Client.ui.gui.vistas;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import pt.isec.pd.tp.Client.logica.ClientManager;
import pt.isec.pd.tp.Client.logica.ClientState;
import pt.isec.pd.tp.Client.ui.InitializableController;
import pt.isec.pd.tp.Utils.Pergunta;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

public class EditarPerguntaPane implements InitializableController {

    private ClientManager clientManager;

    private Pergunta perguntaAtiva;

    private int perguntaId;

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


    public EditarPerguntaPane() {}

    @Override
    public void init(ClientManager clientManager) {
        this.clientManager = clientManager;
        registerHandlers();
    }

    private void registerHandlers() {
        clientManager.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equals(ClientManager.PROP_PERGUNTA_PARA_EDITAR) && evt.getNewValue() != null) {
                this.perguntaAtiva = (Pergunta) evt.getNewValue();
                Platform.runLater(() -> setPergunta(this.perguntaAtiva));
            }
        });
    }

    public void setPergunta(Pergunta pergunta) {
        if (pergunta == null) return;
        this.perguntaId = pergunta.getId();
        taEnunciado.setText(pergunta.getEnunciado());

        optionsContainer.getChildren().clear();
        if (pergunta.getOpcoes() != null) {
            for (String opcao : pergunta.getOpcoes()) {
                optionsContainer.getChildren().add(new TextField(opcao));
            }
        }
        updateSpinner();
        spinnerOpcaoCorreta.getValueFactory().setValue(pergunta.getOpcaoCorreta());

        txtDataInicio.setText(pergunta.getDataInicio());
        txtDataFim.setText(pergunta.getDataFim());
    }

    @FXML
    private void onAddOption() {
        optionsContainer.getChildren().add(new TextField("Opção " + (optionsContainer.getChildren().size() + 1)));
        updateSpinner();
    }

    @FXML
    private void onRemoveOption() {
        if (optionsContainer.getChildren().size() > 2) {
            optionsContainer.getChildren().remove(optionsContainer.getChildren().size() - 1);
            updateSpinner();
        }
    }

    private void updateSpinner() {
        int numOptions = optionsContainer.getChildren().size();
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Math.max(0, numOptions - 1), 0);
        spinnerOpcaoCorreta.setValueFactory(valueFactory);
        if (numOptions > 0 && spinnerOpcaoCorreta.getValue() >= numOptions) {
            spinnerOpcaoCorreta.getValueFactory().setValue(numOptions - 1);
        }
    }

    @FXML
    private void onEdit() {
        String enunciado = taEnunciado.getText();
        List<String> opcoes = optionsContainer.getChildren().stream()
                .filter(node -> node instanceof TextField)
                .map(node -> ((TextField) node).getText())
                .collect(Collectors.toList());
        if (enunciado.isBlank() || opcoes.size() < 2 || opcoes.stream().anyMatch(String::isBlank)) {
            System.out.println("Por favor, preencha o enunciado e forneça pelo menos duas opções válidas.");
            return;
        }

        int opcaoCorreta = spinnerOpcaoCorreta.getValue();

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

        clientManager.editarPergunta(perguntaId, enunciado, opcoes, opcaoCorreta, dataHoraInicio, dataHoraFim);
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
