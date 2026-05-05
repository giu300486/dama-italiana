package com.damaitaliana.core.repository.inmemory;

import com.damaitaliana.core.match.Match;
import com.damaitaliana.core.match.MatchId;
import com.damaitaliana.core.match.MatchStatus;
import com.damaitaliana.core.match.UserRef;
import com.damaitaliana.core.match.event.MatchEvent;
import com.damaitaliana.core.repository.MatchRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory adapter for {@link MatchRepository} (SPEC §7.2 "Adapter in-memory built-in (per LAN
 * host)"). Used by the LAN host (Fase 7 wires it via {@code CoreServerConfiguration}) and by the
 * core-server tests. The Internet server (Fase 6) provides a JPA-backed adapter against the {@code
 * matches}/{@code match_events} tables (SPEC §8.4).
 *
 * <p>Concurrency strategy (PLAN-fase-4 §4.4):
 *
 * <ul>
 *   <li>{@link ConcurrentHashMap} for the per-match snapshot — concurrent reads, point writes.
 *   <li>{@link ConcurrentHashMap} of {@link Collections#synchronizedList(List) synchronized lists}
 *       for the per-match event log. The list is acquired and locked during {@link #appendEvent}
 *       validation so the strict monotonic check (FR-COM-04, SPEC §7.5) and the append are atomic
 *       under contention. The Fase 6 JPA adapter will rely on a {@code UNIQUE(match_id,
 *       sequence_no)} constraint and pessimistic row locking instead.
 * </ul>
 *
 * <p>Sequence semantics: the first event of a match must carry {@code sequenceNo == 0}; the {@code
 * n}-th event must carry {@code sequenceNo == n - 1}. {@link #currentSequenceNo(MatchId)} returns
 * the highest assigned sequence number, or {@code -1L} if the match has no events (or is unknown).
 */
@Component
public final class InMemoryMatchRepository implements MatchRepository {

  private final ConcurrentHashMap<MatchId, Match> snapshots = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<MatchId, List<MatchEvent>> eventLogs = new ConcurrentHashMap<>();

  @Override
  public Match save(Match match) {
    snapshots.put(match.id(), match);
    return match;
  }

  @Override
  public Optional<Match> findById(MatchId id) {
    return Optional.ofNullable(snapshots.get(id));
  }

  @Override
  public List<Match> findByStatus(MatchStatus status) {
    var matches = new ArrayList<Match>();
    for (Match m : snapshots.values()) {
      if (m.status() == status) {
        matches.add(m);
      }
    }
    return List.copyOf(matches);
  }

  @Override
  public List<Match> findByPlayer(UserRef user) {
    var matches = new ArrayList<Match>();
    for (Match m : snapshots.values()) {
      if (m.white().equals(user) || m.black().equals(user)) {
        matches.add(m);
      }
    }
    return List.copyOf(matches);
  }

  @Override
  public void appendEvent(MatchEvent event) {
    List<MatchEvent> log =
        eventLogs.computeIfAbsent(
            event.matchId(), k -> Collections.synchronizedList(new ArrayList<>()));
    synchronized (log) {
      long expected = log.size();
      if (event.sequenceNo() != expected) {
        throw new IllegalStateException(
            "match "
                + event.matchId()
                + ": expected sequenceNo "
                + expected
                + " but event carries "
                + event.sequenceNo());
      }
      log.add(event);
    }
  }

  @Override
  public List<MatchEvent> eventsSince(MatchId matchId, long fromSeq) {
    List<MatchEvent> log = eventLogs.get(matchId);
    if (log == null) {
      return List.of();
    }
    synchronized (log) {
      var suffix = new ArrayList<MatchEvent>();
      for (MatchEvent e : log) {
        if (e.sequenceNo() > fromSeq) {
          suffix.add(e);
        }
      }
      return List.copyOf(suffix);
    }
  }

  @Override
  public long currentSequenceNo(MatchId matchId) {
    List<MatchEvent> log = eventLogs.get(matchId);
    if (log == null) {
      return -1L;
    }
    synchronized (log) {
      return (long) log.size() - 1L;
    }
  }
}
