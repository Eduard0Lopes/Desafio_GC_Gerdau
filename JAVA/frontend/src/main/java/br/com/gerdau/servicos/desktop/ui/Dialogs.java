package br.com.gerdau.servicos.desktop.ui;

import br.com.gerdau.servicos.desktop.client.ApiException;
import br.com.gerdau.servicos.desktop.client.Dto;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Optional;

/** Diálogos da aplicação: mensagens, similaridade (alerta/bloqueio) e envio para a Governança. */
public final class Dialogs {

    public static final int MIN_JUSTIFICATIVA = 10;
    private static final String CSS = Dialogs.class.getResource("/css/theme.css").toExternalForm();

    public enum Mode { BLOQUEIO, ALERTA, INFO }

    public enum Action { REUTILIZAR, JUSTIFICAR, GOVERNANCA }

    /** Resultado do diálogo de similaridade. item (REUTILIZAR) e justificativa (JUSTIFICAR) são opcionais. */
    public record Outcome(Action action, Dto.SimilarItem item, String justificativa) {}

    private Dialogs() {}

    // ------------------------------------------------------------------ mensagens simples

    public static void info(Window owner, String title, String message) {
        show(Alert.AlertType.INFORMATION, owner, title, message);
    }

    public static void warn(Window owner, String title, String message) {
        show(Alert.AlertType.WARNING, owner, title, message);
    }

    public static void error(Window owner, ApiException e) {
        String title = e.isNetworkError() ? "Falha de conexão" : "Não foi possível concluir";
        show(Alert.AlertType.ERROR, owner, title, e.getMessage());
    }

    private static void show(Alert.AlertType type, Window owner, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(title);
        style(alert.getDialogPane());
        alert.showAndWait();
    }

    private static void style(DialogPane pane) {
        pane.getStylesheets().add(CSS);
        pane.getStyleClass().add("dialog-root");
    }

    // ------------------------------------------------------------------ similaridade

