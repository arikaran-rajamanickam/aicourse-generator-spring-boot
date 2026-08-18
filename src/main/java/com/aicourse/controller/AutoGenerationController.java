package com.aicourse.controller;

import com.aicourse.scheduler.LessonEnrichmentScheduler;
import com.aicourse.utils.api.ApiResponse;
import com.auth.model.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST API for controlling the background lesson auto-generation scheduler.
 * Only accessible to ADMIN users via the LLM Operations UI.
 */
@RestController
@RequestMapping("/api/admin/auto-generation")
public class AutoGenerationController {

    @Autowired
    private LessonEnrichmentScheduler scheduler;

    /**
     * GET /api/admin/auto-generation/status
     * Returns the current state and stats of the auto-generation scheduler.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body(ApiResponse.failure("Admin access required"));
        }

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", scheduler.isEnabled());
        status.put("running", scheduler.isRunning());
        status.put("batchSize", scheduler.getBatchSize());
        status.put("intervalMs", scheduler.getIntervalMs());
        status.put("pendingLessons", scheduler.getPendingCount());
        status.put("totalGenerated", scheduler.getTotalGenerated());
        status.put("totalFailed", scheduler.getTotalFailed());
        status.put("lastRunTimestamp", scheduler.getLastRunTimestamp());
        status.put("lastError", scheduler.getLastError());

        return ResponseEntity.ok(ApiResponse.success("Auto-generation status", status));
    }

    /**
     * POST /api/admin/auto-generation/toggle
     * Body: { "enabled": true/false }
     */
    @PostMapping("/toggle")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggle(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body(ApiResponse.failure("Admin access required"));
        }

        Boolean enabled = (Boolean) body.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("'enabled' field is required"));
        }

        scheduler.setEnabled(enabled);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", scheduler.isEnabled());
        result.put("pendingLessons", scheduler.getPendingCount());
        return ResponseEntity.ok(ApiResponse.success(
                enabled ? "Auto-generation enabled" : "Auto-generation disabled", result));
    }

    /**
     * POST /api/admin/auto-generation/configure
     * Body: { "batchSize": 2, "intervalMs": 60000 }
     */
    @PostMapping("/configure")
    public ResponseEntity<ApiResponse<Map<String, Object>>> configure(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body(ApiResponse.failure("Admin access required"));
        }

        if (body.containsKey("batchSize")) {
            scheduler.setBatchSize(((Number) body.get("batchSize")).intValue());
        }
        if (body.containsKey("intervalMs")) {
            scheduler.setIntervalMs(((Number) body.get("intervalMs")).longValue());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batchSize", scheduler.getBatchSize());
        result.put("intervalMs", scheduler.getIntervalMs());
        return ResponseEntity.ok(ApiResponse.success("Configuration updated", result));
    }

    /**
     * GET /api/admin/auto-generation/logs
     * Returns the recent generation logs with pagination.
     */
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body(ApiResponse.failure("Admin access required"));
        }

        java.util.List<com.aicourse.dto.GenerationLog> allLogs = scheduler.getRecentLogs();
        int totalItems = allLogs.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);

        int start = Math.min(page * size, totalItems);
        int end = Math.min(start + size, totalItems);
        java.util.List<com.aicourse.dto.GenerationLog> pagedLogs = allLogs.subList(start, end);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", pagedLogs);
        data.put("totalItems", totalItems);
        data.put("totalPages", totalPages);
        data.put("page", page);
        data.put("size", size);

        return ResponseEntity.ok(ApiResponse.success("Recent auto-generation logs", data));
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        return principal.getUser().getRoles() == com.auth.enums.UserRole.ADMIN;
    }
}


