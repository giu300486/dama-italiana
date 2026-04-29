package com.damaitaliana.client.ui.splash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.damaitaliana.client.app.SceneId;
import com.damaitaliana.client.app.SceneRouter;
import com.damaitaliana.client.i18n.I18n;
import com.damaitaliana.client.persistence.AutosaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SplashControllerTest {

  private SceneRouter sceneRouter;
  private AutosaveService autosaveService;
  private SplashController controller;

  @BeforeEach
  void setUp() {
    sceneRouter = Mockito.mock(SceneRouter.class);
    autosaveService = Mockito.mock(AutosaveService.class);
    var i18n = Mockito.mock(I18n.class);
    controller = new SplashController(sceneRouter, autosaveService, i18n);
  }

  @Test
  void bootstrapDetectsAutosaveWhenPresent() throws Exception {
    when(autosaveService.autosaveExists()).thenReturn(true);
    SplashController.BootstrapResult result = controller.bootstrap();
    assertThat(result.hasAutosave()).isTrue();
  }

  @Test
  void bootstrapDetectsNoAutosaveWhenAbsent() throws Exception {
    when(autosaveService.autosaveExists()).thenReturn(false);
    SplashController.BootstrapResult result = controller.bootstrap();
    assertThat(result.hasAutosave()).isFalse();
  }

  @Test
  void bootstrapWaitsAtLeastMinSplashMillis() throws Exception {
    when(autosaveService.autosaveExists()).thenReturn(false);
    long start = System.currentTimeMillis();
    controller.bootstrap();
    long elapsed = System.currentTimeMillis() - start;
    assertThat(elapsed).isGreaterThanOrEqualTo(SplashController.MIN_SPLASH_MILLIS);
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
