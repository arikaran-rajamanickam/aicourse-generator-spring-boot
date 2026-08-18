import { API_BASE_URL } from "../constants";

/**
 * Central API fetch function with proper token handling, validation, and error recovery
 * Handles Bearer token authentication across all requests
 */
export async function apiFetch(
  url: string,
  options: RequestInit & { requiresAuth?: boolean } = {}
) {
  const { requiresAuth = true, ...fetchOptions } = options;

  // Get and clean the token from localStorage
  let token = localStorage.getItem('token');

  // Aggressive token cleaning - handle all garbage cases
  if (token) {
    token = token.trim();
    // Strip JSON quotes if present
    if (token.startsWith('"') && token.endsWith('"')) {
      token = token.slice(1, -1);
    }
    // If token is HTML garbage, null string, or undefined
    if (
      token.startsWith('<') ||
      token.includes('<!DOCTYPE') ||
      token === 'null' ||
      token === 'undefined' ||
      token === ''
    ) {
      console.warn(
        'Detected invalid token (HTML or empty), clearing storage.'
      );
      localStorage.removeItem('token');
      token = null;
    }
  }

  // Build headers with optional auth
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    "ngrok-skip-browser-warning": "true",
    ...(requiresAuth && token && { Authorization: `Bearer ${token}` }),
    ...(fetchOptions.headers as Record<string, string>) || {},
  };

  // Construct full URL. In dev, keep API_BASE_URL empty and rely on Vite proxy (/api).
  const fullUrl = API_BASE_URL ? `${API_BASE_URL}${url}` : url;

  // Validate URL format (supports both absolute and relative URLs).
  try {
    new URL(fullUrl, window.location.origin);
  } catch (e) {
    console.error("Invalid URL construction:", fullUrl);
    throw new Error(`Invalid URL: ${fullUrl}`);
  }

  console.log("apiFetch Details:", { fullUrl, requiresAuth, hasToken: !!token });

  try {
    const res = await fetch(fullUrl, {
      ...fetchOptions,
      headers,
    });

    // Handle non-OK responses (allow 304 Not Modified as it's a valid successful cache hit)
    if (!res.ok && res.status !== 304) {
      const err = await res.text();
      console.error(`API Error [${url}]:`, res.status, err);
      throw new Error(err || `API Error: ${res.status}`);
    }

    // Always read as text first to handle flexibly
    const text = await res.text();
    const contentType = res.headers.get("content-type");
    console.log(`apiFetch: ${url} -> Status ${res.status} (${contentType || 'no-type'}), length: ${text.length}`);

    if (res.status === 304) {
      console.log("apiFetch: Handling 304 (Not Modified).");
      return {} as any;
    }

    if (text.trim().startsWith("<!DOCTYPE") || text.trim().startsWith("<html")) {
      console.error(`API received HTML instead of data [${url}]:`, text);

      if (text.includes("ERR_NGROK_6024") || text.includes("ngrok.com")) {
        throw new Error(
          "Ngrok returned its browser warning page instead of API JSON. " +
            "Set Vite proxy target correctly and include 'ngrok-skip-browser-warning: true' header."
        );
      }

      throw new Error(
        "Received HTML response from API (likely 404 or 500 error)"
      );
    }

    // Try parsing as JSON first, fallback to text
    if (text.trim() === "") {
      return (res.status === 204 || res.status === 304) ? ("" as any) : ({} as any);
    }

    try {
      return JSON.parse(text);
    } catch (err) {
      console.warn(`apiFetch: Failed to parse response as JSON for ${url}. Returning as text.`);
      return text;
    }
  } catch (error) {
    if (error instanceof TypeError && error.message === "Failed to fetch") {
      throw new Error(
        "Request failed before response was readable. Likely CORS/preflight blocked or network issue. " +
          "In development, keep VITE_API_BASE_URL empty and call /api through Vite proxy."
      );
    }
    throw error;
  }
}


