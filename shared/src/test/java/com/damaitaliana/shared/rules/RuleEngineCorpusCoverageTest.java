package com.damaitaliana.shared.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.shared.rules.CorpusLoader.CorpusFile;
import com.damaitaliana.shared.rules.CorpusLoader.Position;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Guards the structural integrity of the corpus: required category counts (CLAUDE.md §2.4.4),
 * unique ids, schema completeness. Failing here means the rule corpus has been depauperated —
 * always grow it, never shrink it.
 */
class RuleEngineCorpusCoverageTest {

  /** Minimum positions per category — vincolante CLAUDE.md §2.4.4. */
  private static final Map<String, Integer> REQUIRED_MINIMUMS = new LinkedHashMap<>();

  static {
    REQUIRED_MINIMUMS.put("mov-pedina", 3);
    REQUIRED_MINIMUMS.put("mov-dama", 4);
    REQUIRED_MINIMUMS.put("cap-pedina", 4);
    REQUIRED_MINIMUMS.put("cap-dama", 4);
    REQUIRED_MINIMUMS.put("pedina-non-cattura-dama", 3);
    REQUIRED_MINIMUMS.put("presa-multipla", 5);
    REQUIRED_MINIMUMS.put("legge-quantita", 5);
    REQUIRED_MINIMUMS.put("legge-qualita", 5);
    REQUIRED_MINIMUMS.put("legge-precedenza-dama", 3);
    REQUIRED_MINIMUMS.put("legge-prima-dama", 3);
    REQUIRED_MINIMUMS.put("promozione-stop", 3);
    REQUIRED_MINIMUMS.put("tripla-ripetizione", 2);
    REQUIRED_MINIMUMS.put("regola-40-mosse", 2);
    REQUIRED_MINIMUMS.put("stallo-sconfitta", 2);
  }

  @Test
  void corpusHasAtLeast48PositionsTotal() {
    CorpusFile file = CorpusLoader.loadDefault();
    assertThat(file.positions()).hasSizeGreaterThanOrEqualTo(48);
  }

  @Test
  void everyCategoryMeetsItsMinimumCount() {
    CorpusFile file = CorpusLoader.loadDefault();
    Map<String, Long> actualCounts =
        file.positions().stream()
            .collect(
                java.util.stream.Collectors.groupingBy(
                    Position::category, java.util.stream.Collectors.counting()));

    for (Map.Entry<String, Integer> required : REQUIRED_MINIMUMS.entrySet()) {
      String category = required.getKey();
      int min = required.getValue();
      Long actual = actualCounts.getOrDefault(category, 0L);
      assertThat(actual)
          .as("category %s requires at least %d positions", category, min)
          .isGreaterThanOrEqualTo((long) min);
    }
  }

  @Test
  void allCategoriesAreKnown() {
    CorpusFile file = CorpusLoader.loadDefault();
    Set<String> allowedCategories = REQUIRED_MINIMUMS.keySet();
    for (Position p : file.positions()) {
      assertThat(allowedCategories)
          .as("unknown category in %s: %s", p.id(), p.category())
          .contains(p.category());
    }
  }

  @Test
  void positionIdsAreUnique() {
    CorpusFile file = CorpusLoader.loadDefault();
    Set<String> ids = new HashSet<>();
    for (Position p : file.positions()) {
      assertThat(ids.add(p.id())).as("duplicate corpus id: %s", p.id()).isTrue();
    }
  }
}
