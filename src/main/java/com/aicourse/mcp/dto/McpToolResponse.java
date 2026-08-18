package com.aicourse.mcp.dto;

import com.fasterxml.jackson.databind.JsonNode;

public class McpToolResponse {

    private boolean success;
    private String tool;
    private JsonNode data;
    private String error;

    public static McpToolResponse success(String tool, JsonNode data) {
        McpToolResponse response = new McpToolResponse();
        response.setSuccess(true);
        response.setTool(tool);
        response.setData(data);
        return response;
    }

    public static McpToolResponse failure(String tool, String error) {
        McpToolResponse response = new McpToolResponse();
        response.setSuccess(false);
        response.setTool(tool);
        response.setError(error);
        return response;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getTool() {
        return tool;
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    public JsonNode getData() {
        return data;
    }

    public void setData(JsonNode data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}

