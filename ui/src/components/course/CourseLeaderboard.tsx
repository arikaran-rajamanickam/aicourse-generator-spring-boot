import { useEffect, useState } from "react";
import { Trophy, Medal, Clock, BookOpen, Zap, AlertTriangle, Loader2, RefreshCw } from "lucide-react";
import { getCourseLeaderboard } from "@/services/progressApi";
import { cn } from "@/lib/utils";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";

type LeaderboardEntry = {
  userId: number;
  username: string;
  displayName?: string;
  handle?: string;
  rank: number;
  score: number;
  totalProgress: number;
  lessonsCompleted: number;
  quizAccuracy: number;
  totalTimeSeconds: number;
  flaggedCount: number;
};

interface CourseLeaderboardProps {
  courseId: string;
  currentUserId?: number;
}

const MEDAL = [
  { label: "🥇", bg: "from-amber-400/20 to-amber-600/10", border: "border-amber-400/40", text: "text-amber-400" },
  { label: "🥈", bg: "from-slate-400/20 to-slate-500/10", border: "border-slate-400/40", text: "text-slate-400" },
  { label: "🥉", bg: "from-orange-600/20 to-orange-700/10", border: "border-orange-500/40", text: "text-orange-500" },
];

function formatTime(sec: number) {
  if (!sec || sec <= 0) return "0s";
  const h = Math.floor(sec / 3600);
  const m = Math.floor((sec % 3600) / 60);
  const s = sec % 60;
  if (h > 0) return `${h}h ${m}m`;
  if (m > 0) return `${m}m ${s}s`;
  return `${s}s`;
}

