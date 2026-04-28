package com.damaitaliana.shared.ai.search;

import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Piece;
import com.damaitaliana.shared.domain.PieceKind;
import com.damaitaliana.shared.domain.Square;
import com.damaitaliana.shared.notation.FidNotation;
import java.util.SplittableRandom;

/**
 * Zobrist hashing for {@link GameState} (ADR-026).
 *
 * <p>Builds a 64-bit position hash by XOR-ing one pseudo-random number per piece-on-square plus a
 * side-to-move marker. Two positions that have the same pieces on the same dark squares and the
 * same side to move produce the same hash; different positions produce different hashes with very
 * high probability (collision rate ≈ 2^-64 per pair).
 *
 * <p>The internal random table is initialised <em>deterministically</em> from a fixed seed: same
 * jar version ⇒ same Zobrist values, same hashes. This makes the transposition table and any tests
 * that exercise it bit-for-bit reproducible.
 */
public final class ZobristHasher {

  /** Fixed seed for the internal Zobrist tables — see ADR-026. Do not change without an ADR. */
  public static final long SEED = 0xDA4A172L;

  // pieceTable[colorOrdinal][kindOrdinal][squareIndex 0..31]
  private final long[][][] pieceTable;
  private final long blackToMoveKey;

  /** Builds a hasher with the canonical {@link #SEED}. */
  public ZobristHasher() {
    this(SEED);
  }

  /** Builds a hasher with a custom seed. Useful for tests with synthetic collisions. */
  public ZobristHasher(long seed) {
    SplittableRandom rng = new SplittableRandom(seed);
    pieceTable = new long[2][2][32];
    for (int c = 0; c < 2; c++) {
      for (int k = 0; k < 2; k++) {
        for (int s = 0; s < 32; s++) {
          pieceTable[c][k][s] = rng.nextLong();
        }
      }
    }
    blackToMoveKey = rng.nextLong();
  }

  /** Computes the Zobrist hash of {@code state} from scratch. */
  public long hash(GameState state) {
    long h = 0L;
    var board = state.board();
    var iter = board.occupied().iterator();
    while (iter.hasNext()) {
      Square s = iter.next();
      Piece p = board.at(s).orElseThrow();
      int squareIdx = FidNotation.toFid(s) - 1;
      h ^= pieceTable[p.color().ordinal()][p.kind().ordinal()][squareIdx];
    }
    if (state.sideToMove() == Color.BLACK) {
      h ^= blackToMoveKey;
    }
    return h;
  }

  /** Hash of a single piece on a square (test/diagnostic helper). */
  long pieceKey(Color color, PieceKind kind, Square square) {
    int squareIdx = FidNotation.toFid(square) - 1;
    return pieceTable[color.ordinal()][kind.ordinal()][squareIdx];
  }

  /** Hash of the side-to-move bit (test/diagnostic helper). */
  long blackToMoveKey() {
    return blackToMoveKey;
  }
}
