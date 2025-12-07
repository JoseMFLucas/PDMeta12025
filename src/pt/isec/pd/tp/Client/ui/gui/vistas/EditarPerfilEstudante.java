package pt.isec.pd.tp.Client.ui.gui.vistas;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import pt.isec.pd.tp.Client.Client;
import pt.isec.pd.tp.Client.logica.ClientManager;
import pt.isec.pd.tp.Client.logica.ClientState;
import pt.isec.pd.tp.Client.ui.InitializableController;

public class EditarPerfilEstudante implements InitializableController {
    @FXML
    public TextField txtNumero;
    @FXML
    public TextField txtNome;
    @FXML
    public TextField txtEmail;
    @FXML
    public PasswordField txtPass;


    private ClientManager clientManager;
    private String originalNumero;
    private String originalNome;
    private String originalEmail;
    private String originalPassword;


    @Override
    public void init(ClientManager clientManager) {
        this.clientManager = clientManager;
        clientManager.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equals(ClientManager.PROP_STATE) && clientManager.getState() == ClientState.EDITAR_PERFIL_ESTUDANTE) {
                populateFields();
            }
        });
    }

    private void populateFields() {
        Client user = clientManager.getUser();
        if (user != null) {
            originalNumero = String.valueOf(user.getId());
            txtNumero.setText(originalNumero);
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
        String numero = txtNumero.getText();
        String nome = txtNome.getText();
        String email = txtEmail.getText();
        String password = txtPass.getText();

        if (nome.isEmpty() || email.isEmpty() || password.isEmpty() || numero.isEmpty()) {
            System.out.println("Por favor, preencha todos os campos.");
        } else {
            if (!numero.equals(originalNumero)) {
                System.out.println("Número alterado de: " + originalNumero + " para: " + numero);
                clientManager.editarPerfilEstudante("numero_estudante", numero);
            }
            if (!nome.equals(originalNome)) {
                System.out.println("Nome alterado de: " + originalNome + " para: " + nome);
                clientManager.editarPerfilEstudante("nome", nome);
            }
            if (!email.equals(originalEmail)) {
                System.out.println("Email alterado de: " + originalEmail + " para: " + email);
                clientManager.editarPerfilEstudante("email", email);
            }
            if (!password.equals(originalPassword)) {
                System.out.println("Password alterada.");
                clientManager.editarPerfilEstudante("password", password);
            }
        }

    }

    @FXML
    void onBack(ActionEvent event) {
        clientManager.setState(ClientState.ESTUDANTE_HOME);
    }
}