    public static Optional<Outcome> similarity(Window owner, Dto.Similaridade sim, Mode mode) {
        Dialog<Outcome> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(switch (mode) {
            case BLOQUEIO -> "Bloqueio de Duplicidade — Item Similar Detectado";
            case ALERTA -> "Atenção — Itens Semelhantes";
            case INFO -> "Itens Semelhantes";
        });
        DialogPane pane = dialog.getDialogPane();
        style(pane);

        Label banner = new Label(switch (mode) {
            case BLOQUEIO -> "⛔ O sistema identificou itens muito semelhantes já cadastrados";
            case ALERTA -> "⚠ O sistema identificou itens semelhantes já cadastrados";
            case INFO -> "ℹ Itens semelhantes encontrados (somente informativo)";
        });
        banner.getStyleClass().addAll("banner", mode == Mode.BLOQUEIO ? "banner-error" : mode == Mode.ALERTA ? "banner-warn" : "banner-info");
        banner.setWrapText(true);
        banner.setMaxWidth(Double.MAX_VALUE);

        Label why = new Label(switch (mode) {
            case BLOQUEIO -> "Reutilize um item existente ou envie para a Governança de Dados (validação humana).";
            case ALERTA -> "Reutilize um deles ou informe uma justificativa para criar mesmo assim (ficará registrada).";
            case INFO -> "Confira se algum destes já atende antes de criar um novo.";
        });
        why.setWrapText(true);

        String observando = sim.observando() ? " Modo de observação: nada é bloqueado, os casos só são registrados." : "";
        Label policy = new Label(String.format(
                "Política: a partir de %d%% bloqueia; de %d%% a %d%% alerta. A nota é aproximada: indica semelhança, não garante que seja o mesmo serviço.%s",
                Math.round(sim.limiarBloqueio() * 100), Math.round(sim.limiarAlerta() * 100),
                Math.round(sim.limiarBloqueio() * 100) - 1, observando));
        policy.getStyleClass().add("muted-small");
        policy.setWrapText(true);

        ListView<Dto.SimilarItem> list = new ListView<>();
        list.getItems().setAll(sim.similares());
        list.setPrefHeight(190);
        list.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Dto.SimilarItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label main = new Label(String.format("ID: %s | %s | %d%% de semelhança",
                        item.id(), item.texto(), Math.round(item.score() * 100)));
                Label reason = new Label("Motivo: " + item.motivo());
                reason.getStyleClass().add("muted-small");
                VBox box = new VBox(2, main, reason);
                setGraphic(box);
                setText(null);
            }
        });
        if (!sim.similares().isEmpty()) list.getSelectionModel().selectFirst();

        VBox content = new VBox(10, banner, why, list, policy);
        content.setPadding(new Insets(4));
        content.setPrefWidth(620);

        ButtonType reuse = new ButtonType("Reutilizar Código Existente", ButtonData.OTHER);
        ButtonType back = new ButtonType("Voltar", ButtonData.CANCEL_CLOSE);
        ButtonType governance = new ButtonType("Enviar para Governança", ButtonData.OTHER);
        ButtonType proceed = new ButtonType("Prosseguir com Justificativa", ButtonData.OTHER);

        TextArea justification = new TextArea();
        justification.setPromptText("Justificativa para criar mesmo assim (obrigatória, mínimo " + MIN_JUSTIFICATIVA + " caracteres)");
        justification.setPrefRowCount(3);
        justification.setWrapText(true);

        pane.setContent(content);
        switch (mode) {
            case BLOQUEIO -> pane.getButtonTypes().addAll(reuse, governance, back);
            case ALERTA -> {
                content.getChildren().add(2 + 1, justification);
                pane.getButtonTypes().addAll(reuse, proceed, back);
            }
            case INFO -> pane.getButtonTypes().addAll(reuse, back);
        }

        Node reuseButton = pane.lookupButton(reuse);
        reuseButton.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
        if (mode == Mode.ALERTA) {
            pane.lookupButton(proceed).disableProperty().bind(
                    Bindings.createBooleanBinding(() -> justification.getText().strip().length() < MIN_JUSTIFICATIVA,
                            justification.textProperty()));
        }

        dialog.setResultConverter(button -> {
            if (button == reuse) {
                return new Outcome(Action.REUTILIZAR, list.getSelectionModel().getSelectedItem(), null);
            }
            if (button == proceed) {
                return new Outcome(Action.JUSTIFICAR, null, justification.getText().strip());
            }
            if (button == governance) {
                return new Outcome(Action.GOVERNANCA, null, null);
            }
            return null;
        });
        return dialog.showAndWait();
    }

    // ------------------------------------------------------------------ governança

    /** @return a justificativa digitada, se o usuário confirmar o envio. */
    public static Optional<String> governance(Window owner, Dto.Tipo tipo, String texto) {
        Dialog<String> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Envio para Governança");
        DialogPane pane = dialog.getDialogPane();
        style(pane);

        Label banner = new Label("ℹ Atenção: este cadastro passará por validação humana pela Equipe de Governança de Dados");
        banner.getStyleClass().addAll("banner", "banner-info");
        banner.setWrapText(true);
        banner.setMaxWidth(Double.MAX_VALUE);

        Label classe = new Label("Classe");
        classe.getStyleClass().add("field-label");
        Label classeValue = new Label(tipo.label());
        Label descricao = new Label("Descrição");
        descricao.getStyleClass().add("field-label");
        TextField descricaoField = new TextField(texto);
        descricaoField.setEditable(false);
        Label just = new Label("Justificativa da solicitação (por que criar este item e não reutilizar um existente?)");
        just.getStyleClass().add("field-label");
        just.setWrapText(true);
        TextArea area = new TextArea();
        area.setPromptText("Mínimo " + MIN_JUSTIFICATIVA + " caracteres");
        area.setPrefRowCount(4);
        area.setWrapText(true);

        VBox content = new VBox(8, banner, classe, classeValue, descricao, descricaoField, just, area);
        content.setPadding(new Insets(4));
        content.setPrefWidth(560);
        content.setAlignment(Pos.TOP_LEFT);
        pane.setContent(content);

        ButtonType confirm = new ButtonType("Confirmar Envio para Governança", ButtonData.OK_DONE);
        ButtonType back = new ButtonType("Voltar", ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(back, confirm);
        pane.lookupButton(confirm).disableProperty().bind(
                Bindings.createBooleanBinding(() -> area.getText().strip().length() < MIN_JUSTIFICATIVA, area.textProperty()));

        dialog.setResultConverter(button -> button == confirm ? area.getText().strip() : null);
        return dialog.showAndWait();
    }
}
