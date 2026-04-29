package com.damaitaliana.client.controller;

import com.damaitaliana.client.ui.board.BoardRenderer;
import com.damaitaliana.client.ui.board.MoveHistoryViewModel;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.Square;
import com.damaitaliana.shared.rules.IllegalMoveException;
import com.damaitaliana.shared.rules.RuleEngine;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Orchestrator for an active single-player session. Owns the live {@link GameState} (the {@link
 * SinglePlayerGame} record stays immutable so the autosave trigger can publish a fresh snapshot on
 * every move), translates user clicks on the board into legal moves, and — when it is the AI's turn
 * — schedules an asynchronous request through {@link AiTurnService}.
 *
 * <p>Click protocol:
 *
 * <ol>
 *   <li>If the clicked square is a legal target of the currently selected piece, apply the
 *       corresponding move.
 *   <li>Otherwise, if the clicked square hosts a piece of the side to move, select it and show its
 *       legal targets (with mandatory-capture sources still pulsing).
 *   <li>Otherwise, deselect.
 * </ol>
 *
 * <p>While the AI is computing, {@link #busy} is true and {@link #onCellClicked} is a no-op. The AI
 * computation runs on a virtual thread inside {@link AiTurnService}; the resulting {@link
 * CompletableFuture} is composed onto an FX-thread {@link Executor} (configurable via {@link
 * #setFxExecutor} for tests) so the move application happens back on the JavaFX thread. The
 * autosave hook is plumbed via {@link AutosaveTrigger}: empty in production until Task 3.16 wires
 * the disk-backed implementation, mocked in tests.
 *
 * <p>Move animation (Task 3.10) is intentionally still un-wired here; it will be plugged into
 * {@link #applyMove} once the renderer exposes per-square node lookup.
 */
@Component
@Scope("prototype")
public class SinglePlayerController {

  private static final Logger log = LoggerFactory.getLogger(SinglePlayerController.class);

  private final RuleEngine ruleEngine;
  private final Optional<AutosaveTrigger> autosaveTrigger;
  private final Optional<AiTurnService> aiTurnService;
  private final MoveHistoryViewModel history = new MoveHistoryViewModel();
  private final AiThinkingState aiThinkingState = new AiThinkingState();

  private SinglePlayerGame game;
  private BoardRenderer renderer;
  private GameState state;
  private Square selected;
  private Consumer<GameState> stateChangeListener = state -> {};
  private Executor fxExecutor = Platform::runLater;
  private boolean busy;
  private CompletableFuture<Move> pendingAiRequest;

  public SinglePlayerController(
      RuleEngine ruleEngine,
      Optional<AutosaveTrigger> autosaveTrigger,
      Optional<AiTurnService> aiTurnService) {
    this.ruleEngine = Objects.requireNonNull(ruleEngine, "ruleEngine");
    this.autosaveTrigger = Objects.requireNonNull(autosaveTrigger, "autosaveTrigger");
    this.aiTurnService = Objects.requireNonNull(aiTurnService, "aiTurnService");
  }

  /**
   * Attaches the controller to a freshly-created game and renderer. Must be called once before any
   * user interaction. Fires the state-change listener once with the initial state so views (status
   * pane, etc.) render their initial frame, then schedules an AI move if it is already the AI's
   * turn (the human chose Black).
   */
  public void start(SinglePlayerGame game, BoardRenderer renderer) {
    this.game = Objects.requireNonNull(game, "game");
    this.renderer = Objects.requireNonNull(renderer, "renderer");
    this.state = game.state();
    this.selected = null;
    this.busy = false;
    renderer.renderState(state.board());
    renderer.setOnCellClicked(this::onCellClicked);
    refreshMandatoryHighlights();
    fireStateChange();
    scheduleAiTurnIfAi();
  }

  /** Cancels any pending AI request and clears the busy flag. Call when leaving the board view. */
  public void stop() {
    if (pendingAiRequest != null && !pendingAiRequest.isDone()) {
      pendingAiRequest.cancel(true);
    }
    pendingAiRequest = null;
    busy = false;
    aiThinkingState.set(false);
  }

  /**
   * Registers a listener invoked with the current {@link GameState} every time it changes (after
   * {@link #start} and after each successful {@link #applyMove}).
   */
  public void setStateChangeListener(Consumer<GameState> listener) {
    this.stateChangeListener = listener != null ? listener : state -> {};
  }

  /** Replaces the FX-thread executor for AI completion dispatch. Visible for tests. */
  public void setFxExecutor(Executor executor) {
    this.fxExecutor = Objects.requireNonNull(executor, "executor");
  }

  /** Visible for testing. */
  void onCellClicked(Square clicked) {
    Objects.requireNonNull(clicked, "clicked");
    if (busy || state == null || !state.status().isOngoing()) {
      return;
    }
    if (state.sideToMove() != game.humanColor()) {
      // It is the AI's turn: ignore stray clicks even if no request is in flight yet.
      return;
    }

    if (selected != null) {
      Optional<Move> move = matchLegalMoveTo(clicked);
      if (move.isPresent()) {
        applyMove(move.get());
        return;
      }
      if (selected.equals(clicked)) {
        deselect();
        return;
      }
    }

    Optional<Piece> piece = state.board().at(clicked);
    if (piece.isPresent() && piece.get().color() == state.sideToMove()) {
      selectPiece(clicked);
    } else {
      deselect();
    }
  }

  /** Read-only view of the move history. The {@link BoardRenderer} side panel binds to this. */
  public MoveHistoryViewModel history() {
    return history;
  }

  /** Indicator the status pane subscribes to so it can show "AI is thinking…". */
  public AiThinkingState aiThinkingState() {
    return aiThinkingState;
  }

  /** Visible for testing. */
  GameState state() {
    return state;
  }

  /** Visible for testing. */
  Square selectedSquare() {
    return selected;
  }

  /** Visible for testing. */
  boolean busy() {
    return busy;
  }

  private Optional<Move> matchLegalMoveTo(Square target) {
    return MoveSelector.legalMovesFrom(state, ruleEngine, selected).stream()
        .filter(m -> m.to().equals(target))
        .findFirst();
  }

  private void selectPiece(Square s) {
    selected = s;
    List<Move> moves = MoveSelector.legalMovesFrom(state, ruleEngine, s);
    List<Square> targets = moves.stream().map(Move::to).distinct().toList();
    renderer.highlightLegalTargets(targets);
  }

  private void deselect() {
    selected = null;
    renderer.clearHighlights();
    refreshMandatoryHighlights();
  }

  private void applyMove(Move move) {
    Color sideThatMoved = state.sideToMove();
    try {
      state = ruleEngine.applyMove(state, move);
    } catch (IllegalMoveException ex) {
      log.warn("Move {} rejected by rule engine despite legality filter", move, ex);
      deselect();
      return;
    }
    history.appendMove(move, sideThatMoved);
    selected = null;
    renderer.renderState(state.board());
    renderer.clearHighlights();
    refreshMandatoryHighlights();
    autosaveTrigger.ifPresent(t -> t.onMoveApplied(snapshot()));
    fireStateChange();
    scheduleAiTurnIfAi();
  }

  private void scheduleAiTurnIfAi() {
    if (!state.status().isOngoing()) {
      return;
    }
    if (state.sideToMove() == game.humanColor()) {
      return;
    }
    if (aiTurnService.isEmpty()) {
      // No AI bean wired (test harness without AI): leave the game waiting.
      return;
    }
    busy = true;
    aiThinkingState.set(true);
    pendingAiRequest = aiTurnService.get().requestMove(state, game.level(), game.rng());
    pendingAiRequest.whenCompleteAsync(this::onAiCompletion, fxExecutor);
  }

  private void onAiCompletion(Move move, Throwable error) {
    aiThinkingState.set(false);
    busy = false;
    pendingAiRequest = null;
    if (error != null) {
      log.warn("AI move request failed", error);
      return;
    }
    if (move == null) {
      return;
    }
    applyMove(move);
  }

  private void fireStateChange() {
    stateChangeListener.accept(state);
  }

  private void refreshMandatoryHighlights() {
    if (!state.status().isOngoing()) {
      renderer.highlightMandatorySources(List.of());
      return;
    }
    List<Square> sources =
        ruleEngine.legalMoves(state).stream()
            .filter(m -> !m.capturedSquares().isEmpty())
            .map(Move::from)
            .distinct()
            .toList();
    renderer.highlightMandatorySources(sources);
  }

  private SinglePlayerGame snapshot() {
    return new SinglePlayerGame(game.level(), game.humanColor(), game.name(), state, game.rng());
  }
}
