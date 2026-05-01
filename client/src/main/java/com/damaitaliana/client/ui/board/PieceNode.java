package com.damaitaliana.client.ui.board;

import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.PieceKind;
import com.damaitaliana.shared.domain.Square;
import javafx.beans.binding.Bindings;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Visual for a single piece — wood-premium 3D-look (Task 3.5.7).
 *
 * <p>Composition is a {@link StackPane} with up to four children, each picking up its appearance
 * from {@code components.css}:
 *
 * <ul>
 *   <li>{@code body} — {@link Circle} with a radial-gradient fill (style class {@code .piece}, plus
 *       {@code .piece-black} for black, plus {@code .piece-king} for kings — preserves the legacy
 *       class names so the gold king stroke and existing tests still apply).
 *   <li>{@code ring} — outer dashed {@link Circle} ({@code .piece-ring} / {@code
 *       .piece-ring-black}) hinting at the carved groove on a physical disc.
 *   <li>{@code gloss} — small translucent {@link Circle} ({@code .piece-gloss}) translated toward
 *       the upper-left to mimic a light reflection.
 *   <li>{@code crown} — only for kings: {@link Text} U+265B (♛) styled {@code .piece-king-marker}
 *       (gold on white pieces) plus {@code .piece-king-marker-black} (deep red on black pieces).
 *       Font size scales with the cell width via a property binding.
 * </ul>
 */
final class PieceNode extends StackPane {

  static final String CROWN_GLYPH = "♛"; // ♛

  private final Piece piece;
  private final Square square;

  PieceNode(Piece piece, Square square) {
    this.piece = piece;
    this.square = square;
    boolean isBlack = piece.color() == com.damaitaliana.shared.domain.Color.BLACK;
    boolean isKing = piece.kind() == PieceKind.KING;

    Circle body = new Circle();
    body.radiusProperty().bind(widthProperty().divide(2.4));
    body.getStyleClass().add("piece");
    if (isBlack) {
      body.getStyleClass().add("piece-black");
    }
    if (isKing) {
      body.getStyleClass().add("piece-king");
    }
    getChildren().add(body);

    Circle ring = new Circle();
    ring.radiusProperty().bind(widthProperty().multiply(0.405));
    ring.setMouseTransparent(true);
    ring.getStyleClass().add("piece-ring");
    if (isBlack) {
      ring.getStyleClass().add("piece-ring-black");
    }
    getChildren().add(ring);

    Circle gloss = new Circle();
    gloss.radiusProperty().bind(widthProperty().multiply(0.10));
    gloss.translateXProperty().bind(widthProperty().multiply(-0.16));
    gloss.translateYProperty().bind(heightProperty().multiply(-0.16));
    gloss.setMouseTransparent(true);
    gloss.getStyleClass().add("piece-gloss");
    getChildren().add(gloss);

    if (isKing) {
      Text crown = new Text(CROWN_GLYPH);
      crown.setMouseTransparent(true);
      crown.getStyleClass().add("piece-king-marker");
      if (isBlack) {
        crown.getStyleClass().add("piece-king-marker-black");
      }
      crown
          .fontProperty()
          .bind(
              Bindings.createObjectBinding(
                  () -> Font.font("Playfair Display", FontWeight.BOLD, getWidth() * 0.45),
                  widthProperty()));
      getChildren().add(crown);
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
