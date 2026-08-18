package com.challenge.q3;

import com.sharing.model.ShareLinkType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * A share link may carry a maximum-enrollment seat cap. These tests pin down how many learners a
 * capped link is allowed to admit, and what the creator's seat counter reads afterwards.
 */
@DisplayName("Share link seat caps")
class ShareLinkSeatCapTest {

    private SharingWorld world;

    @BeforeEach
    void setUp() {
        world = new SharingWorld();
    }

    @Test
    @DisplayName("a link capped at two seats admits two learners and refuses the third")
    void cappedLinkAdmitsOnlyAsManyLearnersAsSeats() throws Exception {
        List<Long> learners = world.addLearners(3);
        String token = world.generatePublicLink(2).getShareToken();

        world.join(token, learners.get(0));
        world.join(token, learners.get(1));

        assertThatExceptionOfType(Exception.class)
                .as("a link with maxEnrollments=2 must refuse a third distinct learner")
                .isThrownBy(() -> world.join(token, learners.get(2)));

        assertThat(world.seatsUsed(token))
                .as("the seat counter must never exceed the cap")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("a single-seat link admits exactly one learner")
    void singleSeatLinkAdmitsExactlyOneLearner() throws Exception {
        List<Long> learners = world.addLearners(2);
        String token = world.generatePublicLink(1).getShareToken();

        world.join(token, learners.get(0));

        assertThatExceptionOfType(Exception.class)
                .as("a link with maxEnrollments=1 must refuse a second learner")
                .isThrownBy(() -> world.join(token, learners.get(1)));
    }

    @Test
    @DisplayName("a link whose seats are all taken can no longer be resolved")
    void fullLinkNoLongerResolves() throws Exception {
        List<Long> learners = world.addLearners(2);
        String token = world.generatePublicLink(1).getShareToken();

        world.join(token, learners.get(0));

        assertThatExceptionOfType(Exception.class)
                .as("a link that has used up every seat must not resolve for the next visitor")
                .isThrownBy(() -> world.resolve(token, learners.get(1)));
    }

    @Test
    @DisplayName("an uncapped link admits every learner who uses it")
    void uncappedLinkAdmitsEveryLearner() throws Exception {
        List<Long> learners = world.addLearners(5);
        String token = world.generatePublicLink(null).getShareToken();

        for (Long learner : learners) {
            world.join(token, learner);
        }

        assertThat(world.seatsUsed(token))
                .as("an uncapped link must admit all five learners and count all five")
                .isEqualTo(5);
    }

    @Test
    @DisplayName("a freshly generated link reports zero seats used")
    void freshLinkReportsNoSeatsUsed() throws Exception {
        String token = world.generatePublicLink(10).getShareToken();

        assertThat(world.seatsUsed(token))
                .as("nobody has joined yet")
                .isZero();
    }

    @Test
    @DisplayName("seats remaining below the cap keep the link usable")
    void linkBelowCapStaysUsable() throws Exception {
        List<Long> learners = world.addLearners(2);
        String token = world.generatePublicLink(3).getShareToken();

        world.join(token, learners.get(0));

        assertThatCode(() -> world.join(token, learners.get(1)))
                .as("two learners on a three-seat link is still within the cap")
                .doesNotThrowAnyException();
        assertThat(world.seatsUsed(token)).isEqualTo(2);
    }

    @Test
    @DisplayName("an expired link is refused even when seats remain")
    void expiredLinkIsRefused() throws Exception {
        List<Long> learners = world.addLearners(1);
        String token = world.generateLink(ShareLinkType.PUBLIC, OffsetDateTime.now().minusHours(2), 10, List.of())
                .getShareToken();

        assertThatExceptionOfType(Exception.class)
                .as("an expired link must not admit anyone")
                .isThrownBy(() -> world.join(token, learners.get(0)));
    }

    @Test
    @DisplayName("a deactivated link is refused even when seats remain")
    void deactivatedLinkIsRefused() throws Exception {
        List<Long> learners = world.addLearners(1);
        var link = world.generatePublicLink(10);
        world.deactivateLink(link.getId());

        assertThatExceptionOfType(Exception.class)
                .as("a deactivated link must not admit anyone")
                .isThrownBy(() -> world.join(link.getShareToken(), learners.get(0)));
    }
}
