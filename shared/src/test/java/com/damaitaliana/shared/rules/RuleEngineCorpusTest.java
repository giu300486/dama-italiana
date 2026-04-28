package com.damaitaliana.shared.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.rules.CorpusLoader.Position;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * SPEC §3 — parametric Italian Draughts rule corpus. Each position pins down the legal moves the
 * rule engine should produce, and any move that must NOT appear among them.
 */
class RuleEngineCorpusTest {

  private final ItalianRuleEngine engine = new ItalianRuleEngine();

  @ParameterizedTest(name = "{0} ({1})")
  @MethodSource("positions")
  void positionMatchesEngineLegalMoves(String id, String category, Position position) {
    GameState state =
        new GameState(
            CorpusLoader.buildBoard(position.board()),
            position.sideToMoveColor(),
            0,
            List.of(),
            GameStatus.ONGOING);

    List<String> actualMoves =
        engine.legalMoves(state).stream().map(CorpusLoader::formatMove).toList();

    assertThat(actualMoves)
        .as("expected legal moves for %s — %s", id, position.description())
        .containsExactlyInAnyOrderElementsOf(position.expectedLegalMoves());

    if (position.rejectedMoves() != null) {
      for (String rejected : position.rejectedMoves()) {
        assertThat(actualMoves)
            .as("rejected move %s for %s", rejected, id)
            .doesNotContain(rejected);
      }
    }
  }

  static Stream<Arguments> positions() {
    return CorpusLoader.loadDefault().positions().stream()
        .map(p -> Arguments.of(p.id(), p.category(), p));
  }

  /**
   * Diagnostic helper: prints the legal moves the engine produces for every corpus position.
   * Disabled by default; enable manually to seed {@code expectedLegalMoves} when adding new
   * positions.
   */
  @SuppressWarnings("unused")
  private void printActualMovesForAllPositions() {
    for (Position p : CorpusLoader.loadDefault().positions()) {
      GameState state =
          new GameState(
              CorpusLoader.buildBoard(p.board()),
              p.sideToMoveColor(),
              0,
              List.of(),
              GameStatus.ONGOING);
      List<Move> moves = engine.legalMoves(state);
      System.out.printf(
          "%s [%s] : %s%n",
          p.id(), p.category(), moves.stream().map(CorpusLoader::formatMove).toList());
    }
  }
}
