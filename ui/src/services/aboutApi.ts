import { apiFetch } from './apiClient';

/**
 * Fetch the authenticated user's profile and stats snapshot.
 * Response shape matches ProfileResponsePojo:
 * { id, username, roles, createdAt, stats: { totalPoints, ... } }
 */
async function getProfile() {
  return apiFetch('/api/about/profile');
}

/**
 * Update the user's profile.
 */
async function updateProfile(payload: { displayName?: string; handle?: string }) {
  return apiFetch('/api/about/profile', {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

/**
 * Change the user's password.
 * @param {string} currentPassword
 * @param {string} newPassword
 * @param {string} confirmPassword
 */
async function changePassword(
  currentPassword: string,
  newPassword: string,
  confirmPassword: string
) {
  return apiFetch('/api/about/password', {
    method: 'PUT',
    body: JSON.stringify({
      currentPassword,
      newPassword,
      confirmPassword,
    }),
  });
}

export { getProfile, updateProfile, changePassword };
