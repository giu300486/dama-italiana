package com.damaitaliana.client.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.damaitaliana.client.persistence.AutosaveService;
import com.damaitaliana.shared.ai.AiLevel;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SinglePlayerAutosaveTriggerTest {

  private final AutosaveService autosaveService = Mockito.mock(AutosaveService.class);
  private final SinglePlayerAutosaveTrigger trigger =
      new SinglePlayerAutosaveTrigger(autosaveService);

  @Test
  void onMoveAppliedDelegatesToAutosaveService() {
    SinglePlayerGame game = sampleGame();

    trigger.onMoveApplied(game);

    verify(autosaveService).writeAutosave(game);
  }

  @Test
  void onMoveAppliedSwallowsIoFailures() {
    SinglePlayerGame game = sampleGame();
    doThrow(new UncheckedIOException("disk full", new IOException()))
        .when(autosaveService)
        .writeAutosave(game);

    // Must not propagate — the live game keeps going (FR-SP-08).
    trigger.onMoveApplied(game);

    verify(autosaveService).writeAutosave(game);
  }

  private static SinglePlayerGame sampleGame() {
    return SinglePlayerGame.tryCreate(
            AiLevel.ESPERTO, ColorChoice.WHITE, "Trigger test", new SplittableRandom(7L))
        .orElseThrow();
  }
}
