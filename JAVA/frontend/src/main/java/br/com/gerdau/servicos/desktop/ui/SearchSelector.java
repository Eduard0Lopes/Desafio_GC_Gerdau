package br.com.gerdau.servicos.desktop.ui;

import br.com.gerdau.servicos.desktop.client.Dto;
import javafx.animation.PauseTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Seletor com busca (doc. 11.5/11.6): campo de pesquisa, lista de sugestões, indicação do ID, estado de
 * carregamento, mensagem de "nenhum resultado", marca de resultado aproximado e botão de limpar.
 * Enquanto o usuário digita: debounce de 300 ms, mínimo de 2 caracteres, cancelamento da requisição
 * anterior e descarte de respostas atrasadas. Teclado: ↓/↑ navegam, Enter escolhe, Esc fecha.
 */
public class SearchSelector extends VBox {

    private static final int MIN_CHARS = 2;
    private static final Duration DEBOUNCE = Duration.millis(300);

    private final StringProperty title = new SimpleStringProperty("");
    private final ObjectProperty<Dto.Componente> selected = new SimpleObjectProperty<>();

    private final Label titleLabel = new Label();
    private final TextField input = new TextField();
    private final Button clearButton = new Button("✕");
    private final ProgressIndicator spinner = new ProgressIndicator();
    private final ListView<Dto.Componente> list = new ListView<>();
    private final Label placeholder = new Label();
    private final Popup popup = new Popup();
    private final PauseTransition debounce = new PauseTransition(DEBOUNCE);

    private Function<String, CompletableFuture<List<Dto.Componente>>> loader;
    private CompletableFuture<?> inFlight;
    private long sequence; // só é acessada na thread da interface
    private boolean updatingText;

