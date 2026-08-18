package com.aicoach.service;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AiCoachPromptBuilder {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b(\\d{1,2})\\b");

    private String courseTitle;
    private String lessonTitle;
    private String lessonContext;
    private String userMessage;
    private java.util.List<String> previousQuizQuestions;
    private java.util.List<com.aicoach.dto.AiCoachRequest.ChatMessage> chatHistory;

    public AiCoachPromptBuilder courseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
        return this;
    }

    public AiCoachPromptBuilder lessonTitle(String lessonTitle) {
        this.lessonTitle = lessonTitle;
        return this;
    }

    public AiCoachPromptBuilder lessonContext(String lessonContext) {
        this.lessonContext = lessonContext;
        return this;
    }

    public AiCoachPromptBuilder userMessage(String userMessage) {
        this.userMessage = userMessage;
        return this;
    }

    public AiCoachPromptBuilder previousQuizQuestions(java.util.List<String> previousQuizQuestions) {
        this.previousQuizQuestions = previousQuizQuestions;
        return this;
    }

    public AiCoachPromptBuilder chatHistory(java.util.List<com.aicoach.dto.AiCoachRequest.ChatMessage> chatHistory) {
        this.chatHistory = chatHistory;
        return this;
    }

    public String build() {
        String safeCourse = courseTitle == null ? "General course" : courseTitle;
        String safeLesson = lessonTitle == null ? "N/A" : lessonTitle;
        String safeContext = lessonContext == null ? "" : lessonContext;
        String safeMessage = userMessage == null ? "" : userMessage;
        int requestedQuizCount = extractRequestedQuizCount(safeMessage);
        boolean asksQuiz = isQuizRequest(safeMessage);
        boolean asksHarder = asksForHarderDifficulty(safeMessage);
        String noRepeatConstraint = buildNoRepeatConstraint();

        String quizConstraint = "";
        if (asksQuiz) {
            int exactCount = Math.max(1, requestedQuizCount);
            String difficultyHint = asksHarder
                    ? "Use harder conceptual and application-level questions."
                    : "Use mixed easy-to-medium conceptual questions.";
            quizConstraint = "- User requested quiz questions. Return exactly " + exactCount + " quiz_card blocks. " + difficultyHint + "\\n";
        }

        String historyBlock = "";
        if (chatHistory != null && !chatHistory.isEmpty()) {
            StringBuilder historySb = new StringBuilder();
            historySb.append("## RECENT CONVERSATION HISTORY (Context)\\n");
            for (com.aicoach.dto.AiCoachRequest.ChatMessage msg : chatHistory) {
                if (msg != null && msg.getRole() != null && msg.getText() != null) {
                    historySb.append(msg.getRole().toUpperCase()).append(": ").append(msg.getText().replace('\n', ' ')).append("\\n\\n");
                }
            }
            historyBlock = historySb.toString();
        }

        String sb = "You are an empathetic, highly interactive, and expert AI study coach inside a learning platform.\\n" +
                "Your primary goal is to clarify doubts, keep the user highly engaged, and explain concepts clearly without just repeating lesson materials.\\n" +
                "Be appreciative of the user's effort, use dynamic and encouraging greetings, and completely AVOID repetitive or predictable conversational flows.\\n\\n" +
                "## CONTEXT\\n" +
                "- Course: " + safeCourse + "\\n" +
                "- Lesson: " + safeLesson + "\\n" +
                historyBlock +
                "- User Request: " + safeMessage + "\\n\\n" +
                "## LESSON SNAPSHOT (For reference context only; do NOT dump this directly back to the user)\\n" +
                safeContext + "\\n\\n" +
                "## TASK & DYNAMIC GUIDELINES\\n" +
                "1. EXPLAIN CLEARLY: If the user asks a question or has a doubt, provide a clear, fresh, and engaging explanation. Provide clear explanations for everything the user asks.\\n" +
                "2. NO REPEATING CONTENT: Do not give the user all the quizzes or content that were already generated in the lesson snapshot. Create NEW, relevant quizzes ONLY when requested.\\n" +
                "3. BE INTERACTIVE & APPRECIATIVE: Keep the tone dynamic and rewarding. Encourage the user so they want to keep learning. Avoid repeating the same kind of greetings.\\n" +
                "4. USE STRUCTURED BLOCKS: Return structured JSON blocks. Put all your conversational responses and explanations inside a `text` block within the `blocks` array.\\n" +
                "    Optionally include `quiz_card` or `flashcard` if testing knowledge helps clarify their doubt or if they explicitly ask for it.\\n" +
                "    If the user asks for a plan, include a `study_plan` block.\\n" +
                "    When visuals, examples, or external resources would help, include citations with real public links.\\n\\n" +
                "## OUTPUT FORMAT (MANDATORY)\\n" +
                "Return ONLY a single valid JSON object. NO conversational filler before or after the JSON. NO markdown code fences (```json ... ```). NO extra text. If you want to say something, put it in the 'text' block inside the JSON.\\n\\n" +
                "Schema:\\n" +
                "{\\n" +
                "  \"intent\": \"quiz|study_plan|flashcards|explanation\",\\n" +
                "  \"blocks\": [\\n" +
                "    { \"type\": \"text\", \"content\": { \"title\": \"...\", \"body\": \"...\" } },\\n" +
                "    { \"type\": \"quiz_card\", \"content\": { \"question\": \"...\", \"options\": [\"...\",\"...\",\"...\",\"...\"], \"correctIndex\": 0, \"explanation\": \"...\" } },\\n" +
                "    { \"type\": \"flashcard\", \"content\": { \"front\": \"...\", \"back\": \"...\" } },\\n" +
                "    { \"type\": \"study_plan\", \"content\": { \"goal\": \"...\", \"duration\": \"...\", \"steps\": [{ \"title\": \"...\", \"task\": \"...\", \"time\": \"...\" }] } }\\n" +
                "  ],\\n" +
                "  \"citations\": [{ \"title\": \"...\", \"url\": \"https://...\", \"description\": \"...\", \"source\": \"...\" }],\\n" +
                "  \"suggestions\": [\"...\", \"...\", \"...\"]\\n" +
                "}\\n\\n" +
                "Rules:\\n" +
                "- ABSOLUTELY NO markdown code fences.\\n" +
                "- quiz_card must always have exactly 4 options.\\n" +
                quizConstraint +
                noRepeatConstraint +
                "- Keep text concise, highly qualitative, and conversational.\\n" +
                "- suggestions should be quick follow-up prompts to continue the conversation. NEVER put URLs here.\\n" +
                "- citations URLs must be https, and belong STRICTLY in the citations array.\\n";

        return sb;
    }

    private boolean isQuizRequest(String message) {
        String lower = message == null ? "" : message.toLowerCase(Locale.ROOT);
        return lower.contains("quiz")
                || lower.contains("test me")
                || lower.contains("mcq")
                || lower.contains("multiple choice")
                || lower.contains("practice questions")
                || lower.contains("mock test");
    }

    private boolean asksForHarderDifficulty(String message) {
        String lower = message == null ? "" : message.toLowerCase(Locale.ROOT);
        return lower.contains("hard") || lower.contains("harder") || lower.contains("advanced");
    }

    private int extractRequestedQuizCount(String message) {
        if (!isQuizRequest(message)) {
            return 0;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(message == null ? "" : message);
        if (matcher.find()) {
            int parsed = Integer.parseInt(matcher.group(1));
            return Math.max(1, Math.min(parsed, 15));
        }

        String lower = message == null ? "" : message.toLowerCase(Locale.ROOT);
        Map<String, Integer> words = Map.ofEntries(
                Map.entry("one", 1),
                Map.entry("two", 2),
                Map.entry("three", 3),
                Map.entry("four", 4),
                Map.entry("five", 5),
                Map.entry("six", 6),
                Map.entry("seven", 7),
                Map.entry("eight", 8),
                Map.entry("nine", 9),
                Map.entry("ten", 10)
        );
        for (Map.Entry<String, Integer> entry : words.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return 1;
    }

    private String buildNoRepeatConstraint() {
        if (previousQuizQuestions == null || previousQuizQuestions.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("- STRICT RULE: NEVER repeat any of the following prior quiz questions or topics from this session:\\n");

        int count = 0;
        for (String q : previousQuizQuestions) {
            if (q == null || q.isBlank()) {
                continue;
            }
            count++;
            if (count > 8) {
                break;
            }
            sb.append("  - ").append(q.replace('\n', ' ').trim()).append("\\n");
        }

        if (count == 0) {
            return "";
        }
        return sb.toString();
    }
}

