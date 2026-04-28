package com.damaitaliana.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class MoveTest {

  // --- SimpleMove ---

  @Test
  void simpleMoveExposesEndpointsAndNoCaptures() {
    Square from = new Square(0, 0);
    Square to = new Square(1, 1);
    SimpleMove m = new SimpleMove(from, to);
    assertThat(m.from()).isEqualTo(from);
    assertThat(m.to()).isEqualTo(to);
    assertThat(m.capturedSquares()).isEmpty();
    assertThat(m.isCapture()).isFalse();
  }

  @Test
  void simpleMoveRejectsSamesquareEndpoints() {
    Square s = new Square(2, 2);
    assertThatThrownBy(() -> new SimpleMove(s, s)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void simpleMoveRejectsNullEndpoints() {
    assertThatThrownBy(() -> new SimpleMove(null, new Square(0, 0)))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new SimpleMove(new Square(0, 0), null))
        .isInstanceOf(NullPointerException.class);
  }

  // --- CaptureSequence ---

  @Test
  void captureSequenceComputesToFromPath() {
    CaptureSequence c =
        new CaptureSequence(
            new Square(0, 2),
            List.of(new Square(2, 4), new Square(4, 6)),
            List.of(new Square(1, 3), new Square(3, 5)));
    assertThat(c.from()).isEqualTo(new Square(0, 2));
    assertThat(c.to()).isEqualTo(new Square(4, 6));
    assertThat(c.captureCount()).isEqualTo(2);
    assertThat(c.capturedSquares()).hasSize(2);
    assertThat(c.isCapture()).isTrue();
  }

  @Test
  void captureSequenceFreezesItsLists() {
    var path = new java.util.ArrayList<Square>(List.of(new Square(2, 4)));
    var captured = new java.util.ArrayList<Square>(List.of(new Square(1, 3)));
    CaptureSequence c = new CaptureSequence(new Square(0, 2), path, captured);
    path.add(new Square(99 % 8, 99 % 8)); // mutate the source
    captured.add(new Square(0, 0));
    assertThat(c.path()).hasSize(1);
    assertThat(c.captured()).hasSize(1);
  }

  @Test
  void captureSequenceRejectsEmptyPath() {
    assertThatThrownBy(
            () -> new CaptureSequence(new Square(0, 2), List.of(), List.of(new Square(1, 3))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void captureSequenceRejectsMismatchedSizes() {
    assertThatThrownBy(
            () ->
                new CaptureSequence(
                    new Square(0, 2),
                    List.of(new Square(2, 4), new Square(4, 6)),
                    List.of(new Square(1, 3))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("same size");
  }

  @Test
  void captureSequenceRejectsDuplicateCapturedSquare() {
    assertThatThrownBy(
            () ->
                new CaptureSequence(
                    new Square(0, 2),
                    List.of(new Square(2, 4), new Square(0, 2)),
                    List.of(new Square(1, 3), new Square(1, 3))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("distinct");
  }

  @Test
  void captureSequenceRejectsNullArguments() {
    assertThatThrownBy(
            () -> new CaptureSequence(null, List.of(new Square(2, 4)), List.of(new Square(1, 3))))
        .isInstanceOf(NullPointerException.class);
  }

  // --- sealed dispatch ---

  @Test
  void exhaustivePatternMatchOverSealed() {
    Move simple = new SimpleMove(new Square(0, 0), new Square(1, 1));
    Move capture =
        new CaptureSequence(new Square(0, 2), List.of(new Square(2, 4)), List.of(new Square(1, 3)));
    assertThat(describe(simple)).isEqualTo("simple");
    assertThat(describe(capture)).isEqualTo("capture");
  }

  private static String describe(Move m) {
    return switch (m) {
      case SimpleMove ignored -> "simple";
      case CaptureSequence ignored -> "capture";
    };
  }
}
