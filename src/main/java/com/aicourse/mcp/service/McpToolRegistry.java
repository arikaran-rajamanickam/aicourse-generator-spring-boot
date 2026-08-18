package com.aicourse.mcp.service;

import com.aicourse.mcp.dto.McpToolDescriptor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class McpToolRegistry {

    private final Map<String, McpToolHandler> handlers;

    public McpToolRegistry(List<McpToolHandler> handlers) {
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(McpToolHandler::toolName, Function.identity()));
    }

    public McpToolHandler getRequired(String tool) {
        McpToolHandler handler = handlers.get(tool);
        if (handler == null) {
            throw new IllegalArgumentException("Unsupported MCP tool: " + tool);
        }
        return handler;
    }

    public List<McpToolDescriptor> listTools() {
        return handlers.values().stream()
                .map(handler -> new McpToolDescriptor(handler.toolName(), handler.description(), handler.inputSchema()))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }
}

