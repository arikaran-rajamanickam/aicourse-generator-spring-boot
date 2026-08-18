package com.project.controller;

import com.aicourse.model.UserPrincipal;
import com.aicourse.model.Users;
import com.project.dto.ProjectResponse;
import com.project.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ProjectControllerGetProjectTest {

    private ProjectController controller;
    private Authentication auth;
    private static final Logger logger = LoggerFactory.getLogger(ProjectControllerGetProjectTest.class);
    @BeforeEach
    void setUp() throws Exception {
        Users user = new Users();
        user.setId(123L);
        user.setUsername("tester");
        user.setPassword("pw");
        UserPrincipal principal = new UserPrincipal(user);
        auth = new UsernamePasswordAuthenticationToken(principal, "pw");

        controller = new ProjectController();

        ProjectService fake = new ProjectService() {
            @Override
            public ProjectResponse createProject(Long userId, com.project.dto.CreateProjectRequest request) throws Exception {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<ProjectResponse> getProjectsByCreator(Long userId) throws Exception {
                throw new UnsupportedOperationException();
            }

            @Override
            public ProjectResponse getProjectById(Long projectId, Long userId) throws Exception {
                if (projectId == 99L) {
                    throw new RuntimeException("not found");
                }
                if (projectId == 2L) {
                    throw new Exception("boom");
                }
                return new ProjectResponse(projectId, "Title-" + projectId, "Desc", userId, OffsetDateTime.now(), List.of());
            }

            @Override
            public ProjectResponse updateProject(Long projectId, Long userId, com.project.dto.CreateProjectRequest request) throws Exception {
                throw new UnsupportedOperationException();
            }

            @Override
            public void deleteProject(Long projectId, Long userId, boolean deleteCourses) throws Exception {
                throw new UnsupportedOperationException();
            }

            @Override
            public void addCourseToProject(Long projectId, Long courseId, Long userId) throws Exception {
                throw new UnsupportedOperationException();
            }

            @Override
            public void removeCourseFromProject(Long projectId, Long courseId, Long userId) throws Exception {
                throw new UnsupportedOperationException();
            }
        };

        Field f = ProjectController.class.getDeclaredField("projectService");
        f.setAccessible(true);
        f.set(controller, fake);
    }

    @Test
    void getProject_success_returnsOkAndBody() throws Exception {
        var response = controller.getProject(5L, auth);
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof ProjectResponse);
        ProjectResponse body = (ProjectResponse) response.getBody();
        assertEquals(5L, body.getId());
        assertEquals(123L, body.getCreatorId());
    }

    @Test
    void getProject_runtimeException_returns404() throws Exception {
        var response = controller.getProject(99L, auth);
        System.out.println("Response status: " + response.getStatusCode() + ", body: " + response.getBody());
        assertEquals(404, response.getStatusCodeValue());
        assertEquals("not found", response.getBody());
    }

    @Test
    void getProject_genericException_returns500() throws Exception {
        var response = controller.getProject(2L, auth);
        System.out.println("Response status: " + response.getStatusCode() + ", body: " + response.getBody());
        assertEquals(500, response.getStatusCodeValue());
        assertEquals("Failed to fetch project.", response.getBody());
    }
}

