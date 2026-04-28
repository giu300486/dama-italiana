package com.damaitaliana.shared.ai.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damaitaliana.shared.ai.CancellationToken;
import com.damaitaliana.shared.ai.evaluation.Evaluator;
import com.damaitaliana.shared.ai.evaluation.WeightedSumEvaluator;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import org.junit.jupiter.api.Test;

class MinimaxSearchTtTest {

  private final ItalianRuleEngine ruleEngine = new ItalianRuleEngine();
  private final Evaluator evaluator = WeightedSumEvaluator.defaultEvaluator();
  private final MoveOrderer orderer = new StandardMoveOrderer();

  @Test
  void rejectsTtWithoutHasher() {
    assertThatThrownBy(() -> new MinimaxSearch(ruleEngine, new TranspositionTable(8), null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsHasherWithoutTt() {
    assertThatThrownBy(() -> new MinimaxSearch(ruleEngine, null, new ZobristHasher()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void ttEnabledSearchProducesSameBestMoveAsPlainSearch() {
    GameState start = GameState.initial();
    MinimaxSearch plain = new MinimaxSearch(ruleEngine);
    MinimaxSearch withTt =
        new MinimaxSearch(ruleEngine, new TranspositionTable(1024), new ZobristHasher());
    SearchResult plainResult =
        plain.search(start, 4, evaluator, orderer, CancellationToken.never());
    SearchResult ttResult = withTt.search(start, 4, evaluator, orderer, CancellationToken.never());
    assertThat(ttResult.bestMove()).isEqualTo(plainResult.bestMove());
    assertThat(ttResult.score()).isEqualTo(plainResult.score());
  }

  @Test
  void ttPopulatesEntriesDuringSearch() {
    TranspositionTable tt = new TranspositionTable(1024);
    ZobristHasher hasher = new ZobristHasher();
    MinimaxSearch s = new MinimaxSearch(ruleEngine, tt, hasher);
    GameState start = GameState.initial();
    s.search(start, 3, evaluator, orderer, CancellationToken.never());
    // The root must be in the TT after a completed search.
    long h = hasher.hash(start);
    assertThat(tt.probe(h)).isNotNull();
    assertThat(tt.probe(h).depth()).isEqualTo(3);
  }

  @Test
  void warmTtSpeedsUpRepeatedDepthSearch() {
    GameState start = GameState.initial();
    TranspositionTable tt = new TranspositionTable(1024);
    ZobristHasher hasher = new ZobristHasher();
    MinimaxSearch s = new MinimaxSearch(ruleEngine, tt, hasher);
    SearchResult cold = s.search(start, 3, evaluator, orderer, CancellationToken.never());
    SearchResult warm = s.search(start, 3, evaluator, orderer, CancellationToken.never());
    // Warm search reuses the TT; node count must not exceed the cold one.
    assertThat(warm.nodesVisited()).isLessThanOrEqualTo(cold.nodesVisited());
    // Same correctness either way.
    assertThat(warm.bestMove()).isEqualTo(cold.bestMove());
    assertThat(warm.score()).isEqualTo(cold.score());
  }
}
