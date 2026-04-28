package com.damaitaliana.shared.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class CancellationTokenTest {

  // --- never ---

  @Test
  void neverIsNotCancelled() {
    assertThat(CancellationToken.never().isCancelled()).isFalse();
    CancellationToken.never().throwIfCancelled(); // no throw
  }

  @Test
  void neverIsSingleton() {
    assertThat(CancellationToken.never()).isSameAs(CancellationToken.never());
  }

  // --- mutable ---

  @Test
  void mutableStartsUncancelledThenStaysCancelled() {
    MutableCancellationToken t = new MutableCancellationToken();
    assertThat(t.isCancelled()).isFalse();
    t.cancel();
    assertThat(t.isCancelled()).isTrue();
    t.cancel(); // idempotent
    assertThat(t.isCancelled()).isTrue();
  }

  @Test
  void throwIfCancelledRaisesAfterCancellation() {
    MutableCancellationToken t = new MutableCancellationToken();
    t.throwIfCancelled(); // no throw
    t.cancel();
    assertThatThrownBy(t::throwIfCancelled).isInstanceOf(SearchCancelledException.class);
  }

  // --- deadline ---

  @Test
  void deadlineInThePastIsImmediatelyCancelled() {
    Instant past = Instant.now().minus(Duration.ofSeconds(10));
    CancellationToken t = CancellationToken.deadline(past);
    assertThat(t.isCancelled()).isTrue();
  }

  @Test
  void deadlineInTheFarFutureIsNotCancelled() {
    Instant farFuture = Instant.now().plus(Duration.ofHours(1));
    CancellationToken t = CancellationToken.deadline(farFuture);
    assertThat(t.isCancelled()).isFalse();
  }

  @Test
  void deadlineFiresWhenClockReachesIt() {
    Instant fixed = Instant.parse("2026-04-28T17:00:00Z");
    Clock at = Clock.fixed(fixed, ZoneOffset.UTC);
    Clock before = Clock.fixed(fixed.minusSeconds(1), ZoneOffset.UTC);
    Clock after = Clock.fixed(fixed.plusSeconds(1), ZoneOffset.UTC);
    assertThat(CancellationToken.deadline(fixed, before).isCancelled()).isFalse();
    assertThat(CancellationToken.deadline(fixed, at).isCancelled()).isTrue();
    assertThat(CancellationToken.deadline(fixed, after).isCancelled()).isTrue();
  }

  @Test
  void deadlineRejectsNullArguments() {
    assertThatThrownBy(() -> CancellationToken.deadline(null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> CancellationToken.deadline(null, Clock.systemUTC()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> CancellationToken.deadline(Instant.now(), null))
        .isInstanceOf(NullPointerException.class);
  }

  // --- composite ---

  @Test
  void compositeIsCancelledWhenAnyChildIs() {
    MutableCancellationToken a = new MutableCancellationToken();
    MutableCancellationToken b = new MutableCancellationToken();
    CancellationToken c = CancellationToken.composite(a, b);
    assertThat(c.isCancelled()).isFalse();
    a.cancel();
    assertThat(c.isCancelled()).isTrue();
  }

  @Test
  void compositeOfSingleTokenMatchesIt() {
    MutableCancellationToken a = new MutableCancellationToken();
    CancellationToken c = CancellationToken.composite(a);
    assertThat(c.isCancelled()).isFalse();
    a.cancel();
    assertThat(c.isCancelled()).isTrue();
  }

  @Test
  void compositeOfZeroTokensIsNeverCancelled() {
    CancellationToken c = CancellationToken.composite();
    assertThat(c.isCancelled()).isFalse();
  }

  @Test
  void compositeRejectsNullInputs() {
    assertThatThrownBy(() -> CancellationToken.composite((CancellationToken[]) null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> CancellationToken.composite(CancellationToken.never(), null))
        .isInstanceOf(NullPointerException.class);
  }
}
