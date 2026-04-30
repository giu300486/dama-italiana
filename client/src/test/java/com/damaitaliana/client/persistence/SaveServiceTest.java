package com.damaitaliana.client.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damaitaliana.client.app.ClientProperties;
import com.damaitaliana.client.controller.ColorChoice;
import com.damaitaliana.client.controller.SinglePlayerGame;
import com.damaitaliana.shared.ai.AiLevel;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.notation.FidNotation;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import com.damaitaliana.shared.rules.RuleEngine;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SaveServiceTest {

  private static final RuleEngine RULES = new ItalianRuleEngine();

  @TempDir Path tempDir;
  private Path savesDir;
  private SaveService service;

  @BeforeEach
  void setUp() {
    savesDir = tempDir.resolve("saves");
    var props =
        new ClientProperties(savesDir.toString(), tempDir.resolve("config.json").toString());
    service = new SaveService(props);
  }

  @Test
  void saveThenLoadRoundTrips() {
    SavedGame data = sampleSaved("partita-test", "Partita Test");
    service.save("partita-test", data);

    Optional<SavedGame> loaded = service.load("partita-test");

    assertThat(loaded).isPresent();
    SavedGame got = loaded.get();
    assertThat(got.schemaVersion()).isEqualTo(SavedGame.CURRENT_SCHEMA_VERSION);
    assertThat(got.kind()).isEqualTo(SavedGame.KIND_SINGLE_PLAYER);
    assertThat(got.name()).isEqualTo("Partita Test");
    assertThat(got.aiLevel()).isEqualTo(AiLevel.ESPERTO);
    assertThat(got.humanColor()).isEqualTo(Color.WHITE);
    assertThat(got.moves()).isEqualTo(data.moves());
    assertThat(got.currentState().sideToMove()).isEqualTo(data.currentState().sideToMove());
    assertThat(got.currentState().toState().board())
        .isEqualTo(data.currentState().toState().board());
  }

  @Test
  void listSlotsReturnsAllSavedGamesSortedByUpdatedAtDesc() {
    Instant t0 = Instant.parse("2026-04-29T10:00:00Z");
    service.save("alpha", sampleSaved("alpha", "Alpha").withUpdatedAt(t0));
    service.save("bravo", sampleSaved("bravo", "Bravo").withUpdatedAt(t0.plusSeconds(60)));
    service.save("charlie", sampleSaved("charlie", "Charlie").withUpdatedAt(t0.plusSeconds(120)));

    List<SaveSlotMetadata> slots = service.listSlots();

    assertThat(slots)
        .extracting(SaveSlotMetadata::slot)
        .containsExactly("charlie", "bravo", "alpha");
    assertThat(slots)
        .extracting(SaveSlotMetadata::name)
        .containsExactly("Charlie", "Bravo", "Alpha");
    assertThat(slots.get(0).aiLevel()).isEqualTo(AiLevel.ESPERTO);
    assertThat(slots.get(0).humanColor()).isEqualTo(Color.WHITE);
    assertThat(slots.get(0).currentMoveNumber()).isEqualTo(0);
  }

  @Test
  void listSlotsExcludesAutosaveSlot() {
    service.save("user-slot", sampleSaved("user-slot", "User"));
    service.save(SaveService.AUTOSAVE_SLOT, sampleSaved("autosave-name", "Autosave"));

    List<SaveSlotMetadata> visible = service.listSlots();

    assertThat(visible).extracting(SaveSlotMetadata::slot).containsExactly("user-slot");
    assertThat(service.load(SaveService.AUTOSAVE_SLOT)).isPresent();
  }

  @Test
  void deleteRemovesSlot() {
    service.save("removable", sampleSaved("removable", "Removable"));
    assertThat(service.load("removable")).isPresent();

    service.delete("removable");

    assertThat(service.load("removable")).isEmpty();
    assertThat(service.listSlots()).isEmpty();
  }

  @Test
  void deleteIsIdempotentForMissingSlot() {
    service.delete("never-existed");
    assertThat(service.load("never-existed")).isEmpty();
  }

  @Test
  void loadReturnsEmptyWhenSlotMissing() {
    assertThat(service.load("nope")).isEmpty();
    assertThat(service.listSlots()).isEmpty();
  }

  @Test
  void loadFailsOnUnknownSchemaVersion() throws IOException {
    Files.createDirectories(savesDir);
    Path file = savesDir.resolve("future.json");
    Files.writeString(
        file,
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

    assertThatThrownBy(() -> service.load("future"))
        .isInstanceOf(SaveService.UnknownSchemaVersionException.class)
        .hasMessageContaining("99")
        .hasMessageContaining("\"future\"");
  }

  @Test
  void listSlotsSkipsUnknownSchemaVersionFiles() throws IOException {
    Files.createDirectories(savesDir);
    Files.writeString(
        savesDir.resolve("future.json"),
        "{\"schemaVersion\":99,\"kind\":\"SINGLE_PLAYER_GAME\",\"name\":\"X\","
            + "\"createdAt\":\"2099-01-01T00:00:00Z\",\"updatedAt\":\"2099-01-01T00:00:00Z\","
            + "\"aiLevel\":\"ESPERTO\",\"humanColor\":\"WHITE\",\"moves\":[],"
            + "\"currentState\":{\"whiteMen\":[],\"whiteKings\":[],\"blackMen\":[],\"blackKings\":[],"
            + "\"sideToMove\":\"WHITE\",\"halfmoveClock\":0,\"history\":[]}}",
        StandardCharsets.UTF_8);
    service.save("ok", sampleSaved("ok", "OK"));

    List<SaveSlotMetadata> slots = service.listSlots();

    assertThat(slots).extracting(SaveSlotMetadata::slot).containsExactly("ok");
  }

  @Test
  void saveDoesNotLeaveTempFileBehind() {
    service.save("clean", sampleSaved("clean", "Clean"));

    Path tmp = savesDir.resolve("clean.json.tmp");
    assertThat(Files.exists(tmp)).as("tmp file %s", tmp).isFalse();
    assertThat(Files.exists(savesDir.resolve("clean.json"))).isTrue();
  }

  @Test
  void saveOverwritesExistingSlotAtomically() throws IOException {
    Instant t0 = Instant.parse("2026-04-29T10:00:00Z");
    service.save("twice", sampleSaved("twice", "First").withUpdatedAt(t0));
    service.save("twice", sampleSaved("twice", "Second").withUpdatedAt(t0.plusSeconds(30)));

    SavedGame after = service.load("twice").orElseThrow();
    assertThat(after.name()).isEqualTo("Second");
    try (var stream = Files.list(savesDir)) {
      assertThat(stream.filter(p -> p.getFileName().toString().endsWith(".tmp"))).isEmpty();
    }
  }

  @Test
  void saveThrowsForInvalidSlotName() {
    assertThatThrownBy(() -> service.save("Has Spaces", sampleSaved("x", "X")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Has Spaces");
    assertThatThrownBy(() -> service.save("UPPER", sampleSaved("x", "X")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.save("../escape", sampleSaved("x", "X")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void slugifyIsIdempotent() {
    String once = SaveService.slugify("Partita 12 Aprile");
    String twice = SaveService.slugify(once);
    assertThat(once).isEqualTo("partita-12-aprile");
    assertThat(twice).isEqualTo(once);
  }

  @Test
  void slugifyHandlesUnicodeAndSpaces() {
    assertThat(SaveService.slugify("Domèni  co!!"))
        .as("strips diacritics and collapses runs")
        .isEqualTo("domeni-co");
    assertThat(SaveService.slugify("  Già fatto  ")).isEqualTo("gia-fatto");
    assertThat(SaveService.slugify("PARTITA №3")).isEqualTo("partita-3");
    assertThatThrownBy(() -> SaveService.slugify("   "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void concurrentSavesToDifferentSlotsAreSafe() throws InterruptedException, ExecutionException {
    int slots = 8;
    ExecutorService pool = Executors.newFixedThreadPool(slots);
    try {
      List<Future<?>> futures = new java.util.ArrayList<>();
      for (int i = 0; i < slots; i++) {
        final int idx = i;
        futures.add(
            pool.submit(
                () -> service.save("slot-" + idx, sampleSaved("slot-" + idx, "Slot " + idx))));
      }
      for (Future<?> f : futures) {
        f.get();
      }
    } finally {
      pool.shutdown();
    }
    assertThat(service.listSlots()).hasSize(slots);
    for (int i = 0; i < slots; i++) {
      assertThat(service.load("slot-" + i)).as("slot-%d", i).isPresent();
    }
  }

  @Test
  void serializedStateRoundTripsThroughGameStateAfterMoves() {
    GameState initial = GameState.initial();
    Move firstWhite = pickFirstSimpleMove(initial);
    GameState afterOneMove = applyOrThrow(initial, firstWhite);

    SerializedGameState serialized = SerializedGameState.fromState(afterOneMove);
    GameState rebuilt = serialized.toState();

    assertThat(rebuilt.board()).isEqualTo(afterOneMove.board());
    assertThat(rebuilt.sideToMove()).isEqualTo(afterOneMove.sideToMove());
    assertThat(rebuilt.halfmoveClock()).isEqualTo(afterOneMove.halfmoveClock());
    assertThat(rebuilt.history()).hasSize(1);
    assertThat(rebuilt.status()).isEqualTo(afterOneMove.status());
  }

  @Test
  void savedGameOfWiresUpFieldsFromSinglePlayerGame() {
    Instant created = Instant.parse("2026-04-29T09:00:00Z");
    Instant updated = Instant.parse("2026-04-29T09:05:00Z");
    SinglePlayerGame game =
        SinglePlayerGame.tryCreate(AiLevel.CAMPIONE, ColorChoice.BLACK, "Test", fixedRng())
            .orElseThrow();

    SavedGame saved = SavedGame.of(game, created, updated);

    assertThat(saved.schemaVersion()).isEqualTo(SavedGame.CURRENT_SCHEMA_VERSION);
    assertThat(saved.kind()).isEqualTo(SavedGame.KIND_SINGLE_PLAYER);
    assertThat(saved.name()).isEqualTo("Test");
    assertThat(saved.aiLevel()).isEqualTo(AiLevel.CAMPIONE);
    assertThat(saved.humanColor()).isEqualTo(Color.BLACK);
    assertThat(saved.createdAt()).isEqualTo(created);
    assertThat(saved.updatedAt()).isEqualTo(updated);
    assertThat(saved.moves()).isEmpty();
    assertThat(saved.currentState().toState().board()).isEqualTo(game.state().board());
  }

  @Test
  void loadRejectsMalformedJsonAsUncheckedIo() throws IOException {
    Files.createDirectories(savesDir);
    Files.writeString(savesDir.resolve("broken.json"), "{not valid", StandardCharsets.UTF_8);

    assertThatThrownBy(() -> service.load("broken"))
        .isInstanceOf(UncheckedIOException.class)
        .hasMessageContaining("broken");
  }

  // -- helpers ----------------------------------------------------------------------------------

  private SavedGame sampleSaved(String slot, String name) {
    Instant now = Instant.parse("2026-04-29T12:00:00Z");
    SerializedGameState state = SerializedGameState.fromState(GameState.initial());
    return new SavedGame(
        SavedGame.CURRENT_SCHEMA_VERSION,
        SavedGame.KIND_SINGLE_PLAYER,
        name,
        now,
        now,
        AiLevel.ESPERTO,
        Color.WHITE,
        List.of(),
        state);
  }

  private static Move pickFirstSimpleMove(GameState state) {
    return RULES.legalMoves(state).stream()
        .filter(m -> !m.isCapture())
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("no simple moves at root"));
  }

  private static GameState applyOrThrow(GameState state, Move move) {
    try {
      return RULES.applyMove(state, move);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private static RandomGenerator fixedRng() {
    // Deterministic RNG with explicit seed (no security-sensitive randomness needed in tests).
    return RandomGeneratorFactory.of("L64X128MixRandom").create(42L);
  }

  // Defensive use: keep FidNotation explicitly imported to ensure tests fail loudly on shape drift.
  @SuppressWarnings("unused")
  private static final int FID_MIN = FidNotation.MIN;
}
