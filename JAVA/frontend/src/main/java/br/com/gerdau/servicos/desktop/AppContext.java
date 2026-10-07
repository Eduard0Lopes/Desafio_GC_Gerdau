package br.com.gerdau.servicos.desktop;

import br.com.gerdau.servicos.desktop.client.ApiClient;
import javafx.stage.Stage;

import java.util.List;

/** Estado global mínimo da aplicação: janela, cliente HTTP e usuário autenticado (somente em memória). */
public final class AppContext {

    private static Stage stage;
    private static ApiClient api;
    private static String username;
    private static List<String> roles = List.of();

    private AppContext() {}

    static void init(Stage primaryStage, ApiClient client) {
        stage = primaryStage;
        api = client;
    }

    public static Stage stage() { return stage; }
    public static ApiClient api() { return api; }
    public static String username() { return username; }

    public static void login(String user, List<String> userRoles) {
        username = user;
        roles = userRoles == null ? List.of() : List.copyOf(userRoles);
    }

    /** Apenas para a interface (esconder botões). A autorização de verdade é feita no servidor. */
    public static boolean hasRole(String role) {
        return roles.contains(role);
    }

    /** Logout: descarta credenciais e dados da sessão (doc. 13.1). */
    public static void logout() {
        api.clearCredentials();
        username = null;
        roles = List.of();
    }
}
