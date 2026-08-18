import { apiFetch } from './apiClient';

export type SearchResultType = 'COURSE' | 'USER';

export interface SearchResultItem {
  id: string;
  type: SearchResultType;
  label: string;
  description: string;
  score: number;
  handle?: string;
}

export interface SearchResponse {
  results: SearchResultItem[];
  total: number;
}

export interface AutocompleteResponse {
  suggestions: string[];
  topResults: SearchResultItem[];
}

/**
 * Full search — returns ranked results with pagination.
 * GET /api/search?q=&types=COURSE,USER&offset=0&limit=10
 */
export async function search(
  query: string,
  options: {
    types?: SearchResultType[];
    offset?: number;
    limit?: number;
    excludeIds?: string[];
  } = {}
): Promise<SearchResponse> {
  const params = new URLSearchParams({ q: query });
  if (options.types?.length) params.set('types', options.types.join(','));
  if (options.offset !== undefined) params.set('offset', String(options.offset));
  if (options.limit !== undefined) params.set('limit', String(options.limit));
  if (options.excludeIds?.length) params.set('excludeIds', options.excludeIds.join(','));

  const res = await apiFetch(`/api/search?${params.toString()}`, { method: 'GET' });
  // Backend returns plain SearchResponse (not wrapped in ApiResponse)
  const data = res?.data ?? res;
  return data ?? { results: [], total: 0 };
}

/**
 * Autocomplete — fast prefix suggestions + top matching items.
 * GET /api/search/autocomplete?q=&types=&limit=8
 */
export async function autocomplete(
  prefix: string,
  options: {
    types?: SearchResultType[];
    limit?: number;
    excludeIds?: string[];
  } = {}
): Promise<AutocompleteResponse> {
  const params = new URLSearchParams({ q: prefix });
  if (options.types?.length) params.set('types', options.types.join(','));
  if (options.limit !== undefined) params.set('limit', String(options.limit));
  if (options.excludeIds?.length) params.set('excludeIds', options.excludeIds.join(','));

  const res = await apiFetch(`/api/search/autocomplete?${params.toString()}`, { method: 'GET' });
  const data = res?.data ?? res;
  return data ?? { suggestions: [], topResults: [] };
}

/**
 * Legacy: autocomplete user search (kept for backward compatibility with ShareCourse)
 */
export const autocompleteUsers = (query: string, limit = 8) =>
  autocomplete(query, { types: ['USER'], limit });

/**
 * Two-stage prefix resolution:
 * 1. Call /autocomplete to get trie-expanded suggestions + topResults.
 * 2. If topResults is empty (inverted-index couldn't match the raw prefix),
 *    use the expanded suggestion strings to fire a full search so we get
 *    real SearchResultItems with IDs.
 *
 * This is necessary because the backend's inverted index stores full tokens
 * (e.g. "arikaran03") while the trie supports prefix lookup ("ar" → "arikaran03").
 * Without this bridge, prefix queries return no topResults.
 */
export async function resolveByPrefix(
  prefix: string,
  options: { types?: SearchResultType[]; limit?: number; excludeIds?: string[] } = {}
): Promise<SearchResultItem[]> {
  const acRes = await autocomplete(prefix, options);
  let items: SearchResultItem[] = acRes.topResults ?? [];

  // If we have suggestions, use them to expand the search results
  if (acRes.suggestions && acRes.suggestions.length > 0) {
    // Use expanded tokens from the trie to search the inverted index
    const expandedQuery = acRes.suggestions.slice(0, 5).join(' ');
    const searchRes = await search(expandedQuery, {
      types: options.types,
      limit: options.limit ?? 10,
      excludeIds: options.excludeIds,
    });
    
    const searchItems = searchRes.results ?? [];
    
    // Merge results, avoiding duplicates
    const seenIds = new Set(items.map(i => i.id));
    for (const item of searchItems) {
      if (!seenIds.has(item.id)) {
        items.push(item);
        seenIds.add(item.id);
      }
    }
  } else if (items.length === 0 && prefix.length >= 2) {
    // Fallback: if no trie suggestions, try a direct search with the prefix
    const searchRes = await search(prefix, {
      types: options.types,
      limit: options.limit ?? 10,
      excludeIds: options.excludeIds,
    });
    items = searchRes.results ?? [];
  }

  return items;
}
