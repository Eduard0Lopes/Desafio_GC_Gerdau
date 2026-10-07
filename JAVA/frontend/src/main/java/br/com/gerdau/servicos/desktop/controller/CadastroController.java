package br.com.gerdau.servicos.desktop.controller;

import br.com.gerdau.servicos.desktop.AppContext;
import br.com.gerdau.servicos.desktop.Navigator;
import br.com.gerdau.servicos.desktop.client.ApiException;
import br.com.gerdau.servicos.desktop.client.Dto;
import br.com.gerdau.servicos.desktop.ui.Dialogs;
import br.com.gerdau.servicos.desktop.ui.Fx;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** Tela de cadastro de item (doc. 11.7/11.8): contador SAP, similaridade ao sair do campo, bloqueio e governança. */
public class CadastroController {

    private static final int SAP_LIMIT = 40;
    private static final int NAME_LIMIT = 100;

    @FXML private Label userLabel;
    @FXML private ComboBox<Dto.Tipo> tipoBox;
    @FXML private Label textoLabel;
    @FXML private TextField textoField;
    @FXML private Label contadorLabel;
    @FXML private Label siglaLabel;
    @FXML private TextField siglaField;
    @FXML private Label inlineLabel;
    @FXML private ProgressIndicator spinner;
    @FXML private Button salvarButton;
    @FXML private Button verificarButton;

    private long similaritySequence;      // só na thread da interface
    private String pendingKey;            // Idempotency-Key reaproveitada somente em retentativa idêntica
    private String pendingSignature;

