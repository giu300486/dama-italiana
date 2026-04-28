package com.damaitaliana.client.ui.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.damaitaliana.client.app.ClientProperties;
import com.damaitaliana.client.app.SceneId;
import com.damaitaliana.client.app.SceneRouter;
import com.damaitaliana.client.app.UserPromptService;
import com.damaitaliana.client.i18n.I18n;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

class MainMenuControllerTest {

  @TempDir Path tempDir;

  private SceneRouter sceneRouter;
  private I18n i18n;
  private UserPromptService prompt;
  private Path savesDir;
  private MainMenuController controller;

  @BeforeEach
  void setUp() throws IOException {
    sceneRouter = Mockito.mock(SceneRouter.class);
    i18n = Mockito.mock(I18n.class);
    prompt = Mockito.mock(UserPromptService.class);
    savesDir = tempDir.resolve("saves");
    Files.createDirectories(savesDir);
    var props =
        new ClientProperties(savesDir.toString(), tempDir.resolve("config.json").toString());
    controller = new MainMenuController(sceneRouter, i18n, props, prompt);
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
  void autosavePromptResumeKeepsFile() throws IOException {
    Path autosave = savesDir.resolve(MainMenuController.AUTOSAVE_FILENAME);
    Files.writeString(autosave, "{}");
    when(prompt.confirm(
            eq("autosave.prompt.title"),
            eq("autosave.prompt.header"),
            eq("autosave.prompt.content")))
        .thenReturn(true);

    controller.handleAutosavePrompt();

    assertThat(Files.exists(autosave)).as("file kept on resume for Task 3.16 to load").isTrue();
  }

  @Test
  void autosavePromptDiscardDeletesFile() throws IOException {
    Path autosave = savesDir.resolve(MainMenuController.AUTOSAVE_FILENAME);
    Files.writeString(autosave, "{}");
    when(prompt.confirm(
            eq("autosave.prompt.title"),
            eq("autosave.prompt.header"),
            eq("autosave.prompt.content")))
        .thenReturn(false);

    controller.handleAutosavePrompt();

    assertThat(Files.exists(autosave)).isFalse();
  }

  @Test
  void autosavePromptDiscardWithMissingFileIsSilent() {
    when(prompt.confirm(
            eq("autosave.prompt.title"),
            eq("autosave.prompt.header"),
            eq("autosave.prompt.content")))
        .thenReturn(false);

    controller.handleAutosavePrompt();

    assertThat(Files.exists(savesDir.resolve(MainMenuController.AUTOSAVE_FILENAME))).isFalse();
  }
}
