package com.project.service.impl;

import com.aicourse.model.Course;
import com.aicourse.repo.CourseRepo;
import com.aicourse.utils.id.SnowflakeIdGenerator;
import com.auth.model.UserPrincipal;
import com.auth.model.Users;
import com.features.Feature;
import com.features.FeatureGuard;
import com.leaderboard.model.impl.UserStatsService;
import com.project.dto.CreateProjectRequest;
import com.project.dto.ProjectPromptRequest;
import com.project.dto.ProjectPromptResponse;
import com.project.dto.ProjectResponse;
import com.project.model.Project;
import com.project.model.ProjectPrompt;
import com.project.repo.ProjectPromptRepo;
import com.project.repo.ProjectRepo;
import com.project.service.ProjectService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class ProjectServiceImpl implements ProjectService {

    private static final Logger LOGGER = Logger.getLogger(ProjectServiceImpl.class.getName());

    @Autowired
    private ProjectRepo projectRepo;

    @Autowired
    private CourseRepo courseRepo;

    @Autowired
    private ProjectPromptRepo projectPromptRepo;

    @Autowired
    private FeatureGuard featureGuard;

    @Autowired
    private UserStatsService userStatsService;

    @Override
    @Transactional
    public ProjectResponse createProject(Long userId, CreateProjectRequest request, Authentication auth) {
        LOGGER.log(Level.INFO, "Creating project ''{0}'' for userId={1}",
                new Object[]{request.getName(), userId});

        validateName(request.getName());

        if (projectRepo.isProjectAlreadyExist(request.getName().trim(), userId) instanceof Long) {
            throw new IllegalArgumentException(
                    "A project named '" + request.getName().trim() + "' already exists.");
        }
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        Users user = userPrincipal.getUser();
        int lifetimeCount = userStatsService.getTotalProjectsCreated(userId);
        featureGuard.requireWithinLimit(Feature.PROJECT_CREATE, user.getRoles(), lifetimeCount);

        Project project = new Project();
        project.setId(SnowflakeIdGenerator.generateId());
        project.setName(request.getName().trim());
        project.setDescription(request.getDescription());
        project.setCreatorId(userId);

        Project saved = projectRepo.save(project);
        userStatsService.incrementTotalProjectsCreated(userId);
        LOGGER.log(Level.INFO, "Project created with ID: {0}", saved.getId());
        return toResponse(saved);
    }

    @Override
    public List<ProjectResponse> getProjectsByCreator(Long userId) {
        LOGGER.log(Level.FINE, "Fetching all projects for userId={0}", userId);
        List<Project> projects = projectRepo.findByCreatorId(userId);
        if (projects.isEmpty()) {
            return new ArrayList<>();
        }
        return projects
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProjectResponse getProjectById(Long projectId, Long userId) {
        LOGGER.log(Level.FINE, "Fetching project ID: {0}", projectId);
        Project project = findAndVerifyOwner(projectId, userId);
        return toResponse(project);
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(Long projectId, Long userId, CreateProjectRequest request) {
        LOGGER.log(Level.INFO, "Updating project ID: {0}", projectId);

        validateName(request.getName());

        Project project = findAndVerifyOwner(projectId, userId);
        String newName = request.getName().trim();

        if (!project.getName().equals(newName)
                && projectRepo.isProjectAlreadyExist(newName, userId) instanceof Long) {
            throw new IllegalArgumentException(
                    "A project named '" + newName + "' already exists.");
        }

        project.setName(newName);
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }

        Project saved = projectRepo.save(project);
        LOGGER.log(Level.INFO, "Project ID: {0} updated", projectId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteProject(Long projectId, Long userId, boolean deleteCourses) {
        LOGGER.log(Level.INFO, "Deleting project ID: {0}, deleteCourses={1}",
                new Object[]{projectId, deleteCourses});

        Project project = findAndVerifyOwner(projectId, userId);
        List<Course> courses = courseRepo.findByProjectId(projectId);

        if (deleteCourses) {
            courseRepo.deleteAll(courses);
            LOGGER.log(Level.INFO, "Deleted {0} courses from project ID: {1}",
                    new Object[]{courses.size(), projectId});
        } else {
            courses.forEach(c -> c.setProjectId(null));
            courseRepo.saveAll(courses);
            LOGGER.log(Level.INFO, "Unlinked {0} courses from project ID: {1}",
                    new Object[]{courses.size(), projectId});
        }

        projectRepo.delete(project);
        LOGGER.log(Level.INFO, "Project ID: {0} deleted", projectId);
    }

    @Override
    @Transactional
    public void addCourseToProject(Long projectId, Long courseId, Long userId) {
        LOGGER.log(Level.INFO, "Adding course {0} to project {1}", new Object[]{courseId, projectId});

        findAndVerifyOwner(projectId, userId);

        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

        // Only the course owner can assign it to a project
        if (!userId.equals(course.getCreator())) {
            throw new SecurityException("You do not own this course.");
        }

        course.setProjectId(projectId);
        courseRepo.save(course);
        LOGGER.log(Level.INFO, "Course {0} assigned to project {1}",
                new Object[]{courseId, projectId});
    }

    @Override
    @Transactional
    public void removeCourseFromProject(Long projectId, Long courseId, Long userId) {
        LOGGER.log(Level.INFO, "Removing course {0} from project {1}",
                new Object[]{courseId, projectId});

        findAndVerifyOwner(projectId, userId);

        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

        if (!projectId.equals(course.getProjectId())) {
            throw new IllegalArgumentException(
                    "Course " + courseId + " does not belong to project " + projectId);
        }

        course.setProjectId(null);
        courseRepo.save(course);
        LOGGER.log(Level.INFO, "Course {0} removed from project {1}",
                new Object[]{courseId, projectId});
    }

    @Override
    public List<ProjectPromptResponse> getProjectPrompts(Long projectId, Long userId) {
        findAndVerifyOwner(projectId, userId);
        List<ProjectPrompt> prompts = projectPromptRepo.findByProjectIdOrderByUpdatedAtDesc(projectId);
        return prompts.stream().map(this::toPromptResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProjectPromptResponse saveProjectPrompt(Long projectId, Long userId, ProjectPromptRequest request) {
        findAndVerifyOwner(projectId, userId);

        if (request.getText() == null || request.getText().trim().isBlank()) {
            throw new IllegalArgumentException("Prompt cannot be empty");
        }

        Optional<ProjectPrompt> existing = projectPromptRepo.findByProjectIdAndTextIgnoreCase(projectId, request.getText().trim());

        ProjectPrompt prompt;
        if (existing.isPresent()) {
            prompt = existing.get();
            prompt.setText(request.getText().trim());
            if (request.getRelatedCourseId() != null) prompt.setRelatedCourseId(request.getRelatedCourseId());
            if (request.getRelatedCourseTitle() != null) prompt.setRelatedCourseTitle(request.getRelatedCourseTitle());
        } else {
            prompt = new ProjectPrompt();
            prompt.setProjectId(projectId);
            prompt.setText(request.getText().trim());
            prompt.setRelatedCourseId(request.getRelatedCourseId());
            prompt.setRelatedCourseTitle(request.getRelatedCourseTitle());
        }

        ProjectPrompt saved = projectPromptRepo.save(prompt);
        return toPromptResponse(saved);
    }

    @Override
    @Transactional
    public void deleteProjectPrompt(Long projectId, String promptId, Long userId) {
        findAndVerifyOwner(projectId, userId);

        ProjectPrompt prompt = projectPromptRepo.findById(promptId)
                .orElseThrow(() -> new RuntimeException("Prompt not found"));

        if (!prompt.getProjectId().equals(projectId)) {
            throw new SecurityException("Prompt does not belong to this project");
        }

        projectPromptRepo.delete(prompt);
    }

    @Override
    @Transactional
    public ProjectPromptResponse markPromptAsUsed(Long projectId, String promptId, Long userId) {
        findAndVerifyOwner(projectId, userId);

        ProjectPrompt prompt = projectPromptRepo.findById(promptId)
                .orElseThrow(() -> new RuntimeException("Prompt not found"));

        if (!prompt.getProjectId().equals(projectId)) {
            throw new SecurityException("Prompt does not belong to this project");
        }

        prompt.setLastUsedAt(java.time.OffsetDateTime.now());
        ProjectPrompt saved = projectPromptRepo.save(prompt);
        return toPromptResponse(saved);
    }

    private ProjectPromptResponse toPromptResponse(ProjectPrompt prompt) {
        return new ProjectPromptResponse(
                prompt.getId(),
                prompt.getProjectId(),
                prompt.getText(),
                prompt.getRelatedCourseId(),
                prompt.getRelatedCourseTitle(),
                prompt.getCreatedAt(),
                prompt.getUpdatedAt(),
                prompt.getLastUsedAt()
        );
    }

    private Project findAndVerifyOwner(Long projectId, Long userId) {
        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> {
                    LOGGER.log(Level.SEVERE, "Project not found: {0}", projectId);
                    return new RuntimeException("Project not found: " + projectId);
                });

        if (!project.getCreatorId().equals(userId)) {
            LOGGER.log(Level.WARNING, "Unauthorized: userId={0} tried to access project {1}",
                    new Object[]{userId, projectId});
            throw new SecurityException("You do not have access to this project.");
        }
        return project;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Project name cannot be blank.");
        }
    }

    private ProjectResponse toResponse(Project project) {
        List<Course> courses = courseRepo.findByProjectId(project.getId());

        List<ProjectResponse.CourseSummary> summaries = courses.stream()
                .map(c -> new ProjectResponse.CourseSummary(
                        c.getId(),
                        c.getTitle(),
                        c.getDescription(),
                        c.getModules() != null ? c.getModules().size() : 0
                ))
                .collect(Collectors.toList());

        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatorId(),
                project.getCreatedAt(),
                summaries
        );
    }
}