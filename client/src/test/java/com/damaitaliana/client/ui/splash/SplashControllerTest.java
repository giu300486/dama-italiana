package com.damaitaliana.client.ui.splash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.damaitaliana.client.app.ClientProperties;
import com.damaitaliana.client.app.SceneId;
import com.damaitaliana.client.app.SceneRouter;
import com.damaitaliana.client.i18n.I18n;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

class SplashControllerTest {

  @TempDir Path tempDir;

  private Path savesDir;
  private SceneRouter sceneRouter;
  private SplashController controller;

  @BeforeEach
  void setUp() throws IOException {
    savesDir = tempDir.resolve("saves");
    Files.createDirectories(savesDir);
    sceneRouter = Mockito.mock(SceneRouter.class);
    var props =
        new ClientProperties(savesDir.toString(), tempDir.resolve("config.json").toString());
    var i18n = Mockito.mock(I18n.class);
    controller = new SplashController(sceneRouter, props, i18n);
  }

  @Test
  void bootstrapDetectsAutosaveWhenPresent() throws Exception {
    Files.writeString(savesDir.resolve(SplashController.AUTOSAVE_FILENAME), "{}");
    SplashController.BootstrapResult result = controller.bootstrap();
    assertThat(result.hasAutosave()).isTrue();
  }

  @Test
  void bootstrapDetectsNoAutosaveWhenAbsent() throws Exception {
    SplashController.BootstrapResult result = controller.bootstrap();
    assertThat(result.hasAutosave()).isFalse();
  }

  @Test
  void bootstrapWaitsAtLeastMinSplashMillis() throws Exception {
    long start = System.currentTimeMillis();
    controller.bootstrap();
    long elapsed = System.currentTimeMillis() - start;
    assertThat(elapsed).isGreaterThanOrEqualTo(SplashController.MIN_SPLASH_MILLIS);
  }

  @Test
  void bootstrapHandlesBlankSavesDirGracefully() throws Exception {
    var props = new ClientProperties("", tempDir.resolve("config.json").toString());
    var i18n = Mockito.mock(I18n.class);
    var ctl = new SplashController(sceneRouter, props, i18n);
    SplashController.BootstrapResult result = ctl.bootstrap();
    assertThat(result.hasAutosave()).isFalse();
  }

  @Test
  void finishRoutesToMainMenuWithoutFlagWhenNoAutosave() {
    controller.finish(new SplashController.BootstrapResult(false));
    verify(sceneRouter, never()).setAutosavePromptOnNext(Mockito.anyBoolean());
    verify(sceneRouter).show(SceneId.MAIN_MENU);
  }

  @Test
  void finishSetsAutosaveFlagWhenPresent() {
    controller.finish(new SplashController.BootstrapResult(true));
    verify(sceneRouter).setAutosavePromptOnNext(true);
    verify(sceneRouter).show(SceneId.MAIN_MENU);
  }
}
