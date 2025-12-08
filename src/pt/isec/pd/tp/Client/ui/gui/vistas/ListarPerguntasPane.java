package pt.isec.pd.tp.Client.ui.gui.vistas;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import pt.isec.pd.tp.Client.logica.ClientManager;
import pt.isec.pd.tp.Client.logica.ClientState;
import pt.isec.pd.tp.Client.ui.InitializableController;
import pt.isec.pd.tp.Utils.Pergunta;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

public class ListarPerguntasPane implements InitializableController {
    private ClientManager clientManager;

    @FXML
    private ChoiceBox<String> choiceBoxFiltro;
    @FXML
    private TableView<Pergunta> tableViewPerguntas;
    @FXML
    private TableColumn<Pergunta, Integer> colId;
    @FXML
    private TableColumn<Pergunta, String> colEnunciado;
    @FXML
    private TableColumn<Pergunta, String> colCodigo;
    @FXML
    private TableColumn<Pergunta, String> colInicio;
    @FXML
    private TableColumn<Pergunta, String> colFim;
    @FXML
    private TableColumn<Pergunta, Integer> colRespostas;
    @FXML
    private TableColumn<Pergunta, Double> colPercentagemCertas;
    @FXML
    private TableColumn<Pergunta, Void> colAcoes;

    @Override
    public void init(ClientManager clientManager) {
        this.clientManager = clientManager;
        initializeChoiceBox();
        initializeTableView();
        registerHandlers();
    }

    private void initializeChoiceBox() {
        choiceBoxFiltro.setItems(FXCollections.observableArrayList("Todas", "Ativas", "Futuras", "Expiradas"));
        choiceBoxFiltro.setValue("Todas");
        choiceBoxFiltro.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.equals(oldVal)) {
                return;
            }
            if ("Expiradas".equals(newVal)) {
                clientManager.getEstatisticasPerguntas();
            } else {
                clientManager.getPerguntas();
            }
        });
    }

    private void initializeTableView() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEnunciado.setCellValueFactory(new PropertyValueFactory<>("enunciado"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colInicio.setCellValueFactory(new PropertyValueFactory<>("dataInicio"));
        colFim.setCellValueFactory(new PropertyValueFactory<>("dataFim"));
        colRespostas.setCellValueFactory(new PropertyValueFactory<>("totalRespostas"));
        colPercentagemCertas.setCellValueFactory(new PropertyValueFactory<>("percentagemCertas"));
        addButtonToTable();
    }

    private void addButtonToTable() {
        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button viewButton = new Button("View");

            {
                viewButton.setOnAction(event -> {
                    Pergunta pergunta = getTableView().getItems().get(getIndex());
                    clientManager.procurarPergunta(String.valueOf(pergunta.getId()));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(viewButton);
                }
            }
        });
    }

    private void registerHandlers() {
        clientManager.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equals(ClientManager.PROP_PERGUNTAS)) {
                Platform.runLater(this::updateTableView);
            } else if (evt.getPropertyName().equals(ClientManager.PROP_STATE) && clientManager.getState() == ClientState.LISTAR_PERGUNTAS) {
                Platform.runLater(() -> {
                    clientManager.getPerguntas();
                    choiceBoxFiltro.setValue("Todas");
                });
            }
        });
    }

    private void updateTableView() {
        List<Pergunta> perguntas = clientManager.getPerguntasList();
        if (perguntas == null) return;
        String filtro = choiceBoxFiltro.getValue();

        List<Pergunta> filteredList = perguntas.stream()
                .filter(p -> {
                    switch (filtro) {
                        case "Ativas":
                            return isAtiva(p);
                        case "Futuras":
                            return isFutura(p);
                        case "Expiradas":
                            return isExpirada(p);
                        default: // "Todas"
                            return true;
                    }
                })
                .collect(Collectors.toList());

        tableViewPerguntas.setItems(FXCollections.observableArrayList(filteredList));

        boolean showExtraColumns = "Expiradas".equals(filtro);
        colRespostas.setVisible(showExtraColumns);
        colPercentagemCertas.setVisible(showExtraColumns);
    }

    private boolean isAtiva(Pergunta p) {
        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
            LocalDateTime inicio = LocalDateTime.parse(p.getDataInicio(), formatter);
            LocalDateTime fim = LocalDateTime.parse(p.getDataFim(), formatter);
            return now.isAfter(inicio) && now.isBefore(fim);
        } catch (NullPointerException | DateTimeParseException e) {
            System.err.println("Could not parse date for Pergunta " + p.getId() + ": " + e.getMessage());
            return false;
        }
    }

    private boolean isFutura(Pergunta p) {
        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
            LocalDateTime inicio = LocalDateTime.parse(p.getDataInicio(), formatter);
            return now.isBefore(inicio);
        } catch (NullPointerException | DateTimeParseException e) {
            System.err.println("Could not parse date for Pergunta " + p.getId() + ": " + e.getMessage());
            return false;
        }
    }

    private boolean isExpirada(Pergunta p) {
        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
            LocalDateTime fim = LocalDateTime.parse(p.getDataFim(), formatter);
            return now.isAfter(fim);
        } catch (NullPointerException | DateTimeParseException e) {
            System.err.println("Could not parse date for Pergunta " + p.getId() + ": " + e.getMessage());
            return false;
        }
    }

    @FXML
    private void onBack() {
        clientManager.setState(ClientState.DOCENTE_HOME);
    }
}
