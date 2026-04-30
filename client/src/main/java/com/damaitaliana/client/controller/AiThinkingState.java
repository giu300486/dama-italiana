package com.damaitaliana.client.controller;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Tracks whether the AI is currently computing a move so the status pane can show a "thinking…"
 * affordance. Plain POJO with a {@code Consumer<Boolean>} listener — lighter than a full JavaFX
 * {@code BooleanProperty} and keeps the controller / its tests free of toolkit binding plumbing.
 *
 * <p>The owning {@link SinglePlayerController} mutates the state on the FX thread; readers should
 * not assume thread safety beyond the single-writer invariant.
 */
public class AiThinkingState {

  private boolean thinking;
  private Consumer<Boolean> listener = b -> {};

  public boolean isThinking() {
    return thinking;
  }

  public void set(boolean newValue) {
    if (this.thinking == newValue) {
      return;
    }
    this.thinking = newValue;
    listener.accept(newValue);
  }

  /** Replaces any previous listener. {@code null} reverts to the no-op listener. */
  public void onChange(Consumer<Boolean> listener) {
    this.listener = listener != null ? Objects.requireNonNull(listener) : b -> {};
  }
}
