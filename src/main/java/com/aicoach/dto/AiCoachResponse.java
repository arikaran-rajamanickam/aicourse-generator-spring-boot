package com.aicoach.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class AiCoachResponse {

    private String intent;
    private List<CoachBlock> blocks = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();
    private List<Citation> citations = new ArrayList<>();

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public List<CoachBlock> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<CoachBlock> blocks) {
        this.blocks = blocks;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public List<Citation> getCitations() {
        return citations;
    }

    public void setCitations(List<Citation> citations) {
        this.citations = citations;
    }

    public static class CoachBlock {
        private String type;
        private JsonNode content;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public JsonNode getContent() {
            return content;
        }

        public void setContent(JsonNode content) {
            this.content = content;
        }
    }

    public static class Citation {
        private String title;
        private String url;
        private String description;
        private String source;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }
}

