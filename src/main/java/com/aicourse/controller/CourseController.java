package com.aicourse.controller;

import com.aicourse.dto.CourseBuilderRequest;
import com.aicourse.model.Course;
import com.aicourse.model.Module;
import com.aicourse.service.courses.impl.CourseServiceImpl;
import com.aicourse.utils.api.ApiResponse;
import com.auth.model.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private static final Logger LOGGER = Logger.getLogger(CourseController.class.getName());

    @Autowired
    private CourseServiceImpl courseServiceImpl;

    @PostMapping("/create")
    public Course createCourse(@RequestBody Map<String, String> payload, Authentication auth) throws Exception {
        LOGGER.log(Level.INFO, "Request received to generate course: {0}", new Object[]{payload.get("title")});
        try {
            Course course = courseServiceImpl.generateCourse(payload, auth);
            LOGGER.log(Level.INFO, "Course generated successfully with ID: {0}", new Object[]{course.getId()});
            return course;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating course: {0}", new Object[]{e.getMessage()});
            throw e;
        }
    }

    @PostMapping("/generate-outline")
    public JsonNode generateOutline(@RequestBody Map<String, String> payload, Authentication auth) throws Exception {
        LOGGER.log(Level.INFO, "Request received to generate course outline ONLY: {0}", new Object[]{payload.get("title")});
        try {
            return courseServiceImpl.generateCourseOutlineOnly(payload, auth);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating course outline: {0}", new Object[]{e.getMessage()});
            throw e;
        }
    }

    @PostMapping("/build")
    public Course saveBuiltCourse(@RequestBody CourseBuilderRequest payload, Authentication auth) throws Exception {
        LOGGER.log(Level.INFO, "Request received to build custom course: {0}", new Object[]{payload.getTitle()});
        try {
            Course course = courseServiceImpl.saveBuiltCourse(payload, auth);
            LOGGER.log(Level.INFO, "Custom course built successfully with ID: {0}", new Object[]{course.getId()});
            return course;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error building custom course: {0}", new Object[]{e.getMessage()});
            throw e;
        }
    }

    @PutMapping("/{courseId}")
    public ResponseEntity<ApiResponse<Course>> updateCourseName(@PathVariable Long courseId,
                                                                @RequestBody Course courseDO) throws Exception {
        LOGGER.log(Level.INFO, "Request received to update course ID: {0}", new Object[]{courseId});
        try {
            courseServiceImpl.updateCourse(courseId, courseDO);
            LOGGER.log(Level.INFO, "Course ID: {0} updated successfully", new Object[]{courseId});
            return ResponseEntity.ok(ApiResponse.success("Course updated successfully", null));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating course ID: {0}: {1}", new Object[]{courseId, e.getMessage()});
            throw e;
        }
    }

    @GetMapping("/my-courses")
    public List<Course> getMyCourses(Authentication auth) throws Exception {
        LOGGER.log(Level.INFO, "Fetching courses for user: {0}", new Object[]{auth.getName()});
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            Long userId = principal.getUser().getId();
            List<Course> courses = courseServiceImpl.getCoursesByCreator(userId);
            LOGGER.log(Level.INFO, "Found {0} courses for user: {1}", new Object[]{courses.size(), auth.getName()});
            return courses;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching courses for user: {0}: {1}",
                    new Object[]{auth.getName(), e.getMessage()});
            throw e;
        }
    }

    @GetMapping("/shared-by-me")
    public List<Course> getCoursesSharedByMe(Authentication auth) throws Exception {
        LOGGER.log(Level.INFO, "Fetching courses shared by user: {0}", new Object[]{auth.getName()});
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            Long userId = principal.getUser().getId();
            List<Course> courses = courseServiceImpl.getCoursesSharedByCreator(userId);
            LOGGER.log(Level.INFO, "Found {0} shared courses for user: {1}",
                    new Object[]{courses.size(), auth.getName()});
            return courses;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching shared courses for user: {0}: {1}",
                    new Object[]{auth.getName(), e.getMessage()});
            throw e;
        }
    }

    @GetMapping("/{id}")
    public Course getCourse(@PathVariable Long id, Authentication auth) throws Exception {
        LOGGER.log(Level.INFO, "Fetching course details for ID: {0}", new Object[]{id});
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            Course course = courseServiceImpl.getCourseById(id, principal.getUser().getId());
            LOGGER.log(Level.INFO, "Course details retrieved for ID: {0}", new Object[]{id});
            return course;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching course ID: {0}: {1}", new Object[]{id, e.getMessage()});
            throw e;
        }
    }

    @GetMapping("/{courseName}/modules")
    public List<Module> getModulesByCourseName(@PathVariable String courseName) throws Exception {
        LOGGER.log(Level.INFO, "Fetching modules for course: {0}", new Object[]{courseName});
        try {
            List<Module> modules = courseServiceImpl.getModulesByCourseName(courseName);
            LOGGER.log(Level.INFO, "Found {0} modules for course: {1}", new Object[]{modules.size(), courseName});
            return modules;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching modules for course: {0}: {1}",
                    new Object[]{courseName, e.getMessage()});
            throw e;
        }
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable Long courseId) throws Exception {
        LOGGER.log(Level.INFO, "Request received to delete course ID: {0}", new Object[]{courseId});
        try {
            courseServiceImpl.deleteCourse(courseId);
            LOGGER.log(Level.INFO, "Course ID: {0} deleted successfully", new Object[]{courseId});
            return ResponseEntity.ok(ApiResponse.success("Course deleted successfully", null));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting course ID: {0}: {1}", new Object[]{courseId, e.getMessage()});
            throw e;
        }
    }

    @PostMapping("/{courseId}/modules")
    public ResponseEntity<Module> addModule(@PathVariable Long courseId, @RequestBody Map<String, String> payload) throws Exception {
        String title = payload.getOrDefault("title", "New Module");
        Module module = courseServiceImpl.addModule(courseId, title);
        return ResponseEntity.ok(module);
    }

    @PutMapping("/{courseId}/modules/{moduleId}")
    public ResponseEntity<ApiResponse<Void>> renameModule(@PathVariable Long courseId, @PathVariable Long moduleId, @RequestBody Map<String, String> payload) throws Exception {
        String title = payload.get("title");
        courseServiceImpl.renameModule(moduleId, title);
        return ResponseEntity.ok(ApiResponse.success("Module renamed", null));
    }

    @DeleteMapping("/{courseId}/modules/{moduleId}")
    public ResponseEntity<ApiResponse<Void>> deleteModule(@PathVariable Long courseId, @PathVariable Long moduleId) throws Exception {
        courseServiceImpl.deleteModule(moduleId);
        return ResponseEntity.ok(ApiResponse.success("Module deleted", null));
    }

    @PutMapping("/{courseId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateCourse(@PathVariable Long courseId) throws Exception {
        LOGGER.log(Level.INFO, "Request received to deactivate course ID: {0}", new Object[]{courseId});
        try {
            courseServiceImpl.deactivateCourse(courseId);
            LOGGER.log(Level.INFO, "Course ID: {0} deactivated successfully", new Object[]{courseId});
            return ResponseEntity.ok(ApiResponse.success("Course deactivated successfully", null));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deactivating course ID: {0}: {1}", new Object[]{courseId, e.getMessage()});
            throw e;
        }
    }

    @PutMapping("/{courseId}/activate")
    public ResponseEntity<ApiResponse<Void>> activateCourse(@PathVariable Long courseId) throws Exception {
        LOGGER.log(Level.INFO, "Request received to activate course ID: {0}", new Object[]{courseId});
        try {
            courseServiceImpl.activateCourse(courseId);
            LOGGER.log(Level.INFO, "Course ID: {0} activated successfully", new Object[]{courseId});
            return ResponseEntity.ok(ApiResponse.success("Course activated successfully", null));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error activating course ID: {0}: {1}", new Object[]{courseId, e.getMessage()});
            throw e;
        }
    }
}