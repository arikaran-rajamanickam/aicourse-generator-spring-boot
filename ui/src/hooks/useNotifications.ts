import { useCallback, useEffect, useRef, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  fetchSharedWithMeInvites,
  fetchInviteSummary,
  acceptInvite,
  declineInvite,
  markInviteRead,
  markAllInvitesRead,
} from "../services/inviteApi";
import { getProfile } from "../services/aboutApi";
import { useAuth } from "@/auth/AuthContext";
import { toast } from "sonner";

// ─── Unified Notification Type ─────────────────────────────────────
export type NotificationType =
  | "invite"      // course share invite (real: inviteApi)
  | "achievement" // derived from user stats milestones
  | "system";     // welcome / first-time events

export interface AppNotification {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  read: boolean;
  createdAt?: string;
  // Invite-specific (for accept/decline actions)
  inviteId?: string;
  inviteStatus?: "PENDING" | "ACCEPTED" | "DECLINED";
  courseId?: string;
}

// ─── Milestone derivation from profile stats ────────────────────────
function deriveAchievements(stats: any): AppNotification[] {
  if (!stats) return [];
  const achievements: AppNotification[] = [];

  if (stats.totalPoints >= 1) {
    achievements.push({
      id: "ach-points",
      type: "achievement",
      title: "Points Earned",
      message: `You've earned ${stats.totalPoints} total points. Keep learning!`,
      read: true,
      createdAt: undefined,
    });
  }
  if (stats.lessonsCompleted >= 1) {
    achievements.push({
      id: "ach-lessons",
      type: "achievement",
      title: "Lessons Completed",
      message: `You've completed ${stats.lessonsCompleted} lesson${stats.lessonsCompleted > 1 ? "s" : ""}. Great progress!`,
      read: true,
      createdAt: undefined,
    });
  }
  if (stats.coursesCompleted >= 1) {
    achievements.push({
      id: "ach-courses",
      type: "achievement",
      title: "Course Completed",
      message: `You've completed ${stats.coursesCompleted} course${stats.coursesCompleted > 1 ? "s" : ""}. 🎉`,
      read: true,
      createdAt: undefined,
    });
  }
  if (stats.currentStreak >= 3) {
    achievements.push({
      id: "ach-streak",
      type: "achievement",
      title: "On a Streak! 🔥",
      message: `You're on a ${stats.currentStreak}-day learning streak. Keep it going!`,
      read: true,
      createdAt: undefined,
    });
  }

  return achievements;
}

// ─── Invite → AppNotification mapper ───────────────────────────────
function mapInviteToNotification(inv: any): AppNotification {
  const status: "PENDING" | "ACCEPTED" | "DECLINED" =
    inv.inviteStatus ?? "PENDING";
  const isPending = status === "PENDING";
  return {
    id: `invite-${inv.id ?? inv.inviteId}`,
    type: "invite",
    title: isPending ? "Course Invite" : status === "ACCEPTED" ? "Invite Accepted" : "Invite Declined",
    message: `${inv.invitedByName ?? inv.inviterUsername ?? inv.sharedBy ?? "Someone"} invited you to join "${inv.courseName ?? inv.courseTitle ?? inv.title ?? "a course"}"`,
    read: inv.isRead ?? inv.read ?? !isPending,
    createdAt: inv.createdAt ?? inv.enrolledAt,
    inviteId: String(inv.id ?? inv.inviteId),
    inviteStatus: status,
    courseId: inv.courseId,
  };
}

