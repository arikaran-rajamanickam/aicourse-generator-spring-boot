package com.aicourse.mcp.dto;

public class McpToolDescriptor {

    private String name;
    private String description;
    private com.fasterxml.jackson.databind.JsonNode inputSchema;

    public McpToolDescriptor() {
    }

    public McpToolDescriptor(String name, String description, com.fasterxml.jackson.databind.JsonNode inputSchema) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public com.fasterxml.jackson.databind.JsonNode getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(com.fasterxml.jackson.databind.JsonNode inputSchema) {
        this.inputSchema = inputSchema;
    }
}

