import { useMemo, useState, useEffect } from "react";
import { Link } from "react-router-dom";
import {
  BookOpen,
  BarChart3,
  Trash2,
  Sparkles,
  Trophy,
  ToggleLeft,
  ToggleRight,
  TrendingUp,
  Clock,
  ArrowRight,
  TrendingDown,
  Award,
  Flame,
  Activity
} from "lucide-react";
import { 
  AreaChart, 
  Area, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  ResponsiveContainer,
  BarChart,
  Bar,
  Cell
} from "recharts";
import type { LucideIcon } from "lucide-react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { fetchCourses, deleteCourse, updateCourse } from "../services/courseApi";
import { getMyRank } from "../services/leaderboardApi";
import { Button } from "../components/ui/button";
import { toast } from "sonner";
import { cn } from "../lib/utils";
import { Course } from "../types/course";
import { CourseCard } from "../components/CourseCard";
import { useAuth } from "../auth/AuthContext";
import { OnboardingTutorial } from "../components/OnboardingTutorial";

export default function Dashboard() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [togglingId, setTogglingId] = useState<string | null>(null);
  
  const [showTutorial, setShowTutorial] = useState(false);
  const [isNewUser, setIsNewUser] = useState(false);

  // Check tutorial status
  useEffect(() => {
    if (user?.id) {
      const tutorialKey = `tutorial_completed_${user.id}`;
      const hasCompleted = localStorage.getItem(tutorialKey);
      if (!hasCompleted) {
        setShowTutorial(true);
        setIsNewUser(true);
      }
    }
  }, [user?.id]);

  const handleTutorialComplete = () => {
    if (user?.id) {
      localStorage.setItem(`tutorial_completed_${user.id}`, "true");
    }
    setShowTutorial(false);
  };

  const { data: courses = [], isLoading } = useQuery({
    queryKey: ["courses"],
    queryFn: () => fetchCourses(),
  });

  const { data: rankData } = useQuery({
    queryKey: ["leaderboard", "me"],
    queryFn: () => getMyRank().then(res => "rank" in res ? res : JSON.parse(res)),
  });

  const recentActivity = useMemo(() => {
    const items: Array<{ icon: LucideIcon; text: string; time: string }> = [];

    if (Array.isArray(courses)) {
      for (const course of courses.slice(0, 3)) {
        const title = course?.title || "Untitled Course";
        items.push({
          icon: BookOpen,
          text: `Updated \"${title}\"`,
          time: formatRelativeTime(course?.updatedAt ?? course?.createdAt),
        });
      }
    }

    if (rankData?.rank) {
      items.push({
        icon: Trophy,
        text: `You're currently ranked #${rankData.rank} globally`,
        time: rankData?.weeklyPoints ? `+${rankData.weeklyPoints} this week` : "Latest leaderboard snapshot",
      });
    }

    if (items.length === 0) {
      items.push({
        icon: Sparkles,
        text: "Create your first course to start seeing activity",
        time: "No recent activity",
      });
    }

    return items;
  }, [courses, rankData]);

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteCourse(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["courses"] });
      toast.success("Course deleted successfully");
    },
    onError: () => toast.error("Failed to delete course"),
  });

  const toggleActiveMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      updateCourse(id, { active }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["courses"] });
      toast.success("Course status updated");
    },
    onSettled: () => setTogglingId(null),
  });

  const handleDelete = (id: string, title: string) => {
    if (confirm(`Are you sure you want to delete "${title}"?`)) {
      deleteMutation.mutate(id);
    }
  };

  const handleToggleActive = (course: Course) => {
    setTogglingId(course.id);
    toggleActiveMutation.mutate({ id: course.id, active: !course.active });
  };

  return (
    <div className="mx-auto max-w-7xl px-8 py-10 animate-fade-in space-y-10">
      {/* Welcome Header */}
      <div>
        <p className="text-sm font-medium text-muted-foreground uppercase tracking-widest">
          {isNewUser ? "Welcome" : "Welcome back"}
        </p>
        <h1 className="mt-1 font-display text-4xl md:text-5xl font-bold tracking-tight text-foreground mb-2">
          Let’s build something <span className="text-gradient">remarkable</span> today.
        </h1>
      </div>

      {/* Stats Grid */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 mt-8">
        <StatCard 
          label="Courses created" 
          value={String(rankData?.courseCount || courses.length || 0)} 
          delta="+2 this week" 
          type="primary" 
          icon={<BookOpen className="h-3 w-3" />}
        />
        <StatCard 
          label="Total points" 
          value={String(rankData?.totalPoints || 0)} 
          delta={rankData?.weeklyPoints ? `+${rankData.weeklyPoints} pts` : "Active"} 
          type="accent" 
          icon={<Award className="h-3 w-3 text-amber-500" />}
        />
        <StatCard 
          label="Current Streak" 
          value={rankData?.currentStreak ? `${rankData.currentStreak} days` : "0 days"} 
          delta="Top 5%" 
          icon={<Flame className="h-3 w-3 text-orange-500" />}
        />
        <StatCard 
          label="Global Rank" 
          value={rankData?.rank ? `#${rankData.rank}` : "Unranked"} 
          delta="Top 10%" 
          icon={<Trophy className="h-3 w-3 text-yellow-500" />} 
        />
      </div>

      {/* Continue Learning / Courses */}
      <section>
        <div className="flex items-end justify-between mb-6">
          <div>
            <h2 className="font-display text-2xl font-bold text-foreground tracking-tight">Continue learning</h2>
            <p className="text-sm text-muted-foreground mt-1 font-medium">Pick up where you left off</p>
          </div>
          <Link to="/courses" className="text-sm font-bold text-primary hover:text-primary/80 transition-colors flex items-center gap-1">
            View all <ArrowRight className="h-4 w-4" />
          </Link>
        </div>

        {isLoading ? (
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-[320px] rounded-2xl animate-pulse bg-muted border border-border" />
            ))}
          </div>
        ) : courses.length === 0 ? (
          <div className="rounded-3xl p-16 text-center border border-border bg-card shadow-soft">
            <div className="h-16 w-16 bg-primary/10 rounded-2xl flex items-center justify-center mx-auto mb-6 text-primary">
              <BookOpen className="h-8 w-8" />
            </div>
            <h3 className="text-xl font-bold text-foreground mb-2">No courses yet</h3>
            <p className="text-muted-foreground max-w-xs mx-auto mb-8">Start your journey by generating your first AI-powered course.</p>
            <Button variant="hero" size="xl" asChild>
              <Link to="/create-course">Generate first course</Link>
            </Button>
          </div>
        ) : (
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {courses.slice(0, 3).map((course: Course, i: number) => (
              <CourseCard
                key={course.id}
                course={course}
                index={i}
                onDelete={handleDelete}
                onToggleActive={handleToggleActive}
                togglingId={togglingId}
              />
            ))}
          </div>
        )}
      </section>

      {/* Bottom Layout: Activity & AI Coach */}
      <section className="grid gap-6 lg:grid-cols-3 pb-10">
        <div className="lg:col-span-2 rounded-3xl p-8 border border-border bg-card shadow-soft relative overflow-hidden group">
          <div className="flex items-center justify-between mb-6 relative z-10">
            <div>
              <h2 className="font-display text-2xl font-bold text-foreground tracking-tight">Engagement</h2>
              <p className="text-sm text-muted-foreground mt-0.5 font-medium">Daily learning activity hours</p>
            </div>
            <div className="flex items-center gap-2">
               <div className="flex items-center gap-1 px-2 py-1 rounded-md bg-muted text-[10px] font-bold text-muted-foreground border border-border">
                  <Activity className="h-3 w-3 text-emerald-500" />
                  +12.5%
               </div>
               <div className="h-8 w-8 rounded-full bg-muted border border-border flex items-center justify-center text-muted-foreground cursor-pointer hover:text-foreground transition-colors">
                 <Clock className="h-4 w-4" />
               </div>
            </div>
          </div>

          <div className="h-[240px] w-full mt-4 relative z-10">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={[
                { day: "Mon", hours: 1.2 },
                { day: "Tue", hours: 2.1 },
                { day: "Wed", hours: 1.8 },
                { day: "Thu", hours: 3.2 },
                { day: "Fri", hours: 2.8 },
                { day: "Sat", hours: 4.5 },
                { day: "Sun", hours: 3.9 },
              ]}>
                <defs>
                  <linearGradient id="colorEngagement" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="var(--neon-cyan)" stopOpacity={0.4}/>
                    <stop offset="95%" stopColor="var(--neon-cyan)" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border)" opacity={0.5} />
                <XAxis 
                  dataKey="day" 
                  axisLine={false} 
                  tickLine={false} 
                  tick={{ fill: "var(--muted-foreground)", fontSize: 10, fontWeight: "600" }} 
                  dy={10} 
                />
                <YAxis hide />
                <Tooltip 
                  contentStyle={{ 
                    backgroundColor: "var(--card)", 
                    borderColor: "var(--border)", 
                    borderRadius: "12px",
                    fontSize: "12px"
                  }}
                  itemStyle={{ color: "var(--neon-cyan)" }}
                />
                <Area 
                  type="monotone" 
                  dataKey="hours" 
                  stroke="var(--neon-cyan)" 
                  strokeWidth={3}
                  fillOpacity={1} 
                  fill="url(#colorEngagement)" 
                  animationDuration={1500}
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="rounded-3xl p-8 border border-border bg-card shadow-soft relative overflow-hidden flex flex-col">
          <div className="absolute top-0 right-0 p-4">
            <Sparkles className="h-6 w-6 text-primary/10" />
          </div>

          <h2 className="font-display text-2xl font-bold text-foreground tracking-tight mb-1">AI Coach</h2>
          <p className="text-sm text-muted-foreground font-medium mb-8">Ask anything about your courses</p>

          <div className="space-y-4 flex-1">
            <div className="rounded-2xl bg-muted p-4 border border-border transition-colors hover:border-primary/30">
              <p className="text-[10px] uppercase tracking-widest font-bold text-muted-foreground mb-1.5">Suggestion</p>
              <p className="text-sm text-foreground/80 leading-relaxed">Add a 5-question quiz to <span className="font-bold text-foreground">Module 2</span> to lift retention by <span className="text-primary font-bold">18%</span>.</p>
            </div>
            <div className="rounded-2xl bg-muted p-4 border border-border transition-colors hover:border-primary/30">
              <p className="text-[10px] uppercase tracking-widest font-bold text-muted-foreground mb-1.5">Learning Tip</p>
              <p className="text-sm text-foreground/80 leading-relaxed">Two learners are stuck on <span className="font-bold text-foreground">Lesson 3.4</span> — consider a short walkthrough.</p>
            </div>
          </div>

          <Button variant="hero" size="xl" className="mt-8 w-full group" asChild>
            <Link to="/ai-coach">
               Open AI Coach <Sparkles className="h-4 w-4 ml-2 group-hover:animate-pulse" />
            </Link>
          </Button>
        </div>
      </section>

      <OnboardingTutorial 
        open={showTutorial} 
        onComplete={handleTutorialComplete} 
        userName={user?.displayName?.split(' ')?.[0] ?? user?.username} 
      />
    </div>
  );
}

