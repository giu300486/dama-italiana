package com.damaitaliana.core.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.core.match.UserRef;
import com.damaitaliana.core.repository.inmemory.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Direct tests for {@link InMemoryUserRepository} covering register / findById / findByUsername.
 * Closes the F-002 coverage gap from REVIEW Fase 4: in particular the {@link
 * UserRef#ANONYMOUS_LAN_ID} guard in {@link InMemoryUserRepository#findById(long)} (semantica SPEC
 * §11.1: anonymous LAN users not addressable by id).
 */
class InMemoryUserRepositoryTest {

  private InMemoryUserRepository repo;

  @BeforeEach
  void setUp() {
    repo = new InMemoryUserRepository();
  }

  @Test
  void registerPopulatesBothIdAndUsernameIndexesForAuthenticatedUser() {
    UserRef alice = UserRef.authenticated(42L, "alice", "Alice Wonderland");

    repo.register(alice);

    assertThat(repo.findById(42L)).contains(alice);
    assertThat(repo.findByUsername("alice")).contains(alice);
  }

  @Test
  void anonymousLanUserIsIndexedByUsernameOnly() {
    UserRef bob = UserRef.anonymousLan("bob");

    repo.register(bob);

    // SPEC §11.1: anonymous LAN users are not addressable by id.
    assertThat(repo.findById(UserRef.ANONYMOUS_LAN_ID)).isEmpty();
    assertThat(repo.findByUsername("bob")).contains(bob);
  }

  @Test
  void findByIdAlwaysReturnsEmptyForAnonymousLanSentinel() {
    // Even if the byId map were poisoned with the sentinel (it cannot via register, but the
    // public contract documents the guard), findById must short-circuit.
    UserRef alice = UserRef.authenticated(1L, "alice", "Alice");
    repo.register(alice);

    assertThat(repo.findById(UserRef.ANONYMOUS_LAN_ID)).isEmpty();
  }

  @Test
  void findByIdReturnsEmptyForUnknownAuthenticatedId() {
    assertThat(repo.findById(99L)).isEmpty();
  }

  @Test
  void findByUsernameReturnsEmptyForUnknownUsername() {
    assertThat(repo.findByUsername("ghost")).isEmpty();
  }

  @Test
  void registerOverwritesPreviousSnapshotForSameId() {
    UserRef before = UserRef.authenticated(7L, "alice", "Alice");
    UserRef after = UserRef.authenticated(7L, "alice", "Alice The Renamed");

    repo.register(before);
    repo.register(after);

    assertThat(repo.findById(7L)).contains(after);
    assertThat(repo.findByUsername("alice")).contains(after);
  }

  @Test
  void multipleAuthenticatedUsersCoexistInBothIndexes() {
    UserRef alice = UserRef.authenticated(1L, "alice", "Alice");
    UserRef bob = UserRef.authenticated(2L, "bob", "Bob");

    repo.register(alice);
    repo.register(bob);

    assertThat(repo.findById(1L)).contains(alice);
    assertThat(repo.findById(2L)).contains(bob);
    assertThat(repo.findByUsername("alice")).contains(alice);
    assertThat(repo.findByUsername("bob")).contains(bob);
  }
}
