package com.damaitaliana.client.ui.rules;

import com.damaitaliana.client.persistence.SerializedGameState;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * A static, hand-authored board diagram used to illustrate a rule on the rules screen.
 *
 * <p>Re-uses {@link SerializedGameState} (the F1 corpus / save schema) so the same {@link
 * com.damaitaliana.client.ui.save.MiniatureRenderer MiniatureRenderer} that paints save thumbnails
 * also paints these diagrams (FR-RUL-03).
 *
 * @param captionKey i18n key whose value is shown beneath the diagram (paragraph-style).
 * @param position the FID-encoded position to render. {@code halfmoveClock} and {@code history} are
 *     left at their JSON defaults — these positions are illustrative, not playable.
 */
public record RuleDiagram(String captionKey, SerializedGameState position) {

  @JsonCreator
  public RuleDiagram(
      @JsonProperty("captionKey") String captionKey,
      @JsonProperty("position") SerializedGameState position) {
    this.captionKey = Objects.requireNonNull(captionKey, "captionKey");
    this.position = Objects.requireNonNull(position, "position");
  }
}
