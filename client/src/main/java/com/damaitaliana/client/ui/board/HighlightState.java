package com.damaitaliana.client.ui.board;

import com.damaitaliana.shared.domain.Square;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Tracks which squares are currently highlighted as legal-move targets (FR-SP-04) and which are
 * highlighted as mandatory-capture sources (FR-SP-05). Plain POJO with no JavaFX dependency so
 * {@link BoardRenderer} can update node style classes from the same source of truth that unit tests
 * consume.
 */
public final class HighlightState {

  private final Set<Square> legalTargets = new HashSet<>();
  private final Set<Square> mandatorySources = new HashSet<>();

  public void setLegalTargets(List<Square> targets) {
    Objects.requireNonNull(targets, "targets");
    legalTargets.clear();
    legalTargets.addAll(targets);
  }

  public void setMandatorySources(List<Square> sources) {
    Objects.requireNonNull(sources, "sources");
    mandatorySources.clear();
    mandatorySources.addAll(sources);
  }

  public void clear() {
    legalTargets.clear();
    mandatorySources.clear();
  }

  public boolean isLegalTarget(Square s) {
    return legalTargets.contains(s);
  }

  public boolean isMandatorySource(Square s) {
    return mandatorySources.contains(s);
  }

  public Set<Square> legalTargets() {
    return Set.copyOf(legalTargets);
  }

  public Set<Square> mandatorySources() {
    return Set.copyOf(mandatorySources);
  }
}
