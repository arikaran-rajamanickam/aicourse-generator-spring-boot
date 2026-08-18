import { useEffect, useState } from "react";
import { Trophy, Medal, Star, TrendingUp } from "lucide-react";
import { getGlobalLeaderboard, getMyRank } from "../services/leaderboardApi";
import { cn } from "../lib/utils";

export default function Leaderboard() {
  const [leaders, setLeaders] = useState<any[]>([]);
  const [myRank, setMyRank] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;

    async function load() {
      setLoading(true);
      setError("");
      try {
        const [board, rank] = await Promise.allSettled([
          getGlobalLeaderboard(0, 20),
          getMyRank(),
        ]);

        if (!mounted) return;

        if (board.status === "fulfilled") {
          const raw = board.value;
          const list =
            raw?.data?.content ??
            raw?.data ??
            raw?.content ??
            (Array.isArray(raw) ? raw : []);
          setLeaders(list);
        } else {
          setError("Failed to load leaderboard data.");
        }

        if (rank.status === "fulfilled") {
          setMyRank(rank.value?.data ?? rank.value);
        }
      } catch (err) {
        if (mounted) setError("Failed to load leaderboard.");
      } finally {
        if (mounted) setLoading(false);
      }
    }

    load();
    return () => { mounted = false; };
  }, []);

  const rankIcon = (rank: number) => {
    if (rank === 1) return <Trophy className="h-5 w-5 text-yellow-500 dark:text-yellow-400" />;
    if (rank === 2) return <Medal className="h-5 w-5 text-slate-500 dark:text-slate-300" />;
    if (rank === 3) return <Medal className="h-5 w-5 text-amber-700 dark:text-amber-600" />;
    return <span className="font-display font-bold text-muted-foreground">#{rank}</span>;
  };

  return (
    <div className="animate-fade-in">
      <div className="gradient-header px-8 pb-8 pt-8">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl gradient-primary">
            <Trophy className="h-6 w-6 text-primary-foreground" />
          </div>
          <div>
            <h1 className="font-display text-3xl font-bold text-foreground">Leaderboard</h1>
            <p className="mt-0.5 text-sm text-muted-foreground">Top learners by points earned.</p>
          </div>
        </div>

        {/* My Rank Banner */}
        {myRank && (
          <div className="mt-6 flex items-center gap-4 glass-card rounded-xl p-4 border border-primary/20">
            <div className="flex h-10 w-10 items-center justify-center rounded-full gradient-primary">
              <Star className="h-5 w-5 text-primary-foreground" />
            </div>
            <div>
              <p className="text-xs text-muted-foreground uppercase tracking-wider font-medium">Your Rank</p>
              <p className="font-display font-bold text-foreground">
                {myRank.rank && myRank.rank > 0 ? `#${myRank.rank}` : "Unranked"}{" "}
                <span className="text-muted-foreground font-normal text-sm">
                  · {myRank.totalPoints ?? myRank.points ?? 0} pts
                </span>
              </p>
              {myRank.handle && (
                <p className="text-xs text-muted-foreground">@{myRank.handle}</p>
              )}
            </div>
            <div className="ml-auto flex items-center gap-1.5 text-sm text-primary">
              <TrendingUp className="h-4 w-4" />
              <span className="font-medium">{myRank.weeklyPoints ?? 0} this week</span>
            </div>
          </div>
        )}
      </div>

      <div className="p-8">
        {loading ? (
          <div className="glass-card rounded-xl overflow-hidden">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="flex items-center gap-4 px-6 py-4 border-b border-border/50">
                <div className="h-8 w-8 rounded-full bg-secondary/80 animate-pulse" />
                <div className="flex-1 h-4 rounded bg-secondary/80 animate-pulse" />
                <div className="h-4 w-16 rounded bg-secondary/80 animate-pulse" />
              </div>
            ))}
          </div>
        ) : error ? (
          <div className="glass-card rounded-xl p-8 text-center">
            <p className="text-muted-foreground">{error}</p>
          </div>
        ) : leaders.length === 0 ? (
          <div className="glass-card rounded-xl p-8 text-center">
            <Trophy className="h-12 w-12 text-muted-foreground/30 mx-auto mb-3" />
            <p className="text-muted-foreground">No leaderboard data yet. Be the first!</p>
          </div>
        ) : (
          <div className="glass-card rounded-xl overflow-hidden">
            <table className="w-full">
              <thead>
                <tr className="border-b border-border text-left">
                  <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-muted-foreground">Rank</th>
                  <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-muted-foreground">User</th>
                  <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-muted-foreground">Courses</th>
                  <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-muted-foreground">Streak</th>
                  <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-muted-foreground text-right">Points</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/50">
                {leaders.map((l, i) => {
                  const position = l.rank ?? i + 1;
                  const displayName = l.displayName ?? l.username ?? "Unknown";
                  const handle = l.handle ?? l.userHandle ?? "";
                  const courses = l.courseCount ?? 0;
                  const streak = l.currentStreak ?? 0;
                  const points = l.totalPoints ?? 0;

                  return (
                    <tr
                      key={l.id ?? i}
                      className={cn(
                        "hover:bg-secondary/50 transition-colors",
                        position <= 3 && "bg-primary/5"
                      )}
                    >
                      <td className="px-6 py-4">
                        <div className="flex items-center">{rankIcon(position)}</div>
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <div className={cn(
                            "flex h-9 w-9 items-center justify-center rounded-full text-xs font-bold",
                            position === 1 ? "bg-yellow-500/20 text-yellow-600 dark:text-yellow-400" :
                            position === 2 ? "bg-slate-500/20 text-slate-600 dark:text-slate-300" :
                            position === 3 ? "bg-amber-600/20 text-amber-700 dark:text-amber-500" :
                            "bg-primary/20 text-primary"
                          )}>
                            {displayName[0]?.toUpperCase() ?? "?"}
                          </div>
                          <div className="flex flex-col">
                            <span className="font-medium text-foreground">{displayName}</span>
                            {handle && <span className="text-xs text-muted-foreground">@{handle}</span>}
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4 text-muted-foreground">{courses}</td>
                      <td className="px-6 py-4 text-muted-foreground">{streak > 0 ? `🔥 ${streak}d` : "0"}</td>
                      <td className="px-6 py-4 text-right font-display font-bold text-primary">
                        {points.toLocaleString()}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

