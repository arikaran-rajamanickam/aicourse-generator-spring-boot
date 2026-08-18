import { apiFetch } from './apiClient';

function unwrap<T>(payload: any): T {
  if (payload && typeof payload === 'object' && 'data' in payload) {
    return payload.data as T;
  }
  return payload as T;
}

/**
 * Generate a share link for a course
 */
export async function generateShareLink(courseId: string, payload: any): Promise<any> {
  const res = await apiFetch(`/api/courses/${courseId}/share/generate`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return unwrap<any>(res);
}

/**
 * Get all share links for a course
 */
export async function getCourseShareLinks(courseId: string): Promise<any[]> {
  const res = await apiFetch(`/api/courses/${courseId}/share/links`);
  const data = unwrap<any>(res);
  return Array.isArray(data) ? data : [];
}

/**
 * Deactivate a specific share link
 */
export async function deactivateShareLink(courseId: string, shareLinkId: string): Promise<any> {
  const res = await apiFetch(`/api/courses/${courseId}/share/links/${shareLinkId}/deactivate`, {
    method: 'PUT',
  });
  return unwrap<any>(res);
}

/**
 * Activate a specific share link
 */
export async function activateShareLink(courseId: string, shareLinkId: string): Promise<any> {
  const res = await apiFetch(`/api/courses/${courseId}/share/links/${shareLinkId}/activate`, {
    method: 'PUT',
  });
  return unwrap<any>(res);
}

/**
 * Deactivate all share links for a course
 */
export async function deactivateAllShareLinks(courseId: string): Promise<any> {
  const res = await apiFetch(`/api/courses/${courseId}/share/links/deactivate-all`, {
    method: 'PUT',
  });
  return unwrap<any>(res);
}

/**
 * Activate all share links for a course
 */
export async function activateAllShareLinks(courseId: string): Promise<any> {
  const res = await apiFetch(`/api/courses/${courseId}/share/links/activate-all`, {
    method: 'PUT',
  });
  return unwrap<any>(res);
}

/**
 * Delete (revoke) a share link
 */
export async function revokeShareLink(courseId: string, shareLinkId: string): Promise<any> {
  const res = await apiFetch(`/api/courses/${courseId}/share/links/${shareLinkId}`, {
    method: 'DELETE',
  });
  return unwrap<any>(res);
}

/**
 * Update a share link
 */
export async function updateShareLink(courseId: string, shareLinkId: string, payload: any): Promise<any> {
  const res = await apiFetch(`/api/courses/${courseId}/share/links/${shareLinkId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
  return unwrap<any>(res);
}

/**
 * Send direct email invites for a course
 */
export async function sendDirectInvite(courseId: string, recipients: string[]): Promise<any> {
  const res = await apiFetch(`/api/courses/${courseId}/share/invite`, {
    method: 'POST',
    body: JSON.stringify({
      emails: recipients,
      usernames: recipients,
      recipients,
    }),
  });
  return unwrap<any>(res);
}

