package com.damaitaliana.shared.ai;

import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Italian Draughts AI engine, sealed over the three SPEC §12.2 difficulty levels (ADR-015,
 * ADR-024).
 *
 * <p>Implementations MUST honour the supplied {@link CancellationToken}. They never throw {@link
 * SearchCancelledException} from {@link #chooseMove(GameState, CancellationToken)}: the underlying
 * iterative-deepening search converts cancellation into a fall-back move and returns normally. The
 * only way {@code chooseMove} returns {@code null} is when the root state is already terminal.
 *
 * <p>Per-level config (depth, default timeout, evaluator, etc.) lives on each implementation as
 * {@code public static final} constants so callers — notably the {@code VirtualThreadAiExecutor}
 * (Task 2.8) — can compose deadlines without re-deriving SPEC values.
 */
public sealed interface AiEngine permits PrincipianteAi, EspertoAi, CampioneAi {

  /**
   * Selects the move {@code state}'s side-to-move should play.
   *
   * @param state the current position. If {@link GameState#status()} is terminal, the result is
   *     {@code null}.
   * @param cancel cooperative cancellation token. The implementation cooperates: if cancelled,
   *     {@code chooseMove} returns the best move found so far instead of throwing.
   * @return the chosen move, or {@code null} on a terminal state.
   */
  Move chooseMove(GameState state, CancellationToken cancel);

  /** Difficulty level of this engine. */
  AiLevel level();

  /**
   * Convenience factory.
   *
   * @param level desired difficulty.
   * @param rng random generator used by {@link PrincipianteAi} for its 25 % noise (ADR-028).
   *     Esperto and Campione do not consume it but it is still required (non-{@code null}) so
   *     callers stay honest about determinism.
   */
  static AiEngine forLevel(AiLevel level, RandomGenerator rng) {
    Objects.requireNonNull(level, "level");
    Objects.requireNonNull(rng, "rng");
    return switch (level) {
      case PRINCIPIANTE -> new PrincipianteAi(rng);
      case ESPERTO -> new EspertoAi();
      case CAMPIONE -> new CampioneAi();
    };
  }
}
