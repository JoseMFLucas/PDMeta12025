package pt.isec.pd.tp.Client.ui.gui.vistas;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import pt.isec.pd.tp.Client.logica.ClientManager;
import pt.isec.pd.tp.Client.logica.ClientState;
import pt.isec.pd.tp.Client.ui.InitializableController;
import pt.isec.pd.tp.Utils.Pergunta;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static pt.isec.pd.tp.Utils.Configs.Pasta_CSV;
import static pt.isec.pd.tp.Utils.Configs.Pasta_CSV_Vista;

public class VisualizarPerguntaPane implements InitializableController {
    private ClientManager clientManager;
    private Pergunta perguntaAtiva;

    private List<String[]> lista;

    @FXML
    private Label lblCodigo;
    @FXML
    private Label lblEnunciado;
    @FXML
    private VBox optionsDisplayContainer;
    @FXML
    private Label lblOpcaoCorreta;
    @FXML
    private Label lblDataInicio;
    @FXML
    private Label lblDataFim;
    @FXML
    private Label lblRespostas;
    @FXML
    private Label lblPercentagemCertas;
    @FXML
    private HBox boxEstatisticas;
    @FXML
    private Button btnVerResultados;
    @FXML
    private Button btnExportarCsv;
    @FXML
    private TableView<RespostaData> tableViewRespostas;
    @FXML
    private TableColumn<RespostaData, String> colNrEstudante;
    @FXML
    private TableColumn<RespostaData, String> colNome;
    @FXML
    private TableColumn<RespostaData, String> colEmail;
    @FXML
    private TableColumn<RespostaData, String> colResposta;
    @FXML
    private TableColumn<RespostaData, String> colDataHora;

    public VisualizarPerguntaPane() {}

    @Override
    public void init(ClientManager clientManager) {
        this.clientManager = clientManager;
        registerHandlers();
        setupTable();
    }

