package com.damaitaliana.client.ui.board;

import com.damaitaliana.client.controller.SinglePlayerGame;
import com.damaitaliana.client.i18n.I18n;
import com.damaitaliana.shared.ai.AiLevel;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import java.util.Locale;
import java.util.Objects;

/**
 * Pure logic that translates a {@link SinglePlayerGame} + {@link GameState} pair into the localised
 * strings shown by {@link StatusPane}. Stateless apart from the injected {@link I18n}.
 *
 * <p>Key conventions (lowercase enum names): {@code status.turn.<color>}, {@code
 * status.endgame.<status>}, {@code setup.color.<color>}, {@code setup.level.<level>}.
 */
public final class StatusPaneViewModel {

  private final I18n i18n;

  public StatusPaneViewModel(I18n i18n) {
    this.i18n = Objects.requireNonNull(i18n, "i18n");
  }

  public StatusPaneState compute(SinglePlayerGame game, GameState state) {
    Objects.requireNonNull(game, "game");
    Objects.requireNonNull(state, "state");

    Color humanColor = game.humanColor();
    Color aiColor = humanColor.opposite();

    String humanLabel =
        i18n.t("status.player.color", i18n.t("status.player.human"), colorName(humanColor));
    String aiLabel =
        i18n.t(
            "status.player.color",
            i18n.t("status.player.ai", levelName(game.level())),
            colorName(aiColor));

    GameStatus status = state.status();
    if (status.isOngoing()) {
      String turnLabel =
          i18n.t("status.turn." + state.sideToMove().name().toLowerCase(Locale.ROOT));
      return new StatusPaneState(humanLabel, aiLabel, turnLabel, null, state.sideToMove(), false);
    }
    String endgameLabel = i18n.t("status.endgame." + status.name().toLowerCase(Locale.ROOT));
    return new StatusPaneState(humanLabel, aiLabel, null, endgameLabel, state.sideToMove(), true);
  }

  private String colorName(Color color) {
    return i18n.t("setup.color." + color.name().toLowerCase(Locale.ROOT));
  }

  private String levelName(AiLevel level) {
    return i18n.t("setup.level." + level.name().toLowerCase(Locale.ROOT));
  }
}
