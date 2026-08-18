package com.aicourse.mcp.controller;

import com.aicourse.mcp.service.McpAuditService;
import com.auth.enums.UserRole;
import com.auth.model.UserPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/mcp")
public class McpAuditAdminController {

    private final McpAuditService mcpAuditService;

    public McpAuditAdminController(McpAuditService mcpAuditService) {
        this.mcpAuditService = mcpAuditService;
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs(@RequestParam(required = false) String tool,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin access required"));
        }
        return ResponseEntity.ok(mcpAuditService.search(tool, status, from, to, page, size));
    }

    @GetMapping("/audit-logs/filters")
    public ResponseEntity<?> getAuditFilterOptions(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin access required"));
        }
        return ResponseEntity.ok(mcpAuditService.getFilterOptions());
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        return principal.getUser().getRoles() == UserRole.ADMIN;
    }
}

