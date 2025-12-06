package pt.isec.pd.tp.Client.ui.gui.vistas;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import pt.isec.pd.tp.Client.Client;
import pt.isec.pd.tp.Client.logica.ClientManager;
import pt.isec.pd.tp.Client.logica.ClientState;
import pt.isec.pd.tp.Client.ui.InitializableController;

public class RegisterPage implements InitializableController {
    private ClientManager clientManager;

    @FXML
    private RadioButton rbEstudante;
    @FXML
    private RadioButton rbDocente;
    @FXML
    private TextField txtNumero;
    @FXML
    private TextField txtNome;
    @FXML
    private TextField txtEmail;
    @FXML
    private PasswordField txtPass;
    @FXML
    private TextField txtCodigo;

    public RegisterPage() {}

    @Override
    public void init(ClientManager clientManager) {
        this.clientManager = clientManager;
        rbEstudante.setSelected(true);
        onUserTypeSelect();
    }

    @FXML
    private void onUserTypeSelect() {
        boolean isEstudante = rbEstudante.isSelected();
        txtNumero.setVisible(isEstudante);
        txtNumero.setManaged(isEstudante);
        txtCodigo.setVisible(!isEstudante);
        txtCodigo.setManaged(!isEstudante);
    }

    @FXML
    private void onRegister() {
        String nome = txtNome.getText();
        String email = txtEmail.getText();
        String pass = txtPass.getText();

        if (rbEstudante.isSelected()) {
            String numero = txtNumero.getText();
            if (!nome.isEmpty() && !email.isEmpty() && !pass.isEmpty() && !numero.isEmpty()) {
                String [] info = new String[]{numero, nome, email, pass};
                clientManager.registar("ESTUDANTE",info);
            }
        } else {
            String codigo = txtCodigo.getText();
            if (!nome.isEmpty() && !email.isEmpty() && !pass.isEmpty() && !codigo.isEmpty()) {
                String [] info = new String[]{nome, email, pass, codigo};
                clientManager.registar("DOCENTE", info);
            }
        }
    }

    @FXML
    private void onBack() {
        clientManager.setState(ClientState.LOGIN);
    }
}
