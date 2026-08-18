import { apiFetch } from './apiClient';

/**
 * Get global leaderboard
 */
export async function getGlobalLeaderboard(page = 0, size = 10) {
  return apiFetch(`/api/leaderboard/global?page=${page}&size=${size}`);
}

/**
 * Get current user's leaderboard rank
 */
export async function getMyRank() {
  return apiFetch('/api/leaderboard/me');
}

