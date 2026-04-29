package com.damaitaliana.client.ui.board;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.PieceKind;
import com.damaitaliana.shared.domain.Square;
import com.damaitaliana.shared.notation.FidNotation;
import org.junit.jupiter.api.Test;

class CellAccessibleTextTest {

  @Test
  void lightSquaresAreAnnouncedAsNotPlayable() {
    Square light = new Square(0, 1); // file+rank odd → light
    assertThat(CellAccessibleText.describe(light, false, null))
        .isEqualTo("Light square (not playable)");
  }

  @Test
  void darkEmptyAnnouncesFid() {
    Square fid19 = FidNotation.toSquare(19);
    assertThat(CellAccessibleText.describe(fid19, true, null)).isEqualTo("Dark square 19, empty");
  }

  @Test
  void darkOccupiedAppendsPieceDescription() {
    Square fid12 = FidNotation.toSquare(12);
    Piece whiteMan = new Piece(Color.WHITE, PieceKind.MAN);
    assertThat(CellAccessibleText.describe(fid12, true, whiteMan))
        .isEqualTo("Dark square 12, WHITE_MAN at 12");
  }

  @Test
  void blackKingDescriptionMatchesPieceFormat() {
    Square fid7 = FidNotation.toSquare(7);
    Piece blackKing = new Piece(Color.BLACK, PieceKind.KING);
    assertThat(CellAccessibleText.describe(fid7, true, blackKing))
        .isEqualTo("Dark square 7, BLACK_KING at 7");
  }
}
