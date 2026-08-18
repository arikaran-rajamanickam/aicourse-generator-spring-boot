import { apiFetch } from './apiClient';

export interface ProjectCourseSummary {
  id: string;
  title: string;
  description?: string;
  moduleCount?: number;
}

export interface Project {
  id: string;
  name: string;
  description?: string;
  creatorId?: string;
  courses: ProjectCourseSummary[];
  createdAt?: string;
  updatedAt?: string;
}

export interface ProjectCreatePayload {
  name: string;
  description?: string;
}

export interface ProjectMutationResult {
  message: string;
}

function unwrapApiData<T>(payload: any): T {
  if (payload && typeof payload === 'object' && 'data' in payload) {
    return payload.data as T;
  }
  return payload as T;
}

function normalizeProjectCourse(course: any): ProjectCourseSummary {
  const id = course?.id ?? course?.courseId ?? course?.uuid;
  return {
    id: id ? String(id) : '',
    title: course?.title ?? 'Untitled Course',
    description: course?.description,
    moduleCount: typeof course?.moduleCount === 'number' ? course.moduleCount : undefined,
  };
}

function normalizeProject(project: any): Project {
  const id = project?.id ?? project?.projectId ?? project?.uuid;
  const creatorId = project?.creatorId;
  const rawCourses = Array.isArray(project?.courses) ? project.courses : [];

  return {
    id: id ? String(id) : '',
    name: project?.name ?? 'Untitled Project',
    description: project?.description ?? '',
    creatorId: creatorId ? String(creatorId) : undefined,
    createdAt: project?.createdAt,
    updatedAt: project?.updatedAt,
    courses: rawCourses.map(normalizeProjectCourse),
  };
}

/**
 * Create a new project
 */
export async function createProject(payload: ProjectCreatePayload): Promise<Project> {
  const res = await apiFetch('/api/projects', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return normalizeProject(unwrapApiData<any>(res));
}

/**
 * Get all projects for the current user
 */
export async function getMyProjects(): Promise<Project[]> {
  const res = await apiFetch('/api/projects');
  const list = unwrapApiData<any>(res);
  return Array.isArray(list) ? list.map(normalizeProject) : [];
}

/**
 * Get a specific project by ID
 */
export async function getProjectById(projectId: string): Promise<Project> {
  const res = await apiFetch(`/api/projects/${projectId}`);
  return normalizeProject(unwrapApiData<any>(res));
}

/**
 * Update a project
 */
export async function updateProject(projectId: string, payload: Partial<ProjectCreatePayload>): Promise<Project> {
  const res = await apiFetch(`/api/projects/${projectId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
  return normalizeProject(unwrapApiData<any>(res));
}

/**
 * Delete a project
 */
export async function deleteProject(projectId: string, deleteCourses = false): Promise<ProjectMutationResult> {
  const res = await apiFetch(`/api/projects/${projectId}?deleteCourses=${deleteCourses}`, {
    method: 'DELETE',
  });
  const data = unwrapApiData<any>(res);
  return {
    message: typeof data === 'string' ? data : 'Project deleted.',
  };
}

/**
 * Add a course to a project
 */
export async function addCourseToProject(projectId: string, courseId: string): Promise<ProjectMutationResult> {
  const res = await apiFetch(`/api/projects/${projectId}/courses/${courseId}`, {
    method: 'POST',
  });
  const data = unwrapApiData<any>(res);
  return {
    message: typeof data === 'string' ? data : 'Course added to project.',
  };
}

export async function removeCourseFromProject(projectId: string, courseId: string): Promise<ProjectMutationResult> {
  const res = await apiFetch(`/api/projects/${projectId}/courses/${courseId}`, {
    method: 'DELETE',
  });
  const data = unwrapApiData<any>(res);
  return {
    message: typeof data === 'string' ? data : 'Course removed from project.',
  };
}

export interface ProjectPrompt {
  id: string;
  projectId: string;
  text: string;
  createdAt: string;
  updatedAt: string;
  lastUsedAt?: string;
  relatedCourseId?: string;
  relatedCourseTitle?: string;
}

export interface ProjectPromptPayload {
  text: string;
  relatedCourseId?: string;
  relatedCourseTitle?: string;
}

export async function listProjectPrompts(projectId: string): Promise<ProjectPrompt[]> {
  const res = await apiFetch(`/api/projects/${projectId}/prompts`);
  const list = unwrapApiData<any>(res);
  return Array.isArray(list) ? list : [];
}

export async function saveProjectPrompt(projectId: string, payload: ProjectPromptPayload): Promise<ProjectPrompt> {
  const res = await apiFetch(`/api/projects/${projectId}/prompts`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return unwrapApiData<ProjectPrompt>(res);
}

export async function markPromptUsed(projectId: string, promptId: string): Promise<ProjectPrompt> {
  const res = await apiFetch(`/api/projects/${projectId}/prompts/${promptId}/use`, {
    method: 'PUT',
  });
  return unwrapApiData<ProjectPrompt>(res);
}

export async function deleteProjectPrompt(projectId: string, promptId: string): Promise<ProjectMutationResult> {
  const res = await apiFetch(`/api/projects/${projectId}/prompts/${promptId}`, {
    method: 'DELETE',
  });
  const data = unwrapApiData<any>(res);
  return {
    message: typeof data === 'string' ? data : 'Prompt deleted.',
  };
}
