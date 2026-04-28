package com.damaitaliana.client.ui.board;

import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.PieceKind;
import com.damaitaliana.shared.domain.Square;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

/**
 * Visual for a single piece. The body is a {@link Circle} sized to a fraction of the cell, styled
 * via the {@code .piece} / {@code .piece-black} / {@code .piece-king} classes from {@code
 * components.css}. Kings receive an inner {@link Circle} ring for clear visual differentiation
 * independent of stroke variations.
 */
final class PieceNode extends StackPane {

  private final Piece piece;
  private final Square square;

  PieceNode(Piece piece, Square square) {
    this.piece = piece;
    this.square = square;

    Circle body = new Circle();
    body.radiusProperty().bind(widthProperty().divide(2.4));
    body.getStyleClass().add("piece");
    if (piece.color() == com.damaitaliana.shared.domain.Color.BLACK) {
      body.getStyleClass().add("piece-black");
    }
    getChildren().add(body);

    if (piece.kind() == PieceKind.KING) {
      Circle ring = new Circle();
      ring.radiusProperty().bind(widthProperty().divide(4.0));
      ring.getStyleClass().add("piece");
      ring.getStyleClass().add("piece-king");
      if (piece.color() == com.damaitaliana.shared.domain.Color.BLACK) {
        ring.getStyleClass().add("piece-black");
      }
      getChildren().add(ring);
    }

    setMouseTransparent(true);
    setAccessibleText(PieceAccessibleText.describe(piece, square));
  }

  Piece piece() {
    return piece;
  }

  Square square() {
    return square;
  }
}
