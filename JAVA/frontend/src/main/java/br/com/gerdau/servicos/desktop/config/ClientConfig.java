package br.com.gerdau.servicos.desktop.config;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * Configuração do cliente. A URL da API vem de -Dapi.baseUrl=..., da variável API_BASE_URL
 * ou, na falta delas, http://localhost:8080. Nada de segredo é guardado em arquivo.
 */
public record ClientConfig(String baseUrl) {

    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "::1", "[::1]");

    public static ClientConfig load() {
        String raw = firstNonBlank(System.getProperty("api.baseUrl"), System.getenv("API_BASE_URL"), "http://localhost:8080");
        raw = raw.strip();
        while (raw.endsWith("/")) {
            raw = raw.substring(0, raw.length() - 1);
        }
        URI uri = URI.create(raw);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("API_BASE_URL inválida (use http:// ou https://): " + raw);
        }
        return new ClientConfig(raw);
    }

    /** true quando a API é remota e NÃO usa HTTPS (credenciais trafegariam sem criptografia). */
    public boolean insecureRemote() {
        URI uri = URI.create(baseUrl);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        return "http".equalsIgnoreCase(uri.getScheme()) && !LOCAL_HOSTS.contains(host);
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return "";
    }
}
