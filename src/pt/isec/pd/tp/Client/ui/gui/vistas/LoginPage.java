package pt.isec.pd.tp.Client.ui.gui.vistas;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import pt.isec.pd.tp.Client.logica.ClientManager;
import pt.isec.pd.tp.Client.logica.ClientState;
import pt.isec.pd.tp.Client.ui.InitializableController;

public class LoginPage implements InitializableController {
    private ClientManager clientManager;

    @FXML
    private TextField txtEmail;
    @FXML
    private PasswordField txtPass;
    @FXML
    private Button btnLogin;
    @FXML
    private Button btnRegistar;

    // Public no-argument constructor required by FXML
    public LoginPage() {}

    @Override
    public void init(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @FXML
    private void onLogin() {
        String email = txtEmail.getText();
        String pass = txtPass.getText();
        if (!email.isEmpty() && !pass.isEmpty()) {
            clientManager.login(email, pass);

        }
    }

    @FXML
    private void onRegister() {
        clientManager.setState(ClientState.REGISTER);
    }
}
