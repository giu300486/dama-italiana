package com.damaitaliana.client.ui.save;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.damaitaliana.client.app.ClientProperties;
import com.damaitaliana.client.app.UserPromptService;
import com.damaitaliana.client.controller.ColorChoice;
import com.damaitaliana.client.controller.SinglePlayerGame;
import com.damaitaliana.client.i18n.I18n;
import com.damaitaliana.client.persistence.SaveService;
import com.damaitaliana.client.persistence.SavedGame;
import com.damaitaliana.shared.ai.AiLevel;
import com.damaitaliana.shared.domain.Color;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.SplittableRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class SaveDialogControllerTest {

  @TempDir Path tempDir;

  private SaveService saveService;
  private UserPromptService prompt;
  private I18n i18n;
  private SaveDialogController controller;
  private SinglePlayerGame snapshot;

  @BeforeEach
  void setUp() {
    Path savesDir = tempDir.resolve("saves");
    saveService =
        Mockito.spy(
            new SaveService(
                new ClientProperties(
                    savesDir.toString(), tempDir.resolve("config.json").toString())));
    prompt = Mockito.mock(UserPromptService.class);
    i18n = Mockito.mock(I18n.class);
    when(i18n.t(anyString())).thenAnswer(inv -> inv.getArgument(0));
    when(i18n.t(anyString(), any(Object[].class))).thenAnswer(inv -> inv.getArgument(0));

    Clock fixedClock = Clock.fixed(Instant.parse("2026-04-29T10:00:00Z"), ZoneId.of("UTC"));
    controller = new SaveDialogController(saveService, prompt, i18n, fixedClock);

    snapshot =
        SinglePlayerGame.tryCreate(
                AiLevel.ESPERTO, ColorChoice.WHITE, "Partita Test", new SplittableRandom(42L))
            .orElseThrow();
    controller.setSnapshotForTest(snapshot);
  }

  @Test
  void saveDialogPersistsWithEnteredName() {
    SaveDialogController.ConfirmResult result = controller.confirm("Mia Partita");

    assertThat(result).isEqualTo(SaveDialogController.ConfirmResult.SAVED);
    assertThat(controller.wasSaved()).isTrue();

    ArgumentCaptor<SavedGame> captor = ArgumentCaptor.forClass(SavedGame.class);
    verify(saveService).save(eq("mia-partita"), captor.capture());
    SavedGame written = captor.getValue();
    assertThat(written.name()).isEqualTo("Mia Partita");
    assertThat(written.aiLevel()).isEqualTo(AiLevel.ESPERTO);
    assertThat(written.humanColor()).isEqualTo(Color.WHITE);
    assertThat(written.createdAt()).isEqualTo(Instant.parse("2026-04-29T10:00:00Z"));
    assertThat(written.updatedAt()).isEqualTo(Instant.parse("2026-04-29T10:00:00Z"));

    verify(prompt).info(eq("save.toast.success.title"), eq("save.toast.success.content"));
  }

  @Test
  void emptyNameSurfacesValidationErrorAndDoesNotSave() {
    SaveDialogController.ConfirmResult result = controller.confirm("   ");

    assertThat(result).isEqualTo(SaveDialogController.ConfirmResult.NAME_EMPTY);
    assertThat(controller.wasSaved()).isFalse();
    verify(saveService, never()).save(anyString(), any(SavedGame.class));
    verify(prompt, never()).info(anyString(), anyString());
  }

  @Test
  void unsluggableNameSurfacesInvalidErrorAndDoesNotSave() {
    SaveDialogController.ConfirmResult result = controller.confirm("!!!");

    assertThat(result).isEqualTo(SaveDialogController.ConfirmResult.NAME_INVALID);
    assertThat(controller.wasSaved()).isFalse();
    verify(saveService, never()).save(anyString(), any(SavedGame.class));
  }

  @Test
  void existingSlotPromptsForOverwriteAndAbortsWhenDeclined() throws Exception {
    Path savesDir = tempDir.resolve("saves");
    Files.createDirectories(savesDir);
    Files.writeString(savesDir.resolve("mia-partita.json"), "{}");

    when(prompt.confirm(
            eq("save.dialog.confirm.overwrite.title"),
            eq("save.dialog.confirm.overwrite.header"),
            eq("save.dialog.confirm.overwrite.content")))
        .thenReturn(false);

    SaveDialogController.ConfirmResult result = controller.confirm("Mia Partita");

    assertThat(result).isEqualTo(SaveDialogController.ConfirmResult.OVERWRITE_DECLINED);
    assertThat(controller.wasSaved()).isFalse();
    verify(saveService, never()).save(anyString(), any(SavedGame.class));
  }

  @Test
  void existingSlotProceedsWithSaveWhenOverwriteConfirmed() throws Exception {
    Path savesDir = tempDir.resolve("saves");
    Files.createDirectories(savesDir);
    Files.writeString(savesDir.resolve("mia-partita.json"), "{}");

    when(prompt.confirm(
            eq("save.dialog.confirm.overwrite.title"),
            eq("save.dialog.confirm.overwrite.header"),
            eq("save.dialog.confirm.overwrite.content")))
        .thenReturn(true);

    SaveDialogController.ConfirmResult result = controller.confirm("Mia Partita");

    assertThat(result).isEqualTo(SaveDialogController.ConfirmResult.SAVED);
    verify(saveService).save(eq("mia-partita"), any(SavedGame.class));
    verify(prompt, times(1)).info(eq("save.toast.success.title"), eq("save.toast.success.content"));
  }

  @Test
  void ioErrorSurfacesToastAndKeepsSavedFalse() {
    Mockito.doThrow(new UncheckedIOException("disk full", new java.io.IOException()))
        .when(saveService)
        .save(anyString(), any(SavedGame.class));

    SaveDialogController.ConfirmResult result = controller.confirm("Mia Partita");

    assertThat(result).isEqualTo(SaveDialogController.ConfirmResult.IO_ERROR);
    assertThat(controller.wasSaved()).isFalse();
    verify(prompt).info(eq("save.toast.error.title"), eq("save.toast.error.content"));
  }
}
