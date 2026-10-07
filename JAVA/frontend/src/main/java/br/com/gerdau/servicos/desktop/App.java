package br.com.gerdau.servicos.desktop;

import br.com.gerdau.servicos.desktop.client.ApiClient;
import br.com.gerdau.servicos.desktop.config.ClientConfig;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        ClientConfig config = ClientConfig.load();
        stage.setMinWidth(960);
        stage.setMinHeight(600);
        AppContext.init(stage, new ApiClient(config.baseUrl()));
        Navigator.showLogin();
        stage.show();
    }

    @Override
    public void stop() {
        // Ao fechar a janela, descarta qualquer credencial que estivesse em memória.
        if (AppContext.api() != null) {
            AppContext.api().clearCredentials();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
