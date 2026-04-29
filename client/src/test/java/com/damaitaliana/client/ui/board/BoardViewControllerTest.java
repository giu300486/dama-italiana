package com.damaitaliana.client.ui.board;

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
import com.damaitaliana.client.controller.GameSession;
import com.damaitaliana.client.controller.SinglePlayerController;
import com.damaitaliana.client.i18n.I18n;
import com.damaitaliana.client.persistence.AutosaveService;
import com.damaitaliana.client.ui.save.SaveDialogController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;

class BoardViewControllerTest {

  private SceneRouter sceneRouter;
  private GameSession gameSession;
  private I18n i18n;
  private AutosaveService autosaveService;
  private UserPromptService prompt;
  private ObjectProvider<SinglePlayerController> spProvider;
  private ObjectProvider<SaveDialogController> dialogProvider;
  private BoardViewController controller;
  private SinglePlayerController gameController;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    sceneRouter = Mockito.mock(SceneRouter.class);
    gameSession = Mockito.mock(GameSession.class);
    i18n = Mockito.mock(I18n.class);
    when(i18n.t(anyString())).thenAnswer(inv -> inv.getArgument(0));
    autosaveService = Mockito.mock(AutosaveService.class);
    prompt = Mockito.mock(UserPromptService.class);
    spProvider = Mockito.mock(ObjectProvider.class);
    dialogProvider = Mockito.mock(ObjectProvider.class);
    gameController = Mockito.mock(SinglePlayerController.class);

    controller =
        new BoardViewController(
            sceneRouter, gameSession, i18n, autosaveService, prompt, spProvider, dialogProvider);
    controller.setGameControllerForTest(gameController);
  }

  @Test
  void terminateGameClearsAutosaveAndReturnsToMenu() {
    when(prompt.confirm(
            eq("board.terminate.title"),
            eq("board.terminate.header"),
            eq("board.terminate.content")))
        .thenReturn(true);

    boolean terminated = controller.terminate();

    assertThat(terminated).isTrue();
    verify(gameController).stop();
    verify(autosaveService).clearAutosave();
    verify(gameSession).clear();
    verify(sceneRouter).show(SceneId.MAIN_MENU);
  }

  @Test
  void terminateCancelledByUserStaysOnBoard() {
    when(prompt.confirm(anyString(), anyString(), anyString())).thenReturn(false);

    boolean terminated = controller.terminate();

    assertThat(terminated).isFalse();
    verify(gameController, never()).stop();
    verify(autosaveService, never()).clearAutosave();
    verify(gameSession, never()).clear();
    verify(sceneRouter, never()).show(any());
  }

  @Test
  void onLoadStopsAiThenNavigatesToLoadScreen() {
    controller.onLoad();

    verify(gameController).stop();
    verify(sceneRouter).show(SceneId.LOAD);
  }

  @Test
  void onShowRulesStopsAiThenNavigatesToRulesScreen() {
    controller.onShowRules();

    verify(gameController).stop();
    verify(sceneRouter).show(SceneId.RULES);
  }

  @Test
  void openSaveDialogReturnsFalseWhenNoActiveGame() {
    controller.setGameControllerForTest(null);

    boolean opened = controller.openSaveDialog();

    assertThat(opened).isFalse();
    Mockito.verifyNoInteractions(dialogProvider);
  }
}
