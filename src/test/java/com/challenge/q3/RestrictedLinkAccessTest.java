package com.challenge.q3;

import com.sharing.dto.ShareLinkResponse;
import com.sharing.model.ShareLinkType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * A PRIVATE share link carries an explicit allow-list of users. Resolving a link and enrolling
 * with it are two separate endpoints, and they must agree with each other about who is allowed
 * through: a learner the creator never listed must get nowhere with either one.
 */
@DisplayName("Restricted share link access")
class RestrictedLinkAccessTest {

    private SharingWorld world;
    private Long invited;
    private Long stranger;

    @BeforeEach
    void setUp() {
        world = new SharingWorld();
        invited = world.addUser(2001L, "invited-learner").getId();
        stranger = world.addUser(2002L, "stranger").getId();
    }

    @Test
    @DisplayName("a learner on the allow-list can resolve and join a private link")
    void allowListedLearnerGetsThrough() throws Exception {
        String token = world.generatePrivateLink(null, List.of("invited-learner")).getShareToken();

        ShareLinkResponse resolved = world.resolve(token, invited);

        assertThat(resolved.getCourseId())
                .as("the allow-listed learner sees the course behind the link")
                .isEqualTo(SharingWorld.COURSE_ID);
        assertThatCode(() -> world.join(token, invited))
                .as("the allow-listed learner can enroll")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a learner who is not on the allow-list cannot resolve a private link")
    void unlistedLearnerCannotResolvePrivateLink() throws Exception {
        String token = world.generatePrivateLink(null, List.of("invited-learner")).getShareToken();

        assertThatExceptionOfType(Exception.class)
                .as("a private link must not reveal the course to a learner the creator never listed")
                .isThrownBy(() -> world.resolve(token, stranger));
    }

    @Test
    @DisplayName("a learner who is not on the allow-list cannot join through a private link")
    void unlistedLearnerCannotJoinPrivateLink() throws Exception {
        String token = world.generatePrivateLink(null, List.of("invited-learner")).getShareToken();

        assertThatExceptionOfType(Exception.class)
                .as("a private link must not enroll a learner the creator never listed")
                .isThrownBy(() -> world.join(token, stranger));
    }

    @Test
    @DisplayName("an anonymous visitor cannot resolve a private link")
    void anonymousVisitorCannotResolvePrivateLink() throws Exception {
        String token = world.generatePrivateLink(null, List.of("invited-learner")).getShareToken();

        assertThatExceptionOfType(Exception.class)
                .as("a private link requires a signed-in, allow-listed user")
                .isThrownBy(() -> world.resolve(token, null));
    }

    @Test
    @DisplayName("an unlisted learner never becomes enrolled after being refused")
    void refusedLearnerIsNotEnrolled() throws Exception {
        String token = world.generatePrivateLink(null, List.of("invited-learner")).getShareToken();

        try {
            world.join(token, stranger);
        } catch (Exception expected) {
            // refusal is asserted elsewhere; here we only care about the side effects
        }

        assertThat(world.seatsUsed(token))
                .as("a refused learner must not consume a seat")
                .isZero();
        assertThatExceptionOfType(Exception.class)
                .as("a refused learner must not end up with an enrollment record")
                .isThrownBy(() -> world.persistedProgressPercentage(stranger));
    }

    @Test
    @DisplayName("a public link resolves for an anonymous visitor")
    void publicLinkResolvesForAnonymousVisitor() throws Exception {
        String token = world.generatePublicLink(null).getShareToken();

        ShareLinkResponse resolved = world.resolve(token, null);

        assertThat(resolved.getCourseName())
                .as("a public link is browsable without signing in")
                .isEqualTo("Distributed Systems 101");
    }

    @Test
    @DisplayName("a direct-invite link resolves for the signed-in recipient")
    void directInviteLinkResolvesForRecipient() throws Exception {
        String token = world.generateLink(ShareLinkType.DIRECT_INVITE, null, null, List.of()).getShareToken();

        ShareLinkResponse resolved = world.resolve(token, invited);

        assertThat(resolved.getLinkType())
                .as("a direct-invite recipient must be able to open the link they were sent")
                .isEqualTo(ShareLinkType.DIRECT_INVITE);
    }

    @Test
    @DisplayName("a direct-invite link admits the signed-in recipient")
    void directInviteLinkAdmitsRecipient() throws Exception {
        String token = world.generateLink(ShareLinkType.DIRECT_INVITE, null, null, List.of()).getShareToken();

        assertThatCode(() -> world.join(token, invited))
                .as("a direct-invite recipient must be able to enroll through their link")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a private link cannot be created without at least one known user")
    void privateLinkNeedsAKnownUser() {
        assertThatExceptionOfType(Exception.class)
                .as("a PRIVATE link with an empty allow-list is meaningless")
                .isThrownBy(() -> world.generatePrivateLink(null, List.of()));
    }

    @Test
    @DisplayName("an unknown token is refused")
    void unknownTokenIsRefused() {
        assertThatExceptionOfType(Exception.class)
                .as("a token that was never issued must not resolve")
                .isThrownBy(() -> world.resolve("not-a-real-token", invited));
    }

    @Test
    @DisplayName("a deactivated course cannot be joined through a live link")
    void deactivatedCourseCannotBeJoined() throws Exception {
        String token = world.generatePublicLink(null).getShareToken();
        world.deactivateCourse();

        assertThatExceptionOfType(Exception.class)
                .as("a deactivated course is unavailable even through a valid link")
                .isThrownBy(() -> world.join(token, stranger));
    }
}
