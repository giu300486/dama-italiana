package com.damaitaliana.client.app;

import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * JavaFX entry point. Reads the Spring context published by {@link ClientApplication#main} from
 * {@link JavaFxAppContextHolder}, hands the primary stage to the {@link SceneRouter}, and shows the
 * splash placeholder. Closing the stage triggers {@link #stop()} which closes the Spring context.
 */
public class JavaFxApp extends Application {

  @Override
  public void start(Stage primaryStage) {
    ConfigurableApplicationContext context = JavaFxAppContextHolder.requireContext();
    SceneRouter router = context.getBean(SceneRouter.class);
    router.initialize(primaryStage);
    router.show(SceneId.SPLASH);
    primaryStage.setTitle("Dama Italiana");
    primaryStage.show();
  }

  @Override
  public void stop() {
    JavaFxAppContextHolder.closeContext();
  }
}