    @FXML
    private void initialize() {
        userLabel.setText("Usuário: " + AppContext.username() + " | Data: "
                + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        tipoBox.getItems().setAll(Dto.Tipo.values());
        tipoBox.valueProperty().addListener((obs, old, now) -> onTipoChanged());
        textoField.textProperty().addListener((obs, old, now) -> {
            updateCounter();
            hideInline();
        });
        textoField.focusedProperty().addListener((obs, was, focused) -> {
            // "Ao sair do campo ... a aplicação consulta a API e apresenta itens similares"
            if (!focused && !textoField.getText().isBlank()) checkInline();
        });
        tipoBox.getSelectionModel().select(Dto.Tipo.CS);
        setBusy(false);
    }

    // ------------------------------------------------------------------ ações

    @FXML
    private void onSalvar() {
        String problem = localValidation();
        if (problem != null) {
            Dialogs.warn(window(), "Confira os dados", problem);
            return;
        }
        save(null);
    }

    @FXML
    private void onVerificar() {
        String texto = textoField.getText().strip();
        if (texto.isEmpty()) {
            Dialogs.warn(window(), "Confira os dados", "Informe o texto do item para verificar a similaridade.");
            return;
        }
        Dto.Tipo tipo = tipoBox.getValue();
        setBusy(true);
        Fx.run(AppContext.api().similaridade(tipo, texto), sim -> {
            setBusy(false);
            if (sim.similares().isEmpty()) {
                Dialogs.info(window(), "Similaridade", "Nenhum item semelhante encontrado. Pode cadastrar.");
            } else {
                Dialogs.similarity(window(), sim, Dialogs.Mode.INFO)
                        .filter(o -> o.action() == Dialogs.Action.REUTILIZAR && o.item() != null)
                        .ifPresent(o -> reuse(o.item(), tipo));
            }
        }, error -> {
            setBusy(false);
            Dialogs.error(window(), error);
        });
    }

    @FXML
    private void onVoltar() {
        Navigator.showMain(null);
    }

    // ------------------------------------------------------------------ salvar

    private void save(String justificativa) {
        Dto.Tipo tipo = tipoBox.getValue();
        String texto = textoField.getText().strip();
        String sigla = tipo == Dto.Tipo.UM ? siglaField.getText().strip() : null;

        String signature = tipo + "|" + texto + "|" + sigla + "|" + justificativa;
        if (pendingKey == null || !signature.equals(pendingSignature)) {
            pendingKey = UUID.randomUUID().toString();
            pendingSignature = signature;
        }
        setBusy(true);
        Fx.run(AppContext.api().criarItem(tipo, texto, sigla, justificativa, pendingKey), created -> {
            clearPending();
            setBusy(false);
            String extra = created.excecaoRegistrada() ? "\n\nA exceção de similaridade foi registrada para rastreabilidade." : "";
            Dialogs.info(window(), "Item cadastrado", "Cadastrado: " + created.item().rotulo() + extra);
            resetForm();
        }, error -> {
            setBusy(false);
            handleSaveError(error, tipo, texto, sigla);
        });
    }

    private void handleSaveError(ApiException error, Dto.Tipo tipo, String texto, String sigla) {
        boolean high = "SIMILARIDADE_ALTA".equals(error.codigo());
        boolean medium = "SIMILARIDADE_INTERMEDIARIA".equals(error.codigo());
        if ((high || medium) && error.similaridade() != null) {
            clearPending();
            Dialogs.similarity(window(), error.similaridade(), high ? Dialogs.Mode.BLOQUEIO : Dialogs.Mode.ALERTA)
                    .ifPresent(outcome -> {
                        switch (outcome.action()) {
                            case REUTILIZAR -> reuse(outcome.item(), tipo);
                            case JUSTIFICAR -> save(outcome.justificativa());
                            case GOVERNANCA -> sendToGovernance(tipo, texto, sigla);
                        }
                    });
            return;
        }
        // Falha de rede: mantém a chave para a retentativa não duplicar. Demais erros: começa de novo.
        if (!error.isNetworkError()) clearPending();
        Dialogs.error(window(), error);
    }

    private void sendToGovernance(Dto.Tipo tipo, String texto, String sigla) {
        Dialogs.governance(window(), tipo, texto).ifPresent(justificativa -> {
            String key = UUID.randomUUID().toString();
            setBusy(true);
            Fx.run(AppContext.api().solicitarGovernanca(tipo, texto, sigla, justificativa, key), request -> {
                setBusy(false);
                Dialogs.info(window(), "Enviado para a Governança",
                        "Solicitação registrada (protocolo " + request.id() + ").\n"
                                + "A Equipe de Governança de Dados fará a validação humana.");
                resetForm();
            }, error -> {
                setBusy(false);
                Dialogs.error(window(), error);
            });
        });
    }

    private void reuse(Dto.SimilarItem item, Dto.Tipo tipo) {
        Navigator.showMain(new Dto.Componente(item.id(), tipo, item.texto(), item.sigla(),
                item.rotulo(tipo), item.score(), false));
    }

    // ------------------------------------------------------------------ similaridade inline

    private void checkInline() {
        Dto.Tipo tipo = tipoBox.getValue();
        String texto = textoField.getText().strip();
        long mine = ++similaritySequence;
        showInline("Verificando itens semelhantes…", "muted");
        Fx.run(AppContext.api().similaridade(tipo, texto), sim -> {
            if (mine != similaritySequence) return;
            if (sim.similares().isEmpty()) {
                showInline("✔ Nenhum item semelhante encontrado.", "ok-text");
                return;
            }
            String pct = Math.round(sim.scoreMaximo() * 100) + "%";
            String observe = sim.observando() ? " (modo observação: não bloqueia)" : "";
            switch (sim.decisao()) {
                case "REVISAR" -> showInline("⛔ " + sim.similares().size() + " item(ns) muito semelhante(s), maior " + pct
                        + ". Reutilize ou envie para a Governança." + observe, "error-text");
                case "ALERTAR" -> showInline("⚠ " + sim.similares().size() + " item(ns) semelhante(s), maior " + pct
                        + ". Reutilize ou justifique." + observe, "warn-text");
                default -> showInline("ℹ " + sim.similares().size() + " item(ns) com alguma semelhança (maior " + pct + ").", "muted");
            }
        }, error -> {
            if (mine != similaritySequence) return;
            showInline("Não foi possível verificar agora: " + error.getMessage(), "muted");
        });
    }

    private void showInline(String text, String styleClass) {
        inlineLabel.setText(text);
        inlineLabel.getStyleClass().removeAll("ok-text", "warn-text", "error-text", "muted");
        inlineLabel.getStyleClass().add(styleClass);
        inlineLabel.setVisible(true);
        inlineLabel.setManaged(true);
    }

    private void hideInline() {
        similaritySequence++;
        inlineLabel.setVisible(false);
        inlineLabel.setManaged(false);
    }

    // ------------------------------------------------------------------ formulário

    private void onTipoChanged() {
        Dto.Tipo tipo = tipoBox.getValue();
        boolean unidade = tipo == Dto.Tipo.UM;
        textoLabel.setText(tipo == Dto.Tipo.CS ? "Descrição do item" : "Nome");
        siglaLabel.setVisible(unidade);
        siglaLabel.setManaged(unidade);
        siglaField.setVisible(unidade);
        siglaField.setManaged(unidade);
        clearPending();
        hideInline();
        updateCounter();
    }

    private void updateCounter() {
        Dto.Tipo tipo = tipoBox.getValue();
        if (tipo == null) return;
        int limit = tipo == Dto.Tipo.CS ? SAP_LIMIT : NAME_LIMIT;
        String text = textoField.getText();
        int length = text.codePointCount(0, text.length());
        String suffix = tipo == Dto.Tipo.CS ? " caracteres SAP" : " caracteres";
        contadorLabel.getStyleClass().removeAll("over-limit", "near-limit", "within-limit");
        if (length > limit) {
            contadorLabel.setText("⚠ " + length + "/" + limit + suffix + " — excede o limite");
            contadorLabel.getStyleClass().add("over-limit");
        } else if (length >= limit * 0.8) {
            contadorLabel.setText(length + "/" + limit + suffix);
            contadorLabel.getStyleClass().add("near-limit");
        } else {
            contadorLabel.setText(length + "/" + limit + suffix);
            contadorLabel.getStyleClass().add("within-limit");
        }
    }

    /** Validação de conveniência na interface; o servidor valida tudo de novo (doc. 3). */
    private String localValidation() {
        Dto.Tipo tipo = tipoBox.getValue();
        String texto = textoField.getText().strip();
        if (texto.isEmpty()) return "Informe " + (tipo == Dto.Tipo.CS ? "a descrição" : "o nome") + " do item.";
        int limit = tipo == Dto.Tipo.CS ? SAP_LIMIT : NAME_LIMIT;
        int length = texto.codePointCount(0, texto.length());
        if (length > limit) return "O texto tem " + length + " caracteres e o limite é " + limit + ". Abrevie mantendo o sentido.";
        if (tipo == Dto.Tipo.UM && siglaField.getText().isBlank()) return "Informe a sigla da unidade (ex.: HR, M2).";
        return null;
    }

    private void resetForm() {
        clearPending();
        textoField.clear();
        siglaField.clear();
        hideInline();
    }

    private void clearPending() {
        pendingKey = null;
        pendingSignature = null;
    }

    private void setBusy(boolean busy) {
        salvarButton.setDisable(busy);
        verificarButton.setDisable(busy);
        spinner.setVisible(busy);
        spinner.setManaged(busy);
    }

    private Window window() {
        return userLabel.getScene().getWindow();
    }
}