    public SearchSelector() {
        getStyleClass().add("search-selector");
        setSpacing(6);

        titleLabel.textProperty().bind(title);
        titleLabel.getStyleClass().add("field-label");

        input.setPromptText("Pesquisar por ID ou texto…");
        input.getStyleClass().add("search-input");

        clearButton.getStyleClass().add("icon-button");
        clearButton.setFocusTraversable(false);
        clearButton.setAccessibleText("Limpar seleção");
        clearButton.visibleProperty().bind(input.textProperty().isNotEmpty());
        clearButton.managedProperty().bind(clearButton.visibleProperty());
        clearButton.setOnAction(e -> clear());

        spinner.setPrefSize(16, 16);
        spinner.setMaxSize(16, 16);
        setLoading(false);

        HBox row = new HBox(6, input, spinner, clearButton);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(input, Priority.ALWAYS);
        getChildren().addAll(titleLabel, row);

        placeholder.getStyleClass().add("muted");
        list.getStyleClass().add("popup-list");
        list.setPlaceholder(placeholder);
        list.setCellFactory(view -> new ResultCell());
        list.setOnMouseClicked(e -> {
            Dto.Componente item = list.getSelectionModel().getSelectedItem();
            if (item != null) choose(item);
        });
        list.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());
        popup.getContent().add(list);
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        input.textProperty().addListener((obs, oldText, newText) -> {
            if (updatingText) return;
            if (selected.get() != null) {
                selected.set(null);
                input.getStyleClass().remove("chosen");
            }
            scheduleSearch();
        });
        input.focusedProperty().addListener((obs, was, focused) -> {
            if (focused && selected.get() == null) scheduleSearch();
        });
        input.setOnMouseClicked(e -> {
            if (selected.get() == null && !popup.isShowing()) search();
        });
        input.addEventFilter(KeyEvent.KEY_PRESSED, this::onKey);
    }

    // ------------------------------------------------------------------ API pública

    public StringProperty titleProperty() { return title; }
    public String getTitle() { return title.get(); }
    public void setTitle(String value) { title.set(value); }

    public ObjectProperty<Dto.Componente> selectedProperty() { return selected; }
    public Dto.Componente getSelected() { return selected.get(); }

    /** Função de busca (recebe o texto digitado e devolve os resultados de forma assíncrona). */
    public void setLoader(Function<String, CompletableFuture<List<Dto.Componente>>> loader) {
        this.loader = loader;
    }

    public void select(Dto.Componente item) {
        choose(item);
    }

    public void clear() {
        cancelPending();
        updatingText = true;
        input.clear();
        updatingText = false;
        input.getStyleClass().remove("chosen");
        selected.set(null);
        popup.hide();
    }

    // ------------------------------------------------------------------ comportamento

    private void onKey(KeyEvent e) {
        switch (e.getCode()) {
            case DOWN -> {
                if (!popup.isShowing()) search(); else move(1);
                e.consume();
            }
            case UP -> {
                move(-1);
                e.consume();
            }
            case ENTER -> {
                Dto.Componente item = list.getSelectionModel().getSelectedItem();
                if (popup.isShowing() && item != null) {
                    choose(item);
                    e.consume();
                }
            }
            case ESCAPE -> {
                if (popup.isShowing()) {
                    popup.hide();
                    e.consume();
                }
            }
            case TAB -> popup.hide();
            default -> { }
        }
    }

    private void move(int delta) {
        int size = list.getItems().size();
        if (size == 0) return;
        int next = Math.floorMod(list.getSelectionModel().getSelectedIndex() + delta, size);
        list.getSelectionModel().select(next);
        list.scrollTo(next);
    }

    private void scheduleSearch() {
        debounce.setOnFinished(e -> search());
        debounce.playFromStart();
    }

    private void search() {
        if (loader == null) return;
        String query = input.getText().strip();
        if (!query.isEmpty() && query.codePointCount(0, query.length()) < MIN_CHARS) {
            cancelPending();
            list.getItems().clear();
            placeholder.setText("Digite ao menos " + MIN_CHARS + " caracteres");
            showPopup();
            return;
        }
        long mine = ++sequence;
        if (inFlight != null) inFlight.cancel(true);
        setLoading(true);
        placeholder.setText("Carregando…");
        showPopup();

        CompletableFuture<List<Dto.Componente>> future = loader.apply(query);
        inFlight = future;
        Fx.run(future, items -> {
            if (mine != sequence) return; // resposta atrasada: o usuário já digitou outra coisa
            setLoading(false);
            placeholder.setText("Nenhum resultado encontrado");
            list.getItems().setAll(items);
            if (!items.isEmpty()) list.getSelectionModel().selectFirst();
            resizeList();
        }, error -> {
            if (mine != sequence) return;
            setLoading(false);
            list.getItems().clear();
            placeholder.setText(error.getMessage()); // o texto digitado é preservado
            resizeList();
        });
    }

    private void choose(Dto.Componente item) {
        cancelPending();
        updatingText = true;
        input.setText(item.rotulo());
        updatingText = false;
        if (!input.getStyleClass().contains("chosen")) input.getStyleClass().add("chosen");
        selected.set(item);
        popup.hide();
    }

    private void cancelPending() {
        debounce.stop();
        sequence++;
        if (inFlight != null) inFlight.cancel(true);
        setLoading(false);
    }

    private void setLoading(boolean loading) {
        spinner.setVisible(loading);
        spinner.setManaged(loading);
    }

    private void showPopup() {
        if (popup.isShowing() || getScene() == null) return;
        Bounds bounds = input.localToScreen(input.getBoundsInLocal());
        if (bounds == null) return;
        list.setPrefWidth(Math.max(280, bounds.getWidth()));
        resizeList();
        popup.show(input, bounds.getMinX(), bounds.getMaxY() + 2);
    }

    private void resizeList() {
        int size = list.getItems().size();
        list.setPrefHeight(size == 0 ? 72 : Math.min(8, size) * 46 + 4);
    }

    // ------------------------------------------------------------------ célula da lista

    private static final class ResultCell extends ListCell<Dto.Componente> {
        private final Label id = new Label();
        private final Label text = new Label();
        private final Label approx = new Label("≈ aproximado");
        private final HBox box = new HBox(10, id, text, approx);

        ResultCell() {
            id.getStyleClass().add("id-badge");
            approx.getStyleClass().add("approx-tag");
            HBox.setHgrow(text, Priority.ALWAYS);
            text.setMaxWidth(Double.MAX_VALUE);
            box.setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(Dto.Componente item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            id.setText(item.id());
            boolean unidade = item.tipo() == Dto.Tipo.UM && item.sigla() != null && !item.sigla().isBlank();
            text.setText(unidade ? item.texto() + " (" + item.sigla() + ")" : item.texto());
            approx.setVisible(item.aproximado());
            approx.setManaged(item.aproximado());
            setText(null);
            setGraphic(box);
        }
    }
}
