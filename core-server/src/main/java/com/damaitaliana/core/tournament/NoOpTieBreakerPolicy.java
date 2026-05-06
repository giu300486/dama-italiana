package com.damaitaliana.core.tournament;

import com.damaitaliana.core.match.UserRef;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Fase 4 fallback {@link TieBreakerPolicy}: returns the input list unchanged. Used as the default
 * Spring bean so {@link TournamentEngine} can be wired end-to-end before the Fase 9 policy lands.
 *
 * <p>PLAN-fase-4 §4.10 originally suggested {@code @ConditionalOnMissingBean} so a real policy in
 * Fase 9 would automatically displace this one. That annotation lives in {@code
 * spring-boot-autoconfigure} and would violate CLAUDE.md §8.7-8.8 (transport-agnostic invariant),
 * so this class is a plain {@code @Component}. When Fase 9 introduces a real policy bean it will be
 * marked {@code @Primary} or this fallback will be removed — both choices are cheaper than pulling
 * Spring Boot into core-server. Same pattern as {@link
 * com.damaitaliana.core.stomp.LoggingStompPublisher}.
 */
@Component
public final class NoOpTieBreakerPolicy implements TieBreakerPolicy {

  @Override
  public List<UserRef> resolveTies(List<UserRef> tied, RoundRobinTournament tournament) {
    Objects.requireNonNull(tied, "tied");
    Objects.requireNonNull(tournament, "tournament");
    return List.copyOf(tied);
  }
}
