/**
 * Global constants for the application
 */

export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "").trim();

export const USE_MCP_CLIENT =
  String(import.meta.env.VITE_USE_MCP ?? "false").trim().toLowerCase() === "true";

export const TOKEN_STORAGE_KEY = 'token';
