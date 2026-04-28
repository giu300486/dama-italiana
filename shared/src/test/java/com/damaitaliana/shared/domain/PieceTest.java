package com.damaitaliana.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PieceTest {

  @Test
  void colorOppositeIsInvolutive() {
    assertThat(Color.WHITE.opposite()).isEqualTo(Color.BLACK);
    assertThat(Color.BLACK.opposite()).isEqualTo(Color.WHITE);
    assertThat(Color.WHITE.opposite().opposite()).isEqualTo(Color.WHITE);
  }

  @Test
  void rejectsNullColor() {
    assertThatThrownBy(() -> new Piece(null, PieceKind.MAN))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsNullKind() {
    assertThatThrownBy(() -> new Piece(Color.WHITE, null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void manAndKindFlagsAgree() {
    Piece man = new Piece(Color.WHITE, PieceKind.MAN);
    Piece king = new Piece(Color.WHITE, PieceKind.KING);
    assertThat(man.isMan()).isTrue();
    assertThat(man.isKing()).isFalse();
    assertThat(king.isMan()).isFalse();
    assertThat(king.isKing()).isTrue();
  }

  @Test
  void promoteReturnsKingOfSameColor() {
    Piece promoted = new Piece(Color.BLACK, PieceKind.MAN).promote();
    assertThat(promoted).isEqualTo(new Piece(Color.BLACK, PieceKind.KING));
  }

  @Test
  void promoteOnKingThrows() {
    assertThatThrownBy(() -> new Piece(Color.WHITE, PieceKind.KING).promote())
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void recordEqualityIsByValue() {
    assertThat(new Piece(Color.WHITE, PieceKind.MAN))
        .isEqualTo(new Piece(Color.WHITE, PieceKind.MAN));
    assertThat(new Piece(Color.WHITE, PieceKind.MAN))
        .isNotEqualTo(new Piece(Color.BLACK, PieceKind.MAN));
  }
}
