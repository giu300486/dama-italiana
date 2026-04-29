package com.damaitaliana.client.persistence;

import com.damaitaliana.client.app.ClientProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Reads and writes single-player save slots in {@link ClientProperties#savesDir()} (default {@code
 * ~/.dama-italiana/saves/}).
 *
 * <p>Each slot lives in its own JSON file named {@code <slot>.json}. Writes are atomic (ADR-032):
 * the JSON is staged in a sibling {@code .tmp} file and then moved onto the target via {@link
 * StandardCopyOption#ATOMIC_MOVE}. Filesystems that do not support atomic moves fall back to {@link
 * StandardCopyOption#REPLACE_EXISTING} with a WARN log line.
 *
 * <p>The loader validates {@link SavedGame#schemaVersion()} before trusting the rest of the payload
 * (ADR-031, A3.20): an unknown version raises {@link UnknownSchemaVersionException} so the UI can
 * surface the failure as a toast (see Task 3.15).
 *
 * <p>Slot names are user-controlled: external callers feed the human-readable game name through
 * {@link #slugify(String)} to produce a filesystem-safe basename. The reserved slot {@code
 * "_autosave"} is hidden from {@link #listSlots()} so it never shows up in the load screen.
 */
@Component
public class SaveService {

  /** Reserved slot used by the autosave service (Task 3.16); hidden from {@link #listSlots()}. */
  public static final String AUTOSAVE_SLOT = "_autosave";

  private static final Logger log = LoggerFactory.getLogger(SaveService.class);
  private static final String EXTENSION = ".json";
  private static final Pattern SLOT_PATTERN = Pattern.compile("^[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$");
  private static final Pattern SLUG_REPLACE = Pattern.compile("[^a-z0-9]+");
  private static final Pattern SLUG_TRIM = Pattern.compile("(?:^-+)|(?:-+$)");
  private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

  private final Path savesDir;
  private final ObjectMapper mapper;

  public SaveService(ClientProperties properties) {
    Objects.requireNonNull(properties, "properties");
    Objects.requireNonNull(properties.savesDir(), "properties.savesDir");
    this.savesDir = Path.of(properties.savesDir());
    this.mapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);
  }

  /** Directory holding all save files. May not yet exist on disk before the first save. */
  public Path savesDir() {
    return savesDir;
  }

  /** Persists {@code data} under {@code slot}, atomically (ADR-032). */
  public void save(String slot, SavedGame data) {
    requireValidSlot(slot);
    Objects.requireNonNull(data, "data");
    Path target = pathFor(slot);
    try {
      Files.createDirectories(savesDir);
      Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
      try {
        mapper.writeValue(tmp.toFile(), data);
        try {
          Files.move(
              tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
          log.warn(
              "ATOMIC_MOVE not supported on filesystem of {}, falling back to REPLACE_EXISTING",
              target);
          Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
      } finally {
        Files.deleteIfExists(tmp);
      }
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to save slot \"" + slot + "\" to " + target, ex);
    }
  }

  /**
   * Reads {@code slot}.
   *
   * @return {@link Optional#empty()} when the slot does not exist on disk.
   * @throws UnknownSchemaVersionException if the file's {@code schemaVersion} differs from {@link
   *     SavedGame#CURRENT_SCHEMA_VERSION}.
   * @throws UncheckedIOException if the file is unreadable or malformed.
   */
  public Optional<SavedGame> load(String slot) {
    requireValidSlot(slot);
    Path source = pathFor(slot);
    if (!Files.exists(source)) {
      return Optional.empty();
    }
    try {
      SavedGame loaded = mapper.readValue(source.toFile(), SavedGame.class);
      if (loaded.schemaVersion() != SavedGame.CURRENT_SCHEMA_VERSION) {
        throw new UnknownSchemaVersionException(
            slot, loaded.schemaVersion(), SavedGame.CURRENT_SCHEMA_VERSION);
      }
      return Optional.of(loaded);
    } catch (NoSuchFileException ex) {
      return Optional.empty();
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to load slot \"" + slot + "\" from " + source, ex);
    }
  }

  /**
   * Lists all user-visible save slots, sorted by {@code updatedAt} descending. The reserved {@link
   * #AUTOSAVE_SLOT} is excluded; corrupt or schema-incompatible files are skipped with a WARN log
   * line so a single bad slot never breaks the load screen.
   */
  public List<SaveSlotMetadata> listSlots() {
    if (!Files.isDirectory(savesDir)) {
      return List.of();
    }
    List<SaveSlotMetadata> slots = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(savesDir, "*" + EXTENSION)) {
      for (Path file : stream) {
        String name = file.getFileName().toString();
        String slot = name.substring(0, name.length() - EXTENSION.length());
        if (slot.equals(AUTOSAVE_SLOT)) {
          continue;
        }
        try {
          SavedGame data = mapper.readValue(file.toFile(), SavedGame.class);
          if (data.schemaVersion() != SavedGame.CURRENT_SCHEMA_VERSION) {
            log.warn(
                "Skipping slot {}: schemaVersion {} (expected {})",
                slot,
                data.schemaVersion(),
                SavedGame.CURRENT_SCHEMA_VERSION);
            continue;
          }
          slots.add(
              new SaveSlotMetadata(
                  slot,
                  data.name(),
                  data.aiLevel(),
                  data.humanColor(),
                  data.updatedAt(),
                  data.moves().size()));
        } catch (IOException ex) {
          log.warn("Skipping unreadable save file {}", file, ex);
        }
      }
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to list saves in " + savesDir, ex);
    }
    slots.sort((a, b) -> b.updatedAt().compareTo(a.updatedAt()));
    return slots;
  }

  /** Removes {@code slot}. Idempotent: removing a non-existent slot is a no-op. */
  public void delete(String slot) {
    requireValidSlot(slot);
    Path target = pathFor(slot);
    try {
      Files.deleteIfExists(target);
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to delete slot \"" + slot + "\" at " + target, ex);
    }
  }

  /**
   * Maps a user-facing save name to a filesystem-safe slot.
   *
   * <ul>
   *   <li>NFD normalises and strips combining diacritics (so "Domèni{@code co}" → "domenico").
   *   <li>Lower-cases (Italian/English locales).
   *   <li>Replaces every run of non {@code [a-z0-9]} characters with a single {@code -}.
   *   <li>Trims leading and trailing dashes.
   * </ul>
   *
   * <p>Idempotent: {@code slugify(slugify(x)).equals(slugify(x))} for any {@code x}.
   *
   * @throws IllegalArgumentException if the resulting slug is empty (e.g. all-whitespace input) —
   *     callers should validate the user-entered name before saving.
   */
  public static String slugify(String name) {
    Objects.requireNonNull(name, "name");
    String normalised = Normalizer.normalize(name, Normalizer.Form.NFD);
    String stripped = DIACRITICS.matcher(normalised).replaceAll("");
    String lower = stripped.toLowerCase(Locale.ROOT);
    String dashed = SLUG_REPLACE.matcher(lower).replaceAll("-");
    String trimmed = SLUG_TRIM.matcher(dashed).replaceAll("");
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("name does not yield any safe slug: \"" + name + "\"");
    }
    return trimmed;
  }

  private Path pathFor(String slot) {
    return savesDir.resolve(slot + EXTENSION);
  }

  private static void requireValidSlot(String slot) {
    Objects.requireNonNull(slot, "slot");
    if (slot.equals(AUTOSAVE_SLOT)) {
      return;
    }
    if (!SLOT_PATTERN.matcher(slot).matches()) {
      throw new IllegalArgumentException(
          "slot must match [a-z0-9](?:[a-z0-9-]*[a-z0-9])? — was: \"" + slot + "\"");
    }
  }

  /** Thrown by {@link SaveService#load(String)} when a save file's schemaVersion is unknown. */
  public static final class UnknownSchemaVersionException extends RuntimeException {

    private final String slot;
    private final int actual;
    private final int expected;

    public UnknownSchemaVersionException(String slot, int actual, int expected) {
      super(
          "Save slot \""
              + slot
              + "\" has unknown schemaVersion "
              + actual
              + " (expected "
              + expected
              + ")");
      this.slot = slot;
      this.actual = actual;
      this.expected = expected;
    }

    public String slot() {
      return slot;
    }

    public int actualVersion() {
      return actual;
    }

    public int expectedVersion() {
      return expected;
    }
  }
}
