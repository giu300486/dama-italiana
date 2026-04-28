package com.damaitaliana.shared.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.shared.rules.CorpusLoader.BoardSpec;
import com.damaitaliana.shared.rules.CorpusLoader.CorpusFile;
import com.damaitaliana.shared.rules.CorpusLoader.Position;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Validates the JSON schema of {@code test-positions.json} field-by-field. */
class RuleEngineCorpusSchemaTest {

  @Test
  void corpusVersionIs1() {
    assertThat(CorpusLoader.loadDefault().version()).isEqualTo(1);
  }

  @Test
  void allPositionsHaveAllRequiredFields() {
    CorpusFile file = CorpusLoader.loadDefault();
    for (Position p : file.positions()) {
      assertThat(p.id()).as("id missing").isNotBlank();
      assertThat(p.description()).as("description for %s", p.id()).isNotBlank();
      assertThat(p.specReference()).as("specReference for %s", p.id()).isNotBlank();
      assertThat(p.category()).as("category for %s", p.id()).isNotBlank();
      assertThat(p.sideToMove()).as("sideToMove for %s", p.id()).matches("WHITE|BLACK");
      assertThat(p.expectedLegalMoves()).as("expectedLegalMoves for %s", p.id()).isNotNull();
      assertThat(p.board()).as("board for %s", p.id()).isNotNull();
    }
  }

  @Test
  void boardSquaresAreInRangeAndDisjoint() {
    CorpusFile file = CorpusLoader.loadDefault();
    for (Position p : file.positions()) {
      BoardSpec b = p.board();
      Set<Integer> seen = new HashSet<>();
      check(b.whiteMen(), p.id(), seen);
      check(b.whiteKings(), p.id(), seen);
      check(b.blackMen(), p.id(), seen);
      check(b.blackKings(), p.id(), seen);
    }
  }

  private static void check(List<Integer> squares, String id, Set<Integer> seen) {
    for (int n : squares) {
      assertThat(n).as("square in %s out of range [1,32]", id).isBetween(1, 32);
      assertThat(seen.add(n)).as("duplicate square %d in %s", n, id).isTrue();
    }
  }
}
