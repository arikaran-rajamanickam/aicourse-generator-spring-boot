import { useRef, useEffect } from "react";
import { Share2, Trophy, Bell, Check, X, Loader2, Clock, CheckCheck, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { type AppNotification, type NotificationType, useNotifications } from "@/hooks/useNotifications";

// ─── Config ───────────────────────────────────────────────────────
export const notificationTabs = ["All", "Invites", "Achievements"] as const;
export type NotificationTab = (typeof notificationTabs)[number];

const typeIcon: Record<NotificationType, typeof Bell> = {
  invite: Share2,
  achievement: Trophy,
  system: Bell,
};

const typeColor: Record<NotificationType, string> = {
  invite: "bg-primary/15 text-primary",
  achievement: "bg-amber-500/15 text-amber-500",
  system: "bg-muted text-muted-foreground",
};

function timeAgo(dateStr?: string): string {
  if (!dateStr) return "";
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return "just now";
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  return `${Math.floor(hrs / 24)}d ago`;
}

// ─── Panel props ──────────────────────────────────────────────────
interface NotificationPanelProps {
  open: boolean;
  onClose: () => void;
  activeTab: NotificationTab;
  onTabChange: (tab: NotificationTab) => void;
}

export default function NotificationPanel({
  open,
  onClose,
  activeTab,
  onTabChange,
}: NotificationPanelProps) {
  const {
    notifications,
    unreadCount,
    loading,
    processingId,
    accept,
    decline,
    markRead,
    markAllRead,
    refresh,
  } = useNotifications();

  // Close on Escape
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    if (open) window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [open, onClose]);

  // Lock body scroll when open
  useEffect(() => {
    document.body.style.overflow = open ? "hidden" : "";
    return () => { document.body.style.overflow = ""; };
  }, [open]);

  const filtered = notifications.filter((n) => {
    if (activeTab === "All") return true;
    if (activeTab === "Invites") return n.type === "invite";
    if (activeTab === "Achievements") return n.type === "achievement";
    return true;
  });

  const inviteCount = notifications.filter(
    (n) => n.type === "invite" && n.inviteStatus === "PENDING"
  ).length;

  return (
    <>
      {/* Backdrop */}
      <div
        onClick={onClose}
        className={cn(
          "fixed inset-0 z-[100] bg-black/40 transition-opacity duration-300",
          open ? "opacity-100 pointer-events-auto" : "opacity-0 pointer-events-none"
        )}
      />

      {/* Slide-over panel */}
      <div
        className={cn(
          "fixed right-0 top-0 z-[110] h-full w-full max-w-[420px] flex flex-col bg-card border-l border-border shadow-2xl transition-transform duration-300 ease-in-out",
          open ? "translate-x-0" : "translate-x-full"
        )}
      >
        {/* Header */}
        <div className="gradient-header px-5 pt-5 pb-4 flex-shrink-0">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="font-display text-xl font-bold text-foreground">Notifications</h2>
              <p className="mt-0.5 text-xs text-muted-foreground">
                {loading
                  ? "Loading..."
                  : unreadCount > 0
                  ? `${unreadCount} unread`
                  : "All caught up!"}
              </p>
            </div>
            <div className="flex items-center gap-1">
              {unreadCount > 0 && (
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8 text-muted-foreground hover:text-foreground"
                  title="Mark all as read"
                  onClick={markAllRead}
                >
                  <CheckCheck className="h-4 w-4" />
                </Button>
              )}
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8 text-muted-foreground hover:text-foreground"
                title="Refresh"
                onClick={refresh}
              >
                <RefreshCw className={cn("h-4 w-4", loading && "animate-spin")} />
              </Button>
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8 text-muted-foreground hover:text-foreground"
                onClick={onClose}
              >
                <X className="h-4 w-4" />
              </Button>
            </div>
          </div>

          {/* Tabs */}
          <div className="mt-3 flex gap-1 rounded-lg bg-secondary/50 p-1">
            {notificationTabs.map((tab) => (
              <button
                key={tab}
                onClick={() => onTabChange(tab)}
                className={cn(
                  "flex-1 rounded-md px-2 py-1.5 text-xs font-medium transition-all duration-200 relative",
                  activeTab === tab
                    ? "bg-primary text-primary-foreground shadow-sm"
                    : "text-muted-foreground hover:text-foreground"
                )}
              >
                {tab}
                {tab === "Invites" && inviteCount > 0 && (
                  <span className="ml-1 inline-flex h-4 w-4 items-center justify-center rounded-full bg-primary-foreground/20 text-[10px] font-bold">
                    {inviteCount}
                  </span>
                )}
              </button>
            ))}
          </div>
        </div>

        {/* Body (scrollable) */}
        <div className="flex-1 overflow-y-auto p-4 space-y-2">
          {loading ? (
            <div className="flex flex-col items-center justify-center py-16 gap-3 text-muted-foreground">
              <Loader2 className="h-7 w-7 animate-spin text-primary" />
              <p className="text-sm">Loading notifications...</p>
            </div>
          ) : filtered.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 text-center">
              <Bell className="h-10 w-10 text-muted-foreground/30 mb-3" />
              <p className="font-medium text-muted-foreground">No notifications</p>
              <p className="text-xs text-muted-foreground/60 mt-1">
                {activeTab === "All"
                  ? "Invites and achievements will appear here."
                  : `No ${activeTab.toLowerCase()} yet.`}
              </p>
            </div>
          ) : (
            filtered.map((notif) => {
              const Icon = typeIcon[notif.type];
              const isPendingInvite =
                notif.type === "invite" && notif.inviteStatus === "PENDING";
              const isProcessing = processingId === notif.id;

              return (
                <div
                  key={notif.id}
                  onClick={() => markRead(notif)}
                  className={cn(
                    "glass-card w-full flex items-start gap-3 rounded-xl p-4 text-left cursor-pointer transition-all duration-200 hover:border-primary/30",
                    !notif.read && "border-l-2 border-l-primary bg-primary/[0.04]"
                  )}
                >
                  {/* Icon */}
                  <div
                    className={cn(
                      "flex h-9 w-9 shrink-0 items-center justify-center rounded-lg",
                      typeColor[notif.type]
                    )}
                  >
                    <Icon className="h-4.5 w-4.5" />
                  </div>

                  {/* Content */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-1.5 flex-wrap">
                      <span className="font-display text-sm font-semibold text-foreground">
                        {notif.title}
                      </span>
                      {!notif.read && (
                        <span className="h-1.5 w-1.5 rounded-full bg-primary shrink-0" />
                      )}
                    </div>
                    <p className="mt-0.5 text-xs text-muted-foreground leading-relaxed">
                      {notif.message}
                    </p>

                    {/* Invite actions */}
                    {isPendingInvite && (
                      <div className="mt-2.5 flex gap-2">
                        <Button
                          size="sm"
                          variant="gradient"
                          className="h-7 gap-1 text-xs px-3"
                          disabled={isProcessing}
                          onClick={(e) => {
                            e.stopPropagation();
                            accept(notif);
                          }}
                        >
                          {isProcessing ? (
                            <Loader2 className="h-3 w-3 animate-spin" />
                          ) : (
                            <Check className="h-3 w-3" />
                          )}
                          Accept
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          className="h-7 gap-1 text-xs px-3 text-muted-foreground"
                          disabled={isProcessing}
                          onClick={(e) => {
                            e.stopPropagation();
                            decline(notif);
                          }}
                        >
                          <X className="h-3 w-3" />
                          Decline
                        </Button>
                      </div>
                    )}

                    {/* Status badge for non-pending invites */}
                    {notif.type === "invite" && !isPendingInvite && (
                      <span
                        className={cn(
                          "mt-2 inline-block text-[10px] font-semibold px-2 py-0.5 rounded-full border",
                          notif.inviteStatus === "ACCEPTED"
                            ? "bg-green-500/10 text-green-400 border-green-500/20"
                            : "bg-red-500/10 text-red-400 border-red-500/20"
                        )}
                      >
                        {notif.inviteStatus === "ACCEPTED" ? "Enrolled" : "Declined"}
                      </span>
                    )}
                  </div>

                  {/* Time */}
                  {notif.createdAt && (
                    <span className="flex items-center gap-1 text-[10px] text-muted-foreground shrink-0 pt-0.5">
                      <Clock className="h-2.5 w-2.5" />
                      {timeAgo(notif.createdAt)}
                    </span>
                  )}
                </div>
              );
            })
          )}
        </div>
      </div>
    </>
  );
}
