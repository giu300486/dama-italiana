package com.damaitaliana.client.controller;

import com.damaitaliana.client.persistence.AutosaveService;
import java.io.UncheckedIOException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Production wiring of {@link AutosaveTrigger}: forwards every {@code onMoveApplied} callback to
 * {@link AutosaveService#writeAutosave(SinglePlayerGame)}.
 *
 * <p>An autosave write failure must never derail the live game, so a thrown {@link
 * UncheckedIOException} is logged at {@code WARN} and swallowed. The next applied move will retry
 * the write naturally; the user's session keeps going either way (FR-SP-08, A3.5).
 */
@Component
public class SinglePlayerAutosaveTrigger implements AutosaveTrigger {

  private static final Logger log = LoggerFactory.getLogger(SinglePlayerAutosaveTrigger.class);

  private final AutosaveService autosaveService;

  public SinglePlayerAutosaveTrigger(AutosaveService autosaveService) {
    this.autosaveService = Objects.requireNonNull(autosaveService, "autosaveService");
  }

  @Override
  public void onMoveApplied(SinglePlayerGame game) {
    try {
      autosaveService.writeAutosave(game);
    } catch (UncheckedIOException ex) {
      log.warn("Autosave write failed; ignoring (will retry on next move)", ex);
    }
  }
}
