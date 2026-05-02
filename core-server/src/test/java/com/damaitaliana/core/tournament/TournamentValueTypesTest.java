package com.damaitaliana.core.tournament;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TournamentValueTypesTest {

  @Nested
  class TournamentIdValidation {
    @Test
    void randomReturnsUniqueIds() {
      assertThat(TournamentId.random()).isNotEqualTo(TournamentId.random());
    }

    @Test
    void ofParsesUuidString() {
      String text = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
      TournamentId id = TournamentId.of(text);
      assertThat(id.value()).isEqualTo(UUID.fromString(text));
    }

    @Test
    void rejectsNullValue() {
      assertThatNullPointerException().isThrownBy(() -> new TournamentId(null));
    }
  }

  @Nested
  class TournamentMatchRefValidation {
    @Test
    void rejectsNullTournamentId() {
      assertThatNullPointerException().isThrownBy(() -> new TournamentMatchRef(null, 0, 0));
    }

    @Test
    void rejectsNegativeRoundNo() {
      assertThatThrownBy(() -> new TournamentMatchRef(TournamentId.random(), -1, 0))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeMatchIndex() {
      assertThatThrownBy(() -> new TournamentMatchRef(TournamentId.random(), 0, -1))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsZeroValues() {
      TournamentId tid = TournamentId.random();
      TournamentMatchRef ref = new TournamentMatchRef(tid, 0, 0);
      assertThat(ref.roundNo()).isZero();
      assertThat(ref.matchIndex()).isZero();
      assertThat(ref.tournamentId()).isEqualTo(tid);
    }
  }
}
