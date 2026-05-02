package com.damaitaliana.core.match;

import java.util.Objects;

/**
 * Reference to a user participating in a match.
 *
 * <p>Two flavours coexist in v1:
 *
 * <ul>
 *   <li><b>Anonymous LAN</b> — created by {@link #anonymousLan(String)} when the user joins a LAN
 *       host without server-side authentication (SPEC §11.1: "username + password opzionale al
 *       CONNECT"). The id is set to {@link #ANONYMOUS_LAN_ID} ({@code -1}); username and
 *       displayName are typically the same string the user typed in the LAN connect dialog.
 *   <li><b>Authenticated</b> — created by {@link #authenticated(long, String, String)} after
 *       server-side login (Fase 5+). The id is the database primary key from the {@code users}
 *       table (SPEC §8.4) and is non-negative.
 * </ul>
 *
 * <p>Use {@link #isAnonymous()} to discriminate without leaking the magic number.
 */
public record UserRef(long id, String username, String displayName) {

  /** Sentinel id for anonymous LAN users. Distinguishable from any DB-assigned non-negative id. */
  public static final long ANONYMOUS_LAN_ID = -1L;

  public UserRef {
    Objects.requireNonNull(username, "username");
    Objects.requireNonNull(displayName, "displayName");
    if (username.isBlank()) {
      throw new IllegalArgumentException("username must not be blank");
    }
    if (displayName.isBlank()) {
      throw new IllegalArgumentException("displayName must not be blank");
    }
  }

  /** True iff this user joined a LAN host without server-side authentication. */
  public boolean isAnonymous() {
    return id == ANONYMOUS_LAN_ID;
  }

  /** Factory for an anonymous LAN user. {@code displayName} defaults to {@code username}. */
  public static UserRef anonymousLan(String username) {
    return new UserRef(ANONYMOUS_LAN_ID, username, username);
  }

  /**
   * Factory for a server-authenticated user.
   *
   * @throws IllegalArgumentException if {@code id} is negative.
   */
  public static UserRef authenticated(long id, String username, String displayName) {
    if (id < 0) {
      throw new IllegalArgumentException("authenticated user id must be >= 0, got: " + id);
    }
    return new UserRef(id, username, displayName);
  }
}
