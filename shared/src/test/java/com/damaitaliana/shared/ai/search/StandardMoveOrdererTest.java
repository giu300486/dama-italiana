package com.damaitaliana.shared.ai.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.shared.domain.CaptureSequence;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.domain.SimpleMove;
import com.damaitaliana.shared.domain.Square;
import java.util.List;
import org.junit.jupiter.api.Test;

class StandardMoveOrdererTest {

  private final StandardMoveOrderer orderer = new StandardMoveOrderer();

  @Test
  void capturesComeBeforeSimpleMoves() {
    SimpleMove simple = new SimpleMove(new Square(0, 0), new Square(1, 1));
    CaptureSequence capture =
        new CaptureSequence(new Square(2, 2), List.of(new Square(4, 4)), List.of(new Square(3, 3)));
    List<Move> input = List.of(simple, capture);
    List<Move> ordered = orderer.order(input, GameState.initial());
    assertThat(ordered).containsExactly(capture, simple);
  }

  @Test
  void longerCapturesComeBeforeShorter() {
    CaptureSequence single =
        new CaptureSequence(new Square(0, 0), List.of(new Square(2, 2)), List.of(new Square(1, 1)));
    CaptureSequence triple =
        new CaptureSequence(
            new Square(7, 7),
            List.of(new Square(5, 5), new Square(3, 3), new Square(1, 1)),
            List.of(new Square(6, 6), new Square(4, 4), new Square(2, 2)));
    CaptureSequence doubleSeq =
        new CaptureSequence(
            new Square(2, 0),
            List.of(new Square(4, 2), new Square(6, 4)),
            List.of(new Square(3, 1), new Square(5, 3)));
    List<Move> ordered = orderer.order(List.of(single, doubleSeq, triple), GameState.initial());
    assertThat(ordered).containsExactly(triple, doubleSeq, single);
  }

  @Test
  void centerDestinationComesBeforeEdgeAmongSimpleMoves() {
    // (3,3) is a centre square. (0,0) is not.
    SimpleMove toCenter = new SimpleMove(new Square(2, 2), new Square(3, 3));
    SimpleMove toEdge = new SimpleMove(new Square(1, 1), new Square(0, 0));
    List<Move> ordered = orderer.order(List.of(toEdge, toCenter), GameState.initial());
    assertThat(ordered).containsExactly(toCenter, toEdge);
  }

  @Test
  void tiesAreBrokenByFromFidAscending() {
    // Two simple moves with non-central destinations.  FID(from) decides.
    SimpleMove fromHigh = new SimpleMove(new Square(0, 0), new Square(1, 1)); // FID(0,0) = 29
    SimpleMove fromLow = new SimpleMove(new Square(1, 7), new Square(0, 6)); // FID(1,7) = 1
    List<Move> ordered = orderer.order(List.of(fromHigh, fromLow), GameState.initial());
    assertThat(ordered).containsExactly(fromLow, fromHigh);
  }

  @Test
  void orderingIsStableForRepeatedCalls() {
    SimpleMove a = new SimpleMove(new Square(0, 0), new Square(1, 1));
    SimpleMove b = new SimpleMove(new Square(2, 0), new Square(3, 1));
    SimpleMove c = new SimpleMove(new Square(4, 0), new Square(5, 1));
    List<Move> input = List.of(a, b, c);
    List<Move> first = orderer.order(input, GameState.initial());
    List<Move> second = orderer.order(input, GameState.initial());
    assertThat(first).isEqualTo(second);
  }

  @Test
  void emptyInputReturnsEmptyList() {
    assertThat(orderer.order(List.of(), GameState.initial())).isEmpty();
  }

  @Test
  void inputListIsNotMutated() {
    SimpleMove a = new SimpleMove(new Square(0, 0), new Square(1, 1));
    SimpleMove b = new SimpleMove(new Square(2, 0), new Square(3, 1));
    List<Move> input = new java.util.ArrayList<>(List.of(b, a));
    orderer.order(input, GameState.initial());
    assertThat(input).containsExactly(b, a);
  }
}
