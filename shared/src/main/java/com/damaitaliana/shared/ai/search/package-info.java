/**
 * Search algorithm of the AI engine (SPEC §12.1, ADR-024 / ADR-026).
 *
 * <p>Negamax search with alpha-beta pruning, iterative deepening, move ordering, Zobrist hashing
 * and a transposition table. The search consumes a {@link
 * com.damaitaliana.shared.ai.evaluation.Evaluator} for static scores and a {@link
 * com.damaitaliana.shared.rules.RuleEngine} for legal-move generation; both are injected so the
 * search itself stays variant-agnostic.
 *
 * <p>Cancellation is cooperative (see {@link com.damaitaliana.shared.ai.CancellationToken},
 * ADR-027): nodes invoke {@code throwIfCancelled()} so a {@code Future#cancel(true)} on the
 * executing virtual thread takes effect at the next node visit, never inside a tight loop.
 */
package com.damaitaliana.shared.ai.search;
