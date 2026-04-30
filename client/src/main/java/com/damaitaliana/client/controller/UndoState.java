package com.damaitaliana.client.controller;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Tracks whether the {@link SinglePlayerController} can undo or redo a (human + AI) pair, so the
 * board view can disable the menu items / shortcuts when the action would be a no-op (Task 3.24,
 * FR-SP-06). Plain POJO with a {@code BiConsumer<Boolean, Boolean>} listener that fires whenever
 * either flag flips — same lightweight pattern as {@link AiThinkingState} (no JavaFX property
 * plumbing in the controller / tests).
 *
 * <p>The owning {@link SinglePlayerController} mutates the state on the FX thread; readers should
 * not assume thread safety beyond the single-writer invariant.
 */
public class UndoState {

  private boolean canUndo;
  private boolean canRedo;
  private BiConsumer<Boolean, Boolean> listener = (u, r) -> {};

  public boolean canUndo() {
    return canUndo;
  }

  public boolean canRedo() {
    return canRedo;
  }

  public void set(boolean newCanUndo, boolean newCanRedo) {
    if (this.canUndo == newCanUndo && this.canRedo == newCanRedo) {
      return;
    }
    this.canUndo = newCanUndo;
    this.canRedo = newCanRedo;
    listener.accept(newCanUndo, newCanRedo);
  }

  /** Replaces any previous listener. {@code null} reverts to the no-op listener. */
  public void onChange(BiConsumer<Boolean, Boolean> listener) {
    this.listener = listener != null ? Objects.requireNonNull(listener) : (u, r) -> {};
  }
}
