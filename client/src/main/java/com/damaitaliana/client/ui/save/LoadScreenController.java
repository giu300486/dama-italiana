package com.damaitaliana.client.ui.save;

import com.damaitaliana.client.app.SceneId;
import com.damaitaliana.client.app.SceneRouter;
import com.damaitaliana.client.app.UserPromptService;
import com.damaitaliana.client.controller.GameSession;
import com.damaitaliana.client.controller.SinglePlayerGame;
import com.damaitaliana.client.i18n.I18n;
import com.damaitaliana.client.i18n.LocaleService;
import com.damaitaliana.client.persistence.SaveService;
import com.damaitaliana.client.persistence.SaveSlotMetadata;
import com.damaitaliana.client.persistence.SavedGame;
import java.io.UncheckedIOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Controller backing {@code load-screen.fxml}. Lists all save slots reported by {@link
 * SaveService#listSlots()} in a sortable {@link TableView}, lets the user load or delete the
 * selected one (with confirmation), and renders a thumbnail of each saved board through {@link
 * MiniatureRenderer}. The {@code Carica} and {@code Elimina} buttons are bound to the selection so
 * they stay disabled while no row is selected.
 *
 * <p>Loading flow: confirm → {@link SaveService#load(String)} (with typed handling of {@link
 * SaveService.UnknownSchemaVersionException} surfaced as a localized toast, A3.20) → {@link
 * SinglePlayerGame#fromSaved(SavedGame, RandomGenerator)} → publish on {@link GameSession} → {@link
 * SceneRouter#show(SceneId)} {@code BOARD}.
 */
@Component
@Scope("prototype")
public class LoadScreenController {

  private static final Logger log = LoggerFactory.getLogger(LoadScreenController.class);
  private static final int MINIATURE_SIZE_PX = 64;

  /** Outcome of a load attempt; surfaced for unit tests. */
  enum LoadResult {
    LOADED,
    NO_SELECTION,
    CANCELLED,
    MISSING,
    SCHEMA_MISMATCH,
    IO_ERROR
  }

  /** Outcome of a delete attempt; surfaced for unit tests. */
  enum DeleteResult {
    DELETED,
    NO_SELECTION,
    CANCELLED
  }

  private final SceneRouter sceneRouter;
  private final SaveService saveService;
  private final GameSession gameSession;
  private final UserPromptService prompt;
  private final I18n i18n;
  private final LocaleService localeService;
  private final MiniatureRenderer miniatureRenderer;
  private final Supplier<RandomGenerator> rngSupplier;

  @FXML private Label titleLabel;
  @FXML private TableView<SaveSlotMetadata> slotsTable;
  @FXML private TableColumn<SaveSlotMetadata, String> nameColumn;
  @FXML private TableColumn<SaveSlotMetadata, String> dateColumn;
  @FXML private TableColumn<SaveSlotMetadata, String> levelColumn;
  @FXML private TableColumn<SaveSlotMetadata, String> colorColumn;
  @FXML private TableColumn<SaveSlotMetadata, Number> moveColumn;
  @FXML private TableColumn<SaveSlotMetadata, SaveSlotMetadata> miniatureColumn;
  @FXML private Label emptyLabel;
  @FXML private Button backButton;
  @FXML private Button deleteButton;
  @FXML private Button loadButton;

  @Autowired
  public LoadScreenController(
      SceneRouter sceneRouter,
      SaveService saveService,
      GameSession gameSession,
      UserPromptService prompt,
      I18n i18n,
      LocaleService localeService,
      MiniatureRenderer miniatureRenderer) {
    this(
        sceneRouter,
        saveService,
        gameSession,
        prompt,
        i18n,
        localeService,
        miniatureRenderer,
        () -> new SplittableRandom(System.nanoTime()));
  }

  /** Visible for tests: lets a deterministic RNG supplier be injected. */
  LoadScreenController(
      SceneRouter sceneRouter,
      SaveService saveService,
      GameSession gameSession,
      UserPromptService prompt,
      I18n i18n,
      LocaleService localeService,
      MiniatureRenderer miniatureRenderer,
      Supplier<RandomGenerator> rngSupplier) {
    this.sceneRouter = Objects.requireNonNull(sceneRouter, "sceneRouter");
    this.saveService = Objects.requireNonNull(saveService, "saveService");
    this.gameSession = Objects.requireNonNull(gameSession, "gameSession");
    this.prompt = Objects.requireNonNull(prompt, "prompt");
    this.i18n = Objects.requireNonNull(i18n, "i18n");
    this.localeService = Objects.requireNonNull(localeService, "localeService");
    this.miniatureRenderer = Objects.requireNonNull(miniatureRenderer, "miniatureRenderer");
    this.rngSupplier = Objects.requireNonNull(rngSupplier, "rngSupplier");
  }

  @FXML
  void initialize() {
    bindLabels();
    configureColumns();
    bindButtonsToSelection();
    refresh();
  }

  private void bindLabels() {
    titleLabel.setText(i18n.t("load.title"));
    nameColumn.setText(i18n.t("load.column.name"));
    dateColumn.setText(i18n.t("load.column.date"));
    levelColumn.setText(i18n.t("load.column.level"));
    colorColumn.setText(i18n.t("load.column.color"));
    moveColumn.setText(i18n.t("load.column.move"));
    miniatureColumn.setText(i18n.t("load.column.miniature"));
    emptyLabel.setText(i18n.t("load.empty"));
    backButton.setText(i18n.t("common.button.back"));
    deleteButton.setText(i18n.t("load.button.delete"));
    loadButton.setText(i18n.t("load.button.load"));
  }

  private void configureColumns() {
    nameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name()));
    DateTimeFormatter dateFormat =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(localeService.current())
            .withZone(ZoneId.systemDefault());
    dateColumn.setCellValueFactory(
        c -> new SimpleStringProperty(dateFormat.format(c.getValue().updatedAt())));
    levelColumn.setCellValueFactory(
        c ->
            new SimpleStringProperty(
                i18n.t("setup.level." + c.getValue().aiLevel().name().toLowerCase())));
    colorColumn.setCellValueFactory(
        c ->
            new SimpleStringProperty(
                i18n.t("setup.color." + c.getValue().humanColor().name().toLowerCase())));
    moveColumn.setCellValueFactory(
        c -> new SimpleObjectProperty<>(c.getValue().currentMoveNumber()));
    miniatureColumn.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
    miniatureColumn.setCellFactory(col -> new MiniatureCell(this));
  }

  private void bindButtonsToSelection() {
    loadButton
        .disableProperty()
        .bind(Bindings.isNull(slotsTable.getSelectionModel().selectedItemProperty()));
    deleteButton
        .disableProperty()
        .bind(Bindings.isNull(slotsTable.getSelectionModel().selectedItemProperty()));
  }

  private List<SaveSlotMetadata> currentSlots = List.of();

  /** Re-reads the slot list from disk and updates the table + empty-state label. */
  void refresh() {
    this.currentSlots = saveService.listSlots();
    if (slotsTable == null) {
      // Test harness without FXML: skip the UI side of the refresh.
      return;
    }
    ObservableList<SaveSlotMetadata> items = FXCollections.observableArrayList(currentSlots);
    slotsTable.setItems(items);
    boolean empty = currentSlots.isEmpty();
    emptyLabel.setVisible(empty);
    emptyLabel.setManaged(empty);
    slotsTable.setVisible(!empty);
    slotsTable.setManaged(!empty);
  }

  /** Visible for tests: returns the slot list captured by the latest {@link #refresh()} call. */
  List<SaveSlotMetadata> currentSlotsForTest() {
    return currentSlots;
  }

  @FXML
  void onLoad() {
    LoadResult result = loadSelected(slotsTable.getSelectionModel().getSelectedItem());
    if (result == LoadResult.MISSING) {
      refresh();
    }
  }

  @FXML
  void onDelete() {
    DeleteResult result = deleteSelected(slotsTable.getSelectionModel().getSelectedItem());
    if (result == DeleteResult.DELETED) {
      refresh();
    }
  }

  @FXML
  void onBack() {
    sceneRouter.show(SceneId.MAIN_MENU);
  }

  /** Visible for tests. */
  LoadResult loadSelected(SaveSlotMetadata selected) {
    if (selected == null) {
      return LoadResult.NO_SELECTION;
    }
    boolean ok =
        prompt.confirm(
            "load.confirm.load.title",
            "load.confirm.load.header",
            "load.confirm.load.content",
            selected.name());
    if (!ok) {
      return LoadResult.CANCELLED;
    }
    Optional<SavedGame> data;
    try {
      data = saveService.load(selected.slot());
    } catch (SaveService.UnknownSchemaVersionException ex) {
      log.warn("Slot {} has unknown schema {}", selected.slot(), ex.actualVersion());
      prompt.info(
          "load.toast.error.schema.title", "load.toast.error.schema.content", selected.name());
      return LoadResult.SCHEMA_MISMATCH;
    } catch (UncheckedIOException ex) {
      log.warn("Slot {} could not be read", selected.slot(), ex);
      prompt.info("load.toast.error.io.title", "load.toast.error.io.content", selected.name());
      return LoadResult.IO_ERROR;
    }
    if (data.isEmpty()) {
      // Slot vanished between listSlots() and load() — caller refreshes the table.
      return LoadResult.MISSING;
    }
    SinglePlayerGame game = SinglePlayerGame.fromSaved(data.get(), rngSupplier.get());
    gameSession.setCurrentGame(game);
    sceneRouter.show(SceneId.BOARD);
    return LoadResult.LOADED;
  }

  /** Visible for tests. */
  DeleteResult deleteSelected(SaveSlotMetadata selected) {
    if (selected == null) {
      return DeleteResult.NO_SELECTION;
    }
    boolean ok =
        prompt.confirm(
            "load.confirm.delete.title",
            "load.confirm.delete.header",
            "load.confirm.delete.content",
            selected.name());
    if (!ok) {
      return DeleteResult.CANCELLED;
    }
    saveService.delete(selected.slot());
    return DeleteResult.DELETED;
  }

  /** Cell painting the miniature thumbnail for one save row. Pulled out for testability. */
  static final class MiniatureCell extends TableCell<SaveSlotMetadata, SaveSlotMetadata> {

    private final LoadScreenController owner;
    private final ImageView imageView = new ImageView();

    MiniatureCell(LoadScreenController owner) {
      this.owner = owner;
      imageView.setFitWidth(MINIATURE_SIZE_PX);
      imageView.setFitHeight(MINIATURE_SIZE_PX);
      imageView.setPreserveRatio(true);
    }

    @Override
    protected void updateItem(SaveSlotMetadata item, boolean empty) {
      super.updateItem(item, empty);
      if (empty || item == null) {
        setGraphic(null);
        return;
      }
      // The metadata row doesn't carry the full SerializedGameState; load it on demand.
      // Errors here must never break the table rendering: log + leave the cell empty.
      try {
        Optional<SavedGame> saved = owner.saveService.load(item.slot());
        if (saved.isEmpty()) {
          setGraphic(null);
          return;
        }
        imageView.setImage(
            owner.miniatureRenderer.render(saved.get().currentState(), MINIATURE_SIZE_PX));
        setGraphic(imageView);
      } catch (RuntimeException ex) {
        log.debug("Could not render miniature for slot {}", item.slot(), ex);
        setGraphic(null);
      }
    }
  }
}
