package com.damaitaliana.client.ui.board;

import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.PieceKind;
import com.damaitaliana.shared.domain.Square;
import com.damaitaliana.shared.notation.FidNotation;
import java.util.Objects;

/**
 * Builds the {@code accessibleText} for a {@code PieceNode} (NFR-U-03 — accessibility for screen
 * readers). Static helper so the format can be tested without booting the JavaFX toolkit.
 *
 * <p>Format is intentionally English-only in Fase 3: short, predictable, screen-reader-friendly.
 * Localising a11y strings is a Fase 11 polish concern; the form here is "{@code WHITE_MAN at 12}".
 */
public final class PieceAccessibleText {

  private PieceAccessibleText() {}

  public static String describe(Piece piece, Square square) {
    Objects.requireNonNull(piece, "piece");
    Objects.requireNonNull(square, "square");
    String color = piece.color().name();
    String kind = piece.kind() == PieceKind.MAN ? "MAN" : "KING";
    int fid = FidNotation.toFid(square);
    return color + "_" + kind + " at " + fid;
  }
}
