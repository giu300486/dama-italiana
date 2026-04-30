package com.damaitaliana.client.ui.save;

import com.damaitaliana.client.persistence.SerializedGameState;
import com.damaitaliana.shared.notation.FidNotation;
import java.util.List;
import java.util.Objects;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.springframework.stereotype.Component;

/**
 * Production {@link MiniatureRenderer}: paints the position on a {@link Canvas} (8×8 alternating
 * squares + circular discs for pieces) and snapshots the result. Must run on the JavaFX thread —
 * which is naturally the case when the load-screen table cell factory invokes it.
 */
@Component
public class CanvasMiniatureRenderer implements MiniatureRenderer {

  private static final Paint DARK_SQUARE = Color.web("#8B5A2B");
  private static final Paint LIGHT_SQUARE = Color.web("#F5DEB3");
  private static final Paint WHITE_PIECE = Color.web("#F8F4E3");
  private static final Paint BLACK_PIECE = Color.web("#2B2B2B");
  private static final Paint WHITE_KING_RING = Color.web("#C9A227");
  private static final Paint BLACK_KING_RING = Color.web("#C9A227");

  @Override
  public Image render(SerializedGameState state, int sizePx) {
    Objects.requireNonNull(state, "state");
    if (sizePx <= 0) {
      throw new IllegalArgumentException("sizePx must be positive: " + sizePx);
    }
    Canvas canvas = new Canvas(sizePx, sizePx);
    GraphicsContext g = canvas.getGraphicsContext2D();
    double cellSize = (double) sizePx / 8.0;
    paintBoard(g, cellSize);
    paintPieces(g, state, cellSize);
    SnapshotParameters params = new SnapshotParameters();
    params.setFill(Color.TRANSPARENT);
    return canvas.snapshot(params, new WritableImage(sizePx, sizePx));
  }

  private static void paintBoard(GraphicsContext g, double cellSize) {
    for (int rank = 0; rank < 8; rank++) {
      for (int file = 0; file < 8; file++) {
        boolean dark = (file + rank) % 2 == 0;
        g.setFill(dark ? DARK_SQUARE : LIGHT_SQUARE);
        // White at the bottom of the visible image: rank 0 = lowest (drawn at the bottom).
        double y = (7 - rank) * cellSize;
        double x = file * cellSize;
        g.fillRect(x, y, cellSize, cellSize);
      }
    }
  }

  private static void paintPieces(GraphicsContext g, SerializedGameState state, double cellSize) {
    paintGroup(g, state.whiteMen(), cellSize, WHITE_PIECE, null);
    paintGroup(g, state.whiteKings(), cellSize, WHITE_PIECE, WHITE_KING_RING);
    paintGroup(g, state.blackMen(), cellSize, BLACK_PIECE, null);
    paintGroup(g, state.blackKings(), cellSize, BLACK_PIECE, BLACK_KING_RING);
  }

  private static void paintGroup(
      GraphicsContext g, List<Integer> fids, double cellSize, Paint fill, Paint kingRing) {
    double radius = cellSize * 0.38;
    double inset = (cellSize - 2 * radius) / 2.0;
    for (int fid : fids) {
      var sq = FidNotation.toSquare(fid);
      double x = sq.file() * cellSize + inset;
      double y = (7 - sq.rank()) * cellSize + inset;
      g.setFill(fill);
      g.fillOval(x, y, 2 * radius, 2 * radius);
      if (kingRing != null) {
        g.setStroke(kingRing);
        g.setLineWidth(Math.max(1.0, cellSize * 0.08));
        g.strokeOval(x + radius * 0.4, y + radius * 0.4, 2 * radius * 0.6, 2 * radius * 0.6);
      }
    }
  }
}
