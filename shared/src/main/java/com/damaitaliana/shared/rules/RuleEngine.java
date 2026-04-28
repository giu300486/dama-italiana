package com.damaitaliana.shared.rules;

import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import com.damaitaliana.shared.domain.Move;
import java.util.List;

/**
 * Italian Draughts rule engine. Implementations DEVE NOT mix Italian rules with other variants
 * (SPEC §3 preamble).
 */
public interface RuleEngine {

  /**
   * All legal moves available to the side-to-move in the given state.
   *
   * <p>If captures are available, the returned list contains <em>only</em> the capture sequences
   * that survive the four laws of precedence (SPEC §3.4). Otherwise it contains the simple sliding
   * moves.
   *
   * <p>An empty list means the side-to-move is stalemated (a loss in the Italian variant — SPEC
   * §3.6); the caller is expected to fold this into the game status via {@link #computeStatus}.
   */
  List<Move> legalMoves(GameState state);

  /**
   * Applies {@code move} to {@code state}, returning the resulting state with the next side to move
   * and a fresh status (vista {@link #computeStatus}).
   *
   * @throws IllegalMoveException if {@code move} is not in {@link #legalMoves(GameState)}.
   */
  GameState applyMove(GameState state, Move move) throws IllegalMoveException;

  /**
   * Computes the status of {@code state}: ongoing, a side wins, or one of the three draw reasons.
   * SPEC §3.6.
   */
  GameStatus computeStatus(GameState state);
}
