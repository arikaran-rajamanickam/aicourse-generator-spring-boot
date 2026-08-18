package com.aicourse.mcp.transport;

import com.aicourse.mcp.dto.McpToolRequest;
import com.aicourse.mcp.dto.McpToolResponse;
import com.aicourse.mcp.service.McpFacadeService;
import com.aicourse.mcp.service.McpToolRegistry;
import com.auth.model.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mcp")
public class McpController {

    private final McpFacadeService mcpFacadeService;
    private final McpToolRegistry toolRegistry;

    @Value("${mcp.enabled:false}")
    private boolean mcpEnabled;

    public McpController(McpFacadeService mcpFacadeService, McpToolRegistry toolRegistry) {
        this.mcpFacadeService = mcpFacadeService;
        this.toolRegistry = toolRegistry;
    }

    @GetMapping("/tools")
    public ResponseEntity<?> listTools(Authentication authentication) {
        if (!mcpEnabled) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "MCP is disabled"));
        }
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(toolRegistry.listTools());
    }

    @PostMapping("/execute")
    public ResponseEntity<?> execute(@RequestBody McpToolRequest request, Authentication authentication) {
        if (!mcpEnabled) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "MCP is disabled"));
        }
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        McpToolResponse response = mcpFacadeService.execute(request, principal);
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }
}

