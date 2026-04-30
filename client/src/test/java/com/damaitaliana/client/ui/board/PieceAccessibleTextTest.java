package com.damaitaliana.client.ui.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.PieceKind;
import com.damaitaliana.shared.domain.Square;
import com.damaitaliana.shared.notation.FidNotation;
import org.junit.jupiter.api.Test;

class PieceAccessibleTextTest {

  @Test
  void describesWhiteMan() {
    Square s = new Square(0, 0);
    String desc = PieceAccessibleText.describe(new Piece(Color.WHITE, PieceKind.MAN), s);
    assertThat(desc).isEqualTo("WHITE_MAN at " + FidNotation.toFid(s));
  }

  @Test
  void describesBlackKing() {
    Square s = new Square(7, 7);
    String desc = PieceAccessibleText.describe(new Piece(Color.BLACK, PieceKind.KING), s);
    assertThat(desc).isEqualTo("BLACK_KING at " + FidNotation.toFid(s));
  }

  @Test
  void formatIsScreenReaderFriendly() {
    String desc =
        PieceAccessibleText.describe(new Piece(Color.WHITE, PieceKind.KING), new Square(2, 2));
    assertThat(desc).contains("WHITE", "KING", "at");
  }

  @Test
  void rejectsNullArguments() {
    Square s = new Square(0, 0);
    Piece p = new Piece(Color.WHITE, PieceKind.MAN);
    assertThatNullPointerException().isThrownBy(() -> PieceAccessibleText.describe(null, s));
    assertThatNullPointerException().isThrownBy(() -> PieceAccessibleText.describe(p, null));
  }
}
