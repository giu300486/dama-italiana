package com.damaitaliana.core.repository;

import com.damaitaliana.core.repository.inmemory.InMemoryMatchRepository;

/**
 * Concrete activation of {@link MatchRepositoryContractTest} against {@link
 * InMemoryMatchRepository} — runs the 12 contract tests defined by the abstract parent against the
 * Fase 4 in-memory adapter. Fase 6 will introduce a JPA-backed sibling test that extends the same
 * contract, ensuring the two adapters honour identical port semantics.
 */
final class InMemoryMatchRepositoryTest extends MatchRepositoryContractTest {

  @Override
  protected MatchRepository createRepository() {
    return new InMemoryMatchRepository();
  }
}
