package com.sharing.controller;

import com.aicourse.utils.api.ApiResponse;
import com.auth.model.UserPrincipal;
import com.sharing.dto.EnrollmentResponse;
import com.sharing.dto.ShareLinkResponse;
import com.sharing.service.CourseShareService;
import com.sharing.service.LessonProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/join")
public class CourseJoinController {

    private static final Logger LOGGER = Logger.getLogger(CourseJoinController.class.getName());

    @Autowired
    private CourseShareService courseShareService;

    @Autowired
    private LessonProgressService lessonProgressService;

    /**
     * Resolve share token and get course details
     */
    @GetMapping("/{token}")
    public ResponseEntity<ApiResponse<ShareLinkResponse>> resolveShareLink(@PathVariable String token,
                                                                           Authentication auth) {
        LOGGER.log(Level.INFO, "Request received to resolve share token: {0}", token);
        try {
            Long userId = (auth != null && auth.isAuthenticated()) ? ((UserPrincipal) auth.getPrincipal()).getUser().getId() : null;

            ShareLinkResponse response = courseShareService.getShareLinkByToken(token, userId);

            // If it's a PRIVATE link and user is not logged in, the service would have thrown an exception
            // because userId is null. If it's PUBLIC, it returns the response.

            LOGGER.log(Level.INFO, "Share token resolved successfully");
            return ResponseEntity.ok(ApiResponse.success("Share token resolved successfully", response));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error resolving share token: {0}", e.getMessage());
            if (e.getMessage().contains("Login required") || e.getMessage().contains("not allowed")) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.failure(e.getMessage()));
            }
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Invalid or expired share link: " + e.getMessage()));
        }
    }

    /**
     * Enroll user in a course using share token
     */
    @PostMapping("/{token}/enroll")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> enrollUsingShareLink(
            @PathVariable String token,
            Authentication auth) {

        LOGGER.log(Level.INFO, "Request received to enroll user via share token");
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

            // Resolve the share link
            ShareLinkResponse shareLink = courseShareService.getShareLinkByToken(token, principal.getUser().getId());

            // Enroll user
            EnrollmentResponse response = lessonProgressService.enrollUserInCourse(
                    shareLink.getCourseId(),
                    principal.getUser().getId(),
                    shareLink.getId()
            );

            LOGGER.log(Level.INFO, "User enrolled successfully");
            return ResponseEntity.ok(ApiResponse.success("User enrolled successfully", response));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error enrolling user: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error enrolling user: " + e.getMessage()));
        }
    }
}



