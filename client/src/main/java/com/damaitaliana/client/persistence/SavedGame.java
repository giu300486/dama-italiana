package com.damaitaliana.client.persistence;

import com.damaitaliana.client.controller.SinglePlayerGame;
import com.damaitaliana.shared.ai.AiLevel;
import com.damaitaliana.shared.domain.Color;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * On-disk schema-versioned representation of a single-player save (ADR-031).
 *
 * <p>{@link #schemaVersion} is the only knob the loader inspects before trusting the rest of the
 * payload: an unknown version is rejected up-front. Version {@value #CURRENT_SCHEMA_VERSION} is the
 * Fase 3 schema; future incompatible changes bump the constant and add a migration path.
 *
 * <p>The {@code moves} list mirrors {@link com.damaitaliana.shared.domain.GameState#history()} as
 * FID-encoded strings; {@link #currentState} is the materialised position the game should resume
 * at. Both are present together because the loader can choose to either trust the snapshot (cheap,
 * default) or re-derive it from the move list (a future audit feature).
 */
public record SavedGame(
    int schemaVersion,
    String kind,
    String name,
    Instant createdAt,
    Instant updatedAt,
    AiLevel aiLevel,
    Color humanColor,
    List<String> moves,
    SerializedGameState currentState) {

  /** Current on-disk schema version. */
  public static final int CURRENT_SCHEMA_VERSION = 1;

  /** Discriminator for single-player save files. */
  public static final String KIND_SINGLE_PLAYER = "SINGLE_PLAYER_GAME";

  @JsonCreator
  public SavedGame(
      @JsonProperty("schemaVersion") int schemaVersion,
      @JsonProperty("kind") String kind,
      @JsonProperty("name") String name,
      @JsonProperty("createdAt") Instant createdAt,
      @JsonProperty("updatedAt") Instant updatedAt,
      @JsonProperty("aiLevel") AiLevel aiLevel,
      @JsonProperty("humanColor") Color humanColor,
      @JsonProperty("moves") List<String> moves,
      @JsonProperty("currentState") SerializedGameState currentState) {
    this.schemaVersion = schemaVersion;
    this.kind = Objects.requireNonNull(kind, "kind");
    this.name = Objects.requireNonNull(name, "name");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    this.aiLevel = Objects.requireNonNull(aiLevel, "aiLevel");
    this.humanColor = Objects.requireNonNull(humanColor, "humanColor");
    this.moves = moves == null ? List.of() : List.copyOf(moves);
    this.currentState = Objects.requireNonNull(currentState, "currentState");
  }

  /**
   * Snapshots {@code game} into a {@code SavedGame} with the supplied timestamps. The {@code moves}
   * list is taken from {@link SerializedGameState#history()} after serialising the current state.
   */
  public static SavedGame of(SinglePlayerGame game, Instant createdAt, Instant updatedAt) {
    Objects.requireNonNull(game, "game");
    SerializedGameState serialized = SerializedGameState.fromState(game.state());
    return new SavedGame(
        CURRENT_SCHEMA_VERSION,
        KIND_SINGLE_PLAYER,
        game.name(),
        createdAt,
        updatedAt,
        game.level(),
        game.humanColor(),
        serialized.history(),
        serialized);
  }

  /** Returns a copy with {@link #updatedAt} set to {@code newUpdatedAt}. */
  public SavedGame withUpdatedAt(Instant newUpdatedAt) {
    return new SavedGame(
        schemaVersion,
        kind,
        name,
        createdAt,
        newUpdatedAt,
        aiLevel,
        humanColor,
        moves,
        currentState);
  }
}
