package com.damaitaliana.client.app;

import java.util.Objects;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
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
 * <p>Each {@link SceneId} is rendered through a small inline placeholder until the dedicated FXML
 * loader for that screen is wired in by later Fase 3 tasks.
 */
@Component
public class SceneRouter {

  private static final double DEFAULT_WIDTH = 1024;
  private static final double DEFAULT_HEIGHT = 768;

  private Stage stage;

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
    Parent root =
        switch (id) {
          case SPLASH -> placeholder("Dama Italiana");
          case MAIN_MENU -> placeholder("Main Menu");
        };
    Scene scene = stage.getScene();
    if (scene == null) {
      stage.setScene(new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT));
    } else {
      scene.setRoot(root);
    }
  }

  private static Parent placeholder(String text) {
    Label label = new Label(text);
    return new StackPane(label);
  }
}
