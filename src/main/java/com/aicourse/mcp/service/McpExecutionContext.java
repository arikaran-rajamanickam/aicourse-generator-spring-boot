package com.aicourse.mcp.service;

import com.auth.enums.UserRole;

public record McpExecutionContext(Long userId, UserRole role) {
}

