import { apiFetch } from './apiClient';

function unwrap<T>(payload: any): T {
  if (payload && typeof payload === 'object' && 'data' in payload) {
    return payload.data as T;
  }
  return payload as T;
}

async function tryCandidates<T>(candidates: Array<() => Promise<any>>): Promise<T> {
  let lastError: unknown;

  for (const call of candidates) {
    try {
      const res = await call();
      return unwrap<T>(res);
    } catch (error) {
      lastError = error;
    }
  }

  throw lastError instanceof Error
    ? lastError
    : new Error('Unable to resolve join link.');
}

/**
 * Resolve a share token to get course details
 */
export const resolveShareToken = async (token: string) => {
  const safeToken = encodeURIComponent(token);

  return tryCandidates<any>([
    () => apiFetch(`/api/join/${safeToken}`, { requiresAuth: false }),
    () => apiFetch(`/api/join/resolve/${safeToken}`, { requiresAuth: false }),
    () => apiFetch(`/api/join?token=${safeToken}`, { requiresAuth: false }),
    () => apiFetch(`/api/sharing/join/${safeToken}`, { requiresAuth: false }),
    () => apiFetch(`/api/sharing/join/resolve/${safeToken}`, { requiresAuth: false }),
    () => apiFetch(`/api/sharing/join?token=${safeToken}`, { requiresAuth: false }),
    () => apiFetch(`/api/courses/join/${safeToken}`, { requiresAuth: false }),
    () => apiFetch(`/api/courses/join/resolve/${safeToken}`, { requiresAuth: false }),
    () => apiFetch(`/api/courses/join?token=${safeToken}`, { requiresAuth: false }),
    () => apiFetch(`/api/courses/share/${safeToken}`, { requiresAuth: false }),
    () => apiFetch(`/api/courses/share/resolve/${safeToken}`, { requiresAuth: false }),
    () => apiFetch(`/api/courses/share?token=${safeToken}`, { requiresAuth: false }),
  ]);
};

/**
 * Enroll in a course using a share link
 */
export const enrollUsingShareLink = async (token: string) => {
  const safeToken = encodeURIComponent(token);

  return tryCandidates<any>([
    () =>
      apiFetch(`/api/join/${safeToken}/enroll`, {
        method: 'POST',
      }),
    () =>
      apiFetch(`/api/join/enroll/${safeToken}`, {
        method: 'POST',
      }),
    () =>
      apiFetch(`/api/join/enroll?token=${safeToken}`, {
        method: 'POST',
      }),
    () =>
      apiFetch(`/api/sharing/join/${safeToken}/enroll`, {
        method: 'POST',
      }),
    () =>
      apiFetch(`/api/sharing/join/enroll/${safeToken}`, {
        method: 'POST',
      }),
    () =>
      apiFetch(`/api/sharing/join/enroll?token=${safeToken}`, {
        method: 'POST',
      }),
    () =>
      apiFetch(`/api/courses/join/${safeToken}/enroll`, {
        method: 'POST',
      }),
    () =>
      apiFetch(`/api/courses/join/enroll/${safeToken}`, {
        method: 'POST',
      }),
    () =>
      apiFetch(`/api/courses/join/enroll?token=${safeToken}`, {
        method: 'POST',
      }),
  ]);
};

