package com.damaitaliana.client.persistence;

import com.damaitaliana.shared.ai.AiLevel;
import com.damaitaliana.shared.domain.Color;
import java.time.Instant;
import java.util.Objects;

/**
 * Lightweight metadata used by the load screen (Task 3.15) to render a row in the save table
 * without deserialising the full {@link SavedGame} payload.
 *
 * @param slot filesystem slug used as the JSON file basename (no extension).
 * @param name user-facing save name as entered in the save dialog.
 * @param aiLevel AI difficulty the game was started against.
 * @param humanColor colour the player picked.
 * @param updatedAt last write timestamp.
 * @param currentMoveNumber number of full moves played so far (move list size).
 */
public record SaveSlotMetadata(
    String slot,
    String name,
    AiLevel aiLevel,
    Color humanColor,
    Instant updatedAt,
    int currentMoveNumber) {

  public SaveSlotMetadata {
    Objects.requireNonNull(slot, "slot");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(aiLevel, "aiLevel");
    Objects.requireNonNull(humanColor, "humanColor");
    Objects.requireNonNull(updatedAt, "updatedAt");
    if (currentMoveNumber < 0) {
      throw new IllegalArgumentException(
          "currentMoveNumber must be non-negative: " + currentMoveNumber);
    }
  }
}
