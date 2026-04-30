package com.damaitaliana.client.ui.board;

import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.Square;
import com.damaitaliana.shared.notation.FidNotation;
import java.util.Objects;

/**
 * Builds the {@code accessibleText} for a {@link BoardCellNode} (NFR-U-03 — accessibility for
 * screen readers, Task 3.20). Static helper so the format can be tested without booting JavaFX.
 *
 * <p>Format is intentionally English-only in Fase 3 (matches {@link PieceAccessibleText}): screen
 * readers get a stable, terse description independent of the active locale. Localising a11y strings
 * is a Fase 11 concern.
 *
 * <ul>
 *   <li>Light squares: {@code "Light square (not playable)"}.
 *   <li>Dark empty: {@code "Dark square <FID>, empty"}.
 *   <li>Dark with piece: {@code "Dark square <FID>, <piece description>"}, where the piece part
 *       reuses {@link PieceAccessibleText#describe} for consistency.
 * </ul>
 */
public final class CellAccessibleText {

  private CellAccessibleText() {}

  public static String describe(Square square, boolean dark, Piece piece) {
    Objects.requireNonNull(square, "square");
    if (!dark) {
      return "Light square (not playable)";
    }
    int fid = FidNotation.toFid(square);
    if (piece == null) {
      return "Dark square " + fid + ", empty";
    }
    return "Dark square " + fid + ", " + PieceAccessibleText.describe(piece, square);
  }
}
