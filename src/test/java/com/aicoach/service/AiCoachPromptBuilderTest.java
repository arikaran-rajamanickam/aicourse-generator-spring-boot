package com.aicoach.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiCoachPromptBuilderTest {

    @Test
    void buildAddsExactQuizCountAndDifficultyForHardQuizRequests() {
        String prompt = new AiCoachPromptBuilder()
                .courseTitle("Math")
                .lessonTitle("Algebra")
                .lessonContext("Linear equations")
                .userMessage("Give me 5 harder quiz questions")
                .build();

        assertTrue(prompt.contains("Return exactly 5 quiz_card blocks"));
        assertTrue(prompt.contains("Use harder conceptual and application-level questions"));
    }

    @Test
    void buildDoesNotForceQuizCountForNonQuizRequests() {
        String prompt = new AiCoachPromptBuilder()
                .courseTitle("Math")
                .lessonTitle("Algebra")
                .lessonContext("Linear equations")
                .userMessage("Explain this topic simply")
                .build();

        assertFalse(prompt.contains("Return exactly"));
    }
}

