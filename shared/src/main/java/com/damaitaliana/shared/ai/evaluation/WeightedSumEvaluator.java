package com.damaitaliana.shared.ai.evaluation;

import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import java.util.List;
import java.util.Objects;

/**
 * Composite {@link Evaluator} that sums weighted contributions of a list of {@link
 * EvaluationTerm}s.
 *
 * <p>Each term is paired with an {@code int} weight; the total score is {@code Σ weight_i ·
 * term_i.score(state, perspective)}. Weights ARE allowed to be zero (effectively disabling a term)
 * or negative (rare; useful for testing).
 *
 * <p>The default composition follows SPEC §12.1: material (×1), mobility (×5), advancement (×2),
 * edge safety (×8), center control (×10). In Task 2.1 only the material term is wired; the other
 * four are added in Task 2.2.
 */
public final class WeightedSumEvaluator implements Evaluator {

  /** A term paired with its weight. */
  public record WeightedTerm(EvaluationTerm term, int weight) {
    public WeightedTerm {
      Objects.requireNonNull(term, "term");
    }
  }

  private final List<WeightedTerm> terms;

  /**
   * Builds an evaluator from the given weighted terms. Order does not affect the result.
   *
   * @param terms weighted components; must not be {@code null} (empty is allowed and yields a
   *     zero-evaluator).
   */
  public WeightedSumEvaluator(List<WeightedTerm> terms) {
    Objects.requireNonNull(terms, "terms");
    this.terms = List.copyOf(terms);
  }

  /**
   * SPEC §12.1 default composition: material (×1), mobility (×5), advancement (×2), edge safety
   * (×8), center control (×10).
   */
  public static WeightedSumEvaluator defaultEvaluator() {
    return new WeightedSumEvaluator(
        List.of(
            new WeightedTerm(new MaterialTerm(), 1),
            new WeightedTerm(new MobilityTerm(), 5),
            new WeightedTerm(new AdvancementTerm(), 2),
            new WeightedTerm(new EdgeSafetyTerm(), 8),
            new WeightedTerm(new CenterControlTerm(), 10)));
  }

  @Override
  public int evaluate(GameState state, Color perspective) {
    int total = 0;
    for (WeightedTerm wt : terms) {
      total += wt.weight() * wt.term().score(state, perspective);
    }
    return total;
  }

  /** Read-only view of the weighted terms. Useful for tests and diagnostics. */
  public List<WeightedTerm> terms() {
    return terms;
  }
}