// ─── Hook ──────────────────────────────────────────────────────────
export function useNotifications() {
  const { user, token } = useAuth();
  const queryClient = useQueryClient();
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [processingId, setProcessingId] = useState<string | null>(null);

  // Smart Polling (Phase 1) - Long interval, pauses when tab is blurred
  const { data: sharedWithMe = [], refetch: refetchInvites } = useQuery({
    queryKey: ["courses-shared-with-me"],
    queryFn: () => fetchSharedWithMeInvites(),
    refetchInterval: false, // Rely entirely on SSE and window focus
    enabled: !!user,
  });

  const loadAll = useCallback(async () => {
    try {
      const profileRes = await getProfile();
      const stats = (profileRes?.data ?? profileRes)?.stats;
      const achievements = deriveAchievements(stats);

      const invites: AppNotification[] = Array.isArray(sharedWithMe)
        ? sharedWithMe.map(mapInviteToNotification)
        : [];

      setNotifications([...invites, ...achievements]);
    } catch {
      // keep previous state
    } finally {
      setLoading(false);
    }
  }, [sharedWithMe]);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  // Real-time Push (Phase 3) - SSE Connection
  useEffect(() => {
    if (!user?.id) return;

    const sseUrl = `/api/notifications/stream/${user.id}?token=${token}`;
    const eventSource = new EventSource(sseUrl);

    eventSource.onmessage = (event) => {
      console.log("Real-time notification received:", event);
      // When any notification event happens, invalidate the relevant queries
      queryClient.invalidateQueries({ queryKey: ["courses-shared-with-me"] });
      queryClient.invalidateQueries({ queryKey: ["courses-shared-by-me"] });
      refetchInvites();
      
      try {
        const data = JSON.parse(event.data);
        if (data.type === "INVITE_RECEIVED") {
          toast.info(data.message || "New course invite received!", {
            description: "Check your notifications for details.",
          });
        }
      } catch (e) {
        // Fallback if not JSON
      }
    };

    eventSource.addEventListener("INVITE_RECEIVED", (event: any) => {
      queryClient.invalidateQueries({ queryKey: ["courses-shared-with-me"] });
      refetchInvites();
      try {
        const data = JSON.parse(event.data);
        toast.info(data.message || "New course invite received!");
      } catch {}
    });

    eventSource.onerror = () => {
      console.warn("SSE connection lost. Reconnecting...");
      eventSource.close();
    };

    return () => {
      eventSource.close();
    };
  }, [user?.id, queryClient, refetchInvites]);

  // Recompute unread count
  useEffect(() => {
    setUnreadCount(notifications.filter((n) => !n.read).length);
  }, [notifications]);

  // ─── Actions ─────────────────────────────────────────────────────

  const accept = useCallback(async (notif: AppNotification) => {
    if (!notif.inviteId) return;
    setProcessingId(notif.id);
    try {
      await acceptInvite(notif.inviteId);
      await markInviteRead(notif.inviteId);
      queryClient.invalidateQueries({ queryKey: ["courses-shared-with-me"] });
      setNotifications((prev) =>
        prev.map((n) =>
          n.id === notif.id ? { ...n, inviteStatus: "ACCEPTED", read: true } : n
        )
      );
    } finally {
      setProcessingId(null);
    }
  }, [queryClient]);

  const decline = useCallback(async (notif: AppNotification) => {
    if (!notif.inviteId) return;
    setProcessingId(notif.id);
    try {
      await declineInvite(notif.inviteId);
      queryClient.invalidateQueries({ queryKey: ["courses-shared-with-me"] });
      setNotifications((prev) =>
        prev.map((n) =>
          n.id === notif.id ? { ...n, inviteStatus: "DECLINED", read: true } : n
        )
      );
    } finally {
      setProcessingId(null);
    }
  }, [queryClient]);

  const markRead = useCallback(async (notif: AppNotification) => {
    if (notif.read) return;
    setNotifications((prev) =>
      prev.map((n) => (n.id === notif.id ? { ...n, read: true } : n))
    );
    if (notif.inviteId) {
      try {
        await markInviteRead(notif.inviteId);
      } catch {
        setNotifications((prev) =>
          prev.map((n) => (n.id === notif.id ? { ...n, read: false } : n))
        );
      }
    }
  }, []);

  const markAllRead = useCallback(async () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
    try {
      await markAllInvitesRead();
    } catch {}
  }, []);

  const refresh = useCallback(() => {
    setLoading(true);
    loadAll();
    refetchInvites();
  }, [loadAll, refetchInvites]);

  return {
    notifications,
    unreadCount,
    loading,
    processingId,
    accept,
    decline,
    markRead,
    markAllRead,
    refresh,
  };
}
