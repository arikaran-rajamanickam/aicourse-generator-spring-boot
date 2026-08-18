package com.aicourse.controller;

import com.aicourse.model.PrerequisiteExplanation;
import com.aicourse.service.courses.PrerequisiteExplanationService;
import com.aicourse.utils.api.ApiResponse;
import com.auth.model.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/courses/{courseId}/prerequisites")
public class PrerequisiteExplanationController {

    private static final Logger LOGGER = Logger.getLogger(PrerequisiteExplanationController.class.getName());

    @Autowired
    private PrerequisiteExplanationService service;

    /**
     * GET /api/courses/{courseId}/prerequisites/explanation?prerequisite=...&depth=standard
     * Look up a cached prerequisite explanation.
     */
    @GetMapping("/explanation")
    public ResponseEntity<ApiResponse<JsonNode>> getCached(
            @PathVariable Long courseId,
            @RequestParam String prerequisite,
            @RequestParam(defaultValue = "standard") String depth,
            Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            return ResponseEntity.status(401).build();
        }

        Optional<PrerequisiteExplanation> cached = service.findCached(courseId, prerequisite, depth);
        if (cached.isPresent()) {
            LOGGER.log(Level.FINE, "Cache HIT for prerequisite: {0}", prerequisite);
            return ResponseEntity.ok(ApiResponse.success("Cached explanation found", cached.get().getResponseData()));
        }

        LOGGER.log(Level.FINE, "Cache MISS for prerequisite: {0}", prerequisite);
        return ResponseEntity.ok(ApiResponse.success("No cached explanation", null));
    }

    /**
     * POST /api/courses/{courseId}/prerequisites/explanation
     * Store a prerequisite explanation after AI generates it.
     * Body: { "prerequisite": "...", "depth": "standard", "responseData": { ... } }
     */
    @PostMapping("/explanation")
    public ResponseEntity<ApiResponse<PrerequisiteExplanation>> saveExplanation(
            @PathVariable Long courseId,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            return ResponseEntity.status(401).build();
        }

        String prerequisite = (String) body.get("prerequisite");
        String depth = (String) body.getOrDefault("depth", "standard");
        Object responseDataRaw = body.get("responseData");

        if (prerequisite == null || prerequisite.isBlank() || responseDataRaw == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("prerequisite and responseData are required"));
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            JsonNode responseData = mapper.valueToTree(responseDataRaw);

            PrerequisiteExplanation saved = service.save(courseId, prerequisite, depth, responseData);
            LOGGER.log(Level.INFO, "Saved prerequisite explanation for courseId={0}, prerequisite={1}",
                    new Object[]{courseId, prerequisite});
            return ResponseEntity.ok(ApiResponse.success("Explanation saved", saved));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save prerequisite explanation: {0}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.failure("Failed to save: " + e.getMessage()));
        }
    }
}

