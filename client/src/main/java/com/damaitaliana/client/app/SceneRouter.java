package com.damaitaliana.client.app;

import java.io.IOException;
import java.util.Objects;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Single bean responsible for swapping the primary {@link Stage}'s {@link Scene} as the user
 * navigates between top-level views.
 *
 * <p>Lifecycle: Spring instantiates the bean during context bootstrap; {@link
 * JavaFxApp#start(Stage)} then calls {@link #initialize(Stage)} once the JavaFX runtime has
 * supplied a primary {@code Stage}. {@link #show(SceneId)} fails fast if invoked before
 * initialization.
 *
 * <p>For each {@link SceneId} that has a dedicated FXML, the loader is configured with Spring's
 * controller factory so {@code fx:controller="..."} declarations resolve to fully-injected {@link
 * Component} beans. Scenes that have not yet been wired (later Fase 3 tasks) fall back to an inline
 * placeholder.
 *
 * <p>The {@code autosavePromptOnNext} flag is set by the splash bootstrap when an autosave file is
 * detected on disk and consumed by the main menu controller exactly once.
 */
@Component
public class SceneRouter {

  private static final double DEFAULT_WIDTH = 1024;
  private static final double DEFAULT_HEIGHT = 768;

  private final ApplicationContext context;
  private final ThemeService themeService;
  private Stage stage;
  private boolean autosavePromptOnNext;

  public SceneRouter(ApplicationContext context, ThemeService themeService) {
    this.context = Objects.requireNonNull(context, "context");
    this.themeService = Objects.requireNonNull(themeService, "themeService");
  }

  /**
   * Binds the router to the primary stage. Must be called once per JavaFX session, before any call
   * to {@link #show(SceneId)}.
   */
  public void initialize(Stage primaryStage) {
    this.stage = Objects.requireNonNull(primaryStage, "primaryStage");
  }

  public boolean isInitialized() {
    return stage != null;
  }

  /**
   * Replaces the primary stage's scene with the view for {@code id}.
   *
   * @throws IllegalStateException if {@link #initialize(Stage)} has not been called.
   */
  public void show(SceneId id) {
    Objects.requireNonNull(id, "id");
    if (stage == null) {
      throw new IllegalStateException("SceneRouter.initialize must be called before show");
    }
    Parent root = loadRoot(id);
    Scene scene = stage.getScene();
    if (scene == null) {
      scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
      stage.setScene(scene);
    } else {
      scene.setRoot(root);
    }
    themeService.applyTheme(scene);
  }

  /** Sets a one-shot flag the next scene can read via {@link #consumeAutosavePromptOnNext()}. */
  public void setAutosavePromptOnNext(boolean value) {
    this.autosavePromptOnNext = value;
  }

  /** Reads and clears the autosave-prompt flag in a single call. */
  public boolean consumeAutosavePromptOnNext() {
    boolean v = autosavePromptOnNext;
    autosavePromptOnNext = false;
    return v;
  }

  private Parent loadRoot(SceneId id) {
    String fxmlPath = fxmlPathFor(id);
    if (fxmlPath == null) {
      return placeholder(id.name());
    }
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
      loader.setControllerFactory(context::getBean);
      return loader.load();
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to load FXML " + fxmlPath, ex);
    }
  }

  private static String fxmlPathFor(SceneId id) {
    return switch (id) {
      case SPLASH -> "/fxml/splash.fxml";
      case MAIN_MENU -> "/fxml/main-menu.fxml";
      case SP_SETUP -> "/fxml/sp-setup.fxml";
      case BOARD -> "/fxml/board-view.fxml";
      case LOAD -> "/fxml/load-screen.fxml";
      case RULES, SETTINGS -> null;
    };
  }

  private static Parent placeholder(String text) {
    Label label = new Label(text);
    return new StackPane(label);
  }
}
