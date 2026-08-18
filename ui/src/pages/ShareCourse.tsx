import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { ChevronLeft, Link2, Mail, Power, PowerOff, Copy, Trash2, X, Loader2, BarChart2, Search as SearchIcon } from "lucide-react";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { getCourseById } from "../services/courseApi";
import { resolveByPrefix, type SearchResultItem } from "../services/searchApi";
import { getCourseEnrollments } from "../services/progressApi";
import {
  activateShareLink,
  deactivateShareLink,
  generateShareLink,
  getCourseShareLinks,
  revokeShareLink,
  sendDirectInvite,
} from "../services/shareApi";
import { toast } from "sonner";
import { useAuth } from "../auth/AuthContext";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "../components/ui/dialog";
import CourseAnalyticsModal from "../components/course/CourseAnalyticsModal";
import CourseLeaderboard from "../components/course/CourseLeaderboard";

// ─── Types ─────────────────────────────────────────────────────────
type Recipient = Pick<SearchResultItem, "id" | "label" | "description">;
type LinkType = "PUBLIC" | "PRIVATE";
type ShareLinkRow = { id: string; token: string; type: LinkType; active: boolean; url?: string };

function extractTokenFromUrl(url: string): string {
  if (!url) return "";
  const normalized = url.trim();
  const byPath = normalized.match(/\/join\/([^/?#]+)/i);
  if (byPath?.[1]) return byPath[1];
  const byQuery = normalized.match(/[?&]token=([^&#]+)/i);
  return byQuery?.[1] ? decodeURIComponent(byQuery[1]) : "";
}

function normalizeShareLink(item: any): ShareLinkRow {
  let rawUrl =
    String(item?.url ?? item?.link ?? item?.shareUrl ?? item?.joinUrl ?? "").trim();
  // Ignore rawUrl if it's an /api/ path
  if (rawUrl.startsWith('/api/')) {
    rawUrl = '';
  }
  const extracted = extractTokenFromUrl(rawUrl);
  const token = String(item?.token ?? item?.shareToken ?? extracted ?? "").trim();

  const frontendUrl = token ? `${window.location.origin}/join/${token}` : undefined;

  return {
    id: String(item?.id ?? item?.shareLinkId ?? item?.token ?? ""),
    token,
    type: String(item?.type ?? item?.linkType ?? "PUBLIC").toUpperCase() === "PRIVATE" ? "PRIVATE" : "PUBLIC",
    active: Boolean(item?.isActive ?? item?.active ?? item?.enabled ?? true),
    url: frontendUrl,
  };
}

// ─── Debounce hook ──────────────────────────────────────────────────
function useDebounce<T>(value: T, delay: number): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);
  return debounced;
}

// ─── UserAutocomplete: a reusable chip-input that searches backend ──
interface UserAutocompleteProps {
  id: string;
  selected: Recipient[];
  onAdd: (r: Recipient) => void;
  onRemove: (id: string) => void;
  placeholder?: string;
}

function UserAutocomplete({ id, selected, onAdd, onRemove, placeholder }: UserAutocompleteProps) {
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState<SearchResultItem[]>([]);
  const [loading, setLoading] = useState(false);
  const debouncedQuery = useDebounce(query.trim(), 250);
  const selectedIds = useMemo(() => new Set(selected.map((r) => r.id)), [selected]);

  useEffect(() => {
    if (debouncedQuery.length < 2) {
      setSuggestions([]);
      return;
    }
    let cancelled = false;
    setLoading(true);
    resolveByPrefix(debouncedQuery, { types: ["USER"], limit: 8 })
      .then((items) => {
        if (cancelled) return;
        // Filter out already-selected users
        setSuggestions(items.filter((r) => !selectedIds.has(String(r.id))));
      })
      .catch(() => { if (!cancelled) setSuggestions([]); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [debouncedQuery, selectedIds]);

  const handleAdd = (item: SearchResultItem) => {
    onAdd({
      id: String(item.id),
      label: item.label,
      description: item.handle ? `@${item.handle}` : item.description ?? "",
    });
    setQuery("");
    setSuggestions([]);
  };

  const handleFreeform = () => {
    const value = query.trim();
    if (!value) return;
    onAdd({ id: `manual-${value.toLowerCase()}`, label: value, description: "" });
    setQuery("");
    setSuggestions([]);
  };

  return (
    <div className="share-autocomplete-wrap">
      <div className="share-chip-input" onClick={() => document.getElementById(id)?.focus()}>
        {selected.length === 0 && !query ? (
          <span className="text-sm text-muted-foreground">{placeholder ?? "Type a user ID"}</span>
        ) : null}
        {selected.map((r) => (
          <span key={r.id} className="share-chip">
            {r.label}
            <button
              type="button"
              aria-label={`Remove ${r.label}`}
              onClick={() => onRemove(r.id)}
            >
              <X className="h-3.5 w-3.5" />
            </button>
          </span>
        ))}
        <input
          id={id}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => {
            if ((e.key === "Enter" || e.key === ",") && query.trim()) {
              e.preventDefault();
              if (suggestions.length > 0) {
                handleAdd(suggestions[0]);
              } else {
                handleFreeform();
              }
            }
            if (e.key === "Backspace" && !query && selected.length > 0) {
              onRemove(selected[selected.length - 1].id);
            }
          }}
          className="min-w-[140px] bg-transparent outline-none text-sm text-foreground placeholder:text-muted-foreground/60 flex-1"
        />
        {loading && <Loader2 className="h-3.5 w-3.5 animate-spin text-muted-foreground shrink-0" />}
      </div>

      {suggestions.length > 0 && (
        <div className="share-suggestions">
          {suggestions.map((user) => (
            <button
              key={user.id}
              type="button"
              className="share-suggestion-item"
              onMouseDown={() => handleAdd(user)}
            >
              <span className="font-medium text-foreground group-hover:text-primary transition-colors">{user.label}</span>
              {user.handle ? (
                <span className="text-xs text-muted-foreground">@{user.handle}</span>
              ) : user.description && user.description !== "User" ? (
                <span className="text-xs text-muted-foreground">{user.description}</span>
              ) : null}
            </button>
          ))}
        </div>
      )}
      <p className="mt-2 text-xs text-muted-foreground">
        Type at least 2 characters to search users. Press Enter to add.
      </p>
    </div>
  );
}

// ─── Main Component ──────────────────────────────────────────────────
export default function ShareCourse() {
  const { courseId } = useParams();
  const { user: currentUser } = useAuth();
  const queryClient = useQueryClient();
  const [course, setCourse] = useState<any>(null);
  const [courseLoading, setCourseLoading] = useState(true);
  const [courseActive, setCourseActive] = useState(true);
  const [linkType, setLinkType] = useState<LinkType>("PUBLIC");
  const [expiryDays, setExpiryDays] = useState("");
  const [restrictedDays, setRestrictedDays] = useState("");
  const [restrictedOpen, setRestrictedOpen] = useState(false);

  const [selectedRecipients, setSelectedRecipients] = useState<Recipient[]>([]);
  const [allowlistedUsers, setAllowlistedUsers] = useState<Recipient[]>([]);
  const [generatedLinks, setGeneratedLinks] = useState<ShareLinkRow[]>([]);
  const [linksLoading, setLinksLoading] = useState(false);
  const [generatingPublic, setGeneratingPublic] = useState(false);
  const [generatingRestricted, setGeneratingRestricted] = useState(false);
  const [sendingInvites, setSendingInvites] = useState(false);
  const [updatingLinkId, setUpdatingLinkId] = useState<string | null>(null);

  const [enrollments, setEnrollments] = useState<any[]>([]);
  const [enrollmentsLoading, setEnrollmentsLoading] = useState(false);
  const [analyticsOpen, setAnalyticsOpen] = useState(false);
  const [analyticsUser, setAnalyticsUser] = useState<{ id: string, name: string } | null>(null);

  const [enrollmentSearch, setEnrollmentSearch] = useState("");
  const debouncedEnrollmentSearch = useDebounce(enrollmentSearch.trim().toLowerCase(), 300);

  const filteredEnrollments = useMemo(() => {
    const currentUserId = currentUser?.id != null ? String(currentUser.id) : null;
    const visible = currentUserId
      ? enrollments.filter((env) => String(env.userId) !== currentUserId)
      : enrollments;

    if (!debouncedEnrollmentSearch) return visible;
    return visible.filter((env) => {
      const name = String(env.userName || "").toLowerCase();
      const handle = String(env.userHandle || "").toLowerCase();
      const id = String(env.userId || "").toLowerCase();
      return name.includes(debouncedEnrollmentSearch) || handle.includes(debouncedEnrollmentSearch) || id.includes(debouncedEnrollmentSearch);
    });
  }, [enrollments, debouncedEnrollmentSearch, currentUser?.id]);

  useEffect(() => {
    let mounted = true;
    async function loadCourseAndLinks() {
      if (!courseId) { setCourseLoading(false); return; }
      try {
        const [courseData, linksData] = await Promise.all([
          getCourseById(courseId),
          getCourseShareLinks(courseId).catch(() => []),
        ]);
        if (mounted) {
          // Verify ownership: Only creator can share
          const creatorId = String(courseData.creator || courseData.creatorId || "");
          const currentId = String(currentUser?.id || "");
          
          if (creatorId !== currentId) {
            toast.error("You are not authorized to share this course");
            window.location.href = `/courses/${courseId}`;
            return;
          }

          setCourse(courseData);
          setGeneratedLinks(Array.isArray(linksData) ? linksData.map(normalizeShareLink).filter((item) => item.id) : []);
        }

        // Load enrollments separately but initially
        setEnrollmentsLoading(true);
        try {
          const enrollData = await getCourseEnrollments(courseId);
          if (mounted) setEnrollments(enrollData || []);
        } catch (e) {
          console.error("Failed to load enrollments", e);
        } finally {
          if (mounted) setEnrollmentsLoading(false);
        }
      } catch {
        if (mounted) setCourse(null);
      } finally {
        if (mounted) setCourseLoading(false);
      }
    }
    loadCourseAndLinks();
    return () => { mounted = false; };
  }, [courseId]);

  const refreshLinks = useCallback(async () => {
    if (!courseId) return;
    setLinksLoading(true);
    try {
      const links = await getCourseShareLinks(courseId);
      setGeneratedLinks(links.map(normalizeShareLink).filter((item) => item.id));
    } catch (error) {
      console.error("Failed to fetch share links:", error);
      toast.error("Failed to fetch share links");
    } finally {
      setLinksLoading(false);
    }
  }, [courseId]);

  const addRecipient = useCallback((r: Recipient) => {
    setSelectedRecipients((prev) =>
      prev.some((x) => x.id === r.id) ? prev : [...prev, r]
    );
  }, []);

  const removeRecipient = useCallback((id: string) => {
    setSelectedRecipients((prev) => prev.filter((r) => r.id !== id));
  }, []);

  const addAllowlistUser = useCallback((r: Recipient) => {
    setAllowlistedUsers((prev) =>
      prev.some((x) => x.id === r.id) ? prev : [...prev, r]
    );
  }, []);

  const removeAllowlistUser = useCallback((id: string) => {
    setAllowlistedUsers((prev) => prev.filter((r) => r.id !== id));
  }, []);

  const buildExpiryPayload = (daysValue: string) => {
    const parsed = Number(daysValue);
    if (!daysValue || Number.isNaN(parsed) || parsed <= 0) return {};
    const expiresAt = new Date(Date.now() + parsed * 24 * 60 * 60 * 1000).toISOString();
    return { expiresInDays: parsed, expiryDays: parsed, expiresAt };
  };

  const handleGeneratePublicLink = async () => {
    if (!courseId) return;
    setGeneratingPublic(true);
    try {
      const payload = {
        type: "PUBLIC",
        linkType: "PUBLIC",
        ...buildExpiryPayload(expiryDays),
      };
      const created = await generateShareLink(courseId, payload);
      const normalized = normalizeShareLink(created);
      setGeneratedLinks((prev) => [normalized, ...prev.filter((item) => item.id !== normalized.id)]);
      toast.success("Share link generated");
    } catch (error) {
      console.error("Failed to generate public link:", error);
      toast.error(error instanceof Error ? error.message : "Failed to generate link");
    } finally {
      setGeneratingPublic(false);
    }
  };

  const handleGenerateRestrictedLink = async () => {
    if (!courseId) return;
    setGeneratingRestricted(true);
    try {
      const users = allowlistedUsers
        .map((item) => item.description || item.label)
        .map((value) => String(value).trim().replace(/^@/, ""))
        .filter(Boolean);
      const payload = {
        type: "PRIVATE",
        linkType: "PRIVATE",
        allowedUsers: users,
        allowedUsernames: users,
        allowlist: users,
        ...buildExpiryPayload(restrictedDays),
      };
      const created = await generateShareLink(courseId, payload);
      const normalized = normalizeShareLink(created);
      setGeneratedLinks((prev) => [normalized, ...prev.filter((item) => item.id !== normalized.id)]);
      setRestrictedOpen(false);
      setAllowlistedUsers([]);
      setRestrictedDays("");
      toast.success("Private share link generated");
    } catch (error) {
      console.error("Failed to generate private link:", error);
      toast.error(error instanceof Error ? error.message : "Failed to generate private link");
    } finally {
      setGeneratingRestricted(false);
    }
  };

  const handleSendInvites = async () => {
    if (!courseId || selectedRecipients.length === 0) return;
    setSendingInvites(true);
    try {
      const recipients = selectedRecipients
        .map((item) => (item.id.startsWith("manual-") ? item.label : item.description || item.label))
        .map((value) => String(value).trim().replace(/^@/, ""))
        .filter(Boolean);

      await sendDirectInvite(courseId, recipients);
      setSelectedRecipients([]);
      queryClient.invalidateQueries({ queryKey: ["courses-shared-by-me"] });
      toast.success("Invites sent");
    } catch (error) {
      console.error("Failed to send invites:", error);
      toast.error(error instanceof Error ? error.message : "Failed to send invites");
    } finally {
      setSendingInvites(false);
    }
  };

  const handleToggleLink = async (link: ShareLinkRow) => {
    if (!courseId || !link.id) return;
    setUpdatingLinkId(link.id);
    try {
      if (link.active) {
        await deactivateShareLink(courseId, link.id);
      } else {
        await activateShareLink(courseId, link.id);
      }
      setGeneratedLinks((prev) =>
        prev.map((item) => (item.id === link.id ? { ...item, active: !link.active } : item))
      );
      toast.success(link.active ? "Share link deactivated" : "Share link activated");
    } catch (error) {
      console.error("Failed to update link status:", error);
      toast.error(error instanceof Error ? error.message : "Failed to update link status");
    } finally {
      setUpdatingLinkId(null);
    }
  };

  const handleDeleteLink = async (linkId: string) => {
    if (!courseId || !linkId) return;
    setUpdatingLinkId(linkId);
    try {
      await revokeShareLink(courseId, linkId);
      setGeneratedLinks((prev) => prev.filter((item) => item.id !== linkId));
      toast.success("Share link deleted");
    } catch (error) {
      console.error("Failed to delete link:", error);
      toast.error(error instanceof Error ? error.message : "Failed to delete link");
    } finally {
      setUpdatingLinkId(null);
    }
  };

  const handleCopyLink = async (link: ShareLinkRow) => {
    const joinUrl = link.url || (link.token ? `${window.location.origin}/join/${link.token}` : "");
    if (!joinUrl) {
      toast.error("This share link has no token");
      return;
    }

    try {
      await navigator.clipboard.writeText(joinUrl);
      toast.success("Link copied");
    } catch {
      toast.error("Failed to copy link");
    }
  };

  const handleOpenAnalytics = (userId: string, userName: string) => {
    setAnalyticsUser({ id: userId, name: userName });
    setAnalyticsOpen(true);
  };

  if (courseLoading) return <div className="p-8 text-muted-foreground">Loading course...</div>;
  if (!course) return <div className="p-8 text-muted-foreground">Course not found.</div>;

  return (
    <div className="share-page animate-fade-in p-4 md:p-8">
      <Link to={`/courses/${courseId}`}>
        <Button variant="outline" size="sm" className="gap-2">
          <ChevronLeft className="h-4 w-4" />
          Back to Course
        </Button>
      </Link>

      <div className="mt-6 flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="font-display text-3xl font-bold text-foreground">Share: {course.title}</h1>
          <p className="mt-1 text-muted-foreground">Manage access and share links for your course.</p>
        </div>
        <Button
          variant={courseActive ? "outline-destructive" : "success"}
          className="gap-2 self-start md:self-auto"
          onClick={() => setCourseActive((prev) => !prev)}
        >
          {courseActive ? <PowerOff className="h-4 w-4" /> : <Power className="h-4 w-4" />}
          {courseActive ? "Deactivate" : "Activate"}
        </Button>
      </div>

      <div className="share-grid mt-8 grid grid-cols-1 gap-8 lg:grid-cols-2">
        {/* Share Link */}
        <div className="glass-card rounded-xl p-6">
          <div className="flex items-center gap-2">
            <Link2 className="h-5 w-5 text-foreground" />
            <h2 className="font-display text-xl font-bold text-foreground">Create Share Link</h2>
          </div>
          <div className="mt-6 space-y-4">
            <div>
              <label htmlFor="link-type" className="text-sm text-muted-foreground">Link Type</label>
              <select
                id="link-type"
                value={linkType}
                onChange={(e) => {
                  const next = e.target.value as LinkType;
                  setLinkType(next);
                  if (next === "PRIVATE") setRestrictedOpen(true);
                }}
                className="mt-1 h-10 w-full rounded-md border border-input bg-background px-3 text-sm text-foreground"
              >
                <option value="PUBLIC">Public (Anyone with link)</option>
                <option value="PRIVATE">Private (Restricted to specific users)</option>
              </select>
            </div>
            <div>
              <label htmlFor="expiry-days" className="text-sm text-muted-foreground">Expires in (Days)</label>
              <Input
                id="expiry-days"
                type="number"
                min={1}
                placeholder="Never"
                value={expiryDays}
                onChange={(e) => setExpiryDays(e.target.value)}
                className="mt-1"
              />
            </div>
            {linkType === "PRIVATE" ? (
              <Button className="w-full" onClick={() => setRestrictedOpen(true)}>
                Open Restricted Setup
              </Button>
            ) : (
              <Button className="w-full" onClick={handleGeneratePublicLink} disabled={generatingPublic}>
                {generatingPublic ? "Generating..." : "Generate Link"}
              </Button>
            )}
          </div>
        </div>

        {/* Direct Email Invite */}
        <div className="glass-card rounded-xl p-6">
          <div className="flex items-center gap-2">
            <Mail className="h-5 w-5 text-foreground" />
            <h2 className="font-display text-xl font-bold text-foreground">Direct Invite</h2>
          </div>
          <p className="mt-2 text-sm text-muted-foreground">
            Search and select users by user ID to send a direct invite.
          </p>
          <div className="mt-4">
            <UserAutocomplete
              id="invite-search"
              selected={selectedRecipients}
              onAdd={addRecipient}
              onRemove={removeRecipient}
              placeholder="Search by name, handle or ID..."
            />
          </div>
          <Button
            variant="secondary"
            className="mt-4 w-full"
            disabled={selectedRecipients.length === 0 || sendingInvites}
            onClick={handleSendInvites}
          >
            {sendingInvites ? "Sending..." : "Send Invites"}
          </Button>
        </div>
      </div>

      {/* Restricted Link Dialog */}
      <Dialog
        open={restrictedOpen}
        onOpenChange={(open) => {
          setRestrictedOpen(open);
          if (!open) { setAllowlistedUsers([]); setRestrictedDays(""); }
        }}
      >
        <DialogContent className="restricted-dialog border-border/70 bg-card p-5 sm:max-w-[560px]">
          <DialogHeader className="text-left">
            <DialogTitle>Restricted Link Setup</DialogTitle>
            <DialogDescription>
              Search and select users who are allowed to use this link.
            </DialogDescription>
          </DialogHeader>

          <div className="restricted-body mt-2 space-y-4">
            <div>
              <label className="mb-1 block text-sm font-medium text-muted-foreground">Allowed Users</label>
              <UserAutocomplete
                id="allowlist-search"
                selected={allowlistedUsers}
                onAdd={addAllowlistUser}
                onRemove={removeAllowlistUser}
                placeholder="Search by name, handle or ID..."
              />
            </div>
            <div>
              <label htmlFor="restricted-days" className="mb-1 block text-sm font-medium text-muted-foreground">
                Expires in (Days)
              </label>
              <Input
                id="restricted-days"
                type="number"
                min={1}
                placeholder="Never"
                value={restrictedDays}
                onChange={(e) => setRestrictedDays(e.target.value)}
              />
            </div>
          </div>

          <DialogFooter className="restricted-actions mt-4 border-t border-border/60 pt-4">
            <Button variant="outline" onClick={() => setRestrictedOpen(false)}>Cancel</Button>
            <Button
              onClick={handleGenerateRestrictedLink}
              disabled={generatingRestricted}
            >
              {generatingRestricted ? "Generating..." : "Generate Private Link"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Active Share Links Table */}
      <div className="mt-8">
        <h2 className="font-display text-xl font-bold text-foreground">Active Share Links</h2>
        {linksLoading ? (
          <p className="mt-4 text-sm text-muted-foreground">Loading share links...</p>
        ) : generatedLinks.length === 0 ? (
          <p className="mt-4 text-sm text-muted-foreground">No share links generated yet.</p>
        ) : (
          <div className="mt-4 overflow-x-auto rounded-lg border border-border/60">
            <table className="w-full min-w-[600px] text-sm">
              <thead className="bg-muted/50 text-left text-muted-foreground">
                <tr>
                  <th className="px-4 py-3 font-medium">Token</th>
                  <th className="px-4 py-3 font-medium">Type</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3 text-right font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {generatedLinks.map((link) => (
                  <tr key={link.id} className="border-t border-border/50">
                    <td className="px-4 py-3 font-mono text-xs text-foreground">{link.token ? `${link.token.slice(0, 8)}...` : "N/A"}</td>
                    <td className="px-4 py-3 text-foreground">{link.type}</td>
                    <td className="px-4 py-3">
                      <span className={`rounded px-2 py-1 text-xs font-semibold ${link.active ? "bg-emerald-500/20 text-emerald-300" : "bg-red-500/20 text-red-300"}`}>
                        {link.active ? "ACTIVE" : "INACTIVE"}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-2">
                        <Button
                          variant="outline"
                          size="icon"
                          title="Copy link"
                          aria-label="Copy link"
                          onClick={() => handleCopyLink(link)}
                        >
                          <Copy className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="outline"
                          disabled={updatingLinkId === link.id}
                          className="gap-2"
                          onClick={() => handleToggleLink(link)}
                        >
                          {link.active ? <PowerOff className="h-4 w-4" /> : <Power className="h-4 w-4" />}
                          <span>{link.active ? "Deactivate" : "Activate"}</span>
                        </Button>
                        <Button
                          variant="outline-destructive"
                          size="icon"
                          disabled={updatingLinkId === link.id}
                          title="Delete link"
                          aria-label="Delete link"
                          onClick={() => handleDeleteLink(link.id)}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Enrolled Users Table */}
      <div className="mt-12">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between mb-4">
          <div>
            <h2 className="font-display text-xl font-bold text-foreground">Enrolled Students</h2>
            <p className="mt-1 text-sm text-muted-foreground">Monitor progress for users who have joined this course.</p>
          </div>

          <div className="relative w-full sm:w-64">
            <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search students..."
              value={enrollmentSearch}
              onChange={(e) => setEnrollmentSearch(e.target.value)}
              className="pl-9 h-9 bg-white text-black border border-border/50 focus:ring-1 focus:ring-primary/20 transition-all placeholder:text-muted-foreground/70"
            />
            {enrollmentSearch && (
              <button
                onClick={() => setEnrollmentSearch("")}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            )}
          </div>
        </div>

        {enrollmentsLoading ? (
          <p className="mt-4 text-sm text-muted-foreground">Loading enrollments...</p>
        ) : filteredEnrollments.length === 0 ? (
          <div className="mt-4 rounded-lg border border-border/60 bg-muted/20 p-8 text-center">
            <p className="text-muted-foreground">
              {enrollmentSearch ? "No students found matching your search." : "No users have enrolled in this course yet."}
            </p>
          </div>
        ) : (
          <div className="mt-4 overflow-x-auto rounded-lg border border-border/60">
            <table className="w-full min-w-[600px] text-sm">
              <thead className="bg-muted/50 text-left text-muted-foreground border-b border-border/50">
                <tr>
                  <th className="px-4 py-3 font-medium">Student Name</th>
                  <th className="px-4 py-3 font-medium">Progress</th>
                  <th className="px-4 py-3 font-medium">Enrolled Date</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3 text-right font-medium">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/50 bg-card">
                {filteredEnrollments.map((env) => (
                  <tr key={env.id} className="hover:bg-muted/20 transition-colors">
                    <td className="px-4 py-3">
                      <div className="flex flex-col">
                        <span className="text-foreground font-medium">{env.userName || `User ${env.userId}`}</span>
                        {env.userHandle ? (
                          <span className="text-xs text-muted-foreground">@{env.userHandle}</span>
                        ) : (
                          <span className="text-[10px] text-muted-foreground font-mono">ID: {env.userId}</span>
                        )}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <div className="w-24 h-2 rounded-full bg-muted overflow-hidden">
                          <div
                            className="h-full bg-primary"
                            style={{ width: `${Math.round(env.progressPercentage)}%` }}
                          />
                        </div>
                        <span className="text-xs text-muted-foreground">{Math.round(env.progressPercentage)}%</span>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">
                      {env.enrolledAt ? new Date(env.enrolledAt).toLocaleDateString() : 'Unknown'}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold ${env.status === 'ACTIVE' ? 'bg-success/10 text-success' : 'bg-muted text-muted-foreground'
                        }`}>
                        {env.status}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end">
                        <Button
                          variant="ghost"
                          size="sm"
                          className="gap-2 text-primary hover:text-primary hover:bg-primary/10"
                          onClick={() => handleOpenAnalytics(env.userId, env.userName || `User ${env.userId}`)}
                        >
                          <BarChart2 className="h-4 w-4" />
                          <span>View Report</span>
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {courseId && analyticsUser && (
        <CourseAnalyticsModal
          courseId={courseId}
          userId={analyticsUser.id}
          userName={analyticsUser.name}
          open={analyticsOpen}
          onOpenChange={setAnalyticsOpen}
        />
      )}

      {/* Course Leaderboard */}
      {courseId && (
        <CourseLeaderboard
          courseId={courseId}
          currentUserId={currentUser?.id ? Number(currentUser.id) : undefined}
        />
      )}
    </div>
  );
}
