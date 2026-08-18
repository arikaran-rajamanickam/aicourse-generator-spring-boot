package com.aicourse.service.courses;

public class CourseOutlinePromptBuilder {

    private String title;
    private String difficulty = "BEGINNER";
    private String duration = "Self-paced";
    private String targetAudience = "self-taught learners and college students";
    private String language = "English";

    public CourseOutlinePromptBuilder title(String title) {
        this.title = title;
        return this;
    }

    public CourseOutlinePromptBuilder difficulty(String difficulty) {
        if (difficulty != null && !difficulty.trim().isEmpty()) {
            this.difficulty = difficulty;
        }
        return this;
    }

    public CourseOutlinePromptBuilder duration(String duration) {
        if (duration != null && !duration.trim().isEmpty()) {
            this.duration = duration;
        }
        return this;
    }

    public CourseOutlinePromptBuilder targetAudience(String targetAudience) {
        if (targetAudience != null && !targetAudience.trim().isEmpty()) {
            this.targetAudience = targetAudience;
        }
        return this;
    }

    public CourseOutlinePromptBuilder language(String language) {
        if (language != null && !language.trim().isEmpty()) {
            this.language = language;
        }
        return this;
    }

    public String build() {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalStateException("Course title is required");
        }

        String spectrumRule;
        if ("BEGINNER_TO_ADVANCED".equalsIgnoreCase(difficulty)) {
            spectrumRule = "Course MUST progress strictly from absolute beginner -> intermediate -> advanced -> production/architecture. Tag each module with moduleLevel accordingly. Early modules assume zero prior knowledge; later modules cover internals, scaling, and real production use.";
        } else {
            spectrumRule = String.format("Calibrate depth to the '%s' level consistently across all modules.", difficulty);
        }

        String promptTemplate = """
                You are a senior curriculum architect AND a principal engineer who designs scalable, production-grade systems. You design courses the way a staff engineer would mentor a junior: start from first principles, build intuition with real-world analogies, then progress to architecture, trade-offs, and a hands-on mini-project.
                
                ## GOAL
                Produce a complete, deeply-structured course draft about: "%s".
                Difficulty target: %s
                Duration hint: %s
                Audience: %s
                Language: %s
                
                ## NON-NEGOTIABLE PEDAGOGY RULES
                1. ALWAYS open the course with a "Course Overview & Roadmap" module (the FIRST module) containing lessons:
                   - "What you will learn" (high-level outcomes)
                   - "Topics we will cover" (full topic map, bulleted)
                   - "How this course flows" (foundations -> core -> advanced -> capstone)
                   - "Prerequisites and setup"
                   Do NOT name this module literally "Introduction".
                2. %s
                3. Teach every concept from FIRST PRINCIPLES before introducing jargon. Use real-world analogies (postal system, restaurant kitchen, highway traffic, etc.).
                4. For technical topics with code, include runnable code examples.
                   For systems / infra topics (Kafka, Redis, Kubernetes, RabbitMQ, gRPC, databases, queues, caches, microservices), ALWAYS include:
                     - An architecture-overview lesson with a described/ASCII diagram (components, data flow, failure modes).
                     - A "How it works in production" lesson with a real company-scale scenario (e.g., "How LinkedIn uses Kafka to process trillions of messages/day").
                     - Trade-offs, scaling considerations, and common pitfalls.
                5. The FINAL module MUST be a "Capstone Mini-Project" module — a small but realistic end-to-end project, milestones expressed as lessons.
                6. Depth over speed. Do NOT compress content to save tokens. Thin or one-liner lessons are forbidden.
                7. Order lessons so each one strictly depends only on prior lessons. No forward references.
                
                ## SCOPE & SIZE
                - Modules: 5 to 9 (including Overview module #1 and Capstone module last).
                - Lessons per content module: 4 to 8. If a topic is massive or highly hierarchical (e.g. Arrays -> Sliding Window -> Fixed Size Window), you MAY use an array of `subLessons` inside a lesson to group them logically. Do NOT nest deeper than one level of subLessons.
                
                ## TOPIC-AWARE GUIDANCE
                Detect topic family from title and tailor:
                - Programming language / framework -> syntax, idioms, std-lib tour, project.
                - Distributed system / infra -> architecture, internals, replication/partitioning, failure handling, observability, real deployment scenario, capacity planning.
                - Algorithms / theory -> intuition, derivation, complexity, code, problems.
                - Soft / non-technical -> frameworks, case studies, exercises.
                
                ## OUTPUT JSON SHAPE — return ONLY raw JSON. No markdown fences. No prose.
                {
                  "title": "Course Title",
                  "description": "2-4 sentence course description",
                  "overview": {
                    "whatYouWillLearn": ["..."],
                    "topicsCovered": ["..."],
                    "learningFlow": "Describe the journey foundations -> ... -> capstone",
                    "prerequisites": ["..."],
                    "realWorldUseCases": ["..."]
                  },
                  "modules": [
                    {
                      "title": "Module Title",
                      "description": "Module description",
                      "moduleLevel": "overview | foundation | core | advanced | mastery | capstone",
                      "estimatedMinutes": 45,
                      "learningObjectives": ["..."],
                      "lessons": [
                        {
                          "title": "Lesson Title",
                          "estimatedMinutes": 12,
                          "subLessons": [
                            {
                              "title": "Sub-lesson Title",
                              "estimatedMinutes": 8
                            }
                          ]
                        }
                      ]
                    }
                  ],
                  "capstoneProject": {
                    "title": "...",
                    "problemStatement": "...",
                    "milestones": ["..."],
                    "stretchGoals": ["..."]
                  }
                }
                
                ## QUALITY BAR
                - Factually accurate, current as of 2026.
                - No filler. Merge trivial lessons into richer ones.
                - Tone: clear, mentor-like, encouraging.
                - Read as ONE coherent journey, not a list of facts.
                
                Return ONLY the JSON object. No backticks. No commentary.
                """;

        return String.format(promptTemplate, title, difficulty, duration, targetAudience, language, spectrumRule);
    }
}
