import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { BookOpen, Lock, Users, CheckCircle, Loader2, ChevronRight } from "lucide-react";
import { Button } from "../components/ui/button";
import { resolveShareToken, enrollUsingShareLink } from "../services/joinApi";
import { useAuth } from "../auth/AuthContext";
import { toast } from "sonner";
import { cn } from "../lib/utils";

export default function JoinCourse() {
  const { token } = useParams<{ token: string }>();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const [courseInfo, setCourseInfo] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [enrolling, setEnrolling] = useState(false);
  const [enrolled, setEnrolled] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!token) {
      setError("Invalid join link.");
      setLoading(false);
      return;
    }
    let mounted = true;

    async function resolve() {
      try {
        const data = await resolveShareToken(token);
        if (mounted) {
          setCourseInfo(data?.data ?? data);
        }
      } catch (err) {
        if (!mounted) return;

        const raw = err instanceof Error ? err.message : "";
        const message = raw.includes("Received HTML response")
          ? "This share link endpoint is not available on the backend. Please verify the join API route for token resolution."
          : raw || "This link is invalid or has expired.";

        setError(message);
      } finally {
        if (mounted) setLoading(false);
      }
    }

    resolve();
    return () => { mounted = false; };
  }, [token]);

  const handleEnroll = async () => {
    if (!isAuthenticated) {
      toast.error("Please log in to enroll in this course");
      navigate(`/login?redirect=/join/${token}`);
      return;
    }

    try {
      setEnrolling(true);
      const result = await enrollUsingShareLink(token!);
      const courseId = result?.courseId ?? courseInfo?.courseId ?? courseInfo?.id;
      setEnrolled(true);
      toast.success("Successfully enrolled! Redirecting...");
      setTimeout(() => {
        navigate(courseId ? `/courses/${courseId}` : "/");
      }, 1500);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Failed to enroll");
    } finally {
      setEnrolling(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-3 text-muted-foreground">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
          <p className="text-sm">Resolving invite link...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background p-6">
        <div className="glass-card rounded-2xl p-10 max-w-md w-full text-center border border-destructive/20">
          <Lock className="h-12 w-12 text-destructive/60 mx-auto mb-4" />
          <h2 className="font-display text-xl font-bold text-foreground mb-2">Link Unavailable</h2>
          <p className="text-muted-foreground text-sm">{error}</p>
          <Button variant="outline" className="mt-6" onClick={() => navigate("/")}>
            Go to Dashboard
          </Button>
        </div>
      </div>
    );
  }

  const title = courseInfo?.courseName ?? "Unknown Course";
  const description = courseInfo?.courseDescription ?? "";
  const modules = courseInfo?.moduleCount ?? 0;
  const lessons = courseInfo?.lessonCount ?? 0;
  const difficulty = courseInfo?.difficulty ?? "";
  const inviterName = courseInfo?.inviterUsername ?? "";

  return (
    <div className="min-h-screen flex items-center justify-center bg-background p-6">
      {/* Background glow */}
      <div className="pointer-events-none fixed inset-0 overflow-hidden">
        <div className="absolute top-1/4 left-1/2 -translate-x-1/2 h-96 w-96 rounded-full bg-primary/10 blur-3xl" />
      </div>

      <div className="relative w-full max-w-lg">
        {/* Invitation badge */}
        {inviterName && (
          <div className="mb-4 flex items-center justify-center">
            <span className="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/10 px-4 py-1.5 text-xs font-medium text-primary">
              <Users className="h-3.5 w-3.5" />
              {inviterName} invited you to join
            </span>
          </div>
        )}

        <div className="glass-card rounded-2xl overflow-hidden border border-border/80">
          {/* Accent bar */}
          <div className="h-1.5 w-full gradient-primary" />

          <div className="p-8">
            <div className="flex items-start gap-4">
              <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-xl gradient-primary">
                <BookOpen className="h-7 w-7 text-primary-foreground" />
              </div>
              <div>
                <h1 className="font-display text-2xl font-bold text-foreground capitalize">{title}</h1>
                {difficulty && (
                  <span className={cn(
                    "mt-1 inline-block text-xs font-medium px-2 py-0.5 rounded-full border",
                    difficulty.toLowerCase() === "beginner" && "bg-green-500/10 text-green-400 border-green-500/20",
                    difficulty.toLowerCase() === "intermediate" && "bg-yellow-500/10 text-yellow-400 border-yellow-500/20",
                    difficulty.toLowerCase() === "advanced" && "bg-red-500/10 text-red-400 border-red-500/20",
                    !["beginner", "intermediate", "advanced"].includes(difficulty.toLowerCase()) && "bg-primary/10 text-primary border-primary/20"
                  )}>
                    {difficulty}
                  </span>
                )}
              </div>
            </div>

            {description && (
              <p className="mt-5 text-sm leading-relaxed text-muted-foreground">{description}</p>
            )}

            {/* Course meta */}
            {(modules > 0 || lessons > 0) && (
              <div className="mt-5 flex gap-6">
                {modules > 0 && (
                  <div className="text-center">
                    <p className="font-display text-xl font-bold text-foreground">{modules}</p>
                    <p className="text-xs text-muted-foreground">Modules</p>
                  </div>
                )}
                {lessons > 0 && (
                  <div className="text-center">
                    <p className="font-display text-xl font-bold text-foreground">{lessons}</p>
                    <p className="text-xs text-muted-foreground">Lessons</p>
                  </div>
                )}
              </div>
            )}

            <div className="mt-8">
              {enrolled ? (
                <div className="flex items-center justify-center gap-2 text-green-400 py-3">
                  <CheckCircle className="h-5 w-5" />
                  <span className="font-medium">Enrolled! Redirecting...</span>
                </div>
              ) : (
                <Button
                  variant="gradient"
                  size="lg"
                  className="w-full gap-2"
                  onClick={handleEnroll}
                  disabled={enrolling}
                >
                  {enrolling ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      Enrolling...
                    </>
                  ) : (
                    <>
                      Enroll in Course
                      <ChevronRight className="h-4 w-4" />
                    </>
                  )}
                </Button>
              )}

              {!isAuthenticated && (
                <p className="mt-3 text-center text-xs text-muted-foreground">
                  You'll be asked to log in before enrolling.
                </p>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
