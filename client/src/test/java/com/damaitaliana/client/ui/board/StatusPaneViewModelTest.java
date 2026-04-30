package com.damaitaliana.client.ui.board;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.client.controller.SinglePlayerGame;
import com.damaitaliana.client.i18n.I18n;
import com.damaitaliana.client.i18n.LocaleService;
import com.damaitaliana.client.i18n.MessageSourceConfig;
import com.damaitaliana.shared.ai.AiLevel;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import java.util.Locale;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;

class StatusPaneViewModelTest {

  private final MessageSource source = MessageSourceConfig.buildMessageSource();

  private I18n italianI18n() {
    LocaleService loc = Mockito.mock(LocaleService.class);
    Mockito.when(loc.current()).thenReturn(Locale.ITALIAN);
    return new I18n(source, loc);
  }

  private I18n englishI18n() {
    LocaleService loc = Mockito.mock(LocaleService.class);
    Mockito.when(loc.current()).thenReturn(Locale.ENGLISH);
    return new I18n(source, loc);
  }

  private SinglePlayerGame sampleGame(Color humanColor, AiLevel level) {
    return new SinglePlayerGame(
        level, humanColor, "Test", GameState.initial(), new SplittableRandom(0L));
  }

  @Test
  void displaysHumanAndAiNamesInItalian() {
    StatusPaneViewModel vm = new StatusPaneViewModel(italianI18n());
    var game = sampleGame(Color.WHITE, AiLevel.ESPERTO);

    StatusPaneState s = vm.compute(game, GameState.initial());

    assertThat(s.humanLabel()).contains("Tu", "Bianco");
    assertThat(s.aiLabel()).contains("IA", "Esperto", "Nero");
  }

  @Test
  void displaysHumanAndAiNamesInEnglish() {
    StatusPaneViewModel vm = new StatusPaneViewModel(englishI18n());
    var game = sampleGame(Color.BLACK, AiLevel.PRINCIPIANTE);

    StatusPaneState s = vm.compute(game, GameState.initial());

    assertThat(s.humanLabel()).contains("You", "Black");
    assertThat(s.aiLabel()).contains("AI", "Beginner", "White");
  }

  @Test
  void turnIndicatorReflectsSideToMove() {
    StatusPaneViewModel vm = new StatusPaneViewModel(italianI18n());
    var game = sampleGame(Color.WHITE, AiLevel.ESPERTO);

    StatusPaneState s = vm.compute(game, GameState.initial());

    assertThat(s.ended()).isFalse();
    assertThat(s.endgameLabel()).isNull();
    assertThat(s.sideToMove()).isEqualTo(Color.WHITE);
    assertThat(s.turnLabel()).contains("Bianco");
  }

  @Test
  void endgameBannerShownOnTerminalState() {
    StatusPaneViewModel vm = new StatusPaneViewModel(italianI18n());
    var game = sampleGame(Color.WHITE, AiLevel.ESPERTO);
    GameState terminal = endedState(GameStatus.WHITE_WINS);

    StatusPaneState s = vm.compute(game, terminal);

    assertThat(s.ended()).isTrue();
    assertThat(s.turnLabel()).isNull();
    assertThat(s.endgameLabel()).contains("Bianco vince");
  }

  @ParameterizedTest
  @EnumSource(GameStatus.class)
  void allSixGameStatusValuesHaveLocalizedString(GameStatus status) {
    StatusPaneViewModel vm = new StatusPaneViewModel(italianI18n());
    var game = sampleGame(Color.WHITE, AiLevel.ESPERTO);
    GameState state = status.isOngoing() ? GameState.initial() : endedState(status);

    StatusPaneState s = vm.compute(game, state);

    if (status.isOngoing()) {
      assertThat(s.turnLabel()).isNotBlank().doesNotStartWith("[");
    } else {
      assertThat(s.endgameLabel()).isNotBlank().doesNotStartWith("[");
    }
  }

  /**
   * Builds a non-ongoing GameState by replaying a sequence the rule engine can produce — but here
   * we just construct it via {@code initial()} and force the status. Since GameState is a record,
   * we can rewrite it.
   */
  private static GameState endedState(GameStatus status) {
    GameState initial = GameState.initial();
    return new GameState(
        initial.board(), initial.sideToMove(), initial.halfmoveClock(), initial.history(), status);
  }

  /** Sanity check that the rule engine reaches an ongoing initial state (no test on shared). */
  @Test
  void initialStateIsOngoing() {
    assertThat(GameState.initial().status()).isEqualTo(GameStatus.ONGOING);
    new ItalianRuleEngine();
  }
}
