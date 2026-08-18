package com.aicourse.mcp.dto;

import com.fasterxml.jackson.databind.JsonNode;

public class McpToolRequest {

    private String tool;
    private JsonNode input;

    public String getTool() {
        return tool;
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    public JsonNode getInput() {
        return input;
    }

    public void setInput(JsonNode input) {
        this.input = input;
    }
}

