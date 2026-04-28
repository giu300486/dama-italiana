/**
 * AI engine of the Italian Draughts game (SPEC §12).
 *
 * <p>Three difficulty levels — Principiante, Esperto, Campione — implemented as a sealed family
 * over a Minimax + alpha-beta search (ADR-024, ADR-015). Common pieces:
 *
 * <ul>
 *   <li>{@code evaluation} — modular evaluation function with five terms (material, mobility,
 *       advancement, edge safety, center control) per SPEC §12.1 (ADR-025).
 *   <li>{@code search} — the search algorithm itself: minimax with alpha-beta, iterative deepening,
 *       move ordering, Zobrist hashing and a transposition table (ADR-026).
 * </ul>
 *
 * <p>Constraints:
 *
 * <ul>
 *   <li>Pure logic — no Spring, JavaFX, JPA, or WebSocket library (CLAUDE.md §8.7).
 *   <li>Cooperative cancellation only ({@link com.damaitaliana.shared.ai.CancellationToken},
 *       ADR-027). The search must check the token at every node so {@code Future#cancel(true)} on
 *       the executing virtual thread takes effect within ~200 ms (SPEC §12.2 "non blocca la UI").
 *   <li>The Principiante level is the only deterministic-but-noisy actor; its randomness is
 *       injected as a {@link java.util.random.RandomGenerator} so tests can pin a seed (ADR-028).
 * </ul>
 *
 * <p>Extension: to add a new evaluation term, implement {@link
 * com.damaitaliana.shared.ai.evaluation.EvaluationTerm} and compose it inside a {@link
 * com.damaitaliana.shared.ai.evaluation.WeightedSumEvaluator}. To add a new difficulty level, add a
 * new {@code permits} entry to {@link com.damaitaliana.shared.ai.AiEngine}.
 */
package com.damaitaliana.shared.ai;
