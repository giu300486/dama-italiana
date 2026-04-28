package com.damaitaliana.shared.ai.search;

import com.damaitaliana.shared.domain.Move;

/**
 * Outcome of a search.
 *
 * @param bestMove the move chosen at the root, or {@code null} when the root state is already
 *     terminal.
 * @param score the score of {@code bestMove} in centipawns from the root sideToMove's perspective.
 *     {@link MinimaxSearch#MATE_SCORE} (modulo distance-to-mate) marks a forced win, and the
 *     symmetric negative value marks a forced loss.
 * @param depthReached the maximum search depth (in ply) actually completed. Set by the iterative
 *     deepening search when an iteration finishes; set to the requested depth for a one-shot {@link
 *     MinimaxSearch}.
 * @param nodesVisited the number of nodes (positions) the search expanded, including the root.
 */
public record SearchResult(Move bestMove, int score, int depthReached, long nodesVisited) {}
