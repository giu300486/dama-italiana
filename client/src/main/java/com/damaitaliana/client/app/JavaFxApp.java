package com.damaitaliana.client.app;

import com.damaitaliana.client.audio.AudioService;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * JavaFX entry point. Reads the Spring context published by {@link ClientApplication#main} from
 * {@link JavaFxAppContextHolder}, hands the primary stage to the {@link SceneRouter}, and shows the
 * splash placeholder. Closing the stage triggers {@link #stop()} which closes the Spring context.
 *
 * <p>Audio (Fase 3.5): the ambient music playlist is auto-started here so the UX matches a
 * "videogame premium" first impression — music begins on splash and loops until the user mutes or
 * exits. Starting in this method (rather than in a controller) means the call is idempotent and
 * does not have to be repeated when navigating between screens.
 */
public class JavaFxApp extends Application {

  private static final Logger log = LoggerFactory.getLogger(JavaFxApp.class);

  @Override
  public void start(Stage primaryStage) {
    ConfigurableApplicationContext context = JavaFxAppContextHolder.requireContext();
    SceneRouter router = context.getBean(SceneRouter.class);
    router.initialize(primaryStage);
    router.show(SceneId.SPLASH);
    context.getBean(PrimaryStageInitializer.class).initialize(primaryStage);
    primaryStage.setTitle("Dama Italiana");
    primaryStage.show();

    try {
      context.getBean(AudioService.class).playMusicShuffle();
    } catch (RuntimeException ex) {
      log.warn("Could not start ambient music; continuing without audio", ex);
    }
  }

  @Override
  public void stop() {
    JavaFxAppContextHolder.closeContext();
  }
}
