package com.damaitaliana.client.ui.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.damaitaliana.client.app.SceneId;
import com.damaitaliana.client.app.SceneRouter;
import com.damaitaliana.client.app.UserPromptService;
import com.damaitaliana.client.controller.ColorChoice;
import com.damaitaliana.client.controller.GameSession;
import com.damaitaliana.client.controller.SinglePlayerGame;
import com.damaitaliana.client.i18n.I18n;
import com.damaitaliana.client.persistence.AutosaveService;
import com.damaitaliana.client.persistence.SaveService;
import com.damaitaliana.shared.ai.AiLevel;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.SplittableRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MainMenuControllerTest {

  private SceneRouter sceneRouter;
  private I18n i18n;
  private AutosaveService autosaveService;
  private GameSession gameSession;
  private UserPromptService prompt;
  private MainMenuController controller;

  @BeforeEach
  void setUp() {
    sceneRouter = Mockito.mock(SceneRouter.class);
    i18n = Mockito.mock(I18n.class);
    autosaveService = Mockito.mock(AutosaveService.class);
    gameSession = Mockito.mock(GameSession.class);
    prompt = Mockito.mock(UserPromptService.class);
    controller = new MainMenuController(sceneRouter, i18n, autosaveService, gameSession, prompt);
  }

  @Test
  void clickSinglePlayerNavigatesToSetup() {
    controller.openSinglePlayer();
    verify(sceneRouter).show(SceneId.SP_SETUP);
  }

  @Test
  void clickRulesNavigatesToRulesScreen() {
    controller.openRules();
    verify(sceneRouter).show(SceneId.RULES);
  }

  @Test
  void clickSettingsNavigatesToSettings() {
    controller.openSettings();
    verify(sceneRouter).show(SceneId.SETTINGS);
  }

  @Test
  void resumePromptLoadsAutosaveAndNavigatesToBoard() {
    SinglePlayerGame loaded = sampleGame();
    when(prompt.confirm(
            eq("autosave.prompt.title"),
            eq("autosave.prompt.header"),
            eq("autosave.prompt.content")))
        .thenReturn(true);
    when(autosaveService.readAutosave()).thenReturn(Optional.of(loaded));

    MainMenuController.PromptResult result = controller.handleAutosavePrompt();

    assertThat(result).isEqualTo(MainMenuController.PromptResult.RESUMED);
    verify(gameSession).setCurrentGame(loaded);
    verify(sceneRouter).show(SceneId.BOARD);
    verify(autosaveService, never()).clearAutosave();
  }

  @Test
  void discardPromptClearsAutosave() {
    when(prompt.confirm(
            eq("autosave.prompt.title"),
            eq("autosave.prompt.header"),
            eq("autosave.prompt.content")))
        .thenReturn(false);

    MainMenuController.PromptResult result = controller.handleAutosavePrompt();

    assertThat(result).isEqualTo(MainMenuController.PromptResult.DISCARDED);
    verify(autosaveService).clearAutosave();
    verify(autosaveService, never()).readAutosave();
    verifyNoInteractions(gameSession);
    verify(sceneRouter, never()).show(any());
  }

  @Test
  void resumeFromUnknownSchemaShowsToastAndClears() {
    when(prompt.confirm(
            eq("autosave.prompt.title"),
            eq("autosave.prompt.header"),
            eq("autosave.prompt.content")))
        .thenReturn(true);
    when(autosaveService.readAutosave())
        .thenThrow(new SaveService.UnknownSchemaVersionException("_autosave", 99, 1));

    MainMenuController.PromptResult result = controller.handleAutosavePrompt();

    assertThat(result).isEqualTo(MainMenuController.PromptResult.SCHEMA_MISMATCH);
    verify(prompt).info("autosave.toast.error.schema.title", "autosave.toast.error.schema.content");
    verify(autosaveService).clearAutosave();
    verifyNoInteractions(gameSession);
    verify(sceneRouter, never()).show(any());
  }

  @Test
  void resumeWithIoErrorShowsToastAndClears() {
    when(prompt.confirm(
            eq("autosave.prompt.title"),
            eq("autosave.prompt.header"),
            eq("autosave.prompt.content")))
        .thenReturn(true);
    when(autosaveService.readAutosave())
        .thenThrow(new UncheckedIOException(new IOException("boom")));

    MainMenuController.PromptResult result = controller.handleAutosavePrompt();

    assertThat(result).isEqualTo(MainMenuController.PromptResult.IO_ERROR);
    verify(prompt).info("autosave.toast.error.io.title", "autosave.toast.error.io.content");
    verify(autosaveService).clearAutosave();
    verifyNoInteractions(gameSession);
    verify(sceneRouter, never()).show(any());
  }

  @Test
  void resumeWithVanishedFileFallsThroughWithoutNavigation() {
    when(prompt.confirm(
            eq("autosave.prompt.title"),
            eq("autosave.prompt.header"),
            eq("autosave.prompt.content")))
        .thenReturn(true);
    when(autosaveService.readAutosave()).thenReturn(Optional.empty());

    MainMenuController.PromptResult result = controller.handleAutosavePrompt();

    assertThat(result).isEqualTo(MainMenuController.PromptResult.NO_AUTOSAVE);
    verifyNoInteractions(gameSession);
    verify(sceneRouter, never()).show(any());
    verify(autosaveService, never()).clearAutosave();
  }

  private static SinglePlayerGame sampleGame() {
    return SinglePlayerGame.tryCreate(
            AiLevel.ESPERTO, ColorChoice.WHITE, "Resume me", new SplittableRandom(123L))
        .orElseThrow();
  }
}
