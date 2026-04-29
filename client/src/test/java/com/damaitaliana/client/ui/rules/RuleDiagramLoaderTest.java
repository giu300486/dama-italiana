package com.damaitaliana.client.ui.rules;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RuleDiagramLoaderTest {

  private final RuleDiagramLoader loader = new RuleDiagramLoader();

  @Test
  void setupSectionLoadsTheInitialPosition() {
    List<RuleDiagram> diagrams = loader.loadFor(RuleSection.SETUP);

    assertThat(diagrams).hasSize(1);
    RuleDiagram only = diagrams.get(0);
    assertThat(only.captionKey()).isEqualTo("rules.diagram.setup.initial");
    assertThat(only.position().whiteMen()).hasSize(12);
    assertThat(only.position().blackMen()).hasSize(12);
    assertThat(only.position().whiteKings()).isEmpty();
    assertThat(only.position().blackKings()).isEmpty();
  }

  @Test
  void captureSectionExposesTwoDiagrams() {
    List<RuleDiagram> diagrams = loader.loadFor(RuleSection.CAPTURE);

    assertThat(diagrams).hasSize(2);
    assertThat(diagrams)
        .extracting(RuleDiagram::captionKey)
        .containsExactly("rules.diagram.capture.simple", "rules.diagram.capture.man_vs_king");
  }

  @Test
  void sectionsWithoutDiagramsReturnEmptyList() {
    assertThat(loader.loadFor(RuleSection.PRECEDENCE)).isEmpty();
    assertThat(loader.loadFor(RuleSection.ENDGAME)).isEmpty();
    assertThat(loader.loadFor(RuleSection.NOTATION)).isEmpty();
  }

  @Test
  void coverageMeetsThePlanMinimum() {
    int totalDiagrams = 0;
    int sectionsWithDiagrams = 0;
    for (RuleSection section : RuleSection.ALL) {
      List<RuleDiagram> diagrams = loader.loadFor(section);
      if (!diagrams.isEmpty()) {
        sectionsWithDiagrams++;
        totalDiagrams += diagrams.size();
      }
    }
    assertThat(totalDiagrams).isGreaterThanOrEqualTo(5);
    assertThat(sectionsWithDiagrams).isGreaterThanOrEqualTo(4);
  }
}
