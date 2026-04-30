package com.damaitaliana.client.ui.board;

import com.damaitaliana.shared.domain.Color;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Side-panel widget showing the two players, the current side-to-move chip, and an endgame banner
 * once the game terminates. Driven by {@link #update(StatusPaneState)} from {@link
 * StatusPaneViewModel}; pure JavaFX, no logic of its own.
 */
public class StatusPane extends VBox {

  private static final String CHIP_WHITE = "side-chip-white";
  private static final String CHIP_BLACK = "side-chip-black";

  private final Label humanLabel = new Label();
  private final Label aiLabel = new Label();
  private final Region turnChip = new Region();
  private final Label turnLabel = new Label();
  private final HBox turnBox;
  private final Label endgameLabel = new Label();
  private final ProgressIndicator thinkingSpinner = new ProgressIndicator();
  private final Label thinkingLabel = new Label();
  private final HBox thinkingBox;

  public StatusPane() {
    getStyleClass().add("status-pane");
    setSpacing(8);

    humanLabel.getStyleClass().add("label-secondary");
    aiLabel.getStyleClass().add("label-secondary");

    turnChip.getStyleClass().add("side-chip");
    turnChip.setMinSize(12, 12);
    turnChip.setPrefSize(12, 12);
    turnChip.setMaxSize(12, 12);

    turnBox = new HBox(8, turnChip, turnLabel);
    turnBox.setAlignment(Pos.CENTER_LEFT);

    endgameLabel.getStyleClass().add("label-subtitle");
    endgameLabel.setManaged(false);
    endgameLabel.setVisible(false);

    thinkingSpinner.setMaxSize(16, 16);
    thinkingSpinner.setPrefSize(16, 16);
    thinkingLabel.getStyleClass().add("label-secondary");
    thinkingBox = new HBox(8, thinkingSpinner, thinkingLabel);
    thinkingBox.setAlignment(Pos.CENTER_LEFT);
    thinkingBox.setManaged(false);
    thinkingBox.setVisible(false);

    getChildren().addAll(humanLabel, aiLabel, turnBox, thinkingBox, endgameLabel);
  }

  /**
   * Show / hide the AI thinking affordance. {@code label} is the localised "AI is thinking…"
   * string; passed in by the {@link BoardViewController} so this widget stays i18n-agnostic.
   */
  public void setThinking(boolean thinking, String label) {
    thinkingLabel.setText(label);
    thinkingBox.setManaged(thinking);
    thinkingBox.setVisible(thinking);
  }

  public void update(StatusPaneState state) {
    humanLabel.setText(state.humanLabel());
    aiLabel.setText(state.aiLabel());

    turnChip.getStyleClass().removeAll(CHIP_WHITE, CHIP_BLACK);
    turnChip.getStyleClass().add(state.sideToMove() == Color.WHITE ? CHIP_WHITE : CHIP_BLACK);

    if (state.ended()) {
      turnBox.setManaged(false);
      turnBox.setVisible(false);
      endgameLabel.setText(state.endgameLabel());
      endgameLabel.setManaged(true);
      endgameLabel.setVisible(true);
    } else {
      turnBox.setManaged(true);
      turnBox.setVisible(true);
      turnLabel.setText(state.turnLabel());
      endgameLabel.setManaged(false);
      endgameLabel.setVisible(false);
    }
  }
}