export default function CourseLeaderboard({ courseId, currentUserId }: CourseLeaderboardProps) {
  const [entries, setEntries] = useState<LeaderboardEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [hoveredRow, setHoveredRow] = useState<number | null>(null);

  const load = async () => {
    setLoading(true);
    try {
      const data = await getCourseLeaderboard(courseId);
      setEntries(data || []);
    } catch {
      toast.error("Failed to load leaderboard");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (courseId) load();
  }, [courseId]);

  return (
    <div className="mt-12">
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <div>
          <h2 className="font-display text-xl font-bold text-foreground flex items-center gap-2">
            <Trophy className="h-5 w-5 text-amber-400" />
            Course Leaderboard
          </h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Ranked by composite score — lessons, quizzes, and engagement.
          </p>
        </div>
        <Button
          variant="ghost"
          size="sm"
          onClick={load}
          disabled={loading}
          className="gap-2 text-muted-foreground hover:text-foreground"
        >
          <RefreshCw className={cn("h-3.5 w-3.5", loading && "animate-spin")} />
          Refresh
        </Button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-16 text-muted-foreground">
          <Loader2 className="h-5 w-5 animate-spin mr-2" /> Loading leaderboard...
        </div>
      ) : entries.length === 0 ? (
        <div className="rounded-lg border border-border/60 bg-muted/20 p-10 text-center">
          <Trophy className="mx-auto mb-3 h-8 w-8 text-muted-foreground/40" />
          <p className="text-muted-foreground">No enrolled students yet. The leaderboard will appear when students join.</p>
        </div>
      ) : (
        <div className="space-y-2">
          {entries.map((entry) => {
            const medal = MEDAL[entry.rank - 1];
            const isCurrentUser = currentUserId != null && entry.userId === currentUserId;
            const isTop3 = entry.rank <= 3;

            return (
              <div
                key={entry.userId}
                onMouseEnter={() => setHoveredRow(entry.userId)}
                onMouseLeave={() => setHoveredRow(null)}
                className={cn(
                  "relative rounded-xl border px-5 py-4 transition-all duration-200 cursor-default",
                  isCurrentUser
                    ? "border-primary/40 bg-primary/5 ring-1 ring-primary/20"
                    : isTop3
                    ? `border bg-gradient-to-r ${medal.bg} ${medal.border}`
                    : "border-border/50 bg-card hover:bg-muted/20"
                )}
              >
                <div className="flex items-center gap-4">
                  {/* Rank */}
                  <div className="w-8 shrink-0 text-center">
                    {isTop3 ? (
                      <span className="text-xl">{medal.label}</span>
                    ) : (
                      <span className="text-sm font-bold text-muted-foreground">#{entry.rank}</span>
                    )}
                  </div>

                  {/* Avatar + Name */}
                  <div className="flex items-center gap-3 flex-1 min-w-0">
                    <div className={cn(
                      "flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-sm font-bold",
                      isCurrentUser ? "gradient-primary text-primary-foreground" : "bg-muted text-foreground"
                    )}>
                      {(entry.displayName ?? entry.username)?.[0]?.toUpperCase() ?? "?"}
                    </div>
                    <div className="min-w-0">
                      <p className="text-sm font-semibold text-foreground truncate">
                        {entry.displayName ?? entry.username}
                        {isCurrentUser && <span className="ml-2 text-xs font-normal text-primary">(You)</span>}
                      </p>
                      {entry.handle && (
                        <p className="text-xs text-muted-foreground">@{entry.handle}</p>
                      )}
                      {/* Score bar */}
                      <div className="mt-1 flex items-center gap-2">
                        <div className="w-24 h-1.5 rounded-full bg-muted overflow-hidden">
                          <div
                            className={cn(
                              "h-full rounded-full transition-all duration-500",
                              isTop3 ? "bg-amber-400" : isCurrentUser ? "bg-primary" : "bg-muted-foreground/40"
                            )}
                            style={{ width: `${Math.min(100, (entry.score / 1000) * 100)}%` }}
                          />
                        </div>
                        <span className={cn("text-xs font-bold tabular-nums", isTop3 ? medal.text : "text-muted-foreground")}>
                          {entry.score.toFixed(1)} pts
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Stats — desktop */}
                  <div className="hidden sm:flex items-center gap-5 text-xs text-muted-foreground">
                    <span className="flex items-center gap-1">
                      <BookOpen className="h-3.5 w-3.5 text-primary/70" />
                      {entry.lessonsCompleted} lessons
                    </span>
                    <span className="flex items-center gap-1">
                      <Zap className="h-3.5 w-3.5 text-amber-400/80" />
                      {entry.quizAccuracy.toFixed(0)}% quiz
                    </span>
                    <span className="flex items-center gap-1">
                      <Clock className="h-3.5 w-3.5 text-sky-400/80" />
                      {formatTime(entry.totalTimeSeconds)}
                    </span>
                    {entry.flaggedCount > 0 && (
                      <span className="flex items-center gap-1 text-destructive">
                        <AlertTriangle className="h-3.5 w-3.5" />
                        {entry.flaggedCount} flagged
                      </span>
                    )}
                  </div>
                </div>

                {/* Hover breakdown tooltip */}
                {hoveredRow === entry.userId && (
                  <div className="absolute right-4 top-4 z-10 rounded-lg border border-border/60 bg-popover p-3 shadow-xl text-xs min-w-[190px] animate-fade-in">
                    <p className="font-semibold text-foreground mb-2 text-[11px] uppercase tracking-wide">Score Breakdown</p>
                    <div className="space-y-1 text-muted-foreground">
                      <div className="flex justify-between">
                        <span>Lessons</span>
                        <span className="font-mono text-foreground">{(entry.totalProgress / 100 * 500).toFixed(0)} / 500</span>
                      </div>
                      <div className="flex justify-between">
                        <span>Quiz accuracy</span>
                        <span className="font-mono text-foreground">{(entry.quizAccuracy / 100 * 300).toFixed(0)} / 300</span>
                      </div>
                      <div className="flex justify-between">
                        <span>Engagement</span>
                        <span className="font-mono text-foreground">{Math.max(0, entry.score - (entry.totalProgress / 100 * 500) - (entry.quizAccuracy / 100 * 300)).toFixed(0)} / 200</span>
                      </div>
                      {entry.flaggedCount > 0 && (
                        <div className="flex justify-between text-destructive">
                          <span>Penalty ({entry.flaggedCount} flags)</span>
                          <span className="font-mono">−{entry.flaggedCount * 50}</span>
                        </div>
                      )}
                      <div className="mt-1 pt-1 border-t border-border/40 flex justify-between font-semibold text-foreground">
                        <span>Total</span>
                        <span className="font-mono">{entry.score.toFixed(1)}</span>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
