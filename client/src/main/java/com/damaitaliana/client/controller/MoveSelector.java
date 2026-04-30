package com.damaitaliana.client.controller;

import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.domain.Square;
import com.damaitaliana.shared.rules.RuleEngine;
import java.util.List;
import java.util.Objects;

/**
 * Pure helper that filters {@link RuleEngine#legalMoves(GameState)} down to the moves whose {@link
 * Move#from()} matches a single source square.
 *
 * <p>Italian Draughts precedence laws (SPEC §3.4) are already enforced by {@code legalMoves}, so
 * this filter never widens the legal set — it only narrows it to the user-selected piece.
 */
public final class MoveSelector {

  private MoveSelector() {}

  public static List<Move> legalMovesFrom(GameState state, RuleEngine engine, Square from) {
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(engine, "engine");
    Objects.requireNonNull(from, "from");
    return engine.legalMoves(state).stream().filter(m -> m.from().equals(from)).toList();
  }
}
