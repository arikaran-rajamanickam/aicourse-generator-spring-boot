package com.challenge.q3;

import com.sharing.dto.EnrollmentResponse;
import com.sharing.model.EnrollmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Joining with a share link has to be idempotent: a learner who opens the same link twice, or who
 * comes back to the course later, is the same enrollment and the same single seat.
 */
@DisplayName("Repeated joins through the same share link")
class RepeatEnrollmentTest {

    private SharingWorld world;

    @BeforeEach
    void setUp() {
        world = new SharingWorld();
    }

    @Test
    @DisplayName("the same learner joining three times still occupies one seat")
    void repeatedJoinsByOneLearnerOccupyOneSeat() throws Exception {
        Long learner = world.addLearners(1).get(0);
        String token = world.generatePublicLink(5).getShareToken();

        world.join(token, learner);
        world.join(token, learner);
        world.join(token, learner);

        assertThat(world.seatsUsed(token))
                .as("one learner joining three times must consume exactly one seat")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("the seat counter equals the number of distinct learners")
    void seatCounterEqualsDistinctLearnerCount() throws Exception {
        List<Long> learners = world.addLearners(3);
        String token = world.generatePublicLink(null).getShareToken();

        world.join(token, learners.get(0));
        world.join(token, learners.get(1));
        world.join(token, learners.get(0));
        world.join(token, learners.get(2));

        assertThat(world.seatsUsed(token))
                .as("three distinct learners joined, one of them twice")
                .isEqualTo(3);
    }

    @Test
    @DisplayName("one learner reloading the join page does not lock other learners out")
    void repeatedJoinsDoNotExhaustACappedLink() throws Exception {
        List<Long> learners = world.addLearners(2);
        String token = world.generatePublicLink(2).getShareToken();

        world.join(token, learners.get(0));
        world.join(token, learners.get(0));
        world.join(token, learners.get(0));

        assertThatCode(() -> world.join(token, learners.get(1)))
                .as("the second of two seats must still be free after the first learner re-joined")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("re-joining returns the learner's existing enrollment")
    void reJoiningReturnsTheExistingEnrollment() throws Exception {
        Long learner = world.addLearners(1).get(0);
        String token = world.generatePublicLink(5).getShareToken();

        EnrollmentResponse first = world.join(token, learner);
        EnrollmentResponse second = world.join(token, learner);

        assertThat(second.getId())
                .as("re-joining must not create a second enrollment for the same learner")
                .isEqualTo(first.getId());
        assertThat(second.getStatus())
                .as("the enrollment stays active")
                .isEqualTo(EnrollmentStatus.ACTIVE);
    }

    @Test
    @DisplayName("joining marks the enrollment active and accepted")
    void joiningActivatesTheEnrollment() throws Exception {
        Long learner = world.addLearners(1).get(0);
        String token = world.generatePublicLink(5).getShareToken();

        EnrollmentResponse enrollment = world.join(token, learner);

        assertThat(enrollment.getStatus())
                .as("enrollment state after a successful join")
                .isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(enrollment.getInviteStatus())
                .as("invite workflow state after a successful join")
                .isEqualTo("ACCEPTED");
    }

    @Test
    @DisplayName("a learner who dropped the course is reactivated by joining again")
    void droppedLearnerIsReactivatedOnRejoin() throws Exception {
        Long learner = world.addLearners(1).get(0);
        String token = world.generatePublicLink(5).getShareToken();
        world.join(token, learner);
        world.dropOut(learner);

        EnrollmentResponse rejoined = world.join(token, learner);

        assertThat(rejoined.getStatus())
                .as("joining again must revive the dropped enrollment")
                .isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(rejoined.getInviteStatus())
                .as("invite workflow state after being revived")
                .isEqualTo("ACCEPTED");
    }

    @Test
    @DisplayName("a newly joined learner starts at zero progress")
    void newLearnerStartsAtZeroProgress() throws Exception {
        Long learner = world.addLearners(1).get(0);
        String token = world.generatePublicLink(5).getShareToken();

        world.join(token, learner);

        assertThat(world.persistedProgressPercentage(learner))
                .as("a learner who just joined has completed nothing")
                .isEqualTo(0.0);
    }
}
