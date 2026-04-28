package com.damaitaliana.shared.ai.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.shared.domain.Board;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.PieceKind;
import com.damaitaliana.shared.domain.Square;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ZobristHasherTest {

  private final ZobristHasher hasher = new ZobristHasher();

  @Test
  void sameStateProducesSameHash() {
    long h1 = hasher.hash(GameState.initial());
    long h2 = hasher.hash(GameState.initial());
    assertThat(h1).isEqualTo(h2);
  }

  @Test
  void differentSeedsProduceDifferentTables() {
    long h1 = hasher.hash(GameState.initial());
    long h2 = new ZobristHasher(0xCAFEBABEL).hash(GameState.initial());
    assertThat(h1).isNotEqualTo(h2);
  }

  @Test
  void hashIsStableAcrossInstancesWithSameSeed() {
    long h1 = new ZobristHasher().hash(GameState.initial());
    long h2 = new ZobristHasher().hash(GameState.initial());
    assertThat(h1).isEqualTo(h2);
  }

  @Test
  void sideToMoveChangesHash() {
    GameState whiteToMove = GameState.initial();
    GameState blackToMove =
        new GameState(Board.initial(), Color.BLACK, 0, List.of(), GameStatus.ONGOING);
    assertThat(hasher.hash(whiteToMove)).isNotEqualTo(hasher.hash(blackToMove));
  }

  @Test
  void sideToMoveBitMatchesHelper() {
    GameState whiteToMove = GameState.initial();
    GameState blackToMove =
        new GameState(Board.initial(), Color.BLACK, 0, List.of(), GameStatus.ONGOING);
    long delta = hasher.hash(whiteToMove) ^ hasher.hash(blackToMove);
    assertThat(delta).isEqualTo(hasher.blackToMoveKey());
  }

  @Test
  void differentPiecePlacementsProduceDifferentHashes() {
    // Build seven distinct positions reachable from the initial state by white's first moves.
    var ruleEngine = new ItalianRuleEngine();
    GameState start = GameState.initial();
    Set<Long> hashes = new HashSet<>();
    hashes.add(hasher.hash(start));
    for (Move m : ruleEngine.legalMoves(start)) {
      GameState next = ruleEngine.applyMove(start, m);
      hashes.add(hasher.hash(next));
    }
    // initial + 7 legal first moves = 8 distinct positions, all should hash uniquely.
    assertThat(hashes).hasSize(1 + ruleEngine.legalMoves(start).size());
  }

  @Test
  void promotingAManChangesHashByExpectedDelta() {
    // Build two positions differing only by piece kind on (0,0): MAN vs KING for the same colour.
    Square s = new Square(0, 0);
    GameState withMan =
        new GameState(
            Board.empty().with(s, new Piece(Color.WHITE, PieceKind.MAN)),
            Color.WHITE,
            0,
            List.of(),
            GameStatus.ONGOING);
    GameState withKing =
        new GameState(
            Board.empty().with(s, new Piece(Color.WHITE, PieceKind.KING)),
            Color.WHITE,
            0,
            List.of(),
            GameStatus.ONGOING);
    long delta = hasher.hash(withMan) ^ hasher.hash(withKing);
    long expected =
        hasher.pieceKey(Color.WHITE, PieceKind.MAN, s)
            ^ hasher.pieceKey(Color.WHITE, PieceKind.KING, s);
    assertThat(delta).isEqualTo(expected);
  }

  @Test
  void emptyBoardWhiteToMoveHashesToZero() {
    // No pieces, white to move (no side bit) ⇒ no XOR contributions ⇒ hash 0.
    GameState empty = new GameState(Board.empty(), Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    assertThat(hasher.hash(empty)).isZero();
  }
}
