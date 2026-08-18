package com.aicoach.controller;

import com.aicoach.dto.AiCoachRequest;
import com.aicoach.dto.AiCoachResponse;
import com.aicoach.service.AiCoachService;
import com.aicourse.mcp.service.McpFacadeService;
import com.aicourse.utils.api.ApiResponse;
import com.auth.model.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/coach")
public class AiCoachController {

    @Autowired
    private AiCoachService aiCoachService;

    @Autowired
    private McpFacadeService mcpFacadeService;

    @Value("${mcp.enabled:false}")
    private boolean mcpEnabled;

    @PostMapping("/respond")
    public ResponseEntity<ApiResponse<AiCoachResponse>> respond(@RequestBody AiCoachRequest request,
                                                                Authentication authentication) throws Exception {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return ResponseEntity.status(401).build();
        }

        AiCoachResponse response;
        if (mcpEnabled) {
            response = mcpFacadeService.coachRespond(request, principal);
        } else {
            Long userId = principal.getUser().getId();
            response = aiCoachService.respond(userId, principal.getUser().getRoles(), request);
        }
        return ResponseEntity.ok(ApiResponse.success("Coach response generated", response));
    }

    @PostMapping(value = "/respond/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamRespond(@RequestBody AiCoachRequest request,
                                                    Authentication authentication) throws Exception {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return ResponseEntity.status(401).build();
        }

        Long userId = principal.getUser().getId();
        SseEmitter emitter = aiCoachService.streamRespond(userId, principal.getUser().getRoles(), request);
        return ResponseEntity.ok(emitter);
    }
}
