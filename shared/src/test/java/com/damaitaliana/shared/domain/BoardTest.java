package com.damaitaliana.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BoardTest {

  // --- empty / initial factories ---

  @Test
  void emptyBoardHasNoPieces() {
    Board b = Board.empty();
    assertThat(b.totalPieces()).isZero();
    for (int rank = 0; rank < 8; rank++) {
      for (int file = 0; file < 8; file++) {
        assertThat(b.at(new Square(file, rank))).isEmpty();
      }
    }
  }

  @Test
  void initialBoardHasTwentyFourPieces() {
    Board b = Board.initial();
    assertThat(b.totalPieces()).isEqualTo(24);
    assertThat(b.countPieces(Color.WHITE)).isEqualTo(12);
    assertThat(b.countPieces(Color.BLACK)).isEqualTo(12);
  }

  @Test
  void initialBoardPlacesWhiteOnRanks0to2AndBlackOnRanks5to7() {
    Board b = Board.initial();
    for (int rank = 0; rank < 8; rank++) {
      for (int file = 0; file < 8; file++) {
        Square s = new Square(file, rank);
        if (!s.isDark() || rank == 3 || rank == 4) {
          assertThat(b.at(s)).as("square %s", s).isEmpty();
        } else if (rank <= 2) {
          assertThat(b.at(s).orElseThrow())
              .as("white side at %s", s)
              .isEqualTo(new Piece(Color.WHITE, PieceKind.MAN));
        } else {
          assertThat(b.at(s).orElseThrow())
              .as("black side at %s", s)
              .isEqualTo(new Piece(Color.BLACK, PieceKind.MAN));
        }
      }
    }
  }

  // --- with / without ---

  @Test
  void withPlacesAndIsImmutable() {
    Board original = Board.empty();
    Square s = new Square(0, 0);
    Piece p = new Piece(Color.WHITE, PieceKind.KING);
    Board updated = original.with(s, p);
    assertThat(original.at(s)).isEmpty();
    assertThat(updated.at(s)).contains(p);
    assertThat(updated).isNotSameAs(original);
  }

  @Test
  void withRejectsLightSquare() {
    Square light = new Square(1, 0);
    assertThat(light.isDark()).isFalse();
    assertThatThrownBy(() -> Board.empty().with(light, new Piece(Color.WHITE, PieceKind.MAN)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("light");
  }

  @Test
  void withRejectsNullPiece() {
    assertThatThrownBy(() -> Board.empty().with(new Square(0, 0), null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void withReplacesExistingPiece() {
    Square s = new Square(0, 0);
    Piece man = new Piece(Color.WHITE, PieceKind.MAN);
    Piece king = new Piece(Color.BLACK, PieceKind.KING);
    Board b = Board.empty().with(s, man).with(s, king);
    assertThat(b.at(s)).contains(king);
  }

  @Test
  void withoutClearsAndIsImmutable() {
    Square s = new Square(0, 0);
    Board placed = Board.empty().with(s, new Piece(Color.WHITE, PieceKind.MAN));
    Board cleared = placed.without(s);
    assertThat(placed.at(s)).isPresent();
    assertThat(cleared.at(s)).isEmpty();
  }

  @Test
  void withoutOnEmptySquareIsIdentity() {
    Board b = Board.empty();
    assertThat(b.without(new Square(0, 0))).isSameAs(b);
  }

  // --- streams and counts ---

  @Test
  void occupiedByFiltersCorrectly() {
    Board b = Board.initial();
    assertThat(b.occupiedBy(Color.WHITE).count()).isEqualTo(12);
    assertThat(b.occupiedBy(Color.BLACK).count()).isEqualTo(12);
  }

  @Test
  void occupiedByRejectsNullColor() {
    assertThatThrownBy(() -> Board.empty().occupiedBy(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void occupiedListsAllPieces() {
    Board b =
        Board.empty()
            .with(new Square(0, 0), new Piece(Color.WHITE, PieceKind.MAN))
            .with(new Square(2, 0), new Piece(Color.BLACK, PieceKind.KING));
    assertThat(b.occupied()).containsExactlyInAnyOrder(new Square(0, 0), new Square(2, 0));
  }

  // --- equals / hashCode ---

  @Test
  void equalsAndHashCodeAreContentBased() {
    Board a = Board.initial();
    Board b = Board.initial();
    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
  }

  @Test
  void equalsDistinguishesDifferentBoards() {
    Board a = Board.initial();
    Board b = a.without(new Square(0, 0));
    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void notEqualToDifferentType() {
    assertThat(Board.empty()).isNotEqualTo("not a board");
  }

  // --- toString smoke (we don't pin layout, but it shouldn't crash and should mention pieces) ---

  @Test
  void toStringContainsPieceSymbols() {
    String s = Board.initial().toString();
    assertThat(s).contains("w").contains("b");
  }

  @Test
  void toStringHasFileFooter() {
    assertThat(Board.empty().toString()).contains("abcdefgh");
  }
}
