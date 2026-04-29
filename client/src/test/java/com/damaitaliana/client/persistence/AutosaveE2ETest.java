package com.damaitaliana.client.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.client.app.ClientProperties;
import com.damaitaliana.client.controller.ColorChoice;
import com.damaitaliana.client.controller.SinglePlayerAutosaveTrigger;
import com.damaitaliana.client.controller.SinglePlayerGame;
import com.damaitaliana.shared.ai.AiLevel;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import com.damaitaliana.shared.rules.RuleEngine;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Optional;
import java.util.SplittableRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end coverage of the autosave + recovery flow (A3.5). Boots the real {@link SaveService} +
 * {@link AutosaveService} stack over a temp dir, simulates the {@link SinglePlayerAutosaveTrigger}
 * firing on every applied move, then exercises the two restart branches: file-present prompt and
 * terminate-clears.
 */
class AutosaveE2ETest {

  private static final RuleEngine RULES = new ItalianRuleEngine();

  @TempDir Path tempDir;

  private ClientProperties properties;
  private SaveService saveService;
  private AutosaveService autosaveService;
  private SinglePlayerAutosaveTrigger trigger;

  @BeforeEach
  void setUp() {
    properties =
        new ClientProperties(
            tempDir.resolve("saves").toString(), tempDir.resolve("config.json").toString());
    saveService = new SaveService(properties);
    autosaveService =
        new AutosaveService(
            saveService, properties, () -> new SplittableRandom(7L), Clock.systemUTC());
    trigger = new SinglePlayerAutosaveTrigger(autosaveService);
  }

  @Test
  void promptOnRestartWhenAutosavePresent() {
    SinglePlayerGame game = newGame();

    // Simulate two applied moves. After each move, SinglePlayerController fires the trigger.
    SinglePlayerGame afterFirst = applyOneMove(game);
    trigger.onMoveApplied(afterFirst);
    SinglePlayerGame afterSecond = applyOneMove(afterFirst);
    trigger.onMoveApplied(afterSecond);

    // Restart: a fresh AutosaveService against the same dir (mimics next app launch).
    AutosaveService freshlyStarted =
        new AutosaveService(
            saveService, properties, () -> new SplittableRandom(99L), Clock.systemUTC());

    assertThat(freshlyStarted.autosaveExists()).isTrue();
    Optional<SinglePlayerGame> resumed = freshlyStarted.readAutosave();
    assertThat(resumed).isPresent();
    assertThat(resumed.get().name()).isEqualTo(game.name());
    assertThat(resumed.get().state().board()).isEqualTo(afterSecond.state().board());
    assertThat(resumed.get().state().sideToMove()).isEqualTo(afterSecond.state().sideToMove());
    assertThat(resumed.get().state().history()).hasSize(2);
  }

  @Test
  void clearOnTerminate() {
    trigger.onMoveApplied(applyOneMove(newGame()));
    assertThat(autosaveService.autosaveExists()).isTrue();

    // Simulate "Termina partita".
    autosaveService.clearAutosave();

    assertThat(autosaveService.autosaveExists()).isFalse();
    assertThat(autosaveService.readAutosave()).isEmpty();
    // The reserved slot is hidden from the load screen regardless.
    assertThat(saveService.listSlots()).isEmpty();
  }

  @Test
  void writeFailureToleratedWhenSavesDirIsAFile() throws Exception {
    // Create a file at the savesDir path so SaveService can't create the directory on write.
    java.nio.file.Files.createFile(Path.of(properties.savesDir()));
    SinglePlayerGame game = applyOneMove(newGame());

    // Trigger swallows IO errors; no exception must escape.
    trigger.onMoveApplied(game);

    // No autosave got written — the next start sees a clean main menu.
    assertThat(autosaveService.autosaveExists()).isFalse();
  }

  // -- helpers ------------------------------------------------------------------------------------

  private SinglePlayerGame newGame() {
    return SinglePlayerGame.tryCreate(
            AiLevel.ESPERTO, ColorChoice.WHITE, "E2E test", new SplittableRandom(42L))
        .orElseThrow();
  }

  private SinglePlayerGame applyOneMove(SinglePlayerGame game) {
    GameState state = game.state();
    Move next =
        RULES.legalMoves(state).stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("no legal moves"));
    GameState advanced;
    try {
      advanced = RULES.applyMove(state, next);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    return new SinglePlayerGame(game.level(), game.humanColor(), game.name(), advanced, game.rng());
  }
}
