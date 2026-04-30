package com.damaitaliana.client.ui.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damaitaliana.shared.domain.CaptureSequence;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.SimpleMove;
import com.damaitaliana.shared.domain.Square;
import com.damaitaliana.shared.notation.FidNotation;
import java.util.List;
import org.junit.jupiter.api.Test;

class MoveHistoryViewModelTest {

  private final MoveHistoryViewModel vm = new MoveHistoryViewModel();

  @Test
  void freshViewModelIsEmpty() {
    assertThat(vm.rows()).isEmpty();
  }

  @Test
  void appendingWhiteMoveCreatesNewRow() {
    SimpleMove white = new SimpleMove(new Square(2, 2), new Square(3, 3));

    vm.appendMove(white, Color.WHITE);

    assertThat(vm.rows()).hasSize(1);
    MoveHistoryRow row = vm.rows().get(0);
    assertThat(row.moveNumber()).isEqualTo(1);
    assertThat(row.whiteFid())
        .isEqualTo(
            FidNotation.formatMove(
                List.of(FidNotation.toFid(new Square(2, 2)), FidNotation.toFid(new Square(3, 3))),
                false));
    assertThat(row.hasBlackMove()).isFalse();
  }

  @Test
  void appendingBlackMoveCompletesRow() {
    SimpleMove white = new SimpleMove(new Square(2, 2), new Square(3, 3));
    SimpleMove black = new SimpleMove(new Square(5, 5), new Square(4, 4));

    vm.appendMove(white, Color.WHITE);
    vm.appendMove(black, Color.BLACK);

    assertThat(vm.rows()).hasSize(1);
    MoveHistoryRow row = vm.rows().get(0);
    assertThat(row.hasBlackMove()).isTrue();
    assertThat(row.blackFid()).isNotEmpty();
  }

  @Test
  void multipleTurnsProduceMultipleRows() {
    vm.appendMove(new SimpleMove(new Square(2, 2), new Square(3, 3)), Color.WHITE);
    vm.appendMove(new SimpleMove(new Square(5, 5), new Square(4, 4)), Color.BLACK);
    vm.appendMove(new SimpleMove(new Square(0, 2), new Square(1, 3)), Color.WHITE);
    vm.appendMove(new SimpleMove(new Square(7, 5), new Square(6, 4)), Color.BLACK);

    assertThat(vm.rows()).hasSize(2);
    assertThat(vm.rows().get(0).moveNumber()).isEqualTo(1);
    assertThat(vm.rows().get(1).moveNumber()).isEqualTo(2);
    assertThat(vm.rows().get(0).hasBlackMove()).isTrue();
    assertThat(vm.rows().get(1).hasBlackMove()).isTrue();
  }

  @Test
  void formatsCaptureSequenceWithCrossNotation() {
    CaptureSequence cs =
        new CaptureSequence(new Square(2, 2), List.of(new Square(4, 4)), List.of(new Square(3, 3)));

    vm.appendMove(cs, Color.WHITE);

    String fid = vm.rows().get(0).whiteFid();
    assertThat(fid).contains("x");
    assertThat(fid).doesNotContain("-");
  }

  @Test
  void formatsMultiCaptureWithMultipleCrosses() {
    CaptureSequence cs =
        new CaptureSequence(
            new Square(2, 2),
            List.of(new Square(4, 4), new Square(6, 2)),
            List.of(new Square(3, 3), new Square(5, 3)));

    vm.appendMove(cs, Color.WHITE);

    String fid = vm.rows().get(0).whiteFid();
    long crosses = fid.chars().filter(c -> c == 'x').count();
    assertThat(crosses).isEqualTo(2);
  }

  @Test
  void blackBeforeWhiteThrows() {
    SimpleMove black = new SimpleMove(new Square(5, 5), new Square(4, 4));
    assertThatThrownBy(() -> vm.appendMove(black, Color.BLACK))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void whiteBeforeBlackCompletesPreviousTurnThrows() {
    vm.appendMove(new SimpleMove(new Square(2, 2), new Square(3, 3)), Color.WHITE);

    assertThatThrownBy(
            () -> vm.appendMove(new SimpleMove(new Square(0, 2), new Square(1, 3)), Color.WHITE))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rejectsNullArguments() {
    assertThatNullPointerException().isThrownBy(() -> vm.appendMove(null, Color.WHITE));
    assertThatNullPointerException()
        .isThrownBy(() -> vm.appendMove(new SimpleMove(new Square(2, 2), new Square(3, 3)), null));
  }

  @Test
  void replaceWithHistoryRebuildsRowsAlternatingFromWhite() {
    SimpleMove white1 = new SimpleMove(new Square(2, 2), new Square(3, 3));
    SimpleMove black1 = new SimpleMove(new Square(0, 4), new Square(1, 3));
    SimpleMove white2 = new SimpleMove(new Square(4, 2), new Square(5, 3));
    vm.appendMove(white1, Color.WHITE);
    vm.appendMove(black1, Color.BLACK);
    vm.appendMove(white2, Color.WHITE);

    SimpleMove other = new SimpleMove(new Square(2, 2), new Square(1, 3));
    vm.replaceWithHistory(List.of(other));

    assertThat(vm.rows()).hasSize(1);
    assertThat(vm.rows().get(0).whiteFid()).isEqualTo(MoveHistoryViewModel.formatFid(other));
    assertThat(vm.rows().get(0).hasBlackMove()).isFalse();
  }

  @Test
  void replaceWithHistoryAcceptsEmptyHistoryAndClearsRows() {
    vm.appendMove(new SimpleMove(new Square(2, 2), new Square(3, 3)), Color.WHITE);
    vm.replaceWithHistory(List.of());
    assertThat(vm.rows()).isEmpty();
  }

  @Test
  void replaceWithHistoryRebuildsTwoFullTurns() {
    SimpleMove w1 = new SimpleMove(new Square(2, 2), new Square(3, 3));
    SimpleMove b1 = new SimpleMove(new Square(0, 4), new Square(1, 3));
    SimpleMove w2 = new SimpleMove(new Square(4, 2), new Square(5, 3));
    SimpleMove b2 = new SimpleMove(new Square(0, 6), new Square(1, 5));

    vm.replaceWithHistory(List.of(w1, b1, w2, b2));

    assertThat(vm.rows()).hasSize(2);
    assertThat(vm.rows().get(0).hasBlackMove()).isTrue();
    assertThat(vm.rows().get(1).hasBlackMove()).isTrue();
  }
}
