import { apiFetch } from './apiClient';

interface CachedExplanation {
  blocks: any[];
  suggestions: string[];
}

/**
 * Fetch a cached prerequisite explanation from backend.
 * Returns null if no cache exists.
 */
export async function getCachedPrereqExplanation(
  courseId: string,
  prerequisite: string,
  depth: string = 'standard'
): Promise<CachedExplanation | null> {
  try {
    const params = new URLSearchParams({ prerequisite, depth });
    const response = await apiFetch(
      `/api/courses/${courseId}/prerequisites/explanation?${params.toString()}`
    );

    const data = response?.data ?? response;
    if (data && data.blocks) {
      return data as CachedExplanation;
    }
    return null;
  } catch {
    return null;
  }
}

/**
 * Save a prerequisite explanation to backend cache.
 */
export async function savePrereqExplanation(
  courseId: string,
  prerequisite: string,
  depth: string,
  responseData: CachedExplanation
): Promise<void> {
  try {
    await apiFetch(`/api/courses/${courseId}/prerequisites/explanation`, {
      method: 'POST',
      body: JSON.stringify({
        prerequisite,
        depth,
        responseData,
      }),
    });
  } catch (err) {
    // Silently fail — caching is best-effort, shouldn't block user
    console.warn('Failed to cache prerequisite explanation:', err);
  }
}

