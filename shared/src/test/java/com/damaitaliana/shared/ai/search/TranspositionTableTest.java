package com.damaitaliana.shared.ai.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damaitaliana.shared.ai.search.TranspositionTable.NodeType;
import com.damaitaliana.shared.ai.search.TranspositionTable.TtEntry;
import com.damaitaliana.shared.domain.SimpleMove;
import com.damaitaliana.shared.domain.Square;
import org.junit.jupiter.api.Test;

class TranspositionTableTest {

  private final SimpleMove someMove = new SimpleMove(new Square(0, 0), new Square(1, 1));

  @Test
  void rejectsNonPowerOfTwoSize() {
    assertThatThrownBy(() -> new TranspositionTable(7))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new TranspositionTable(0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new TranspositionTable(1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void emptyTableReturnsNullOnProbe() {
    TranspositionTable tt = new TranspositionTable(8);
    assertThat(tt.probe(0xDEAD_BEEFL)).isNull();
  }

  @Test
  void probeReturnsStoredEntryWhenHashMatches() {
    TranspositionTable tt = new TranspositionTable(8);
    TtEntry e = new TtEntry(0xABCDL, 42, 5, NodeType.EXACT, someMove);
    tt.store(e);
    assertThat(tt.probe(0xABCDL)).isSameAs(e);
  }

  @Test
  void probeReturnsNullForSlotCollisionWithDifferentHash() {
    TranspositionTable tt = new TranspositionTable(8); // mask = 0b111
    TtEntry e = new TtEntry(0x1L, 42, 5, NodeType.EXACT, someMove);
    tt.store(e);
    // 0x9 has the same low 3 bits as 0x1 (both = 0b001).
    assertThat(tt.probe(0x9L)).isNull();
  }

  @Test
  void storeAlwaysReplacesExisting() {
    TranspositionTable tt = new TranspositionTable(8);
    TtEntry first = new TtEntry(0x1L, 10, 3, NodeType.EXACT, someMove);
    TtEntry second = new TtEntry(0x9L, 20, 5, NodeType.LOWER_BOUND, someMove);
    tt.store(first);
    tt.store(second); // 0x9 maps to the same slot as 0x1; overwrites it.
    assertThat(tt.probe(0x1L)).isNull();
    assertThat(tt.probe(0x9L)).isSameAs(second);
  }

  @Test
  void clearWipesAllSlots() {
    TranspositionTable tt = new TranspositionTable(8);
    tt.store(new TtEntry(0x1L, 10, 3, NodeType.EXACT, someMove));
    tt.store(new TtEntry(0x2L, 20, 4, NodeType.LOWER_BOUND, someMove));
    tt.clear();
    assertThat(tt.probe(0x1L)).isNull();
    assertThat(tt.probe(0x2L)).isNull();
  }

  @Test
  void sizeReportsCapacity() {
    assertThat(new TranspositionTable(64).size()).isEqualTo(64);
    assertThat(new TranspositionTable().size()).isEqualTo(TranspositionTable.DEFAULT_SIZE);
  }

  @Test
  void rejectsNullEntryOnStore() {
    TranspositionTable tt = new TranspositionTable(8);
    assertThatThrownBy(() -> tt.store(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsNullNodeTypeInRecord() {
    assertThatThrownBy(() -> new TtEntry(0L, 0, 0, null, someMove))
        .isInstanceOf(NullPointerException.class);
  }
}
