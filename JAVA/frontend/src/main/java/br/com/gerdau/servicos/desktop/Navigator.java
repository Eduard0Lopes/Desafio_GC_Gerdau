package br.com.gerdau.servicos.desktop;

import br.com.gerdau.servicos.desktop.client.Dto;
import br.com.gerdau.servicos.desktop.controller.MainController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;

/** Troca de telas dentro da mesma janela (a Scene é reaproveitada, então o tamanho é mantido). */
public final class Navigator {

    private static final String TITLE = "Gerdau Service Code Standardization System";

    private Navigator() {}

    public static void showLogin() {
        load("login", " — Entrar");
    }

    /** @param preselect item a já deixar selecionado (fluxo "Reutilizar Código Existente"); pode ser null. */
    public static void showMain(Dto.Componente preselect) {
        MainController controller = load("main", "");
        if (preselect != null) {
            controller.preselect(preselect);
        }
    }

    public static void showCadastro() {
        load("cadastro", " — Cadastrar Novo Item");
    }

    private static <T> T load(String fxml, String titleSuffix) {
        try {
            FXMLLoader loader = new FXMLLoader(Navigator.class.getResource("/fxml/" + fxml + ".fxml"));
            Parent root = loader.load();
            Stage stage = AppContext.stage();
            Scene scene = stage.getScene();
            if (scene == null) {
                scene = new Scene(root, 1180, 720);
                scene.getStylesheets().add(Navigator.class.getResource("/css/theme.css").toExternalForm());
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
            stage.setTitle(TITLE + titleSuffix);
            return loader.getController();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao carregar a tela " + fxml, e);
        }
    }
}
