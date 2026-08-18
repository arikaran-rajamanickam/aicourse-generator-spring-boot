import { useEffect, useState } from "react";
import { Bell, CheckCheck, BookOpen, Share2, Clock, Check, X, Loader2 } from "lucide-react";
import { Button } from "../components/ui/button";
import { cn } from "../lib/utils";
import {
  fetchSharedWithMeInvites,
  acceptInvite,
  declineInvite,
  markInviteRead,
  markAllInvitesRead,
} from "../services/inviteApi";
import { toast } from "sonner";

const tabs = ["All", "Pending", "Accepted", "Declined"] as const;
type Tab = (typeof tabs)[number];

interface Invite {
  id: string;
  courseTitle: string;
  inviterUsername: string;
  status: "PENDING" | "ACCEPTED" | "DECLINED";
  read: boolean;
  createdAt?: string;
}

function timeAgo(dateStr?: string): string {
  if (!dateStr) return "";
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return "just now";
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  const days = Math.floor(hrs / 24);
  return `${days}d ago`;
}

export default function Notifications() {
  const [invites, setInvites] = useState<Invite[]>([]);
  const [loading, setLoading] = useState(true);
  const [processingId, setProcessingId] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<Tab>("All");

  useEffect(() => {
    let mounted = true;
    async function load() {
      try {
        const data = await fetchSharedWithMeInvites();
        if (mounted) setInvites(Array.isArray(data) ? data : []);
      } catch {
        if (mounted) setInvites([]);
      } finally {
        if (mounted) setLoading(false);
      }
    }
    load();
    return () => { mounted = false; };
  }, []);

  const filtered = invites.filter((inv) => {
    if (activeTab === "All") return true;
    if (activeTab === "Pending") return inv.status === "PENDING";
    if (activeTab === "Accepted") return inv.status === "ACCEPTED";
    if (activeTab === "Declined") return inv.status === "DECLINED";
    return true;
  });

  const unreadCount = invites.filter((inv) => !inv.read).length;

  const handleAccept = async (inv: Invite) => {
    setProcessingId(inv.id);
    try {
      await acceptInvite(inv.id);
      await markInviteRead(inv.id);
      setInvites((prev) =>
        prev.map((item) =>
          item.id === inv.id ? { ...item, status: "ACCEPTED", read: true } : item
        )
      );
      toast.success(`Enrolled in "${inv.courseTitle}"! Check your Dashboard.`);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Failed to accept invite");
    } finally {
      setProcessingId(null);
    }
  };

  const handleDecline = async (inv: Invite) => {
    setProcessingId(inv.id);
    try {
      await declineInvite(inv.id);
      setInvites((prev) =>
        prev.map((item) =>
          item.id === inv.id ? { ...item, status: "DECLINED", read: true } : item
        )
      );
      toast.success("Invite declined");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Failed to decline invite");
    } finally {
      setProcessingId(null);
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await markAllInvitesRead();
      setInvites((prev) => prev.map((item) => ({ ...item, read: true })));
      toast.success("All notifications marked as read");
    } catch {
      toast.error("Failed to mark all as read");
    }
  };

  const handleMarkRead = async (inv: Invite) => {
    if (inv.read) return;
    try {
      await markInviteRead(inv.id);
      setInvites((prev) =>
        prev.map((item) => (item.id === inv.id ? { ...item, read: true } : item))
      );
    } catch {
      // silent — non-critical
    }
  };

  return (
    <div className="animate-fade-in">
      {/* Header */}
      <div className="gradient-header px-8 pb-8 pt-8">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="font-display text-3xl font-bold text-foreground">Notifications</h1>
            <p className="mt-1.5 text-sm text-muted-foreground">
              {loading
                ? "Loading..."
                : unreadCount > 0
                ? `You have ${unreadCount} unread notification${unreadCount > 1 ? "s" : ""}`
                : "You're all caught up!"}
            </p>
          </div>
          {unreadCount > 0 && (
            <Button variant="outline" className="gap-2" onClick={handleMarkAllRead}>
              <CheckCheck className="h-4 w-4" />
              Mark all as read
            </Button>
          )}
        </div>

        {/* Tabs */}
        <div className="mt-6 flex gap-1 rounded-lg bg-secondary/50 p-1 w-fit">
          {tabs.map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={cn(
                "rounded-md px-4 py-2 text-sm font-medium transition-all duration-200",
                activeTab === tab
                  ? "bg-primary text-primary-foreground shadow-sm"
                  : "text-muted-foreground hover:text-foreground"
              )}
            >
              {tab}
              {tab === "Pending" && invites.filter((i) => i.status === "PENDING").length > 0 && (
                <span className="ml-1.5 rounded-full bg-primary/20 px-1.5 py-0.5 text-xs font-semibold text-primary-foreground">
                  {invites.filter((i) => i.status === "PENDING").length}
                </span>
              )}
            </button>
          ))}
        </div>
      </div>

      {/* List */}
      <div className="p-8 space-y-3">
        {loading ? (
          <div className="flex flex-col items-center justify-center py-20 gap-3 text-muted-foreground">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
            <p className="text-sm">Loading notifications...</p>
          </div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-center">
            <Bell className="h-12 w-12 text-muted-foreground/40 mb-4" />
            <p className="text-lg font-medium text-muted-foreground">
              {activeTab === "All" ? "No notifications yet" : `No ${activeTab.toLowerCase()} invites`}
            </p>
            <p className="text-sm text-muted-foreground/70 mt-1">
              {activeTab === "All"
                ? "Course invites from other users will appear here."
                : "Switch tabs to see other notifications."}
            </p>
          </div>
        ) : (
          filtered.map((inv) => {
            const isPending = inv.status === "PENDING";
            const isAccepted = inv.status === "ACCEPTED";
            const isProcessing = processingId === inv.id;

            return (
              <div
                key={inv.id}
                onClick={() => handleMarkRead(inv)}
                className={cn(
                  "glass-card w-full flex items-start gap-4 rounded-xl p-5 transition-all duration-200 hover:border-primary/30 hover:shadow-lg hover:shadow-primary/5 cursor-pointer",
                  !inv.read && "border-l-2 border-l-primary bg-primary/[0.03]"
                )}
              >
                {/* Icon */}
                <div
                  className={cn(
                    "flex h-10 w-10 shrink-0 items-center justify-center rounded-lg",
                    isPending ? "bg-primary/15 text-primary" : "bg-emerald-500/15 text-emerald-500"
                  )}
                >
                  {isPending ? (
                    <Share2 className="h-5 w-5" />
                  ) : isAccepted ? (
                    <BookOpen className="h-5 w-5" />
                  ) : (
                    <X className="h-5 w-5 text-red-400" />
                  )}
                </div>

                {/* Content */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="font-display font-semibold text-foreground">
                      {isPending ? "Course Invite" : isAccepted ? "Enrolled" : "Invite Declined"}
                    </span>
                    {!inv.read && <span className="h-2 w-2 rounded-full bg-primary shrink-0" />}
                    {!isPending && (
                      <span
                        className={cn(
                          "text-xs font-medium px-2 py-0.5 rounded-full border",
                          isAccepted
                            ? "bg-green-500/10 text-green-400 border-green-500/20"
                            : "bg-red-500/10 text-red-400 border-red-500/20"
                        )}
                      >
                        {isAccepted ? "Accepted" : "Declined"}
                      </span>
                    )}
                  </div>
                  <p className="mt-1 text-sm text-muted-foreground leading-relaxed">
                    <span className="font-medium text-foreground">{inv.inviterUsername}</span> invited
                    you to join{" "}
                    <span className="font-semibold text-foreground">"{inv.courseTitle}"</span>
                  </p>

                  {isPending && (
                    <div className="mt-3 flex items-center gap-2">
                      <Button
                        size="sm"
                        variant="gradient"
                        className="gap-1.5 h-8"
                        disabled={isProcessing}
                        onClick={(e) => { e.stopPropagation(); handleAccept(inv); }}
                      >
                        {isProcessing ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Check className="h-3.5 w-3.5" />}
                        Accept
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        className="gap-1.5 h-8 text-muted-foreground hover:text-foreground"
                        disabled={isProcessing}
                        onClick={(e) => { e.stopPropagation(); handleDecline(inv); }}
                      >
                        <X className="h-3.5 w-3.5" />
                        Decline
                      </Button>
                    </div>
                  )}
                </div>

                {/* Time */}
                {inv.createdAt && (
                  <div className="flex items-center gap-1.5 text-xs text-muted-foreground shrink-0 pt-0.5">
                    <Clock className="h-3 w-3" />
                    {timeAgo(inv.createdAt)}
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
