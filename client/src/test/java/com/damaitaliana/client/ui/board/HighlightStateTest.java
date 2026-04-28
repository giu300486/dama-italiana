package com.damaitaliana.client.ui.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.damaitaliana.shared.domain.Square;
import java.util.List;
import org.junit.jupiter.api.Test;

class HighlightStateTest {

  private final HighlightState state = new HighlightState();

  @Test
  void freshStateHasNoHighlights() {
    assertThat(state.legalTargets()).isEmpty();
    assertThat(state.mandatorySources()).isEmpty();
  }

  @Test
  void setLegalTargetsReplacesPreviousSet() {
    state.setLegalTargets(List.of(new Square(0, 0), new Square(2, 2)));
    state.setLegalTargets(List.of(new Square(4, 4)));
    assertThat(state.legalTargets()).containsExactly(new Square(4, 4));
  }

  @Test
  void setMandatorySourcesReplacesPreviousSet() {
    state.setMandatorySources(List.of(new Square(1, 1)));
    state.setMandatorySources(List.of());
    assertThat(state.mandatorySources()).isEmpty();
  }

  @Test
  void clearRemovesBothCategories() {
    state.setLegalTargets(List.of(new Square(0, 0)));
    state.setMandatorySources(List.of(new Square(2, 2)));
    state.clear();
    assertThat(state.legalTargets()).isEmpty();
    assertThat(state.mandatorySources()).isEmpty();
  }

  @Test
  void containsChecks() {
    Square s = new Square(3, 3);
    state.setLegalTargets(List.of(s));
    assertThat(state.isLegalTarget(s)).isTrue();
    assertThat(state.isLegalTarget(new Square(0, 0))).isFalse();
    assertThat(state.isMandatorySource(s)).isFalse();
  }

  @Test
  void exposedSetsAreImmutableCopies() {
    state.setLegalTargets(List.of(new Square(0, 0)));
    var snapshot = state.legalTargets();
    state.clear();
    // Snapshot kept the value at the time it was taken
    assertThat(snapshot).containsExactly(new Square(0, 0));
  }

  @Test
  void nullArgumentsRejected() {
    assertThatNullPointerException().isThrownBy(() -> state.setLegalTargets(null));
    assertThatNullPointerException().isThrownBy(() -> state.setMandatorySources(null));
  }
}
