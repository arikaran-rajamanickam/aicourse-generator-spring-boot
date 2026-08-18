import { apiFetch } from './apiClient';

/**
 * Fetch invites shared with me
 */
export async function fetchSharedWithMeInvites() {
  const res = await apiFetch('/api/sharing/invites/shared-with-me', {
    method: 'GET',
  });
  return res.data;
}

/**
 * Fetch invites shared by me
 */
export async function fetchSharedByMeInvites() {
  const res = await apiFetch('/api/sharing/invites/shared-by-me', {
    method: 'GET',
  });
  return res.data;
}

/**
 * Fetch invite summary (count of new invites, etc.)
 */
export async function fetchInviteSummary() {
  const res = await apiFetch('/api/sharing/invites/summary', {
    method: 'GET',
  });
  return res.data;
}

/**
 * Mark a specific invite as read
 */
export async function markInviteRead(inviteId: string) {
  const res = await apiFetch(`/api/sharing/invites/${inviteId}/read`, {
    method: 'PUT',
  });
  return res.data;
}

/**
 * Accept an invite
 */
export async function acceptInvite(inviteId: string) {
  const res = await apiFetch(`/api/sharing/invites/${inviteId}/accept`, {
    method: 'PUT',
  });
  return res.data;
}

/**
 * Decline an invite
 */
export async function declineInvite(inviteId: string) {
  const res = await apiFetch(`/api/sharing/invites/${inviteId}/decline`, {
    method: 'PUT',
  });
  return res.data;
}

/**
 * Mark all invites as read
 */
export async function markAllInvitesRead() {
  const res = await apiFetch('/api/sharing/invites/mark-all-read', {
    method: 'PUT',
  });
  return res.data;
}

