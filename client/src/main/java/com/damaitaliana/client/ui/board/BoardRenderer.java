package com.damaitaliana.client.ui.board;

import com.damaitaliana.shared.domain.Board;
import com.damaitaliana.shared.domain.Square;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;

/**
 * 8×8 grid of {@link BoardCellNode}s with overlays for legal-move and mandatory-capture highlights.
 * Sized to fit its containing parent (square aspect, {@link #layoutChildren} sizes each cell to
 * {@code min(width, height) / 8}).
 *
 * <p>The renderer maintains a {@link HighlightState} as the source of truth for highlights so
 * pure-Java unit tests can verify the bookkeeping without booting JavaFX; the JavaFX cell nodes are
 * kept in sync with the state on every mutator. Animation of the {@code .pulse-mandatory} class is
 * layered on by Task 3.10.
 */
public class BoardRenderer extends Region {

  static final int BOARD_SIZE = BoardLayoutMath.BOARD_SIZE;

  private final BoardCellNode[][] cells = new BoardCellNode[BOARD_SIZE][BOARD_SIZE];
  private final HighlightState highlightState = new HighlightState();
  private Consumer<Square> cellClickHandler;

  public BoardRenderer() {
    for (int file = 0; file < BOARD_SIZE; file++) {
      for (int rank = 0; rank < BOARD_SIZE; rank++) {
        BoardCellNode cell = new BoardCellNode(new Square(file, rank), this::dispatchClick);
        cells[file][rank] = cell;
        getChildren().add(cell);
      }
    }
    getStyleClass().add("board-renderer");
  }

  /** Registers the listener invoked when the user clicks a cell. */
  public void setOnCellClicked(Consumer<Square> handler) {
    this.cellClickHandler = handler;
  }

  /** Repaints every cell with the piece (or absence) at that square. */
  public void renderState(Board board) {
    Objects.requireNonNull(board, "board");
    for (int file = 0; file < BOARD_SIZE; file++) {
      for (int rank = 0; rank < BOARD_SIZE; rank++) {
        Square s = new Square(file, rank);
        cells[file][rank].setPiece(board.at(s).orElse(null));
      }
    }
  }

  public void highlightLegalTargets(List<Square> targets) {
    highlightState.setLegalTargets(targets);
    for (int file = 0; file < BOARD_SIZE; file++) {
      for (int rank = 0; rank < BOARD_SIZE; rank++) {
        cells[file][rank].setLegalTarget(highlightState.isLegalTarget(new Square(file, rank)));
      }
    }
  }

  public void highlightMandatorySources(List<Square> sources) {
    highlightState.setMandatorySources(sources);
    for (int file = 0; file < BOARD_SIZE; file++) {
      for (int rank = 0; rank < BOARD_SIZE; rank++) {
        cells[file][rank].setMandatorySource(
            highlightState.isMandatorySource(new Square(file, rank)));
      }
    }
  }

  public void clearHighlights() {
    highlightState.clear();
    for (int file = 0; file < BOARD_SIZE; file++) {
      for (int rank = 0; rank < BOARD_SIZE; rank++) {
        cells[file][rank].setLegalTarget(false);
        cells[file][rank].setMandatorySource(false);
      }
    }
  }

  /** Returns the current highlight bookkeeping. Test hook. */
  public HighlightState highlightState() {
    return highlightState;
  }

  /**
   * @return the {@link Node} of the piece currently rendered at {@code square}, or {@code null}
   *     when the cell is empty. Used by the animation orchestrator to identify the moving and
   *     captured pieces before {@link #renderState} replaces them.
   */
  public Node pieceAt(Square square) {
    Objects.requireNonNull(square, "square");
    return cells[square.file()][square.rank()].pieceNode();
  }

  /** Cell side length in pixels at the current size. */
  public double currentCellSize() {
    return BoardLayoutMath.cellSize(getWidth(), getHeight());
  }

  /** Snapshot for save miniatures and rules-screen diagrams. */
  public WritableImage snapshot(int sizePx) {
    SnapshotParameters params = new SnapshotParameters();
    return snapshot(params, new WritableImage(sizePx, sizePx));
  }

  @Override
  protected void layoutChildren() {
    double cellSize = BoardLayoutMath.cellSize(getWidth(), getHeight());
    for (int file = 0; file < BOARD_SIZE; file++) {
      for (int rank = 0; rank < BOARD_SIZE; rank++) {
        cells[file][rank].resizeRelocate(
            BoardLayoutMath.xFor(file, cellSize),
            BoardLayoutMath.yFor(rank, cellSize),
            cellSize,
            cellSize);
      }
    }
  }

  private void dispatchClick(Square square) {
    if (cellClickHandler != null) {
      cellClickHandler.accept(square);
    }
  }
}
