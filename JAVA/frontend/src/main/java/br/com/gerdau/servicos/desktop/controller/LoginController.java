package br.com.gerdau.servicos.desktop.controller;

import br.com.gerdau.servicos.desktop.AppContext;
import br.com.gerdau.servicos.desktop.Navigator;
import br.com.gerdau.servicos.desktop.client.ApiClient;
import br.com.gerdau.servicos.desktop.config.ClientConfig;
import br.com.gerdau.servicos.desktop.ui.Fx;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private ProgressIndicator spinner;
    @FXML private Label errorLabel;
    @FXML private Label apiLabel;

    @FXML
    private void initialize() {
        ApiClient api = AppContext.api();
        String warning = ClientConfig.load().insecureRemote()
                ? "\n⚠ A API remota não usa HTTPS: suas credenciais trafegariam sem criptografia." : "";
        apiLabel.setText("API: " + api.baseUrl() + warning);
        hideError();
        setBusy(false);
    }

    @FXML
    private void onLogin() {
        String user = usernameField.getText().strip();
        String password = passwordField.getText();
        if (user.isEmpty() || password.isEmpty()) {
            showError("Informe usuário e senha.");
            return;
        }
        hideError();
        setBusy(true);
        ApiClient api = AppContext.api();
        api.useBasicCredentials(user, password); // somente em memória
        Fx.run(api.me(), me -> {
            AppContext.login(me.username(), me.roles());
            passwordField.clear();
            Navigator.showMain(null);
        }, error -> {
            api.clearCredentials();
            setBusy(false);
            showError(error.getMessage());
        });
    }

    private void setBusy(boolean busy) {
        loginButton.setDisable(busy);
        usernameField.setDisable(busy);
        passwordField.setDisable(busy);
        spinner.setVisible(busy);
        spinner.setManaged(busy);
    }

    private void showError(String message) {
        errorLabel.setText("⛔ " + message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
