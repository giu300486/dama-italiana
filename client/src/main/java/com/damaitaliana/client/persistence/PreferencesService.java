package com.damaitaliana.client.persistence;

import com.damaitaliana.client.app.ClientProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Reads and writes {@link UserPreferences} on disk at the path configured by {@link
 * ClientProperties#configFile()} (default {@code ~/.dama-italiana/config.json}).
 *
 * <p>Writes are atomic (ADR-032): the JSON is staged in a sibling {@code .tmp} file and then moved
 * onto the target via {@link StandardCopyOption#ATOMIC_MOVE}. Filesystems that do not support
 * atomic moves fall back to {@link StandardCopyOption#REPLACE_EXISTING} with a WARN log line.
 *
 * <p>Reads are defensive. A missing file, an unreadable file, a malformed JSON or an unknown {@code
 * schemaVersion} all return {@link UserPreferences#defaults()} with a log line — the client always
 * boots into a usable state regardless of disk content.
 */
@Component
public class PreferencesService {

  private static final Logger log = LoggerFactory.getLogger(PreferencesService.class);

  private final Path configFile;
  private final ObjectMapper mapper;

  public PreferencesService(ClientProperties properties) {
    Objects.requireNonNull(properties, "properties");
    Objects.requireNonNull(properties.configFile(), "properties.configFile");
    this.configFile = Path.of(properties.configFile());
    this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
  }

  public Path configFile() {
    return configFile;
  }

  public UserPreferences load() {
    if (!Files.exists(configFile)) {
      log.info("Config file {} not found, using defaults", configFile);
      return UserPreferences.defaults();
    }
    try {
      UserPreferences prefs = mapper.readValue(configFile.toFile(), UserPreferences.class);
      int version = prefs.schemaVersion();
      if (version == UserPreferences.CURRENT_SCHEMA_VERSION) {
        return prefs;
      }
      if (version == 1 || version == 2) {
        // v1 → v3: Jackson missing-key path filled audio defaults (SPEC §13.4) and window
        //          fields stay null (no persisted Stage state — fallback to computed 80%).
        // v2 → v3: only window fields stay null; audio already populated.
        log.info(
            "Migrating config {} from schemaVersion {} to {}",
            configFile,
            version,
            UserPreferences.CURRENT_SCHEMA_VERSION);
        return prefs.withSchemaVersion(UserPreferences.CURRENT_SCHEMA_VERSION);
      }
      log.warn(
          "Unknown config schemaVersion {} (expected {} or earlier supported), using defaults",
          version,
          UserPreferences.CURRENT_SCHEMA_VERSION);
      return UserPreferences.defaults();
    } catch (IOException ex) {
      log.warn("Failed to parse config {} — using defaults", configFile, ex);
      return UserPreferences.defaults();
    }
  }

  public void save(UserPreferences prefs) {
    Objects.requireNonNull(prefs, "prefs");
    try {
      Path parent = configFile.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Path tmp = configFile.resolveSibling(configFile.getFileName() + ".tmp");
      try {
        mapper.writeValue(tmp.toFile(), prefs);
        try {
          Files.move(
              tmp, configFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
          log.warn(
              "ATOMIC_MOVE not supported on filesystem of {}, falling back to REPLACE_EXISTING",
              configFile);
          Files.move(tmp, configFile, StandardCopyOption.REPLACE_EXISTING);
        }
      } finally {
        Files.deleteIfExists(tmp);
      }
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to save preferences to " + configFile, ex);
    }
  }
}
