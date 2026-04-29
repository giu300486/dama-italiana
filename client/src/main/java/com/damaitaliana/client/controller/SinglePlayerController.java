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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Orchestrator for an active single-player session. Owns the live {@link GameState} (the {@link
 * SinglePlayerGame} record stays immutable so the autosave trigger can publish a fresh snapshot on
 * every move) and translates user clicks on the board into legal moves.
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
 * <p>Forward dependencies (animation Task 3.10, AI scheduling Task 3.13) are deliberately not wired
 * here yet; once those tasks land, {@link #applyMove} grows two more steps. The autosave hook is
 * plumbed via {@link AutosaveTrigger}: empty in Fase 3 Task 3.9, replaced by a real disk-backed
 * implementation in Task 3.16.
 */
@Component
@Scope("prototype")
public class SinglePlayerController {

  private static final Logger log = LoggerFactory.getLogger(SinglePlayerController.class);

  private final RuleEngine ruleEngine;
  private final Optional<AutosaveTrigger> autosaveTrigger;
  private final MoveHistoryViewModel history = new MoveHistoryViewModel();

  private SinglePlayerGame game;
  private BoardRenderer renderer;
  private GameState state;
  private Square selected;

  public SinglePlayerController(RuleEngine ruleEngine, Optional<AutosaveTrigger> autosaveTrigger) {
    this.ruleEngine = Objects.requireNonNull(ruleEngine, "ruleEngine");
    this.autosaveTrigger = Objects.requireNonNull(autosaveTrigger, "autosaveTrigger");
  }

  /**
   * Attaches the controller to a freshly-created game and renderer. Must be called once before any
   * user interaction.
   */
  public void start(SinglePlayerGame game, BoardRenderer renderer) {
    this.game = Objects.requireNonNull(game, "game");
    this.renderer = Objects.requireNonNull(renderer, "renderer");
    this.state = game.state();
    this.selected = null;
    renderer.renderState(state.board());
    renderer.setOnCellClicked(this::onCellClicked);
    refreshMandatoryHighlights();
  }

  /** Visible for testing. */
  void onCellClicked(Square clicked) {
    Objects.requireNonNull(clicked, "clicked");
    if (state == null || !state.status().isOngoing()) {
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

  /** Visible for testing. */
  GameState state() {
    return state;
  }

  /** Visible for testing. */
  Square selectedSquare() {
    return selected;
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
