package pt.isec.pd.tp.Client.ui.gui.vistas;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import pt.isec.pd.tp.Client.Client;
import pt.isec.pd.tp.Client.logica.ClientManager;
import pt.isec.pd.tp.Client.logica.ClientState;
import pt.isec.pd.tp.Client.ui.InitializableController;

public class EditarPerfilDocente implements InitializableController {
    @FXML
    public TextField txtNome;
    @FXML
    public TextField txtEmail;
    @FXML
    public PasswordField txtPass;
    @FXML
    public TextField txtCodigo;

    private ClientManager clientManager;
    private String originalNome;
    private String originalEmail;
    private String originalPassword;


    @Override
    public void init(ClientManager clientManager) {
        this.clientManager = clientManager;
        clientManager.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equals(ClientManager.PROP_STATE) && clientManager.getState() == ClientState.EDITAR_PERFIL_DOCENTE) {
                txtCodigo.clear();
                populateFields();
            }
        });
    }

    private void populateFields() {
        Client user = clientManager.getUser();
        if (user != null) {
            originalNome = user.getNome();
            txtNome.setText(originalNome);
            originalEmail = user.getEmail();
            txtEmail.setText(originalEmail);
            originalPassword = user.getPassword();
            txtPass.setText(originalPassword);
        }
    }

    @FXML
    void onEditarPerfil(ActionEvent event) {
        String nome = txtNome.getText();
        String email = txtEmail.getText();
        String password = txtPass.getText();
        String codigo = txtCodigo.getText();

        if (nome.isEmpty() || email.isEmpty() || password.isEmpty() || codigo.isEmpty()) {
            System.out.println("Por favor, preencha todos os campos.");
        } else {
            if (!nome.equals(originalNome)) {
                System.out.println("Nome alterado de: " + originalNome + " para: " + nome);
                clientManager.editarPerfilDocente("nome", nome, codigo);
            }
            if (!email.equals(originalEmail)) {
                System.out.println("Email alterado de: " + originalEmail + " para: " + email);
                clientManager.editarPerfilDocente("email", email, codigo);
            }
            if (!password.equals(originalPassword)) {
                System.out.println("Password alterada.");
                clientManager.editarPerfilDocente("password", password, codigo);
            }
        }

    }

    @FXML
    void onBack(ActionEvent event) {
        clientManager.setState(ClientState.DOCENTE_HOME);
    }
}
