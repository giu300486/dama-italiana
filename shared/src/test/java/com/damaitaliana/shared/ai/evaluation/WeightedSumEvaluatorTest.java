package com.damaitaliana.shared.ai.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damaitaliana.shared.ai.evaluation.WeightedSumEvaluator.WeightedTerm;
import com.damaitaliana.shared.domain.Board;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.PieceKind;
import com.damaitaliana.shared.domain.Square;
import java.util.List;
import org.junit.jupiter.api.Test;

class WeightedSumEvaluatorTest {

  // --- composition ---

  @Test
  void emptyCompositionEvaluatesToZero() {
    WeightedSumEvaluator evaluator = new WeightedSumEvaluator(List.of());
    assertThat(evaluator.evaluate(GameState.initial(), Color.WHITE)).isZero();
  }

  @Test
  void compositeReturnsWeightedSumOfTerms() {
    EvaluationTerm constantFive = (s, p) -> 5;
    EvaluationTerm constantTen = (s, p) -> 10;
    WeightedSumEvaluator evaluator =
        new WeightedSumEvaluator(
            List.of(new WeightedTerm(constantFive, 3), new WeightedTerm(constantTen, 2)));
    // 3 * 5 + 2 * 10 = 35
    assertThat(evaluator.evaluate(GameState.initial(), Color.WHITE)).isEqualTo(35);
  }

  @Test
  void zeroWeightZeroesContributionToTotal() {
    EvaluationTerm constantOne = (s, p) -> 1;
    EvaluationTerm constantHundred = (s, p) -> 100;
    WeightedSumEvaluator evaluator =
        new WeightedSumEvaluator(
            List.of(new WeightedTerm(constantOne, 4), new WeightedTerm(constantHundred, 0)));
    assertThat(evaluator.evaluate(GameState.initial(), Color.WHITE)).isEqualTo(4);
  }

  @Test
  void negativeWeightFlipsContribution() {
    EvaluationTerm constantFive = (s, p) -> 5;
    WeightedSumEvaluator evaluator =
        new WeightedSumEvaluator(List.of(new WeightedTerm(constantFive, -3)));
    assertThat(evaluator.evaluate(GameState.initial(), Color.WHITE)).isEqualTo(-15);
  }

  // --- defaults ---

  @Test
  void defaultEvaluatorIsZeroOnInitialPosition() {
    WeightedSumEvaluator evaluator = WeightedSumEvaluator.defaultEvaluator();
    assertThat(evaluator.evaluate(GameState.initial(), Color.WHITE)).isZero();
    assertThat(evaluator.evaluate(GameState.initial(), Color.BLACK)).isZero();
  }

  @Test
  void defaultEvaluatorIncludesMaterialTermAtWeightOne() {
    WeightedSumEvaluator evaluator = WeightedSumEvaluator.defaultEvaluator();
    Board b =
        Board.empty()
            .with(new Square(0, 0), new Piece(Color.WHITE, PieceKind.KING))
            .with(new Square(7, 7), new Piece(Color.BLACK, PieceKind.MAN));
    GameState s = new GameState(b, Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    // king(300) - man(100) = 200 from White's perspective
    assertThat(evaluator.evaluate(s, Color.WHITE))
        .isEqualTo(MaterialTerm.KING_VALUE - MaterialTerm.MAN_VALUE);
  }

  @Test
  void termsListIsImmutable() {
    EvaluationTerm constantOne = (s, p) -> 1;
    var input = new java.util.ArrayList<WeightedTerm>();
    input.add(new WeightedTerm(constantOne, 1));
    WeightedSumEvaluator evaluator = new WeightedSumEvaluator(input);
    assertThatThrownBy(() -> evaluator.terms().add(new WeightedTerm(constantOne, 1)))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  // --- input validation ---

  @Test
  void rejectsNullTermsList() {
    assertThatThrownBy(() -> new WeightedSumEvaluator(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsNullTermInsideRecord() {
    assertThatThrownBy(() -> new WeightedTerm(null, 1)).isInstanceOf(NullPointerException.class);
  }
}
