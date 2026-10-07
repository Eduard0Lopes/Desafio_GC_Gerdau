package br.com.gerdau.servicos.desktop.controller;

import br.com.gerdau.servicos.desktop.AppContext;
import br.com.gerdau.servicos.desktop.Navigator;
import br.com.gerdau.servicos.desktop.client.ApiClient;
import br.com.gerdau.servicos.desktop.client.Dto;
import br.com.gerdau.servicos.desktop.ui.Dialogs;
import br.com.gerdau.servicos.desktop.ui.Fx;
import br.com.gerdau.servicos.desktop.ui.SearchSelector;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/** Tela de seleção do serviço completo (doc. 11.5): quatro seletores, prévia do código e "Vincular Serviço". */
public class MainController {

    @FXML private Label userLabel;
    @FXML private SearchSelector gsSelector;
    @FXML private SearchSelector cfSelector;
    @FXML private SearchSelector csSelector;
    @FXML private SearchSelector umSelector;
    @FXML private VBox previewBox;
    @FXML private Label previewId;
    @FXML private Label previewDesc;
    @FXML private Label previewChars;
    @FXML private Label previewStatus;
    @FXML private ProgressIndicator previewSpinner;
    @FXML private Button similarButton;
    @FXML private Button vincularButton;
    @FXML private Button cadastrarButton;

    private long previewSequence;           // só na thread da interface
    private Dto.Preview currentPreview;
    private String idempotencyKey;          // mantida entre retentativas por falha de rede, para não duplicar

