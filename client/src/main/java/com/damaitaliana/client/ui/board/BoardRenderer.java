package com.damaitaliana.client.ui.board;

import com.damaitaliana.shared.domain.Board;
import com.damaitaliana.shared.domain.Square;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
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

  private static final String STYLE_FOCUSED_KEYBOARD = "cell-focused-keyboard";

  private final BoardCellNode[][] cells = new BoardCellNode[BOARD_SIZE][BOARD_SIZE];
  private final HighlightState highlightState = new HighlightState();
  private final Pane particleLayer = new Pane();
  private Consumer<Square> cellClickHandler;
  private Runnable escapeHandler;
  private Square keyboardFocus;

  public BoardRenderer() {
    for (int file = 0; file < BOARD_SIZE; file++) {
      for (int rank = 0; rank < BOARD_SIZE; rank++) {
        BoardCellNode cell = new BoardCellNode(new Square(file, rank), this::dispatchClick);
        cells[file][rank] = cell;
        getChildren().add(cell);
      }
    }
    // Particle overlay sits on top of all cells (Task 3.5.8) so capture splashes / promotion glows
    // composed by AnimationOrchestrator paint above piece nodes. Mouse-transparent so click
    // dispatch keeps targeting the cells underneath.
    particleLayer.setMouseTransparent(true);
    particleLayer.getStyleClass().add("board-particle-layer");
    getChildren().add(particleLayer);
    getStyleClass().add("board-renderer");
    // English-only in Fase 3 (matches CellAccessibleText / PieceAccessibleText); a11y i18n is a
    // Fase 11 concern.
    setAccessibleText("Italian draughts board, 8 by 8 grid");
    setFocusTraversable(true);
    addEventHandler(KeyEvent.KEY_PRESSED, this::onKeyPressed);
    focusedProperty()
        .addListener(
            (obs, was, isNow) -> {
              if (Boolean.TRUE.equals(isNow) && keyboardFocus == null) {
                setKeyboardFocus(new Square(0, 0));
              }
            });
  }

  /** Registers the listener invoked when the user clicks a cell. */
  public void setOnCellClicked(Consumer<Square> handler) {
    this.cellClickHandler = handler;
  }

  /**
   * Registers the listener invoked when the user presses Escape with the board focused (Task 3.20 —
   * keyboard navigation; intended to clear any pending selection on the controller side).
   */
  public void setOnEscape(Runnable handler) {
    this.escapeHandler = handler;
  }

  /** Visible for tests: returns the cell currently highlighted as keyboard-focused, or null. */
  public Square keyboardFocus() {
    return keyboardFocus;
  }

  /** Visible for tests: drives the keyboard focus from outside (used by arrow-key handling). */
  void setKeyboardFocus(Square next) {
    Objects.requireNonNull(next, "next");
    if (keyboardFocus != null) {
      cells[keyboardFocus.file()][keyboardFocus.rank()]
          .getStyleClass()
          .remove(STYLE_FOCUSED_KEYBOARD);
    }
    keyboardFocus = next;
    BoardCellNode cell = cells[next.file()][next.rank()];
    if (!cell.getStyleClass().contains(STYLE_FOCUSED_KEYBOARD)) {
      cell.getStyleClass().add(STYLE_FOCUSED_KEYBOARD);
    }
  }

  private void onKeyPressed(KeyEvent ev) {
    KeyCode code = ev.getCode();
    if (code == KeyCode.ENTER && keyboardFocus != null) {
      dispatchClick(keyboardFocus);
      ev.consume();
      return;
    }
    if (code == KeyCode.ESCAPE) {
      if (escapeHandler != null) {
        escapeHandler.run();
      }
      ev.consume();
      return;
    }
    int dFile = 0;
    int dRank = 0;
    if (code == KeyCode.UP) {
      dRank = 1;
    } else if (code == KeyCode.DOWN) {
      dRank = -1;
    } else if (code == KeyCode.LEFT) {
      dFile = -1;
    } else if (code == KeyCode.RIGHT) {
      dFile = 1;
    } else {
      return;
    }
    Square next =
        nextDarkCell(keyboardFocus != null ? keyboardFocus : new Square(0, 0), dFile, dRank);
    if (next != null) {
      setKeyboardFocus(next);
    }
    ev.consume();
  }

  /**
   * Returns the next dark cell in the direction {@code (dFile, dRank)} starting from {@code from},
   * or {@code null} if the move would leave the board. Arrow keys traverse two cells at a time (one
   * dark step) so the focus visibly hops between playable squares.
   */
  private static Square nextDarkCell(Square from, int dFile, int dRank) {
    int file = from.file();
    int rank = from.rank();
    // Two steps: dark squares are 2 apart in any orthogonal direction.
    int candidateFile = file + 2 * dFile;
    int candidateRank = rank + 2 * dRank;
    if (candidateFile < 0
        || candidateFile >= BOARD_SIZE
        || candidateRank < 0
        || candidateRank >= BOARD_SIZE) {
      return null;
    }
    Square candidate = new Square(candidateFile, candidateRank);
    if (BoardLayoutMath.isDarkSquare(candidate)) {
      return candidate;
    }
    return null;
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

  /**
   * Pane that sits above the cell grid; the {@link
   * com.damaitaliana.client.ui.board.animation.AnimationOrchestrator} drops particle nodes here for
   * capture splashes and promotion glows (Task 3.5.8). Mouse-transparent.
   */
  public Pane particleHost() {
    return particleLayer;
  }

  /** Snapshot for save miniatures and rules-screen diagrams. */
  public WritableImage snapshot(int sizePx) {
    SnapshotParameters params = new SnapshotParameters();
    return snapshot(params, new WritableImage(sizePx, sizePx));
  }

  @Override
  protected void layoutChildren() {
    double cellSize = BoardLayoutMath.cellSize(getWidth(), getHeight());
    double total = BOARD_SIZE * cellSize;
    // F4.5 Task 4.5.4: center the 8×8 grid inside the renderer. When the available
    // area is non-square (typical at ultrawide aspect ratios, or whenever the parent
    // BorderPane allocates the renderer a slot wider than tall), the board sits in
    // the middle so the wood frame surrounds it equally on all sides instead of
    // leaving a wood-only strip on one side. Pre-F4.5 cells were anchored top-left.
    double xOffset = (getWidth() - total) / 2.0;
    double yOffset = (getHeight() - total) / 2.0;
    for (int file = 0; file < BOARD_SIZE; file++) {
      for (int rank = 0; rank < BOARD_SIZE; rank++) {
        cells[file][rank].resizeRelocate(
            xOffset + BoardLayoutMath.xFor(file, cellSize),
            yOffset + BoardLayoutMath.yFor(rank, cellSize),
            cellSize,
            cellSize);
      }
    }
    particleLayer.resizeRelocate(xOffset, yOffset, total, total);
  }

  private void dispatchClick(Square square) {
    if (cellClickHandler != null) {
      cellClickHandler.accept(square);
    }
  }
}
