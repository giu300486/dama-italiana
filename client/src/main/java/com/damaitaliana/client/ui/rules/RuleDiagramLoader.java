package com.damaitaliana.client.ui.rules;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Loads {@link RuleDiagram} lists from classpath JSON files (one file per {@link RuleSection}).
 *
 * <p>Format: each file is a JSON array of {@link RuleDiagram} objects (caption key + position).
 * Loaded once per call — files are tiny enough that caching is not worth the complexity.
 */
@Component
public class RuleDiagramLoader {

  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * @return the diagrams configured for {@code section}, or {@link List#of() empty} if the section
   *     has no diagram resource.
   * @throws IllegalStateException if the resource path is set but the file is missing on the
   *     classpath.
   * @throws UncheckedIOException on JSON parse failure.
   */
  public List<RuleDiagram> loadFor(RuleSection section) {
    Objects.requireNonNull(section, "section");
    if (!section.hasDiagrams()) {
      return List.of();
    }
    String path = section.diagramResource();
    try (InputStream in = getClass().getResourceAsStream(path)) {
      if (in == null) {
        throw new IllegalStateException("Rule diagram resource missing on classpath: " + path);
      }
      return mapper.readValue(in, new TypeReference<List<RuleDiagram>>() {});
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to parse rule diagrams at " + path, ex);
    }
  }
}
