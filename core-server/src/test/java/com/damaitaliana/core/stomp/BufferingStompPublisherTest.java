package com.damaitaliana.core.stomp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.damaitaliana.core.stomp.BufferingStompPublisher.Published;
import org.junit.jupiter.api.Test;

/**
 * Behavioural test for the test-scope {@link BufferingStompPublisher}. Verifies (i) FIFO capture,
 * (ii) clear semantics, (iii) snapshot immutability, (iv) null guards on the {@link Published}
 * record. The MatchManager-level integration ("each MatchEvent reaches /topic/match/{id}", A4.10)
 * lives with the MatchManager tests of Task 4.8.
 */
class BufferingStompPublisherTest {

  private final BufferingStompPublisher publisher = new BufferingStompPublisher();

  @Test
  void publishToTopicCollectsPairsInPublishOrder() {
    publisher.publishToTopic("/topic/a", "one");
    publisher.publishToTopic("/topic/b", "two");
    publisher.publishToTopic("/topic/a", "three");

    assertThat(publisher.published())
        .containsExactly(
            new Published("/topic/a", "one"),
            new Published("/topic/b", "two"),
            new Published("/topic/a", "three"));
  }

  @Test
  void publishedReturnsEmptyListForFreshPublisher() {
    assertThat(publisher.published()).isEmpty();
  }

  @Test
  void clearEmptiesTheBuffer() {
    publisher.publishToTopic("/topic/a", "one");
    publisher.publishToTopic("/topic/b", "two");
    publisher.clear();

    assertThat(publisher.published()).isEmpty();
  }

  @Test
  void publishedReturnsImmutableSnapshotIndependentOfFurtherWrites() {
    publisher.publishToTopic("/topic/a", "one");
    var snapshot = publisher.published();
    publisher.publishToTopic("/topic/a", "two");

    assertThat(snapshot).hasSize(1).containsExactly(new Published("/topic/a", "one"));
    assertThat(publisher.published()).hasSize(2);
  }

  @Test
  void publishedRecordRejectsNullTopic() {
    assertThatNullPointerException()
        .isThrownBy(() -> new Published(null, "payload"))
        .withMessageContaining("topic");
  }

  @Test
  void publishedRecordRejectsNullPayload() {
    assertThatNullPointerException()
        .isThrownBy(() -> new Published("/topic/a", null))
        .withMessageContaining("payload");
  }
}
