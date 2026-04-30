package com.damaitaliana.client.ui.board;

import com.damaitaliana.shared.domain.CaptureSequence;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.domain.SimpleMove;
import com.damaitaliana.shared.domain.Square;
import com.damaitaliana.shared.notation.FidNotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * ObservableList of {@link MoveHistoryRow}s. {@link #appendMove} formats the move via FID notation
 * (SPEC §3.8) and either starts a new row (White) or completes the previous one (Black).
 *
 * <p>Strict alternation is enforced — appending two consecutive moves of the same colour throws,
 * since Italian Draughts always alternates White/Black. The board controller therefore never has to
 * special-case mid-game errors here.
 */
public class MoveHistoryViewModel {

  private final ObservableList<MoveHistoryRow> rows = FXCollections.observableArrayList();

  public ObservableList<MoveHistoryRow> rows() {
    return rows;
  }

  /**
   * Replaces every row with a fresh sequence rebuilt from {@code history}, alternating colours
   * starting from White (Italian Draughts convention — first move is always White, ADR-013). Used
   * when undoing or redoing a pair (Task 3.24): the controller restores a snapshot and asks the
   * view-model to mirror its history.
   */
  public void replaceWithHistory(List<Move> history) {
    Objects.requireNonNull(history, "history");
    rows.clear();
    Color next = Color.WHITE;
    for (Move move : history) {
      appendMove(move, next);
      next = (next == Color.WHITE) ? Color.BLACK : Color.WHITE;
    }
  }

  public void appendMove(Move move, Color color) {
    Objects.requireNonNull(move, "move");
    Objects.requireNonNull(color, "color");

    String fid = formatFid(move);
    if (color == Color.WHITE) {
      if (!rows.isEmpty() && !rows.get(rows.size() - 1).hasBlackMove()) {
        throw new IllegalStateException(
            "white move appended before black completed the previous turn");
      }
      rows.add(new MoveHistoryRow(rows.size() + 1, fid, null));
    } else {
      if (rows.isEmpty() || rows.get(rows.size() - 1).hasBlackMove()) {
        throw new IllegalStateException("black move appended without a preceding white move");
      }
      int last = rows.size() - 1;
      MoveHistoryRow previous = rows.get(last);
      rows.set(last, new MoveHistoryRow(previous.moveNumber(), previous.whiteFid(), fid));
    }
  }

  static String formatFid(Move move) {
    if (move instanceof SimpleMove sm) {
      return FidNotation.formatMove(
          List.of(FidNotation.toFid(sm.from()), FidNotation.toFid(sm.to())), false);
    }
    if (move instanceof CaptureSequence cs) {
      List<Integer> squares = new ArrayList<>(cs.path().size() + 1);
      squares.add(FidNotation.toFid(cs.from()));
      for (Square s : cs.path()) {
        squares.add(FidNotation.toFid(s));
      }
      return FidNotation.formatMove(squares, true);
    }
    throw new IllegalArgumentException("unknown Move kind: " + move.getClass());
  }
}
