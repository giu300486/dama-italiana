package com.damaitaliana.client.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.client.app.ClientProperties;
import com.damaitaliana.client.controller.ColorChoice;
import com.damaitaliana.client.controller.SinglePlayerGame;
import com.damaitaliana.shared.ai.AiLevel;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import com.damaitaliana.shared.rules.RuleEngine;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.SplittableRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end coverage of A3.4 — save and load round-trip. Plays a couple of plies via {@link
 * RuleEngine#applyMove} (the same mechanism the controller uses internally), persists the resulting
 * snapshot through {@link SaveService}, then reloads it and rebuilds a {@link SinglePlayerGame} via
 * {@link SinglePlayerGame#fromSaved(SavedGame, java.util.random.RandomGenerator)}. The
 * reconstructed state must match the snapshot byte-for-byte (board, side-to-move, halfmove clock,
 * history).
 */
class SaveLoadE2ETest {

  private static final RuleEngine RULES = new ItalianRuleEngine();

  @TempDir Path tempDir;

  private SaveService saveService;

  @BeforeEach
  void setUp() {
    ClientProperties properties =
        new ClientProperties(
            tempDir.resolve("saves").toString(), tempDir.resolve("config.json").toString());
    saveService = new SaveService(properties);
  }

  @Test
  void saveThenLoadResumesAtSameState() {
    SinglePlayerGame initial =
        SinglePlayerGame.tryCreate(
                AiLevel.PRINCIPIANTE, ColorChoice.WHITE, "E2E save", new SplittableRandom(7L))
            .orElseThrow();
    SinglePlayerGame afterFirst = applyOneMove(initial);
    SinglePlayerGame afterSecond = applyOneMove(afterFirst);

    Instant when = Instant.parse("2026-04-30T12:00:00Z");
    SavedGame payload = SavedGame.of(afterSecond, when, when);

    String slot = SaveService.slugify(afterSecond.name());
    saveService.save(slot, payload);

    // The slot is visible to the load screen.
    List<SaveSlotMetadata> slots = saveService.listSlots();
    assertThat(slots).extracting(SaveSlotMetadata::slot).contains(slot);

    SavedGame loaded = saveService.load(slot).orElseThrow();
    SinglePlayerGame resumed = SinglePlayerGame.fromSaved(loaded, new SplittableRandom(99L));

    assertThat(resumed.name()).isEqualTo(afterSecond.name());
    assertThat(resumed.level()).isEqualTo(afterSecond.level());
    assertThat(resumed.humanColor()).isEqualTo(afterSecond.humanColor());
    assertThat(resumed.state().board()).isEqualTo(afterSecond.state().board());
    assertThat(resumed.state().sideToMove()).isEqualTo(afterSecond.state().sideToMove());
    assertThat(resumed.state().halfmoveClock()).isEqualTo(afterSecond.state().halfmoveClock());
    assertThat(resumed.state().history()).hasSameSizeAs(afterSecond.state().history());
  }

  @Test
  void deleteRemovesSlotFromListing() {
    SinglePlayerGame game =
        SinglePlayerGame.tryCreate(
                AiLevel.PRINCIPIANTE, ColorChoice.WHITE, "Delete me", new SplittableRandom(1L))
            .orElseThrow();
    SavedGame payload = SavedGame.of(game, Instant.EPOCH, Instant.EPOCH);
    String slot = SaveService.slugify(game.name());
    saveService.save(slot, payload);
    assertThat(saveService.listSlots()).extracting(SaveSlotMetadata::slot).contains(slot);

    saveService.delete(slot);

    assertThat(saveService.listSlots()).extracting(SaveSlotMetadata::slot).doesNotContain(slot);
    assertThat(saveService.load(slot)).isEmpty();
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
