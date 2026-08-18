package com.aicourse.service.courses;

/**
 * Dynamic, type-safe prompt builder for AI lesson content generation.
 * Produces structured JSON that maps 1:1 to the frontend LessonBlock types.
 */
public class LessonPromptBuilder {

    // Required
    private String lessonTitle;
    private String courseTitle;
    private String moduleTitle;

    // Optional with defaults
    private int quizCount = 2;
    private int youtubeCount = 1;
    private boolean includeCodeExamples = true;
    private boolean includeReferences = true;
    private boolean includeTable = true;
    private String difficultyLevel = "beginner"; // beginner | intermediate | advanced
    private String targetAudience = "college students";
    private String language = "English";
    private String courseLevelPath = "core"; // foundation | core | advanced | capstone
    private boolean includeArchitectureDiagram = true;
    private boolean includeRealWorldScenario = true;
    private int minTextBlocks = 4;

    // --- Fluent Setters ---

    public LessonPromptBuilder lessonTitle(String lessonTitle) {
        this.lessonTitle = lessonTitle;
        return this;
    }

    public LessonPromptBuilder courseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
        return this;
    }

    public LessonPromptBuilder moduleTitle(String moduleTitle) {
        this.moduleTitle = moduleTitle;
        return this;
    }

    public LessonPromptBuilder quizCount(int quizCount) {
        this.quizCount = Math.max(0, Math.min(quizCount, 10));
        return this;
    }

    public LessonPromptBuilder youtubeCount(int youtubeCount) {
        this.youtubeCount = Math.max(0, Math.min(youtubeCount, 5));
        return this;
    }

    public LessonPromptBuilder includeCodeExamples(boolean includeCodeExamples) {
        this.includeCodeExamples = includeCodeExamples;
        return this;
    }

    public LessonPromptBuilder includeReferences(boolean includeReferences) {
        this.includeReferences = includeReferences;
        return this;
    }

    public LessonPromptBuilder includeTable(boolean includeTable) {
        this.includeTable = includeTable;
        return this;
    }

    public LessonPromptBuilder difficultyLevel(String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
        return this;
    }

    public LessonPromptBuilder targetAudience(String targetAudience) {
        this.targetAudience = targetAudience;
        return this;
    }

    public LessonPromptBuilder language(String language) {
        this.language = language;
        return this;
    }

    public LessonPromptBuilder courseLevelPath(String courseLevelPath) {
        this.courseLevelPath = courseLevelPath;
        return this;
    }

    public LessonPromptBuilder includeArchitectureDiagram(boolean includeArchitectureDiagram) {
        this.includeArchitectureDiagram = includeArchitectureDiagram;
        return this;
    }

    public LessonPromptBuilder includeRealWorldScenario(boolean includeRealWorldScenario) {
        this.includeRealWorldScenario = includeRealWorldScenario;
        return this;
    }

    public LessonPromptBuilder minTextBlocks(int minTextBlocks) {
        this.minTextBlocks = minTextBlocks;
        return this;
    }

    // --- Build ---

    public String build() {
        if (lessonTitle == null || courseTitle == null || moduleTitle == null) {
            throw new IllegalStateException("lessonTitle, courseTitle, and moduleTitle are required");
        }

        StringBuilder sb = new StringBuilder();

        // ---- ROLE ----
        sb.append("You are an expert educational content creator specializing in structured, interactive digital lessons.\n\n");

        // ---- CONTEXT ----
        sb.append("## CONTEXT\n");
        sb.append("- Lesson Title: \"").append(lessonTitle).append("\"\n");
        sb.append("- Course: \"").append(courseTitle).append("\"\n");
        sb.append("- Module: \"").append(moduleTitle).append("\"\n");
        sb.append("- Difficulty: ").append(difficultyLevel).append("\n");
        sb.append("- Target Audience: ").append(targetAudience).append("\n");
        sb.append("- Language: ").append(language).append("\n\n");

        // ---- TASK ----
        sb.append("## TASK\n");
        sb.append("Generate a comprehensive, mentor-style lesson as a JSON object containing an array of content blocks. Depth and clarity matter more than brevity. Do NOT compress.\n\n");

        // ---- STRUCTURE REQUIREMENTS ----
        sb.append("## DYNAMIC TEACHING FLOW (Crucial!)\n");
        sb.append("To prevent the user from getting bored, DO NOT follow a rigid 'Intro -> Concept -> Code -> Summary' template for every lesson.\n");
        sb.append("Instead, dynamically choose an engaging pedagogical approach suited to the specific topic. Examples of valid dynamic flows:\n");
        sb.append("- **The Mystery/Problem First:** Start by presenting a broken code snippet, a failing system, or a paradox. Then explain the theory, and finally show how the concept solves the mystery.\n");
        sb.append("- **The Storyteller:** Start with a historical anecdote (e.g., 'Why did NASA need this algorithm?'). Teach the concept through the lens of that story.\n");
        sb.append("- **The Socratic Method:** Ask a series of thought-provoking questions, then provide the answers.\n");
        sb.append("- **The Traditional Deep-Dive:** (Use sparingly) Concept -> Architecture -> Code -> Trade-offs.\n\n");
        sb.append("Regardless of the flow you choose, you MUST ALWAYS include:\n");
        sb.append("1. A 'heading' block as the lesson title at the very top.\n");
        sb.append("2. High-quality 'text' blocks explaining the content (extremely simple 10th-grade English).\n");
        if (includeCodeExamples) {
            sb.append("3. 'code' blocks (only if the topic involves code; otherwise omit entirely).\n");
        }
        if (includeArchitectureDiagram) {
            sb.append("4. For system/architecture topics, include an ASCII diagram inside a 'text' language 'code' block.\n");
        }
        if (includeTable) {
            sb.append("5. A 'table' block summarizing key takeaways or trade-offs.\n");
        }
        if (youtubeCount > 0) {
            sb.append("6. 'youtube' blocks (use ONLY real, reputable channels; do NOT invent URLs).\n");
        }
        if (quizCount > 0) {
            sb.append("7. quiz blocks: each question tests a DIFFERENT concept\n");
        }
        if (includeReferences) {
            sb.append("8. reference: 4-6 authoritative links\n");
        }
        sb.append("9. A 'heading' block titled \"Recap & What's Next\" + closing text linking to next lesson's topic\n\n");

        // ---- STRICT JSON SCHEMA ----
        sb.append("## BLOCK TYPE SCHEMAS (follow EXACTLY)\n\n");

        sb.append("### heading\n");
        sb.append("```json\n{ \"type\": \"heading\", \"content\": \"Section Title Here\" }\n```\n\n");

        sb.append("### text\n");
        sb.append("Supports inline markdown: **bold**, *italic*, `code`\n");
        sb.append("```json\n{ \"type\": \"text\", \"content\": \"Paragraph text here with **bold** and `inline code`.\" }\n```\n\n");

        sb.append("### image\n");
        sb.append("content is an object with \"url\" (string), \"alt\" (string), and \"prompt\" (description for AI generation).\n");
        sb.append("```json\n{ \"type\": \"image\", \"content\": { \"url\": \"\", \"alt\": \"Description of image\", \"prompt\": \"A detailed prompt for generating this image...\" } }\n```\n\n");

        sb.append("### list\n");
        sb.append("content is a JSON array of strings. Supports **bold** markdown in items.\n");
        sb.append("```json\n{ \"type\": \"list\", \"content\": [\"**Item 1:** Description\", \"**Item 2:** Description\"] }\n```\n\n");

        if (includeCodeExamples) {
            sb.append("### code\n");
            sb.append("content is an object with \"language\" (string) and \"code\" (string).\n");
            sb.append("```json\n{ \"type\": \"code\", \"content\": { \"language\": \"python\", \"code\": \"print('Hello')\" } }\n```\n\n");
        }

        sb.append("### table\n");
        sb.append("content is an object with \"headers\" (string[]) and \"rows\" (string[][]). Supports **bold** in cells.\n");
        sb.append("```json\n{ \"type\": \"table\", \"content\": { \"headers\": [\"Col1\", \"Col2\"], \"rows\": [[\"val1\", \"val2\"]] } }\n```\n\n");

        if (quizCount > 0) {
            sb.append("### quiz\n");
            sb.append("content is an object. \"options\" must have exactly 4 items. \"correctIndex\" is 0-based. \"explanation\" is required.\n");
            sb.append("```json\n{ \"type\": \"quiz\", \"content\": { \"question\": \"What is X?\", \"options\": [\"A\", \"B\", \"C\", \"D\"], \"correctIndex\": 1, \"explanation\": \"B is correct because...\" } }\n```\n\n");
        }

        if (youtubeCount > 0) {
            sb.append("### youtube\n");
            sb.append("content is an object with \"url\" (must be a real YouTube URL) and \"title\" (string).\n");
            sb.append("```json\n{ \"type\": \"youtube\", \"content\": { \"url\": \"https://www.youtube.com/watch?v=VIDEO_ID\", \"title\": \"Video Title\" } }\n```\n\n");
        }

        if (includeReferences) {
            sb.append("### reference\n");
            sb.append("content is an array of objects with \"title\", \"url\", and optional \"description\".\n");
            sb.append("```json\n{ \"type\": \"reference\", \"content\": [{ \"title\": \"Official Docs\", \"url\": \"https://example.com\", \"description\": \"Comprehensive guide\" }] }\n```\n\n");
        }

        // ---- QUALITY RULES ----
        sb.append("## QUALITY RULES\n");
        sb.append("- Content must be factually accurate and up-to-date\n");
        sb.append("- USE EXTREMELY SIMPLE ENGLISH. Write as if you are explaining to a 10th-grade student.\n");
        sb.append("- Avoid complex jargon. If you must use a technical term, explain it immediately with a simple real-world analogy.\n");
        sb.append("- Keep sentences short and clear.\n");
        sb.append("- Each quiz question must test a different concept from the lesson\n");
        sb.append("- Quiz options must be plausible (no obviously wrong answers)\n");
        sb.append("- Code examples must be syntactically correct and runnable\n");
        sb.append("- YouTube URLs must be real videos from well-known channels (do NOT invent URLs)\n");
        sb.append("- References must link to real, existing websites\n");
        if (difficultyLevel.equals("advanced")) {
            sb.append("- Even for advanced concepts, explain the *why* using simple analogies before diving into the complex *how*.\n");
            sb.append("- Include real-world production considerations and advanced edge cases.\n");
            sb.append("- Add more references to research papers and official documentation\n");
        } else if (difficultyLevel.equals("intermediate")) {
            sb.append("- Balance theory with practical examples\n");
            sb.append("- Include common pitfalls and best practices\n");
        } else {
            sb.append("- Use very simple analogies (e.g., a postal system, a restaurant kitchen).\n");
            sb.append("- Absolutely zero unexplained jargon.\n");
        }
        sb.append("- Minimum ").append(minTextBlocks).append(" substantive text blocks across the lesson — never one-liners.\n");
        sb.append("- If the topic has no code, OMIT code blocks entirely instead of inventing trivial snippets.\n");
        sb.append("- Architecture diagrams in code blocks must use language `text` with clearly labelled boxes/arrows.\n");
        sb.append("- Real-world scenarios MUST name a real product/company pattern (e.g., LinkedIn-Kafka, Discord-Cassandra, Netflix-Cassandra), not generic 'a company'.\n");
        sb.append("\n");

        // ---- OUTPUT FORMAT ----
        sb.append("## OUTPUT FORMAT (CRITICAL!)\n");
        sb.append("YOU MUST respond with ONLY a valid JSON object containing a single key `blocks` which is an array of objects.\n");
        sb.append("Do NOT include:\n");
        sb.append("- Markdown code fences (```json or ```)\n");
        sb.append("- Explanatory text before or after the JSON\n");
        sb.append("- Any text other than the JSON object\n\n");
        sb.append("VALID RESPONSE FORMAT:\n");
        sb.append("{\"blocks\": [{\"type\":\"heading\",\"content\":\"Title\"},{\"type\":\"text\",\"content\":\"Paragraph\"}]}\n\n");
        sb.append("INVALID RESPONSE FORMAT:\n");
        sb.append("```json\n{...}\n```\n");
        sb.append("\"Here is the lesson: {...}\" \n\n");
        sb.append("The response must:\n");
        sb.append("- Start with exactly { (no spaces, no preamble)\n");
        sb.append("- End with exactly } (no spaces, no explanation)\n");
        sb.append("- Contain ONLY valid JSON\n");
        sb.append("- Have every block inside the `blocks` array with exactly two keys: \"type\" and \"content\"\n");
        sb.append("- Be properly formatted JSON\n\n");
        sb.append("Allowed block types: ");

        StringBuilder types = new StringBuilder("\"heading\", \"text\", \"image\", \"list\", \"table\"");
        if (includeCodeExamples) types.append(", \"code\"");
        if (quizCount > 0) types.append(", \"quiz\"");
        if (youtubeCount > 0) types.append(", \"youtube\"");
        if (includeReferences) types.append(", \"reference\"");
        sb.append(types).append("\n");

        return sb.toString();
    }
}

