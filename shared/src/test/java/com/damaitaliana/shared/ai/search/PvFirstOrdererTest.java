package com.damaitaliana.shared.ai.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.domain.SimpleMove;
import com.damaitaliana.shared.domain.Square;
import java.util.List;
import org.junit.jupiter.api.Test;

class PvFirstOrdererTest {

  private final MoveOrderer base = new StandardMoveOrderer();

  @Test
  void pvMoveIsFirstWhenPresent() {
    SimpleMove a = new SimpleMove(new Square(0, 0), new Square(1, 1));
    SimpleMove b = new SimpleMove(new Square(2, 0), new Square(3, 1));
    SimpleMove c = new SimpleMove(new Square(4, 0), new Square(5, 1));
    List<Move> ordered = new PvFirstOrderer(base, c).order(List.of(a, b, c), GameState.initial());
    assertThat(ordered).hasSize(3);
    assertThat(ordered.get(0)).isEqualTo(c);
    assertThat(ordered).containsExactlyInAnyOrder(a, b, c);
  }

  @Test
  void fallsBackToBaseOrderWhenPvNotInList() {
    SimpleMove a = new SimpleMove(new Square(0, 0), new Square(1, 1));
    SimpleMove b = new SimpleMove(new Square(2, 0), new Square(3, 1));
    SimpleMove notInList = new SimpleMove(new Square(6, 0), new Square(7, 1));
    List<Move> ordered =
        new PvFirstOrderer(base, notInList).order(List.of(a, b), GameState.initial());
    assertThat(ordered).isEqualTo(base.order(List.of(a, b), GameState.initial()));
  }

  @Test
  void nullPvFallsBackToBaseOrder() {
    SimpleMove a = new SimpleMove(new Square(0, 0), new Square(1, 1));
    SimpleMove b = new SimpleMove(new Square(2, 0), new Square(3, 1));
    List<Move> ordered = new PvFirstOrderer(base, null).order(List.of(a, b), GameState.initial());
    assertThat(ordered).isEqualTo(base.order(List.of(a, b), GameState.initial()));
  }

  @Test
  void preservesAllMovesInResult() {
    SimpleMove a = new SimpleMove(new Square(0, 0), new Square(1, 1));
    SimpleMove b = new SimpleMove(new Square(2, 0), new Square(3, 1));
    SimpleMove c = new SimpleMove(new Square(4, 0), new Square(5, 1));
    SimpleMove d = new SimpleMove(new Square(6, 0), new Square(7, 1));
    List<Move> ordered =
        new PvFirstOrderer(base, b).order(List.of(a, b, c, d), GameState.initial());
    assertThat(ordered).containsExactlyInAnyOrder(a, b, c, d);
    assertThat(ordered.get(0)).isEqualTo(b);
  }
}
