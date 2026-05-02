package com.damaitaliana.core.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MatchValueTypesTest {

  @Nested
  class MatchIdValidation {
    @Test
    void randomReturnsUniqueIds() {
      MatchId a = MatchId.random();
      MatchId b = MatchId.random();
      assertThat(a).isNotEqualTo(b);
    }

    @Test
    void ofParsesUuidString() {
      String text = "11111111-2222-3333-4444-555555555555";
      MatchId id = MatchId.of(text);
      assertThat(id.value()).isEqualTo(UUID.fromString(text));
    }

    @Test
    void ofRejectsMalformedString() {
      assertThatThrownBy(() -> MatchId.of("not-a-uuid"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullValue() {
      assertThatNullPointerException().isThrownBy(() -> new MatchId(null));
    }
  }

  @Nested
  class UserRefValidation {
    @Test
    void anonymousLanFactoryUsesSentinelId() {
      UserRef anon = UserRef.anonymousLan("guest");
      assertThat(anon.id()).isEqualTo(UserRef.ANONYMOUS_LAN_ID);
      assertThat(anon.isAnonymous()).isTrue();
      assertThat(anon.username()).isEqualTo("guest");
      assertThat(anon.displayName()).isEqualTo("guest");
    }

    @Test
    void authenticatedFactoryRequiresNonNegativeId() {
      assertThatThrownBy(() -> UserRef.authenticated(-1L, "u", "U"))
          .isInstanceOf(IllegalArgumentException.class);
      UserRef ok = UserRef.authenticated(42L, "alice", "Alice");
      assertThat(ok.isAnonymous()).isFalse();
      assertThat(ok.id()).isEqualTo(42L);
    }

    @Test
    void rejectsBlankUsername() {
      assertThatThrownBy(() -> new UserRef(0L, "  ", "x"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankDisplayName() {
      assertThatThrownBy(() -> new UserRef(0L, "x", ""))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullUsername() {
      assertThatNullPointerException().isThrownBy(() -> new UserRef(0L, null, "x"));
    }

    @Test
    void rejectsNullDisplayName() {
      assertThatNullPointerException().isThrownBy(() -> new UserRef(0L, "x", null));
    }
  }

  @Nested
  class TimeControlValidation {
    @Test
    void presetAsTimeControlYieldsCanonicalValues() {
      TimeControl tc = TimeControlPreset.BLITZ_5_3.asTimeControl();
      assertThat(tc.preset()).isEqualTo(TimeControlPreset.BLITZ_5_3);
      assertThat(tc.initialMillis()).isEqualTo(5 * 60_000L);
      assertThat(tc.incrementMillis()).isEqualTo(3_000L);
    }

    @Test
    void unlimitedFactoryReturnsZeroValues() {
      TimeControl tc = TimeControl.unlimited();
      assertThat(tc.isUnlimited()).isTrue();
      assertThat(tc.initialMillis()).isZero();
      assertThat(tc.incrementMillis()).isZero();
    }

    @Test
    void rejectsNegativeInitialMillis() {
      assertThatThrownBy(() -> new TimeControl(TimeControlPreset.BLITZ_5_3, -1L, 0L))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeIncrementMillis() {
      assertThatThrownBy(() -> new TimeControl(TimeControlPreset.BLITZ_5_3, 0L, -1L))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullPreset() {
      assertThatNullPointerException().isThrownBy(() -> new TimeControl(null, 0L, 0L));
    }

    @Test
    void allPresetsExposeNonNegativeValues() {
      for (TimeControlPreset p : TimeControlPreset.values()) {
        assertThat(p.defaultInitialMillis()).isGreaterThanOrEqualTo(0L);
        assertThat(p.defaultIncrementMillis()).isGreaterThanOrEqualTo(0L);
      }
    }
  }
}
