package com.aicourse.utils.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JsonParserUtil {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger LOGGER = Logger.getLogger(JsonParserUtil.class.getName());

    private JsonParserUtil() {

    }

    /**
     * Converts a JSON-formatted String into a JsonNode (JSONObject equivalent).
     *
     * @param jsonString JSON in String format
     * @return JsonNode (JSONObject)
     * @throws IllegalArgumentException if input is null or invalid JSON
     */
    public static JsonNode parseStringToJsonObject(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON string cannot be null or empty");
        }

        try {
            return OBJECT_MAPPER.readTree(jsonString);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to parse JSON: {0}", new Object[]{e.getMessage()});
            throw new IllegalArgumentException("Invalid JSON string: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts raw JSON from AI response, removing markdown fences and extra text.
     * This method aggressively removes markdown patterns to isolate the JSON array/object.
     *
     * @param aiResponse Raw response from AI
     * @return Cleaned JSON string
     * @throws IllegalArgumentException if response is empty or cannot be cleaned to valid JSON
     */
    public static String extractRawJson(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            throw new IllegalArgumentException("AI response is empty");
        }

        String cleaned = aiResponse.trim();
        LOGGER.log(Level.FINE, "Original response length: {0}", new Object[]{cleaned.length()});

        // Remove markdown code fences (```json, ```, etc.)
        cleaned = cleaned.replaceAll("^```[a-zA-Z]*\\n?", "");
        cleaned = cleaned.replaceAll("\\n?```$", "");

        // Remove any text before the first [ or {
        int jsonStart = Math.min(
                cleaned.indexOf('[') >= 0 ? cleaned.indexOf('[') : Integer.MAX_VALUE,
                cleaned.indexOf('{') >= 0 ? cleaned.indexOf('{') : Integer.MAX_VALUE
        );
        if (jsonStart > 0 && jsonStart != Integer.MAX_VALUE) {
            LOGGER.log(Level.FINE, "Removing preamble text of length: {0}", new Object[]{jsonStart});
            cleaned = cleaned.substring(jsonStart);
        }

        // Remove any text after the last ] or }
        int jsonEnd = Math.max(
                cleaned.lastIndexOf(']'),
                cleaned.lastIndexOf('}')
        );
        if (jsonEnd >= 0 && jsonEnd < cleaned.length() - 1) {
            LOGGER.log(Level.FINE, "Removing trailing text after position: {0}", new Object[]{jsonEnd});
            cleaned = cleaned.substring(0, jsonEnd + 1);
        }

        cleaned = cleaned.trim();
        LOGGER.log(Level.FINE, "Cleaned response length: {0}, starts with: {1}",
                new Object[]{cleaned.length(), cleaned.substring(0, Math.min(50, cleaned.length()))});

        if (!cleaned.startsWith("[") && !cleaned.startsWith("{")) {
            throw new IllegalArgumentException("Extracted JSON does not start with [ or {. Response may not be valid JSON.");
        }

        return cleaned;
    }

    /**
     * Validates that a string is valid JSON
     *
     * @param jsonString String to validate
     * @return true if valid JSON, false otherwise
     */
    public static boolean isValidJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return false;
        }

        try {
            OBJECT_MAPPER.readTree(jsonString);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "JSON validation failed: {0}", new Object[]{e.getMessage()});
            return false;
        }
    }
}