    private void setupTable() {
        colNrEstudante.setCellValueFactory(cellData -> cellData.getValue().nrEstudanteProperty());
        colNome.setCellValueFactory(cellData -> cellData.getValue().nomeProperty());
        colEmail.setCellValueFactory(cellData -> cellData.getValue().emailProperty());
        colResposta.setCellValueFactory(cellData -> cellData.getValue().respostaProperty());
        colDataHora.setCellValueFactory(cellData -> cellData.getValue().dataHoraProperty());

        String style = "-fx-alignment: CENTER;";

        colNrEstudante.setStyle(style);
        colNome.setStyle(style);
        colEmail.setStyle(style);
        colResposta.setStyle(style);
        colDataHora.setStyle(style);

        tableViewRespostas.setPlaceholder(new Label("Não existem respostas para esta pergunta."));

        tableViewRespostas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void registerHandlers() {
        clientManager.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equals(ClientManager.PROP_PERGUNTA_ENCONTRADA) && evt.getNewValue() != null && clientManager.getState() == ClientState.DETALHES_PERGUNTA) {
                this.perguntaAtiva = (Pergunta) evt.getNewValue();
                Platform.runLater(() -> showPergunta(perguntaAtiva));
            } else if (evt.getPropertyName().equals(ClientManager.PROP_ESTATISTICAS_PERGUNTA)) {
                this.lista = (List<String[]>) evt.getNewValue();
                List<String[]> estatisticas = (List<String[]>) evt.getNewValue();
                Platform.runLater(() -> mostrarResultados(estatisticas));
            }
        });
    }

    public void showPergunta(Pergunta pergunta) {
        if (pergunta != null) {
            lblCodigo.setText(pergunta.getCodigo());
            lblEnunciado.setText(pergunta.getEnunciado());
            optionsDisplayContainer.getChildren().clear();

            String[] opcoes = pergunta.getOpcoes();
            if (opcoes != null) {
                for (int i = 0; i < opcoes.length; i++) {
                    Label lblOpcao = new Label((i + 1) + ": " + opcoes[i]);
                    lblOpcao.setStyle("-fx-font-weight: bold; -fx-text-fill: #28a745; -fx-font-size: 16px;");
                    optionsDisplayContainer.getChildren().add(lblOpcao);
                }
            }

            int correctOptionIndex = pergunta.getOpcaoCorreta() + 1;
            if (opcoes != null && correctOptionIndex > 0 && correctOptionIndex <= opcoes.length) {
                lblOpcaoCorreta.setText(opcoes[correctOptionIndex - 1] + " (Índice: " + correctOptionIndex + ")");
            } else {
                lblOpcaoCorreta.setText("N/A");
            }

            lblDataInicio.setText(pergunta.getDataInicio());
            lblDataFim.setText(pergunta.getDataFim());

            // Reset and hide results view for the new question
            boxEstatisticas.setVisible(false);
            boxEstatisticas.setManaged(false);
            tableViewRespostas.setItems(FXCollections.observableArrayList()); // Clear table data
            tableViewRespostas.setVisible(false);
            tableViewRespostas.setManaged(false);
            btnVerResultados.setVisible(false);
            btnVerResultados.setManaged(false);
            btnExportarCsv.setVisible(false);
            btnExportarCsv.setManaged(false);

            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
                LocalDateTime dataFim = LocalDateTime.parse(pergunta.getDataFim(), formatter);
                if (LocalDateTime.now().isAfter(dataFim)) {
                    btnVerResultados.setVisible(true);
                    btnVerResultados.setManaged(true);
                }
            } catch (Exception e) {
                System.err.println("Erro ao fazer parse da data de fim: '" + pergunta.getDataFim() + "'");
            }
        }
    }

    private void mostrarResultados( List<String[]> estatisticas) {
        int j = 0;
        for (String[] arrayEstatistica : estatisticas) {
            System.out.println("--- Array (Índice " + j++ + ") ---");

            // Loop para iterar sobre os elementos (String) dentro do array
            for (String elemento : arrayEstatistica) {
                System.out.println("Elemento: " + elemento);
            }
            System.out.println("--------------------");
        }

        btnExportarCsv.setVisible(true);
        btnExportarCsv.setManaged(true);
        boxEstatisticas.setVisible(true);
        boxEstatisticas.setManaged(true);
        tableViewRespostas.setVisible(true);
        tableViewRespostas.setManaged(true);

        if (estatisticas == null || estatisticas.size() < 3) {
            lblPercentagemCertas.setText("0%");
            lblRespostas.setText("0");
            tableViewRespostas.setItems(FXCollections.observableArrayList());
        } else {
            lblPercentagemCertas.setText(estatisticas.get(1)[0]);
            lblRespostas.setText(String.valueOf(estatisticas.size() - 2));

            ObservableList<RespostaData> data = FXCollections.observableArrayList();
            for (int i = 2; i < estatisticas.size(); i++) {
                String[] resposta = estatisticas.get(i);
                if (resposta.length >= 4) { // Ensure we have the minimum expected data
                    String dataHora = (resposta.length > 4) ? resposta[4] : ""; // Safely get optional dataHora
                    data.add(new RespostaData(resposta[0], resposta[1], resposta[2], resposta[3], dataHora));
                }
            }
            tableViewRespostas.setItems(data);
        }
    }

    @FXML
    private void onVerResultados() {
        if (perguntaAtiva != null) {
            clientManager.verEstatisticas(perguntaAtiva.getId());
        }
    }

    @FXML
    private void onExportarCsv() {
        String filenameRaw = "pergunta_" + perguntaAtiva.getId() + ".csv";
        String filename;

        if (!filenameRaw.toLowerCase().endsWith(".csv")) {
            filename = filenameRaw + ".csv";
        } else {
            filename = filenameRaw;
        }

        // Usar um caminho absoluto para garantir que a pasta é criada no local esperado
        File projectDir = new File(System.getProperty("user.dir"));
        File csvDir = new File(projectDir, "csv");


        if (!csvDir.exists()) {
            if (csvDir.mkdirs()) {
                System.out.println("Pasta 'csv' criada com sucesso em: " + csvDir.getAbsolutePath());
            } else {
                System.out.println("ERRO: Não foi possível criar a pasta 'csv'. A exportação não será efetuada no local desejado.");
                return;
            }
        }

        File finalFile = new File(csvDir, filename);
        String fullPath = finalFile.getAbsolutePath();

        try (FileWriter writer = new FileWriter(finalFile)) {
            String enunciado = perguntaAtiva.getEnunciado();
            String opcoesStr = Arrays.toString(perguntaAtiva.getOpcoes());
            int opcaoCorretaIdx = perguntaAtiva.getOpcaoCorreta();

            String[] inicioParts = perguntaAtiva.getDataInicio().split(" ");
            String[] fimParts = perguntaAtiva.getDataFim().split(" ");
            String[] dataParts = inicioParts[0].split("/");
            String dia = dataParts[2] + "-" + dataParts[1] + "-" + dataParts[0];
            String horaInicial = inicioParts[1];
            String horaFinal = fimParts[1];
            char opcaoCorretaLetra = (char) ('a' + opcaoCorretaIdx);

            writer.append("\"dia\";\"hora inicial\";\"hora final\";\"enunciado da pergunta\";\"opção certa\"\n");
            writer.append(String.format("\"%s\";\"%s\";\"%s\";\"%s\";\"%c\"\n", dia, horaInicial, horaFinal, enunciado, opcaoCorretaLetra));
            writer.append("\"opção\";\"texto da opção\"\n");

            String[] opcoes = opcoesStr.substring(1, opcoesStr.length() - 1).split(", ");
            for (int i = 0; i < opcoes.length; i++) {
                char letraOpcao = (char) ('a' + i);
                writer.append(String.format("\"%c\";\"%s\"\n", letraOpcao, opcoes[i]));
            }

            writer.append("\"número de estudante\";\"nome\";\"e-mail\";\"resposta\"\n");

            for (int i = 2; i < lista.size(); i++) {
                String[] resposta = lista.get(i);
                String numEstudante = resposta[0];
                String nomeEstudante = resposta[1];
                String emailEstudante = resposta[2];
                String respostaLetra = resposta[3];
                writer.append(String.format("\"%s\";\"%s\";\"%s\";\"%s\"\n", numEstudante, nomeEstudante, emailEstudante, respostaLetra));
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Exportado com Sucesso!");
            alert.setHeaderText("Exportado com Sucesso!");
            alert.setContentText("Resultados exportados com sucesso para " + fullPath);
            alert.showAndWait();

            System.out.println("Resultados exportados com sucesso para " + fullPath);
        } catch (IOException e) {
            System.out.println("Ocorreu um erro ao escrever o ficheiro CSV: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Ocorreu um erro inesperado durante a exportação para CSV: " + e.getMessage());
        }
    }

    @FXML
    private void onEditarPergunta() {
        if (perguntaAtiva != null) {
            clientManager.setPerguntaParaEditar(perguntaAtiva);
            clientManager.setState(ClientState.EDITAR_PERGUNTA);
        }
    }

    @FXML
    private void onEliminarPergunta() {
        if (perguntaAtiva != null) {
            clientManager.eliminarPergunta(perguntaAtiva.getId());
        }
    }

    @FXML
    private void onBack() {
        clientManager.setState(ClientState.DOCENTE_HOME);
    }

    public static class RespostaData {
        private final SimpleStringProperty nrEstudante;
        private final SimpleStringProperty nome;
        private final SimpleStringProperty email;
        private final SimpleStringProperty resposta;
        private final SimpleStringProperty dataHora;

        public RespostaData(String nrEstudante, String nome, String email, String resposta, String dataHora) {
            this.nrEstudante = new SimpleStringProperty(nrEstudante);
            this.nome = new SimpleStringProperty(nome);
            this.email = new SimpleStringProperty(email);
            this.resposta = new SimpleStringProperty(resposta);
            this.dataHora = new SimpleStringProperty(dataHora);
        }

        public SimpleStringProperty nrEstudanteProperty() { return nrEstudante; }
        public SimpleStringProperty nomeProperty() { return nome; }
        public SimpleStringProperty emailProperty() { return email; }
        public SimpleStringProperty respostaProperty() { return resposta; }
        public SimpleStringProperty dataHoraProperty() { return dataHora; }
    }
}
