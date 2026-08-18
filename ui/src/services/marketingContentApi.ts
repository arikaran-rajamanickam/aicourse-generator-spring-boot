import { API_BASE_URL } from "../constants";
import {
  fallbackLandingContent,
  fallbackLoginContent,
  type LandingContent,
  type LoginContent,
} from "../data/marketingContent";

type MarketingResponse<T> = {
  pageKey: string;
  version: number;
  updatedAt: string;
  content: T;
};

const pageCache = new Map<string, { etag?: string; body?: unknown }>();

async function getMarketingPage<T>(page: string, fallback: T): Promise<T> {
  const fullUrl = API_BASE_URL ? `${API_BASE_URL}/api/content/public/${page}` : `/api/content/public/${page}`;
  const cached = pageCache.get(page);

  const headers: Record<string, string> = { Accept: "application/json" };
  if (cached?.etag) {
    headers["If-None-Match"] = cached.etag;
  }

  try {
    const res = await fetch(fullUrl, { method: "GET", headers });

    if (res.status === 304 && cached?.body) {
      return cached.body as T;
    }

    if (!res.ok) {
      throw new Error(`Failed to load ${page} content: ${res.status}`);
    }

    const parsed = (await res.json()) as MarketingResponse<T>;
    const etag = res.headers.get("etag") ?? undefined;
    pageCache.set(page, { etag, body: parsed.content });
    return parsed.content;
  } catch (_error) {
    return fallback;
  }
}

export function getLandingContent() {
  return getMarketingPage<LandingContent>("landing", fallbackLandingContent);
}

export function getLoginContent() {
  return getMarketingPage<LoginContent>("login", fallbackLoginContent);
}

