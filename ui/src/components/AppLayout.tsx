import { Navigate, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useEffect, useMemo, useRef, useState } from "react";
import AppSidebar from "./AppSidebar";
import { useAuth } from "@/auth/AuthContext";
import { useTheme } from "./ThemeProvider";
import { Search, Bell, Sparkles, Sun, Moon } from "lucide-react";
import { Button } from "./ui/button";
import { resolveByPrefix, type SearchResultItem } from "@/services/searchApi";
import { ProfileDropdown } from "./ProfileDropdown";
import NotificationPanel, { notificationTabs, type NotificationTab } from "./NotificationPanel";
import { useNotifications } from "@/hooks/useNotifications";
import { cn } from "@/lib/utils";

export default function AppLayout() {
  const { token, loading } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const navigate = useNavigate();
  const location = useLocation();
  const searchWrapRef = useRef<HTMLDivElement | null>(null);
  const [query, setQuery] = useState("");
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchLoading, setSearchLoading] = useState(false);
  const [searchResults, setSearchResults] = useState<SearchResultItem[]>([]);

  // Notification state
  const { unreadCount } = useNotifications();
  const [notifOpen, setNotifOpen] = useState(false);
  const [notifTab, setNotifTab] = useState<NotificationTab>("All");

  const trimmedQuery = query.trim();
  const canSearch = trimmedQuery.length >= 2;

  useEffect(() => {
    if (!canSearch) {
      setSearchResults([]);
      setSearchLoading(false);
      return;
    }

    let cancelled = false;
    const timer = window.setTimeout(async () => {
      setSearchLoading(true);
      try {
        const items = await resolveByPrefix(trimmedQuery, { limit: 6 });
        if (!cancelled) {
          setSearchResults(items);
          setSearchOpen(true);
        }
      } catch {
        if (!cancelled) {
          setSearchResults([]);
        }
      } finally {
        if (!cancelled) {
          setSearchLoading(false);
        }
      }
    }, 250);

    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [canSearch, trimmedQuery]);

  useEffect(() => {
    const onOutsideClick = (event: MouseEvent) => {
      if (!searchWrapRef.current) return;
      if (!searchWrapRef.current.contains(event.target as Node)) {
        setSearchOpen(false);
      }
    };

    document.addEventListener("mousedown", onOutsideClick);
    return () => document.removeEventListener("mousedown", onOutsideClick);
  }, []);

  const visibleResults = useMemo(() => searchResults.slice(0, 6), [searchResults]);

  const goToResult = (result: SearchResultItem) => {
    setSearchOpen(false);
    setQuery("");

    if (result.type === "COURSE") {
      navigate(`/courses/${result.id}`);
      return;
    }

    navigate("/profile");
  };

  const onSearchSubmit = () => {
    if (visibleResults.length > 0) {
      goToResult(visibleResults[0]);
      return;
    }

    if (trimmedQuery.length > 0) {
      navigate("/projects");
      setSearchOpen(false);
    }
  };

  const isSearchVisible = [
    "/dashboard",
    "/courses",
  ].some(path => location.pathname === path || location.pathname.startsWith(path + "/"));

  if (loading) {
    return <div className="flex h-screen items-center justify-center bg-background">
      <div className="flex flex-col items-center gap-4">
        <div className="h-12 w-12 rounded-2xl border-4 border-primary/30 border-t-primary animate-spin" />
        <p className="text-xs font-bold uppercase tracking-widest text-muted-foreground animate-pulse">Syncing environment...</p>
      </div>
    </div>;
  }

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  const isCollapsedMode = location.pathname.includes("/lessons/") || 
                          location.pathname.includes("/ai-coach") || 
                          location.pathname.endsWith("/coach");

  return (
    <div className="flex h-screen w-full bg-background font-body text-foreground selection:bg-primary/20 overflow-hidden">

      {/* Background layers */}
      <div aria-hidden className="pointer-events-none fixed inset-0 -z-10 bg-background" />

      {/* Premium Sidebar Wrapper */}
      <div className={cn("hidden lg:block shrink-0 h-full relative z-50 transition-all duration-300", isCollapsedMode ? "w-[72px]" : "w-64")}>
        <AppSidebar isCollapsed={isCollapsedMode} />
      </div>

      <div className="flex flex-1 flex-col min-w-0 h-full relative z-10">
        {/* Topbar — uses semantic tokens, works in both light and dark */}
        <header className="sticky top-0 z-40 flex h-14 shrink-0 items-center gap-3 border-b border-border bg-background/90 px-6 backdrop-blur-sm">
          {/* Search */}
          {isSearchVisible ? (
            <div ref={searchWrapRef} className="relative flex h-9 w-full max-w-md items-center gap-2 rounded-md border border-transparent bg-muted px-3 text-sm transition-colors focus-within:border-border focus-within:bg-card">
              <Search className="h-4 w-4 shrink-0 text-muted-foreground" />
              <input
                type="text"
                placeholder="Search courses, projects, prompts..."
                value={query}
                onChange={(e) => {
                  setQuery(e.target.value);
                  setSearchOpen(true);
                }}
                onFocus={() => setSearchOpen(true)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    onSearchSubmit();
                  }
                  if (e.key === "Escape") {
                    setSearchOpen(false);
                  }
                }}
                className="w-full bg-transparent text-foreground placeholder:text-muted-foreground focus:outline-none text-sm"
              />
              <kbd className="hidden rounded bg-background border border-border px-1.5 py-0.5 text-[10px] text-muted-foreground sm:inline-block">
                ⌘K
              </kbd>

              {searchOpen && (canSearch || searchLoading) && (
                <div className="absolute left-0 right-0 top-[calc(100%+0.5rem)] z-50 overflow-hidden rounded-xl border border-border bg-popover shadow-card">
                  {searchLoading ? (
                    <div className="px-4 py-3 text-xs text-muted-foreground">
                      Searching...
                    </div>
                  ) : visibleResults.length === 0 ? (
                    <div className="px-4 py-3 text-xs text-muted-foreground">
                      No results
                    </div>
                  ) : (
                    <ul className="max-h-72 overflow-y-auto py-1">
                      {visibleResults.map((result) => (
                        <li key={`${result.type}-${result.id}`}>
                          <button
                            type="button"
                            onClick={() => goToResult(result)}
                            className="flex w-full items-center justify-between px-4 py-2.5 text-left text-sm text-foreground hover:bg-muted"
                          >
                            <span className="truncate pr-3">{result.label}</span>
                            <span className="shrink-0 rounded-sm border border-border bg-muted px-1.5 py-0.5 text-[10px] font-medium uppercase tracking-widest text-muted-foreground">
                              {result.type}
                            </span>
                          </button>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )}
            </div>
          ) : (
            <div className="flex-1" />
          )}

          <div className="flex flex-1 items-center justify-end gap-1">
            {/* Generate CTA */}
            <Button 
              variant="hero" 
              size="sm" 
              className="mr-2 h-8 gap-1.5 px-3"
              onClick={() => navigate("/create-course")}
            >
              <Sparkles className="h-3.5 w-3.5" />
              Generate
            </Button>

            <span className="mr-1 h-5 w-px bg-border" />

            {/* Bell */}
            <button
              onClick={() => setNotifOpen(true)}
              className="relative h-8 w-8 flex items-center justify-center rounded-full text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
              aria-label="Open notifications"
            >
              <Bell className="h-4 w-4" />
              {unreadCount > 0 && (
                <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-primary border-2 border-background" />
              )}
            </button>

            {/* Theme toggle */}
            <button
              onClick={toggleTheme}
              className="h-8 w-8 flex items-center justify-center rounded-full text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
              aria-label="Toggle theme"
            >
              {theme === "light" ? <Moon className="h-4 w-4" /> : <Sun className="h-4 w-4" />}
            </button>

            <span className="h-4 w-px bg-border/60 mx-1" />

            {/* Profile Dropdown */}
            <ProfileDropdown />
          </div>
        </header>

        {/* Unified Scrollable Content Area */}
        <div className="flex-1 overflow-y-auto overflow-x-hidden relative">
          <Outlet />
        </div>

      </div>

      {/* Global Components */}
      <NotificationPanel
        open={notifOpen}
        onClose={() => setNotifOpen(false)}
        activeTab={notifTab}
        onTabChange={setNotifTab}
      />
    </div>
  );
}
