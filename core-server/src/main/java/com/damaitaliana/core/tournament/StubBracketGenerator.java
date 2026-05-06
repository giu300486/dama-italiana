package com.damaitaliana.core.tournament;

import com.damaitaliana.core.match.UserRef;
import java.util.List;

/**
 * Fase 4 placeholder {@link BracketGenerator}: every call throws {@link
 * UnsupportedOperationException} (PLAN-fase-4 §7.1, risk R-2). Not registered as a Spring bean —
 * Fase 8 introduces a real {@code SingleEliminationBracketGenerator} {@code @Component} without
 * collision. This class exists only to (a) prove the interface compiles end-to-end and (b) give
 * tests a quick handle to assert that the contract throws until Fase 8.
 */
public final class StubBracketGenerator implements BracketGenerator {

  @Override
  public List<UserRef> generate(List<UserRef> seeds) {
    throw new UnsupportedOperationException(
        "BracketGenerator.generate is deferred to Fase 8 (single-elimination)."
            + " PLAN-fase-4 §4.10 risk R-2.");
  }
}
