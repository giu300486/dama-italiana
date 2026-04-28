package com.damaitaliana.shared.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import com.damaitaliana.shared.rules.RuleEngine;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tournament simulations between AI levels.
 *
 * <p>Two flavours:
 *
 * <ul>
 *   <li>{@link #campionDominatesPrincipianteInQuickSanityCheck()} — fast, untagged. Five games
 *       only. Smoke-checks that the simulation harness works and Campione is decisively stronger
 *       than Principiante.
 *   <li>{@link #campionWinsAtLeast95OutOf100AgainstPrincipiante()} — the SPEC acceptance gating
 *       test (PLAN-fase-2 §A2.2). 100 games at full SPEC depths/timeouts. Tagged {@code "slow"} —
 *       typical runtime ~20–40 min depending on hardware. Run with the {@code slow} group enabled
 *       (or do not exclude it).
 * </ul>
 *
 * <p>Determinism: Principiante's RNG is seeded as {@code 42 + gameIndex}; Campione is fully
 * deterministic. Same hardware ⇒ same outcome bit-for-bit.
 */
class AiTournamentSimulationTest {

  private static final int MAX_PLY_PER_GAME = 200;
  private static final RuleEngine RULE_ENGINE = new ItalianRuleEngine();

  /** Result of a single simulated game. */
  private record GameOutcome(Color winner, GameStatus finalStatus, int plies) {
    /** Returns true if {@code campioneSide} won. */
    boolean campioneWon(Color campioneSide) {
      return winner == campioneSide;
    }
  }

  // --- fast sanity ---

  @Test
  void campionDominatesPrincipianteInQuickSanityCheck() {
    int campioneWins = simulate(5);
    // Even on a small sample Campione should win at least 4 out of 5.
    assertThat(campioneWins).isGreaterThanOrEqualTo(4);
  }

  // --- gating ---

  @Test
  @Tag("slow")
  void campionWinsAtLeast95OutOf100AgainstPrincipiante() {
    int campioneWins = simulate(100);
    assertThat(campioneWins)
        .as("Campione wins out of 100 (PLAN-fase-2 §A2.2 requires >= 95)")
        .isGreaterThanOrEqualTo(95);
  }

  // --- helpers ---

  /**
   * Plays {@code games} matches between Campione and Principiante alternating colours and returns
   * the count of Campione's wins. Draws and games that hit the move cap count as non-wins.
   */
  private static int simulate(int games) {
    int campioneWins = 0;
    for (int i = 0; i < games; i++) {
      Color campioneSide = (i < games / 2) ? Color.WHITE : Color.BLACK;
      AiEngine campione = new CampioneAi();
      AiEngine principiante = new PrincipianteAi(new SplittableRandom(42L + i));
      AiEngine white = (campioneSide == Color.WHITE) ? campione : principiante;
      AiEngine black = (campioneSide == Color.BLACK) ? campione : principiante;
      GameOutcome outcome = playGame(white, black);
      if (outcome.campioneWon(campioneSide)) {
        campioneWins++;
      }
    }
    return campioneWins;
  }

  private static GameOutcome playGame(AiEngine white, AiEngine black) {
    GameState state = GameState.initial();
    int plies = 0;
    while (state.status().isOngoing() && plies < MAX_PLY_PER_GAME) {
      AiEngine current = (state.sideToMove() == Color.WHITE) ? white : black;
      Move move = current.chooseMove(state, CancellationToken.never());
      if (move == null) {
        break; // shouldn't happen on ongoing state, defensive.
      }
      state = RULE_ENGINE.applyMove(state, move);
      plies++;
    }
    GameStatus status = state.status();
    Color winner = null;
    if (status == GameStatus.WHITE_WINS) {
      winner = Color.WHITE;
    } else if (status == GameStatus.BLACK_WINS) {
      winner = Color.BLACK;
    }
    return new GameOutcome(winner, status, plies);
  }
}