function formatRelativeTime(raw?: string) {
  if (!raw) return "Recently";
  const ts = new Date(raw).getTime();
  if (Number.isNaN(ts)) return "Recently";

  const diffMs = Date.now() - ts;
  const minute = 60_000;
  const hour = 60 * minute;
  const day = 24 * hour;

  if (diffMs < minute) return "Just now";
  if (diffMs < hour) return `${Math.max(1, Math.floor(diffMs / minute))} min ago`;
  if (diffMs < day) return `${Math.max(1, Math.floor(diffMs / hour))} hr ago`;
  return `${Math.max(1, Math.floor(diffMs / day))} day(s) ago`;
}

function StatCard({ label, value, delta, type = "default", icon }: { label: string; value: string; delta: string; type?: "default" | "primary" | "accent"; icon?: React.ReactNode }) {
  return (
    <div className="rounded-2xl p-6 border border-border bg-card shadow-soft relative overflow-hidden group transition-all duration-300">
      <div className="absolute top-0 right-0 w-24 h-24 bg-gradient-brand opacity-0 group-hover:opacity-5 blur-2xl transition-opacity" />
      <div className="text-xs uppercase tracking-widest font-bold text-muted-foreground">{label}</div>
      <div className="mt-4 flex items-baseline justify-between">
        <div className="font-display text-4xl font-bold text-foreground tracking-tighter">{value}</div>
        <div className={cn(
          "text-[10px] font-bold px-2 py-1 rounded-lg border flex items-center gap-1",
          type === "primary" ? "text-primary border-primary/20 bg-primary/5" :
            type === "accent" ? "text-accent border-accent/20 bg-accent/5" :
              "text-muted-foreground border-border bg-muted"
        )}>
          {icon} {delta}
        </div>
      </div>
    </div>
  );
}


