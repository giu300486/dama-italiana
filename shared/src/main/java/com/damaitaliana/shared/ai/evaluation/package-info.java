/**
 * Modular static evaluation of a {@link com.damaitaliana.shared.domain.GameState} (SPEC §12.1,
 * ADR-025).
 *
 * <p>An {@link com.damaitaliana.shared.ai.evaluation.Evaluator} returns a centipawn score from a
 * given perspective: positive favours the perspective, negative favours the opponent, zero is
 * balanced.
 *
 * <p>The reference implementation, {@link
 * com.damaitaliana.shared.ai.evaluation.WeightedSumEvaluator}, is a weighted sum of pure {@link
 * com.damaitaliana.shared.ai.evaluation.EvaluationTerm} components. Each term is deterministic,
 * side-effect free, and gives its score in centipawns once multiplied by its weight — which keeps
 * composition readable and tuning local.
 *
 * <p>The five SPEC §12.1 terms (material, mobility, advancement, edge safety, center control) are
 * shipped here. The {@link
 * com.damaitaliana.shared.ai.evaluation.WeightedSumEvaluator#defaultEvaluator()} factory composes
 * them with the SPEC default weights.
 */
package com.damaitaliana.shared.ai.evaluation;
