package com.damaitaliana.client.ui.board;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * {@link ListView} that displays the move-history rows in three columns: turn number, White's FID
 * move, Black's FID move (empty until Black completes the turn).
 */
public class MoveHistoryView extends ListView<MoveHistoryRow> {

  public MoveHistoryView() {
    getStyleClass().add("move-history-view");
    setCellFactory(lv -> new MoveHistoryCell());
  }

  private static final class MoveHistoryCell extends ListCell<MoveHistoryRow> {

    private final Label numberLabel = new Label();
    private final Label whiteLabel = new Label();
    private final Label blackLabel = new Label();
    private final HBox container = new HBox(8);

    MoveHistoryCell() {
      numberLabel.setMinWidth(28);
      numberLabel.getStyleClass().add("label-secondary");

      Region whiteWrap = wrap(whiteLabel);
      Region blackWrap = wrap(blackLabel);
      HBox.setHgrow(whiteWrap, Priority.ALWAYS);
      HBox.setHgrow(blackWrap, Priority.ALWAYS);
      container.setAlignment(Pos.CENTER_LEFT);
      container.getStyleClass().add("move-history-row");
      container.getChildren().addAll(numberLabel, whiteWrap, blackWrap);
    }

    private static Region wrap(Label label) {
      HBox box = new HBox(label);
      box.setAlignment(Pos.CENTER_LEFT);
      return box;
    }

    @Override
    protected void updateItem(MoveHistoryRow row, boolean empty) {
      super.updateItem(row, empty);
      if (empty || row == null) {
        setGraphic(null);
        return;
      }
      numberLabel.setText(row.moveNumber() + ".");
      whiteLabel.setText(row.whiteFid());
      blackLabel.setText(row.hasBlackMove() ? row.blackFid() : "");
      setGraphic(container);
    }
  }
}
