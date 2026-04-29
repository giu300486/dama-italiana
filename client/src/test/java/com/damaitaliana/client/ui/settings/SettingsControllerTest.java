package com.damaitaliana.client.ui.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.damaitaliana.client.app.SceneId;
import com.damaitaliana.client.app.SceneRouter;
import com.damaitaliana.client.app.UserPromptService;
import com.damaitaliana.client.i18n.I18n;
import com.damaitaliana.client.persistence.PreferencesService;
import com.damaitaliana.client.persistence.UserPreferences;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class SettingsControllerTest {

  private SceneRouter sceneRouter;
  private I18n i18n;
  private PreferencesService preferencesService;
  private UserPromptService prompt;
  private SettingsController controller;

  @BeforeEach
  void setUp() {
    sceneRouter = Mockito.mock(SceneRouter.class);
    i18n = Mockito.mock(I18n.class);
    preferencesService = Mockito.mock(PreferencesService.class);
    prompt = Mockito.mock(UserPromptService.class);
    when(i18n.t(anyString())).thenAnswer(inv -> inv.getArgument(0));
    when(i18n.t(anyString(), any(Object[].class))).thenAnswer(inv -> inv.getArgument(0));
    controller = new SettingsController(sceneRouter, i18n, preferencesService, prompt);
  }

  @Test
  void localeChangePersistsAndPromptsRestart() {
    when(preferencesService.load()).thenReturn(UserPreferences.defaults());

    SettingsController.SaveOutcome outcome = controller.saveSelections(Locale.ENGLISH, 100);

    assertThat(outcome).isEqualTo(SettingsController.SaveOutcome.SAVED_RESTART_REQUIRED);
    ArgumentCaptor<UserPreferences> saved = ArgumentCaptor.forClass(UserPreferences.class);
    verify(preferencesService).save(saved.capture());
    assertThat(saved.getValue().locale()).isEqualTo(Locale.ENGLISH);
    assertThat(saved.getValue().uiScalePercent()).isEqualTo(100);
    verify(prompt)
        .info(
            eq("settings.toast.locale.restart.title"), eq("settings.toast.locale.restart.content"));
  }

  @Test
  void localeUnchangedDoesNotPromptRestart() {
    when(preferencesService.load()).thenReturn(UserPreferences.defaults());

    SettingsController.SaveOutcome outcome = controller.saveSelections(Locale.ITALIAN, 100);

    assertThat(outcome).isEqualTo(SettingsController.SaveOutcome.SAVED_NO_RESTART_NEEDED);
    verify(prompt, never()).info(anyString(), anyString());
  }

  @Test
  void scalingChangePersistsImmediately() {
    when(preferencesService.load()).thenReturn(UserPreferences.defaults());

    controller.saveSelections(Locale.ITALIAN, 125);

    ArgumentCaptor<UserPreferences> saved = ArgumentCaptor.forClass(UserPreferences.class);
    verify(preferencesService).save(saved.capture());
    assertThat(saved.getValue().uiScalePercent()).isEqualTo(125);
    assertThat(saved.getValue().locale()).isEqualTo(Locale.ITALIAN);
    verify(prompt, never()).info(anyString(), anyString());
  }

  @Test
  void invalidScalingFallsBackTo100() {
    when(preferencesService.load()).thenReturn(UserPreferences.defaults());

    controller.saveSelections(Locale.ITALIAN, 200);

    ArgumentCaptor<UserPreferences> saved = ArgumentCaptor.forClass(UserPreferences.class);
    verify(preferencesService).save(saved.capture());
    assertThat(saved.getValue().uiScalePercent()).isEqualTo(100);
  }

  @Test
  void onBackNavigatesToMainMenuWithoutSaving() {
    controller.onBack();

    verify(sceneRouter).show(SceneId.MAIN_MENU);
    verify(preferencesService, never()).save(any(UserPreferences.class));
  }

  @Test
  void applyScalingToSceneIsNullSafe() {
    SettingsController.applyScalingToScene(null, 125);
  }
}
