package com.damaitaliana.core.match;

/**
 * Thrown by {@link MatchManager} when an operation references a {@link MatchId} that has no
 * corresponding entry in the {@link com.damaitaliana.core.repository.MatchRepository}.
 *
 * <p>Unchecked because callers downstream of the application boundary (REST controllers in the Fase
 * 6 server, JavaFX controllers in the Fase 7 LAN host) translate it into transport-specific
 * outcomes — typically a {@code 404} response — rather than handling it locally.
 */
public final class MatchNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public MatchNotFoundException(MatchId matchId) {
    super("Match not found: " + matchId);
  }
}
