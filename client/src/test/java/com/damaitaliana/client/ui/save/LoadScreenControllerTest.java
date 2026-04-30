package com.damaitaliana.client.ui.save;

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
import com.damaitaliana.client.controller.SinglePlayerGame;
import com.damaitaliana.client.i18n.I18n;
import com.damaitaliana.client.i18n.LocaleService;
import com.damaitaliana.client.persistence.SaveService;
import com.damaitaliana.client.persistence.SaveSlotMetadata;
import com.damaitaliana.client.persistence.SavedGame;
import com.damaitaliana.client.persistence.SerializedGameState;
import com.damaitaliana.shared.ai.AiLevel;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class LoadScreenControllerTest {

  private SceneRouter sceneRouter;
  private SaveService saveService;
  private GameSession gameSession;
  private UserPromptService prompt;
  private I18n i18n;
  private LocaleService localeService;
  private MiniatureRenderer miniatureRenderer;
  private LoadScreenController controller;

  @BeforeEach
  void setUp() {
    sceneRouter = Mockito.mock(SceneRouter.class);
    saveService = Mockito.mock(SaveService.class);
    gameSession = Mockito.mock(GameSession.class);
    prompt = Mockito.mock(UserPromptService.class);
    i18n = Mockito.mock(I18n.class);
    when(i18n.t(anyString())).thenAnswer(inv -> inv.getArgument(0));
    when(i18n.t(anyString(), any(Object[].class))).thenAnswer(inv -> inv.getArgument(0));
    localeService = Mockito.mock(LocaleService.class);
    when(localeService.current()).thenReturn(java.util.Locale.ITALIAN);
    miniatureRenderer = Mockito.mock(MiniatureRenderer.class);

    controller =
        new LoadScreenController(
            sceneRouter,
            saveService,
            gameSession,
            prompt,
            i18n,
            localeService,
            miniatureRenderer,
            () -> new SplittableRandom(42L));
  }

  @Test
  void loadScreenListsAllSaves() {
    Instant t0 = Instant.parse("2026-04-29T10:00:00Z");
    when(saveService.listSlots())
        .thenReturn(
            List.of(
                new SaveSlotMetadata("alpha", "Alpha", AiLevel.ESPERTO, Color.WHITE, t0, 4),
                new SaveSlotMetadata(
                    "bravo", "Bravo", AiLevel.CAMPIONE, Color.BLACK, t0.plusSeconds(60), 7),
                new SaveSlotMetadata(
                    "charlie",
                    "Charlie",
                    AiLevel.PRINCIPIANTE,
                    Color.WHITE,
                    t0.plusSeconds(120),
                    1)));

    controller.refresh();

    assertThat(controller.currentSlotsForTest())
        .extracting(SaveSlotMetadata::slot)
        .containsExactly("alpha", "bravo", "charlie");
  }

  @Test
  void clickRowAndLoadResumesAtState() {
    SaveSlotMetadata meta =
        new SaveSlotMetadata(
            "alpha",
            "Alpha",
            AiLevel.ESPERTO,
            Color.BLACK,
            Instant.parse("2026-04-29T10:00:00Z"),
            0);
    SavedGame saved =
        new SavedGame(
            SavedGame.CURRENT_SCHEMA_VERSION,
            SavedGame.KIND_SINGLE_PLAYER,
            "Alpha",
            Instant.parse("2026-04-29T10:00:00Z"),
            Instant.parse("2026-04-29T10:00:00Z"),
            AiLevel.ESPERTO,
            Color.BLACK,
            List.of(),
            SerializedGameState.fromState(GameState.initial()));
    when(saveService.load("alpha")).thenReturn(Optional.of(saved));
    when(prompt.confirm(
            eq("load.confirm.load.title"),
            eq("load.confirm.load.header"),
            eq("load.confirm.load.content"),
            eq("Alpha")))
        .thenReturn(true);

    LoadScreenController.LoadResult result = controller.loadSelected(meta);

    assertThat(result).isEqualTo(LoadScreenController.LoadResult.LOADED);
    ArgumentCaptor<SinglePlayerGame> captor = ArgumentCaptor.forClass(SinglePlayerGame.class);
    verify(gameSession).setCurrentGame(captor.capture());
    SinglePlayerGame loaded = captor.getValue();
    assertThat(loaded.level()).isEqualTo(AiLevel.ESPERTO);
    assertThat(loaded.humanColor()).isEqualTo(Color.BLACK);
    assertThat(loaded.name()).isEqualTo("Alpha");
    assertThat(loaded.state().board()).isEqualTo(GameState.initial().board());
    verify(sceneRouter).show(SceneId.BOARD);
  }

  @Test
  void loadCancelledByUserKeepsBoardUnchanged() {
    SaveSlotMetadata meta =
        new SaveSlotMetadata("alpha", "Alpha", AiLevel.ESPERTO, Color.WHITE, Instant.now(), 0);
    when(prompt.confirm(
            eq("load.confirm.load.title"),
            eq("load.confirm.load.header"),
            eq("load.confirm.load.content"),
            eq("Alpha")))
        .thenReturn(false);

    LoadScreenController.LoadResult result = controller.loadSelected(meta);

    assertThat(result).isEqualTo(LoadScreenController.LoadResult.CANCELLED);
    verify(saveService, never()).load(anyString());
    verify(gameSession, never()).setCurrentGame(any());
    verify(sceneRouter, never()).show(any());
  }

  @Test
  void loadHandlesUnknownSchemaWithToast() {
    SaveSlotMetadata meta =
        new SaveSlotMetadata("future", "Future", AiLevel.ESPERTO, Color.WHITE, Instant.now(), 0);
    when(prompt.confirm(anyString(), anyString(), anyString(), eq("Future"))).thenReturn(true);
    when(saveService.load("future"))
        .thenThrow(new SaveService.UnknownSchemaVersionException("future", 99, 1));

    LoadScreenController.LoadResult result = controller.loadSelected(meta);

    assertThat(result).isEqualTo(LoadScreenController.LoadResult.SCHEMA_MISMATCH);
    verify(prompt)
        .info(
            eq("load.toast.error.schema.title"),
            eq("load.toast.error.schema.content"),
            eq("Future"));
    verify(sceneRouter, never()).show(any());
  }

  @Test
  void loadHandlesIoErrorWithToast() {
    SaveSlotMetadata meta =
        new SaveSlotMetadata("broken", "Broken", AiLevel.ESPERTO, Color.WHITE, Instant.now(), 0);
    when(prompt.confirm(anyString(), anyString(), anyString(), eq("Broken"))).thenReturn(true);
    when(saveService.load("broken"))
        .thenThrow(new UncheckedIOException("disk read failure", new IOException()));

    LoadScreenController.LoadResult result = controller.loadSelected(meta);

    assertThat(result).isEqualTo(LoadScreenController.LoadResult.IO_ERROR);
    verify(prompt)
        .info(eq("load.toast.error.io.title"), eq("load.toast.error.io.content"), eq("Broken"));
  }

  @Test
  void loadWithNoSelectionIsNoOp() {
    LoadScreenController.LoadResult result = controller.loadSelected(null);

    assertThat(result).isEqualTo(LoadScreenController.LoadResult.NO_SELECTION);
    verify(saveService, never()).load(anyString());
    verify(prompt, never()).confirm(anyString(), anyString(), anyString(), any(Object[].class));
  }

  @Test
  void deleteRemovesRow() {
    SaveSlotMetadata meta =
        new SaveSlotMetadata("alpha", "Alpha", AiLevel.ESPERTO, Color.WHITE, Instant.now(), 0);
    when(prompt.confirm(
            eq("load.confirm.delete.title"),
            eq("load.confirm.delete.header"),
            eq("load.confirm.delete.content"),
            eq("Alpha")))
        .thenReturn(true);

    LoadScreenController.DeleteResult result = controller.deleteSelected(meta);

    assertThat(result).isEqualTo(LoadScreenController.DeleteResult.DELETED);
    verify(saveService).delete("alpha");
  }

  @Test
  void deleteCancelledByUserKeepsSlotIntact() {
    SaveSlotMetadata meta =
        new SaveSlotMetadata("alpha", "Alpha", AiLevel.ESPERTO, Color.WHITE, Instant.now(), 0);
    when(prompt.confirm(anyString(), anyString(), anyString(), eq("Alpha"))).thenReturn(false);

    LoadScreenController.DeleteResult result = controller.deleteSelected(meta);

    assertThat(result).isEqualTo(LoadScreenController.DeleteResult.CANCELLED);
    verify(saveService, never()).delete(anyString());
  }

  @Test
  void deleteWithNoSelectionIsNoOp() {
    LoadScreenController.DeleteResult result = controller.deleteSelected(null);

    assertThat(result).isEqualTo(LoadScreenController.DeleteResult.NO_SELECTION);
    verify(saveService, never()).delete(anyString());
  }

  @Test
  void onBackNavigatesToMainMenu() {
    controller.onBack();

    verify(sceneRouter).show(SceneId.MAIN_MENU);
  }
}
