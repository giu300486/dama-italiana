package com.damaitaliana.shared.ai.search;

import com.damaitaliana.shared.domain.Move;
import java.util.Objects;

/**
 * Fixed-size transposition table backed by a circular array (ADR-026).
 *
 * <p>An entry is keyed by its full 64-bit Zobrist hash (the table compares the stored hash to the
 * probed one for collision detection — a slot collision is not a hash collision). The default size
 * is 2^20 entries ≈ 32 MB at 32 bytes per entry; tests use smaller sizes.
 *
 * <p>Replacement policy: always-replace. Simpler than depth-based replacement, and good enough at
 * Campione's depth budget. If profiling later shows that high-depth entries get evicted too
 * aggressively, this class can grow a "prefer deeper/newer" rule.
 */
public final class TranspositionTable {

  /** Default size: 2^20 entries. */
  public static final int DEFAULT_SIZE = 1 << 20;

  /** Score bound semantics for a stored entry. */
  public enum NodeType {
    /** {@code score} is the exact value of the position at the stored depth. */
    EXACT,
    /** A beta cutoff happened; {@code score} is a lower bound. */
    LOWER_BOUND,
    /** No move improved alpha; {@code score} is an upper bound. */
    UPPER_BOUND
  }

  /** A single TT slot. */
  public record TtEntry(long hash, int score, int depth, NodeType type, Move bestMove) {
    public TtEntry {
      Objects.requireNonNull(type, "type");
    }
  }

  private final TtEntry[] entries;
  private final int mask;

  /** Builds a table with the {@link #DEFAULT_SIZE}. */
  public TranspositionTable() {
    this(DEFAULT_SIZE);
  }

  /**
   * Builds a table with the requested size. {@code size} MUST be a power of two and at least {@code
   * 2}.
   */
  public TranspositionTable(int size) {
    if (size < 2 || (size & (size - 1)) != 0) {
      throw new IllegalArgumentException("size must be a power of two >= 2, got " + size);
    }
    this.entries = new TtEntry[size];
    this.mask = size - 1;
  }

  /**
   * Probes the table by hash. Returns {@code null} if the slot is empty or holds a different hash
   * (slot collision).
   */
  public TtEntry probe(long hash) {
    TtEntry e = entries[(int) (hash & mask)];
    if (e != null && e.hash == hash) {
      return e;
    }
    return null;
  }

  /** Stores {@code entry} at its slot, always replacing whatever was there. */
  public void store(TtEntry entry) {
    Objects.requireNonNull(entry, "entry");
    entries[(int) (entry.hash() & mask)] = entry;
  }

  /** Wipes every slot. Useful between games to avoid stale entries. */
  public void clear() {
    for (int i = 0; i < entries.length; i++) {
      entries[i] = null;
    }
  }

  /** Number of slots in the table (not the count of populated entries). */
  public int size() {
    return entries.length;
  }
}
