package com.damaitaliana.client.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damaitaliana.client.app.ClientProperties;
import com.damaitaliana.client.controller.ColorChoice;
import com.damaitaliana.client.controller.SinglePlayerGame;
import com.damaitaliana.shared.ai.AiLevel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AutosaveServiceTest {

  private static final Instant FROZEN = Instant.parse("2026-04-29T12:00:00Z");

  @TempDir Path tempDir;

  private Path savesDir;
  private ClientProperties properties;
  private SaveService saveService;
  private AutosaveService autosaveService;

  @BeforeEach
  void setUp() {
    savesDir = tempDir.resolve("saves");
    properties =
        new ClientProperties(savesDir.toString(), tempDir.resolve("config.json").toString());
    saveService = new SaveService(properties);
    autosaveService =
        new AutosaveService(
            saveService, properties, () -> fixedRng(), Clock.fixed(FROZEN, ZoneOffset.UTC));
  }

  @Test
  void writeAutosavePersistsToReservedSlot() {
    autosaveService.writeAutosave(sampleGame());

    Path file = savesDir.resolve(SaveService.AUTOSAVE_SLOT + ".json");
    assertThat(Files.exists(file)).isTrue();
    Optional<SavedGame> reloaded = saveService.load(SaveService.AUTOSAVE_SLOT);
    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().createdAt()).isEqualTo(FROZEN);
    assertThat(reloaded.get().updatedAt()).isEqualTo(FROZEN);
    assertThat(reloaded.get().name()).isEqualTo("Autosave Test");
  }

  @Test
  void writeAutosaveOverwritesExisting() {
    autosaveService.writeAutosave(sampleGame("First"));
    SavedGame first = saveService.load(SaveService.AUTOSAVE_SLOT).orElseThrow();

    autosaveService.writeAutosave(sampleGame("Second"));
    SavedGame second = saveService.load(SaveService.AUTOSAVE_SLOT).orElseThrow();

    assertThat(first.name()).isEqualTo("First");
    assertThat(second.name()).isEqualTo("Second");
  }

  @Test
  void readAutosaveReturnsEmptyWhenAbsent() {
    assertThat(autosaveService.readAutosave()).isEmpty();
  }

  @Test
  void readAutosaveRoundTripsSinglePlayerGame() {
    SinglePlayerGame original = sampleGame();
    autosaveService.writeAutosave(original);

    Optional<SinglePlayerGame> recovered = autosaveService.readAutosave();

    assertThat(recovered).isPresent();
    SinglePlayerGame got = recovered.get();
    assertThat(got.name()).isEqualTo(original.name());
    assertThat(got.level()).isEqualTo(original.level());
    assertThat(got.humanColor()).isEqualTo(original.humanColor());
    assertThat(got.state().board()).isEqualTo(original.state().board());
    assertThat(got.state().sideToMove()).isEqualTo(original.state().sideToMove());
    // RNG is intentionally re-injected from the supplier — not equal to the original.
    assertThat(got.rng()).isNotNull();
  }

  @Test
  void readAutosavePropagatesUnknownSchemaVersion() throws IOException {
    Files.createDirectories(savesDir);
    Files.writeString(
        savesDir.resolve(SaveService.AUTOSAVE_SLOT + ".json"),
        """
        {
          "schemaVersion": 99,
          "kind": "SINGLE_PLAYER_GAME",
          "name": "From the future",
          "createdAt": "2099-01-01T00:00:00Z",
          "updatedAt": "2099-01-01T00:00:00Z",
          "aiLevel": "CAMPIONE",
          "humanColor": "BLACK",
          "moves": [],
          "currentState": {
            "whiteMen": [], "whiteKings": [], "blackMen": [], "blackKings": [],
            "sideToMove": "WHITE",
            "halfmoveClock": 0,
            "history": []
          }
        }
        """,
        StandardCharsets.UTF_8);

    assertThatThrownBy(() -> autosaveService.readAutosave())
        .isInstanceOf(SaveService.UnknownSchemaVersionException.class)
        .hasMessageContaining("99");
  }

  @Test
  void clearAutosaveRemovesFile() {
    autosaveService.writeAutosave(sampleGame());
    assertThat(autosaveService.autosaveExists()).isTrue();

    autosaveService.clearAutosave();

    assertThat(autosaveService.autosaveExists()).isFalse();
    assertThat(autosaveService.readAutosave()).isEmpty();
  }

  @Test
  void clearAutosaveIsIdempotentWhenAbsent() {
    autosaveService.clearAutosave();
    autosaveService.clearAutosave();
    assertThat(autosaveService.autosaveExists()).isFalse();
  }

  @Test
  void autosaveExistsReflectsFilesystemState() {
    assertThat(autosaveService.autosaveExists()).isFalse();

    autosaveService.writeAutosave(sampleGame());
    assertThat(autosaveService.autosaveExists()).isTrue();

    autosaveService.clearAutosave();
    assertThat(autosaveService.autosaveExists()).isFalse();
  }

  // -- helpers ------------------------------------------------------------------------------------

  private SinglePlayerGame sampleGame() {
    return sampleGame("Autosave Test");
  }

  private SinglePlayerGame sampleGame(String name) {
    return SinglePlayerGame.tryCreate(AiLevel.ESPERTO, ColorChoice.WHITE, name, fixedRng())
        .orElseThrow();
  }

  private static RandomGenerator fixedRng() {
    return new SplittableRandom(42L);
  }
}
