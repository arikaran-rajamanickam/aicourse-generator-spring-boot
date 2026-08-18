package com.aicourse.ai.controller;

import com.aicourse.ai.dto.*;
import com.aicourse.ai.service.LlmAdminService;
import com.auth.enums.UserRole;
import com.auth.model.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/llm")
public class LlmAdminController {

    private final LlmAdminService llmAdminService;

    public LlmAdminController(LlmAdminService llmAdminService) {
        this.llmAdminService = llmAdminService;
    }

    @GetMapping("/providers")
    public ResponseEntity<?> getProviders(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin access required"));
        }
        List<LlmProviderResponse> providers = llmAdminService.getProviders();
        return ResponseEntity.ok(providers);
    }

    @GetMapping("/health")
    public ResponseEntity<?> getProviderHealth(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin access required"));
        }
        List<LlmProviderHealthResponse> health = llmAdminService.getProviderHealthSnapshots();
        return ResponseEntity.ok(health);
    }

    @PostMapping("/providers")
    public ResponseEntity<?> createProvider(@RequestBody LlmProviderUpsertRequest request,
                                            Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin access required"));
        }

        try {
            LlmProviderResponse response = llmAdminService.upsertProvider(null, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("/providers/{code}")
    public ResponseEntity<?> updateProvider(@PathVariable String code,
                                            @RequestBody LlmProviderUpsertRequest request,
                                            Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin access required"));
        }

        try {
            LlmProviderResponse response = llmAdminService.upsertProvider(code, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/routes")
    public ResponseEntity<?> getRoutes(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin access required"));
        }

        List<LlmRouteResponse> routes = llmAdminService.getRoutes();
        return ResponseEntity.ok(routes);
    }

    @PutMapping("/routes")
    public ResponseEntity<?> upsertRoute(@RequestBody LlmRouteRequest request,
                                         Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin access required"));
        }

        try {
            LlmRouteResponse response = llmAdminService.upsertRoute(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
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


