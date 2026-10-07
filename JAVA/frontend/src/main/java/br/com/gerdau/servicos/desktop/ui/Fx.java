package br.com.gerdau.servicos.desktop.ui;

import br.com.gerdau.servicos.desktop.client.ApiException;
import javafx.application.Platform;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

public final class Fx {

    private Fx() {}

    /** Consome um futuro garantindo que os callbacks rodem na thread da interface. Cancelamentos são ignorados. */
    public static <T> void run(CompletableFuture<T> future, Consumer<T> onSuccess, Consumer<ApiException> onError) {
        future.whenComplete((value, error) -> Platform.runLater(() -> {
            if (error == null) {
                onSuccess.accept(value);
                return;
            }
            Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
            if (cause instanceof CancellationException) {
                return;
            }
            if (cause instanceof ApiException api) {
                onError.accept(api);
            } else {
                onError.accept(new ApiException(0, "ERRO_INESPERADO", "Erro inesperado: " + cause.getMessage(), null));
            }
        }));
    }
}
