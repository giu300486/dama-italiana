package com.damaitaliana.client.controller;

import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Singleton bridge that lets the {@link
 * com.damaitaliana.client.ui.setup.SinglePlayerSetupController setup screen} hand the configured
 * {@link SinglePlayerGame} to the board view across the FXML scene change (the next controller is
 * instantiated with no constructor argument carrying the game).
 *
 * <p>Held in a {@code volatile} field so writes from the FX thread are visible to subsequent reads
 * on any thread (the AI loop runs on a virtual thread).
 */
@Component
public class GameSession {

  private volatile SinglePlayerGame currentGame;

  public void setCurrentGame(SinglePlayerGame game) {
    this.currentGame = Objects.requireNonNull(game, "game");
  }

  public Optional<SinglePlayerGame> currentGame() {
    return Optional.ofNullable(currentGame);
  }

  public void clear() {
    this.currentGame = null;
  }
}
