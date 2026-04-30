package com.damaitaliana.client.persistence;

import com.damaitaliana.client.app.ClientProperties;
import com.damaitaliana.client.controller.SinglePlayerGame;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Single-player autosave wrapper around {@link SaveService}, bound to the reserved {@link
 * SaveService#AUTOSAVE_SLOT} (SPEC §9.2.2). Centralises the read/write/clear/exists API so
 * controllers don't have to spell out the slot name or reproduce the {@code _autosave.json} path
 * resolution.
 *
 * <p>Writes happen on the JavaFX application thread after every applied move (Task 3.16 hook in
 * {@link com.damaitaliana.client.controller.SinglePlayerController}). They reuse the atomic
 * tmp-and-move strategy of {@link SaveService} (ADR-032) so a crash mid-write never produces a
 * corrupt file.
 *
 * <p>The materialised {@link SinglePlayerGame} returned by {@link #readAutosave()} is rebuilt from
 * the on-disk {@link SavedGame} via {@link SinglePlayerGame#fromSaved}; the {@link RandomGenerator}
 * is freshly supplied because RNG state is not persisted (Principiante's noise resets per session,
 * ADR-028).
 */
@Component
public class AutosaveService {

  private final SaveService saveService;
  private final Path autosaveFile;
  private final Supplier<RandomGenerator> rngSupplier;
  private final Clock clock;

  @Autowired
  public AutosaveService(SaveService saveService, ClientProperties properties) {
    this(saveService, properties, () -> new SplittableRandom(System.nanoTime()), Clock.systemUTC());
  }

  /** Visible for tests: lets a deterministic RNG supplier and clock be injected. */
  AutosaveService(
      SaveService saveService,
      ClientProperties properties,
      Supplier<RandomGenerator> rngSupplier,
      Clock clock) {
    this.saveService = Objects.requireNonNull(saveService, "saveService");
    Objects.requireNonNull(properties, "properties");
    Objects.requireNonNull(properties.savesDir(), "properties.savesDir");
    this.autosaveFile = Path.of(properties.savesDir(), SaveService.AUTOSAVE_SLOT + ".json");
    this.rngSupplier = Objects.requireNonNull(rngSupplier, "rngSupplier");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /**
   * Persists {@code game} to the autosave slot, atomically. Existing autosave content is replaced.
   *
   * @throws java.io.UncheckedIOException if the disk write fails.
   */
  public void writeAutosave(SinglePlayerGame game) {
    Objects.requireNonNull(game, "game");
    Instant now = Instant.now(clock);
    SavedGame data = SavedGame.of(game, now, now);
    saveService.save(SaveService.AUTOSAVE_SLOT, data);
  }

  /**
   * Reads the autosave slot.
   *
   * @return {@link Optional#empty()} when no autosave exists on disk.
   * @throws SaveService.UnknownSchemaVersionException if the file uses a {@code schemaVersion} this
   *     build does not understand (ADR-031). The caller is expected to surface this as an error
   *     toast and clear the autosave.
   * @throws java.io.UncheckedIOException if the file is unreadable or malformed.
   */
  public Optional<SinglePlayerGame> readAutosave() {
    return saveService
        .load(SaveService.AUTOSAVE_SLOT)
        .map(saved -> SinglePlayerGame.fromSaved(saved, rngSupplier.get()));
  }

  /** Removes the autosave file. Idempotent: a no-op when no autosave exists. */
  public void clearAutosave() {
    saveService.delete(SaveService.AUTOSAVE_SLOT);
  }

  /**
   * Tests whether an autosave file is present. Cheap file-existence check; does not parse the JSON,
   * so a corrupt or schema-mismatched file still reports {@code true} and surfaces the issue on the
   * next {@link #readAutosave()}.
   */
  public boolean autosaveExists() {
    return Files.exists(autosaveFile);
  }
}
