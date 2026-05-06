package com.damaitaliana.core.tournament;

import com.damaitaliana.core.match.UserRef;
import java.util.List;

/**
 * Fase 4 placeholder {@link RoundRobinScheduler}: every call throws {@link
 * UnsupportedOperationException} (PLAN-fase-4 §7.1, risk R-2). Not registered as a Spring bean —
 * Fase 9 introduces a real {@code BergerRoundRobinScheduler} {@code @Component} without collision.
 * Same role as {@link StubBracketGenerator} for the round-robin format.
 */
public final class StubRoundRobinScheduler implements RoundRobinScheduler {

  @Override
  public List<UserRef> schedule(List<UserRef> participants) {
    throw new UnsupportedOperationException(
        "RoundRobinScheduler.schedule is deferred to Fase 9 (round-robin)."
            + " PLAN-fase-4 §4.10 risk R-2.");
  }
}
