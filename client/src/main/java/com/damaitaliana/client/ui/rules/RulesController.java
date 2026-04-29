package com.damaitaliana.client.ui.rules;

import com.damaitaliana.client.app.SceneId;
import com.damaitaliana.client.app.SceneRouter;
import com.damaitaliana.client.i18n.I18n;
import com.damaitaliana.client.ui.save.MiniatureRenderer;
import java.util.List;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Controller backing {@code rules.fxml}. Renders the seven rule sections (FR-RUL-02), with at least
 * one diagram for setup, movement, capture and promotion (FR-RUL-03). The same {@link
 * MiniatureRenderer} that paints save miniatures is reused here so the diagrams share visual
 * vocabulary with the in-game board (PLAN-fase-3 §Task 3.18).
 *
 * <p>Selecting a section in the left {@link ListView} rebuilds the right-hand content pane: a title
 * Label, the body text split on {@code \n\n} into one Label per paragraph, then for sections with
 * diagrams a stack of {@code ImageView + caption Label} per {@link RuleDiagram}. Selection is
 * driven by listening to {@code selectedItemProperty}; tests can call {@link
 * #selectSection(RuleSection)} directly to bypass FXML wiring.
 */
@Component
@Scope("prototype")
public class RulesController {

  /** Diagram side, in pixels, used by {@link MiniatureRenderer#render}. */
  static final int DIAGRAM_PX = 220;

  private final SceneRouter sceneRouter;
  private final I18n i18n;
  private final RuleDiagramLoader diagramLoader;
  private final MiniatureRenderer renderer;

  @FXML private Label titleLabel;
  @FXML private Button backButton;
  @FXML private ListView<RuleSection> sectionsList;
  @FXML private ScrollPane contentScroll;
  @FXML private VBox contentBox;

  private RuleSection currentSection;

  public RulesController(
      SceneRouter sceneRouter,
      I18n i18n,
      RuleDiagramLoader diagramLoader,
      MiniatureRenderer renderer) {
    this.sceneRouter = Objects.requireNonNull(sceneRouter, "sceneRouter");
    this.i18n = Objects.requireNonNull(i18n, "i18n");
    this.diagramLoader = Objects.requireNonNull(diagramLoader, "diagramLoader");
    this.renderer = Objects.requireNonNull(renderer, "renderer");
  }

  @FXML
  void initialize() {
    titleLabel.setText(i18n.t("rules.title"));
    backButton.setText(i18n.t("common.button.back"));

    sectionsList.getItems().setAll(RuleSection.ALL);
    sectionsList.setCellFactory(
        list -> {
          var cell =
              new javafx.scene.control.ListCell<RuleSection>() {
                @Override
                protected void updateItem(RuleSection item, boolean empty) {
                  super.updateItem(item, empty);
                  setText(empty || item == null ? null : i18n.t(item.titleKey()));
                }
              };
          return cell;
        });
    sectionsList
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              if (newVal != null) {
                renderSection(newVal);
              }
            });
    sectionsList.getSelectionModel().selectFirst();
  }

  /** Visible for tests: selects {@code section} and renders its content into the box. */
  void selectSection(RuleSection section) {
    Objects.requireNonNull(section, "section");
    if (sectionsList != null) {
      sectionsList.getSelectionModel().select(section);
    } else {
      renderSection(section);
    }
  }

  /** Visible for tests. */
  RuleSection currentSection() {
    return currentSection;
  }

  private void renderSection(RuleSection section) {
    currentSection = section;
    if (contentBox == null) {
      return;
    }
    contentBox.getChildren().clear();

    Label header = new Label(i18n.t(section.titleKey()));
    header.getStyleClass().add("label-subtitle");
    contentBox.getChildren().add(header);

    String body = i18n.t(section.bodyKey());
    for (String paragraph : body.split("\n\n")) {
      Label p = new Label(paragraph);
      p.setWrapText(true);
      contentBox.getChildren().add(p);
    }

    for (RuleDiagram diagram : diagramLoader.loadFor(section)) {
      VBox figure = new VBox(8);
      figure.setAlignment(Pos.CENTER_LEFT);

      Image image = renderer.render(diagram.position(), DIAGRAM_PX);
      ImageView view = new ImageView(image);
      view.setFitWidth(DIAGRAM_PX);
      view.setFitHeight(DIAGRAM_PX);
      view.setPreserveRatio(true);
      view.getStyleClass().add("rule-diagram");

      Label caption = new Label(i18n.t(diagram.captionKey()));
      caption.setWrapText(true);
      caption.getStyleClass().add("label-secondary");

      figure.getChildren().addAll(view, caption);
      contentBox.getChildren().add(figure);
    }
  }

  @FXML
  void onBack() {
    sceneRouter.show(SceneId.MAIN_MENU);
  }

  /** Visible for tests: read-only view of the rendered children for assertions. */
  List<javafx.scene.Node> contentChildren() {
    return contentBox == null ? List.of() : List.copyOf(contentBox.getChildren());
  }
}
