package br.com.gerdau.servicos.desktop.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Cliente HTTP da API REST/JSON. Todas as chamadas são assíncronas (nunca bloqueiam a tela) e
 * cancelar o CompletableFuture devolvido cancela também a requisição em andamento.
 */
public final class ApiClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private volatile String authorization;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String baseUrl() { return baseUrl; }

    /** Credenciais ficam só em memória; clearCredentials() as descarta (logout). */
    public void useBasicCredentials(String username, String password) {
        String token = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        this.authorization = "Basic " + token;
    }

    public void clearCredentials() {
        this.authorization = null;
    }

    // ------------------------------------------------------------------ endpoints

    public CompletableFuture<Dto.Me> me() {
        return send(get("/api/v1/me"), new TypeReference<Dto.Me>() {});
    }

    public CompletableFuture<List<Dto.Componente>> buscar(Dto.Tipo tipo, String query, int limit) {
        String q = URLEncoder.encode(query == null ? "" : query, StandardCharsets.UTF_8);
        return send(get("/api/v1/" + tipo.path() + "?query=" + q + "&limit=" + limit),
                new TypeReference<List<Dto.Componente>>() {});
    }

    public CompletableFuture<Dto.Preview> preview(Dto.Composicao c) {
        return send(post("/api/v1/codigos-completos/preview", composicao(c), null), new TypeReference<Dto.Preview>() {});
    }

    public CompletableFuture<Dto.CodigoCompleto> criarCodigoCompleto(Dto.Composicao c, String idempotencyKey) {
        return send(post("/api/v1/codigos-completos", composicao(c), idempotencyKey),
                new TypeReference<Dto.CodigoCompleto>() {});
    }

    public CompletableFuture<Dto.Similaridade> similaridade(Dto.Tipo tipo, String texto) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tipo", tipo.name());
        body.put("texto", texto);
        return send(post("/api/v1/itens/similaridade", body, null), new TypeReference<Dto.Similaridade>() {});
    }

    public CompletableFuture<Dto.ItemCriado> criarItem(Dto.Tipo tipo, String texto, String sigla,
                                                       String justificativa, String idempotencyKey) {
        return send(post("/api/v1/itens", itemBody(tipo, texto, sigla, justificativa), idempotencyKey),
                new TypeReference<Dto.ItemCriado>() {});
    }

    public CompletableFuture<Dto.Solicitacao> solicitarGovernanca(Dto.Tipo tipo, String texto, String sigla,
                                                                  String justificativa, String idempotencyKey) {
        return send(post("/api/v1/governanca", itemBody(tipo, texto, sigla, justificativa), idempotencyKey),
                new TypeReference<Dto.Solicitacao>() {});
    }

    // ------------------------------------------------------------------ infraestrutura

    private static Map<String, Object> composicao(Dto.Composicao c) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("gsId", c.gsId());
        body.put("cfId", c.cfId());
        body.put("csId", c.csId());
        body.put("umId", c.umId());
        return body;
    }

    private static Map<String, Object> itemBody(Dto.Tipo tipo, String texto, String sigla, String justificativa) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tipo", tipo.name());
        body.put("texto", texto);
        if (sigla != null && !sigla.isBlank()) body.put("sigla", sigla);
        if (justificativa != null && !justificativa.isBlank()) body.put("justificativa", justificativa);
        return body;
    }

    private HttpRequest get(String path) {
        return builder(path).GET().build();
    }

    private HttpRequest post(String path, Object body, String idempotencyKey) {
        HttpRequest.Builder b = builder(path).header("Content-Type", "application/json");
        if (idempotencyKey != null) {
            b.header("Idempotency-Key", idempotencyKey);
        }
        try {
            return b.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8)).build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao montar a requisição", e);
        }
    }

    private HttpRequest.Builder builder(String path) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json");
        String auth = authorization;
        if (auth != null) {
            b.header("Authorization", auth);
        }
        return b;
    }

    private <T> CompletableFuture<T> send(HttpRequest request, TypeReference<T> type) {
        CompletableFuture<T> out = new CompletableFuture<>();
        CompletableFuture<HttpResponse<String>> raw =
                http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        raw.whenComplete((response, error) -> {
            if (error != null) {
                out.completeExceptionally(unavailable(error));
                return;
            }
            try {
                if (response.statusCode() / 100 == 2) {
                    out.complete(mapper.readValue(response.body(), type));
                } else {
                    out.completeExceptionally(toApiException(response));
                }
            } catch (Exception e) {
                out.completeExceptionally(new ApiException(response.statusCode(), "RESPOSTA_INVALIDA",
                        "A API respondeu de forma inesperada (HTTP " + response.statusCode() + ").", null));
            }
        });
        // Cancelar o futuro devolvido (ex.: o usuário continuou digitando) cancela a requisição HTTP.
        out.whenComplete((value, error) -> {
            if (out.isCancelled()) {
                raw.cancel(true);
            }
        });
        return out;
    }

    private ApiException unavailable(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        if (cause instanceof HttpTimeoutException) {
            return new ApiException(0, "API_TIMEOUT", "A API demorou demais para responder. Tente novamente.", null);
        }
        return new ApiException(0, "API_INDISPONIVEL",
                "Não foi possível conectar à API em " + baseUrl + ". Verifique se ela está no ar e sua rede.", null);
    }

    private ApiException toApiException(HttpResponse<String> response) {
        int status = response.statusCode();
        String codigo = "ERRO_HTTP_" + status;
        String message = switch (status) {
            case 401 -> "Usuário ou senha inválidos, ou sessão expirada.";
            case 403 -> "Seu perfil não permite esta operação.";
            default -> "A API retornou um erro (HTTP " + status + ").";
        };
        Dto.Similaridade similaridade = null;
        try {
            JsonNode node = mapper.readTree(response.body());
            if (node != null && node.isObject()) {
                codigo = node.path("codigo").asText(codigo);
                message = node.path("detail").asText(message);
                if (node.hasNonNull("similaridade")) {
                    similaridade = mapper.treeToValue(node.get("similaridade"), Dto.Similaridade.class);
                }
            }
        } catch (Exception ignored) {
            // corpo vazio ou não-JSON (ex.: 401): mantém a mensagem padrão
        }
        return new ApiException(status, codigo, message, similaridade);
    }
}
