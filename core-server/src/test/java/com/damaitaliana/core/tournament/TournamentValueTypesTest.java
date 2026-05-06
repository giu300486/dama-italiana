package com.damaitaliana.core.tournament;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

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
}
