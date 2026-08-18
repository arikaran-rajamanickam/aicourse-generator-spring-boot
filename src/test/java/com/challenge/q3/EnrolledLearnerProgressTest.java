package com.challenge.q3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Once a learner has joined through a share link, their completion percentage is both stored on the
 * enrollment (what the creator's roster shows) and reported on their own progress screen. The two
 * numbers must agree.
 */
@DisplayName("Progress of a learner who joined through a share link")
class EnrolledLearnerProgressTest {

    private static final long LESSON_ONE = 7001L;
    private static final long LESSON_TWO = 7002L;
    private static final long LESSON_THREE = 7003L;
    private static final long LESSON_FOUR = 7004L;

    private SharingWorld world;
    private Long learner;

    @BeforeEach
    void setUp() throws Exception {
        world = new SharingWorld();
        world.setTotalLessons(4);
        learner = world.addLearners(1).get(0);
        world.join(world.generatePublicLink(null).getShareToken(), learner);
    }

    @Test
    @DisplayName("completing one lesson out of four stores 25%")
    void oneOfFourLessonsIsTwentyFivePercent() throws Exception {
        world.completeLesson(LESSON_ONE, learner);

        assertThat(world.persistedProgressPercentage(learner))
                .as("one of four lessons completed")
                .isEqualTo(25.0);
    }

    @Test
    @DisplayName("the stored percentage matches the percentage reported to the learner")
    void storedAndReportedPercentagesAgree() throws Exception {
        world.completeLesson(LESSON_ONE, learner);
        world.completeLesson(LESSON_TWO, learner);

        assertThat(world.reportedCourseProgress(learner))
                .as("the learner's progress screen and their enrollment record must agree")
                .isEqualTo(world.persistedProgressPercentage(learner));
        assertThat(world.persistedProgressPercentage(learner))
                .as("two of four lessons completed")
                .isEqualTo(50.0);
    }

    @Test
    @DisplayName("completing every lesson stores 100%")
    void completingEveryLessonReachesOneHundred() throws Exception {
        world.completeLesson(LESSON_ONE, learner);
        world.completeLesson(LESSON_TWO, learner);
        world.completeLesson(LESSON_THREE, learner);
        world.completeLesson(LESSON_FOUR, learner);

        assertThat(world.persistedProgressPercentage(learner))
                .as("all four lessons completed")
                .isEqualTo(100.0);
    }

    @Test
    @DisplayName("completing the same lesson twice does not double count it")
    void repeatedCompletionOfOneLessonCountsOnce() throws Exception {
        world.completeLesson(LESSON_ONE, learner);
        world.completeLesson(LESSON_ONE, learner);

        assertThat(world.persistedProgressPercentage(learner))
                .as("the same lesson completed twice is still one lesson")
                .isEqualTo(25.0);
    }

    @Test
    @DisplayName("un-completing a lesson lowers the stored percentage")
    void unCompletingALessonLowersThePercentage() throws Exception {
        world.completeLesson(LESSON_ONE, learner);
        world.completeLesson(LESSON_TWO, learner);

        world.uncompleteLesson(LESSON_TWO, learner);

        assertThat(world.persistedProgressPercentage(learner))
                .as("one of four lessons remains completed")
                .isEqualTo(25.0);
    }
}