    @FXML
    private void initialize() {
        ApiClient api = AppContext.api();
        userLabel.setText("Usuário: " + AppContext.username() + " | Data: "
                + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        gsSelector.setLoader(q -> api.buscar(Dto.Tipo.GS, q, 10));
        cfSelector.setLoader(q -> api.buscar(Dto.Tipo.CF, q, 10));
        csSelector.setLoader(q -> api.buscar(Dto.Tipo.CS, q, 10));
        umSelector.setLoader(q -> api.buscar(Dto.Tipo.UM, q, 10));
        for (SearchSelector selector : List.of(gsSelector, cfSelector, csSelector, umSelector)) {
            selector.selectedProperty().addListener((obs, old, now) -> onSelectionChanged());
        }

        // Só esconde o que o perfil não pode usar; o servidor continua validando tudo.
        boolean canWrite = AppContext.hasRole("SOLICITANTE");
        cadastrarButton.setVisible(canWrite);
        cadastrarButton.setManaged(canWrite);
        vincularButton.setVisible(canWrite);
        vincularButton.setManaged(canWrite);
        hidePreview();
    }

    /** Fluxo "Reutilizar Código Existente": deixa o item escolhido já selecionado. */
    public void preselect(Dto.Componente item) {
        switch (item.tipo()) {
            case GS -> gsSelector.select(item);
            case CF -> cfSelector.select(item);
            case CS -> csSelector.select(item);
            case UM -> umSelector.select(item);
        }
    }

    // ------------------------------------------------------------------ ações

    @FXML
    private void onVincular() {
        if (currentPreview == null) return;
        ApiClient api = AppContext.api();
        Dto.Composicao composicao = composicao();
        if (idempotencyKey == null) idempotencyKey = UUID.randomUUID().toString();
        vincularButton.setDisable(true);
        Fx.run(api.criarCodigoCompleto(composicao, idempotencyKey), created -> {
            idempotencyKey = null;
            Dialogs.info(window(), "Serviço vinculado",
                    "Código completo criado com sucesso:\n\n" + created.idCompleto() + "\n" + created.descricaoSnapshot());
            clearAll();
        }, error -> {
            // Falha de rede: mantém a chave para a retentativa não duplicar. Outros erros: nova tentativa = nova chave.
            if (!error.isNetworkError()) idempotencyKey = null;
            vincularButton.setDisable(currentPreview == null);
            Dialogs.error(window(), error);
        });
    }

    @FXML
    private void onVerSimilares() {
        if (currentPreview == null || currentPreview.similaridade() == null) return;
        Dialogs.similarity(window(), currentPreview.similaridade(), Dialogs.Mode.INFO)
                .filter(o -> o.action() == Dialogs.Action.REUTILIZAR && o.item() != null)
                .ifPresent(o -> csSelector.select(new Dto.Componente(o.item().id(), Dto.Tipo.CS, o.item().texto(),
                        null, o.item().rotulo(Dto.Tipo.CS), o.item().score(), false)));
    }

    @FXML
    private void onCadastrar() {
        Navigator.showCadastro();
    }

    @FXML
    private void onLimpar() {
        clearAll();
    }

    @FXML
    private void onLogout() {
        AppContext.logout();
        Navigator.showLogin();
    }

    // ------------------------------------------------------------------ prévia

    private void onSelectionChanged() {
        idempotencyKey = null;
        currentPreview = null;
        vincularButton.setDisable(true);
        if (gsSelector.getSelected() == null || cfSelector.getSelected() == null
                || csSelector.getSelected() == null || umSelector.getSelected() == null) {
            previewSequence++;
            hidePreview();
            return;
        }
        long mine = ++previewSequence;
        previewBox.setVisible(true);
        previewBox.setManaged(true);
        previewSpinner.setVisible(true);
        previewSpinner.setManaged(true);
        previewId.setText("…");
        previewDesc.setText("");
        previewChars.setText("");
        setStatus("Calculando prévia…", "muted");
        similarButton.setVisible(false);
        similarButton.setManaged(false);

        Fx.run(AppContext.api().preview(composicao()), preview -> {
            if (mine != previewSequence) return;
            showPreview(preview);
        }, error -> {
            if (mine != previewSequence) return;
            previewSpinner.setVisible(false);
            previewSpinner.setManaged(false);
            previewId.setText("—");
            setStatus("⛔ " + error.getMessage(), "error-text");
        });
    }

    private void showPreview(Dto.Preview p) {
        currentPreview = p;
        previewSpinner.setVisible(false);
        previewSpinner.setManaged(false);
        previewId.setText(p.idCompleto());
        previewDesc.setText(p.descricaoFinal());
        previewChars.setText("Descrição do C.S.: " + p.quantidadeCaracteres() + "/" + p.limiteCaracteres() + " caracteres (SAP)");

        boolean hasSimilar = p.similaridade() != null && !p.similaridade().similares().isEmpty();
        similarButton.setVisible(hasSimilar);
        similarButton.setManaged(hasSimilar);

        boolean canLink = !p.existeCodigoCompleto() && p.dentroDoLimite();
        if (p.existeCodigoCompleto()) {
            setStatus("⛔ Este código completo já está cadastrado. Nada a criar.", "error-text");
        } else if (!p.dentroDoLimite()) {
            setStatus("⛔ A descrição do Código de Serviço excede o limite de " + p.limiteCaracteres()
                    + " caracteres do SAP. Peça a correção do item antes de vincular.", "error-text");
        } else if (hasSimilar && !"PERMITIR".equals(p.decisaoRecomendada())) {
            setStatus("⚠ Existem " + p.similaridade().similares().size() + " itens de serviço semelhantes (maior: "
                    + Math.round(p.similaridade().scoreMaximo() * 100) + "%). Confira antes de vincular.", "warn-text");
        } else {
            setStatus("✔ Pronto para vincular.", "ok-text");
        }
        vincularButton.setDisable(!canLink);
    }

    private void setStatus(String text, String styleClass) {
        previewStatus.setText(text);
        previewStatus.getStyleClass().removeAll("ok-text", "warn-text", "error-text", "muted");
        previewStatus.getStyleClass().add(styleClass);
    }

    private void hidePreview() {
        previewBox.setVisible(false);
        previewBox.setManaged(false);
        vincularButton.setDisable(true);
    }

    private Dto.Composicao composicao() {
        return new Dto.Composicao(gsSelector.getSelected().id(), cfSelector.getSelected().id(),
                csSelector.getSelected().id(), umSelector.getSelected().id());
    }

    private void clearAll() {
        for (SearchSelector selector : List.of(gsSelector, cfSelector, csSelector, umSelector)) {
            selector.clear();
        }
    }

    private Window window() {
        return userLabel.getScene().getWindow();
    }
}
