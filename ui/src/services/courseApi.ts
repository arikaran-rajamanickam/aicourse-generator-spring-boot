import { apiFetch } from './apiClient';
import { executeMcpTool } from './mcpApi';
import { USE_MCP_CLIENT } from '../constants';
import { Course, CourseCreatePayload } from '../types/course';

function unwrapApiData<T>(payload: any): T {
  if (payload && typeof payload === 'object' && 'data' in payload) {
    return payload.data as T;
  }
  return payload as T;
}

function normalizeLesson(lesson: any) {
  const id = lesson?.id ?? lesson?.lessonId ?? lesson?.uuid;
  return {
    ...lesson,
    id: id ? String(id) : '',
    title: lesson?.title ?? lesson?.name ?? 'Untitled Lesson',
  };
}

function normalizeModule(module: any) {
  const id = module?.id ?? module?.moduleId ?? module?.uuid;
  const rawLessons =
    (Array.isArray(module?.lessons) && module.lessons) ||
    (Array.isArray(module?.lessonDtos) && module.lessonDtos) ||
    (Array.isArray(module?.lessonList) && module.lessonList) ||
    [];

  return {
    ...module,
    id: id ? String(id) : '',
    title: module?.title ?? module?.name ?? 'Untitled Module',
    number: module?.number ?? module?.moduleNumber,
    lessons: rawLessons.map(normalizeLesson),
  };
}

function normalizeCourse(course: any): Course {
  const id = course?.id ?? course?.courseId ?? course?.uuid;
  const rawModules =
    (Array.isArray(course?.modules) && course.modules) ||
    (Array.isArray(course?.moduleDtos) && course.moduleDtos) ||
    (Array.isArray(course?.moduleList) && course.moduleList) ||
    [];

  return {
    ...course,
    id: id ? String(id) : '',
    title: course?.title ?? course?.topic ?? 'Untitled Course',
    modules: rawModules.map(normalizeModule),
  };
}

/**
 * Create a new course
 */
export async function createCourse(data: any) {
  const payload = {
    ...data,
    title: data?.title ?? data?.topic,
  };

  const response = await apiFetch('/api/courses/create', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return normalizeCourse(unwrapApiData<any>(response));
}

/**
 * Create a custom built course directly via JSON
 */
export async function saveBuiltCourse(data: any) {
  const response = await apiFetch('/api/courses/build', {
    method: 'POST',
    body: JSON.stringify(data),
  });
  return normalizeCourse(unwrapApiData<any>(response));
}

/**
 * Generate editable AI scaffold draft (modules + lessons + lesson content blocks, no saving)
 */
export async function generateCourseOutlineAPI(data: any) {
  const payload = {
    ...data,
    title: data?.title ?? data?.topic,
  };

  const response = await apiFetch('/api/courses/generate-outline', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return unwrapApiData<any>(response);
}

/**
 * Fetch all courses for the current user
 */
export async function fetchCourses() {
  const response = await apiFetch('/api/courses/my-courses');
  const list = unwrapApiData<any>(response);
  return Array.isArray(list) ? list.map(normalizeCourse) : [];
}

/**
 * Fetch courses shared by the user
 */
export async function fetchCoursesSharedByMe() {
  const response = await apiFetch('/api/courses/shared-by-me');
  const list = unwrapApiData<any>(response);
  return Array.isArray(list) ? list.map(normalizeCourse) : [];
}

/**
 * Get a specific course by ID
 */
export async function getCourseById(id: string) {
  const response = await apiFetch(`/api/courses/${id}`);
  return normalizeCourse(unwrapApiData<any>(response));
}

/**
 * Update an existing course
 */
export async function updateCourse(id: string, data: any) {
  return apiFetch(`/api/courses/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  });
}

/**
 * Delete a course
 */
export async function deleteCourse(id: string) {
  return apiFetch(`/api/courses/${id}`, {
    method: 'DELETE',
  });
}

/**
 * Activate a course (make it accessible)
 */
export async function activateCourse(id: string) {
  return apiFetch(`/api/courses/${id}/activate`, {
    method: 'PUT',
  });
}

/**
 * Deactivate a course (make it inaccessible)
 */
export async function deactivateCourse(id: string) {
  return apiFetch(`/api/courses/${id}/deactivate`, {
    method: 'PUT',
  });
}

/**
 * Generate lesson content for a specific lesson
 */
export async function generateLessonContent(
  courseId: string,
  moduleId: string,
  lessonId: string
) {
  if (USE_MCP_CLIENT) {
    const mcpResponse = await executeMcpTool<any>({
      tool: 'lesson.generate',
      input: {
        // Keep Snowflake IDs as strings to avoid JS number precision loss.
        courseId,
        moduleId,
        lessonId,
      },
    });

    if (!mcpResponse?.success) {
      throw new Error(mcpResponse?.error || 'MCP lesson.generate failed');
    }
    return mcpResponse.data;
  }

  return apiFetch(
    `/api/courses/${courseId}/modules/${moduleId}/lessons/${lessonId}/generate`,
    {
      method: 'POST',
    }
  );
}

/**
 * Get a specific lesson by ID
 */
export async function getLessonById(id: string) {
  try {
    const response = await apiFetch(`/api/courses/lessons/${id}`);
    return unwrapApiData<any>(response);
  } catch {
    const response = await apiFetch(`/api/lessons/${id}`);
    return unwrapApiData<any>(response);
  }
}

/**
 * Add a module to a course
 */
export async function addModule(courseId: string, title: string) {
  return apiFetch(`/api/courses/${courseId}/modules`, {
    method: 'POST',
    body: JSON.stringify({ title }),
  });
}

/**
 * Rename a module
 */
export async function renameModule(courseId: string, moduleId: string, title: string) {
  return apiFetch(`/api/courses/${courseId}/modules/${moduleId}`, {
    method: 'PUT',
    body: JSON.stringify({ title }),
  });
}

/**
 * Delete a module
 */
export async function deleteModule(courseId: string, moduleId: string) {
  return apiFetch(`/api/courses/${courseId}/modules/${moduleId}`, {
    method: 'DELETE',
  });
}

/**
 * Add a lesson to a module
 */
export async function addLesson(courseId: string, moduleId: string, title: string) {
  return apiFetch(`/api/courses/${courseId}/modules/${moduleId}/lessons`, {
    method: 'POST',
    body: JSON.stringify({ title }),
  });
}

/**
 * Rename a lesson
 */
export async function renameLesson(courseId: string, moduleId: string, lessonId: string, title: string) {
  return apiFetch(`/api/courses/${courseId}/modules/${moduleId}/lessons/${lessonId}`, {
    method: 'PUT',
    body: JSON.stringify({ title }),
  });
}

/**
 * Delete a lesson
 */
export async function deleteLesson(courseId: string, moduleId: string, lessonId: string) {
  return apiFetch(`/api/courses/${courseId}/modules/${moduleId}/lessons/${lessonId}`, {
    method: 'DELETE',
  });
}

/**
 * Batch generate lessons for a module
 */
export async function batchGenerateLessons(
  courseId: string,
  moduleId: string,
  limit: number = 3
) {
  const response = await apiFetch(
    `/api/courses/${courseId}/modules/${moduleId}/lessons/batch-generate?limit=${limit}`,
    {
      method: 'POST',
    }
  );
  return unwrapApiData<any>(response);
}
