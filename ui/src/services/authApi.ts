import { apiFetch } from './apiClient';

/**
 * Login user with credentials
 */
export async function login(credentials: any) {
  return apiFetch('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
    requiresAuth: false,
  });
}

/**
 * Register new user
 */
export async function register(data: any) {
  return apiFetch('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(data),
    requiresAuth: false,
  });
}

/**
 * Get current authenticated user
 */
export async function getMe() {
  return apiFetch('/api/auth/me');
}

/**
 * Logout user
 */
export async function logout() {
  try {
    await apiFetch('/api/auth/logout', {
      method: 'POST',
      requiresAuth: true,
    });
  } catch (err) {
    console.warn('Logout endpoint failed or not implemented', err);
  }
}

