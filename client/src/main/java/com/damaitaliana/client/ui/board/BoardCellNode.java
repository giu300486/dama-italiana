package com.damaitaliana.client.ui.board;

import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.Square;
import java.util.function.Consumer;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

/**
 * One of the 64 grid cells. Light cells carry only the {@code .board-cell-light} style class and
 * never host a piece (Italian Draughts plays only on dark cells); dark cells can mount a {@link
 * PieceNode} via {@link #setPiece(Piece)} and toggle the {@code .legal-target} / {@code
 * .pulse-mandatory} classes.
 */
final class BoardCellNode extends Region {

  private static final String STYLE_LIGHT = "board-cell-light";
  private static final String STYLE_DARK = "board-cell-dark";
  static final String STYLE_LEGAL_TARGET = "legal-target";
  static final String STYLE_PULSE_MANDATORY = "pulse-mandatory";

  private final Square square;
  private final boolean dark;
  private PieceNode pieceNode;

  BoardCellNode(Square square, Consumer<Square> clickHandler) {
    this.square = square;
    this.dark = BoardLayoutMath.isDarkSquare(square);
    getStyleClass().add(dark ? STYLE_DARK : STYLE_LIGHT);
    addEventHandler(
        MouseEvent.MOUSE_CLICKED,
        ev -> {
          if (clickHandler != null) {
            clickHandler.accept(square);
          }
        });
  }

  void setPiece(Piece piece) {
    if (pieceNode != null) {
      getChildren().remove(pieceNode);
      pieceNode = null;
    }
    if (piece != null && dark) {
      pieceNode = new PieceNode(piece, square);
      pieceNode.prefWidthProperty().bind(widthProperty());
      pieceNode.prefHeightProperty().bind(heightProperty());
      pieceNode.maxWidthProperty().bind(widthProperty());
      pieceNode.maxHeightProperty().bind(heightProperty());
      getChildren().add(pieceNode);
    }
  }

  void setLegalTarget(boolean active) {
    setStyleClassActive(STYLE_LEGAL_TARGET, active);
  }

  void setMandatorySource(boolean active) {
    setStyleClassActive(STYLE_PULSE_MANDATORY, active);
  }

  private void setStyleClassActive(String styleClass, boolean active) {
    boolean present = getStyleClass().contains(styleClass);
    if (active && !present) {
      getStyleClass().add(styleClass);
    } else if (!active && present) {
      getStyleClass().remove(styleClass);
    }
  }

  Square square() {
    return square;
  }

  boolean isDark() {
    return dark;
  }

  PieceNode pieceNode() {
    return pieceNode;
  }
}
