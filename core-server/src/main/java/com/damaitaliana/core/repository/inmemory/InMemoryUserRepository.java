package com.damaitaliana.core.repository.inmemory;

import com.damaitaliana.core.match.UserRef;
import com.damaitaliana.core.repository.UserRepository;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory adapter for {@link UserRepository}. Skeleton in Fase 4: holds two indexes (by primary
 * key and by username) populated via {@link #register(UserRef)}. The full user lifecycle
 * (registration with BCrypt cost ≥ 12, JWT issuance) lands in Fase 5 with a JPA-backed adapter in
 * the {@code server} module (SPEC §8.4).
 *
 * <p>Lookup of {@link UserRef#ANONYMOUS_LAN_ID} via {@link #findById(long)} always returns empty —
 * anonymous LAN users are not addressable by id (SPEC §11.1: "username + password opzionale al
 * CONNECT").
 */
@Component
public final class InMemoryUserRepository implements UserRepository {

  private final ConcurrentHashMap<Long, UserRef> byId = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, UserRef> byUsername = new ConcurrentHashMap<>();

  /**
   * Adds or replaces {@code user} in both indexes. Anonymous LAN users (id == {@link
   * UserRef#ANONYMOUS_LAN_ID}) are indexed by username only — id-side lookup of the sentinel always
   * returns empty.
   */
  public void register(UserRef user) {
    if (!user.isAnonymous()) {
      byId.put(user.id(), user);
    }
    byUsername.put(user.username(), user);
  }

  @Override
  public Optional<UserRef> findById(long id) {
    if (id == UserRef.ANONYMOUS_LAN_ID) {
      return Optional.empty();
    }
    return Optional.ofNullable(byId.get(id));
  }

  @Override
  public Optional<UserRef> findByUsername(String username) {
    return Optional.ofNullable(byUsername.get(username));
  }
}
