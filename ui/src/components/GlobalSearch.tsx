import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Search, BookOpen, User, Loader2, X } from "lucide-react";
import { cn } from "@/lib/utils";
import { resolveByPrefix, autocomplete, type SearchResultItem, type SearchResultType } from "@/services/searchApi";

// Debounce helper
function useDebounce<T>(value: T, delay: number): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);
  return debounced;
}

const typeIcon: Record<SearchResultType, typeof BookOpen> = {
  COURSE: BookOpen,
  USER: User,
};

const typeLabel: Record<SearchResultType, string> = {
  COURSE: "Course",
  USER: "User",
};

function resultPath(item: SearchResultItem): string {
  if (item.type === "COURSE") return `/courses/${item.id}`;
  return `/`;
}

interface GlobalSearchProps {
  className?: string;
}

export default function GlobalSearch({ className }: GlobalSearchProps) {
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<SearchResultItem[]>([]);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const inputRef = useRef<HTMLInputElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  const debouncedQuery = useDebounce(query.trim(), 250);

  // Fetch autocomplete results
  useEffect(() => {
    if (debouncedQuery.length < 2) {
      setResults([]);
      setSuggestions([]);
      setOpen(false);
      return;
    }

    let cancelled = false;
    setLoading(true);

    // Run both in parallel:
    // 1. resolveByPrefix → actual SearchResultItems (handles trie→inverted-index bridge)
    // 2. autocomplete → raw suggestion strings for pill display
    Promise.allSettled([
      resolveByPrefix(debouncedQuery, { limit: 8 }),
      autocomplete(debouncedQuery, { limit: 5 }),
    ]).then(([itemsResult, acResult]) => {
      if (cancelled) return;
      setResults(itemsResult.status === "fulfilled" ? itemsResult.value : []);
      setSuggestions(
        acResult.status === "fulfilled" ? (acResult.value.suggestions ?? []) : []
      );
      setOpen(true);
      setActiveIndex(-1);
    }).catch(() => {
      if (!cancelled) { setResults([]); setSuggestions([]); }
    }).finally(() => { if (!cancelled) setLoading(false); });

    return () => { cancelled = true; };
  }, [debouncedQuery]);

  // Close on outside click
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (!containerRef.current?.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  const handleSelect = useCallback(
    (item: SearchResultItem) => {
      setQuery("");
      setOpen(false);
      setResults([]);
      navigate(resultPath(item));
    },
    [navigate]
  );

  const handleSuggestionClick = useCallback((suggestion: string) => {
    setQuery(suggestion);
    inputRef.current?.focus();
  }, []);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (!open) return;
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setActiveIndex((i) => Math.min(i + 1, results.length - 1));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setActiveIndex((i) => Math.max(i - 1, -1));
    } else if (e.key === "Enter") {
      if (activeIndex >= 0 && results[activeIndex]) {
        handleSelect(results[activeIndex]);
      }
    } else if (e.key === "Escape") {
      setOpen(false);
      inputRef.current?.blur();
    }
  };

  const hasContent = results.length > 0 || suggestions.length > 0;

  return (
    <div ref={containerRef} className={cn("relative", className)}>
      {/* Input */}
      <div className="relative flex items-center">
        {loading ? (
          <Loader2 className="pointer-events-none absolute left-3 h-3.5 w-3.5 text-muted-foreground animate-spin" />
        ) : (
          <Search className="pointer-events-none absolute left-3 h-3.5 w-3.5 text-muted-foreground" />
        )}
        <input
          ref={inputRef}
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onFocus={() => { if (results.length > 0) setOpen(true); }}
          onKeyDown={handleKeyDown}
          placeholder="Search courses, users..."
          className="h-9 w-full rounded-lg border border-border bg-secondary/50 pl-9 pr-8 text-sm text-foreground placeholder:text-muted-foreground/70 focus:outline-none focus:ring-1 focus:ring-primary/50 transition-all"
        />
        {query && (
          <button
            type="button"
            onClick={() => { setQuery(""); setOpen(false); setResults([]); }}
            className="absolute right-2 text-muted-foreground hover:text-foreground transition-colors"
          >
            <X className="h-3.5 w-3.5" />
          </button>
        )}
      </div>

      {/* Dropdown */}
      {open && hasContent && (
        <div className="absolute left-0 right-0 top-full z-50 mt-1.5 max-h-80 overflow-y-auto rounded-xl border border-border bg-card shadow-xl shadow-black/20">
          {/* Suggestions (prefix completions) */}
          {suggestions.length > 0 && (
            <div className="border-b border-border/60 px-3 py-2">
              <p className="mb-1.5 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
                Suggestions
              </p>
              <div className="flex flex-wrap gap-1.5">
                {suggestions.slice(0, 5).map((s) => (
                  <button
                    key={s}
                    type="button"
                    onMouseDown={() => handleSuggestionClick(s)}
                    className="rounded-full border border-border bg-secondary/60 px-2.5 py-0.5 text-xs text-foreground hover:border-primary/40 hover:bg-primary/10 transition-colors"
                  >
                    {s}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Top results */}
          {results.length > 0 && (
            <div className="py-1">
              <p className="px-3 pt-2 pb-1 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
                Results
              </p>
              {results.map((item, i) => {
                const Icon = typeIcon[item.type] ?? BookOpen;
                return (
                  <button
                    key={`${item.type}-${item.id}`}
                    type="button"
                    onMouseDown={() => handleSelect(item)}
                    className={cn(
                      "flex w-full items-start gap-3 px-3 py-2.5 text-left transition-colors",
                      i === activeIndex
                        ? "bg-primary/10 text-foreground"
                        : "hover:bg-secondary/60 text-foreground"
                    )}
                  >
                    <div className={cn(
                      "mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-md",
                      item.type === "COURSE" ? "bg-primary/15 text-primary" : "bg-sky-500/15 text-sky-500"
                    )}>
                      <Icon className="h-3.5 w-3.5" />
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium">{item.label}</p>
                      {item.description && (
                        <p className="truncate text-xs text-muted-foreground">{item.description}</p>
                      )}
                    </div>
                    <span className="mt-0.5 shrink-0 rounded-sm border border-border/60 bg-secondary/50 px-1.5 py-0.5 text-[10px] text-muted-foreground">
                      {typeLabel[item.type]}
                    </span>
                  </button>
                );
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
