package com.damaitaliana.client.controller;

import com.damaitaliana.client.persistence.SavedGame;
import com.damaitaliana.shared.ai.AiLevel;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * A configured single-player session: AI difficulty, the human player's colour, the user-facing
 * name, the (currently initial) {@link GameState}, and the {@link RandomGenerator} the AI engine
 * will consume for its noise (Principiante level — ADR-028).
 *
 * <p>Used as the payload that {@link com.damaitaliana.client.ui.setup.SinglePlayerSetupController}
 * hands to {@link GameSession} for the board view to pick up after the scene change.
 */
public record SinglePlayerGame(
    AiLevel level, Color humanColor, String name, GameState state, RandomGenerator rng) {

  public SinglePlayerGame {
    Objects.requireNonNull(level, "level");
    Objects.requireNonNull(humanColor, "humanColor");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(rng, "rng");
  }

  /**
   * Builds a {@code SinglePlayerGame} from raw setup-screen values. Returns {@link
   * Optional#empty()} when the supplied {@code name} is blank — the controller surfaces this as a
   * validation error in the UI rather than constructing a half-formed record.
   */
  public static Optional<SinglePlayerGame> tryCreate(
      AiLevel level, ColorChoice colorChoice, String name, RandomGenerator rng) {
    Objects.requireNonNull(level, "level");
    Objects.requireNonNull(colorChoice, "colorChoice");
    Objects.requireNonNull(rng, "rng");
    if (name == null || name.isBlank()) {
      return Optional.empty();
    }
    Color humanColor = colorChoice.resolve(rng);
    return Optional.of(
        new SinglePlayerGame(level, humanColor, name.strip(), GameState.initial(), rng));
  }

  /**
   * Rebuilds a {@code SinglePlayerGame} from an on-disk {@link SavedGame}. The {@link
   * RandomGenerator} is re-injected fresh because RNG state is not persisted (Principiante's
   * pseudo-random move pick stays deterministic only within a single session).
   */
  public static SinglePlayerGame fromSaved(SavedGame saved, RandomGenerator rng) {
    Objects.requireNonNull(saved, "saved");
    Objects.requireNonNull(rng, "rng");
    return new SinglePlayerGame(
        saved.aiLevel(), saved.humanColor(), saved.name(), saved.currentState().toState(), rng);
  }
}
