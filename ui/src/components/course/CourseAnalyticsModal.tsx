import { useEffect, useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Clock, CheckCircle, BarChart2, AlertTriangle, BookOpen, Layers } from "lucide-react";
import { getSharedCourseUsage } from "@/services/progressApi";
import { toast } from "sonner";
import { cn } from "@/lib/utils";

interface CourseAnalyticsModalProps {
  courseId: string;
  userId: string;
  userName?: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export default function CourseAnalyticsModal({ courseId, userId, userName, open, onOpenChange }: CourseAnalyticsModalProps) {
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (open && courseId && userId) {
      loadData();
    }
  }, [open, courseId, userId]);

  const loadData = async () => {
    setLoading(true);
    try {
      const usage = await getSharedCourseUsage(courseId, userId);
      setData(usage);
    } catch (error) {
      console.error("Failed to load course usage", error);
      toast.error("Failed to fetch analytics data");
    } finally {
      setLoading(false);
    }
  };

  const formatTime = (seconds: number) => {
    if (!seconds || seconds <= 0) return "0s";
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    if (h > 0) return `${h}h ${m}m`;
    if (m > 0) return `${m}m ${s}s`;
    return `${s}s`;
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="border-border/70 bg-card p-6 sm:max-w-[700px] max-h-[85vh] overflow-y-auto">
        <DialogHeader className="mb-4">
          <DialogTitle className="text-2xl font-display text-foreground flex items-center gap-2">
            <BarChart2 className="h-6 w-6 text-primary" />
            Analytics: {userName || `User ${userId}`}
          </DialogTitle>
          <DialogDescription>
            Detailed progress and engagement metrics for {data?.courseTitle || 'this course'}.
          </DialogDescription>
        </DialogHeader>

        {loading ? (
          <div className="py-12 flex justify-center text-muted-foreground animate-pulse">
            Loading analytics data...
          </div>
        ) : !data ? (
          <div className="py-12 flex justify-center text-muted-foreground">
            No data available.
          </div>
        ) : (
          <div className="space-y-6 animate-fade-in">
            {/* Overview Stats */}
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div className="glass-card rounded-xl p-4 flex flex-col items-center justify-center text-center">
                <CheckCircle className="h-6 w-6 text-success mb-2" />
                <span className="text-sm text-muted-foreground font-medium">Lessons Completed</span>
                <span className="text-2xl font-bold text-foreground mt-1">
                  {data.completedLessons} / {data.totalLessons}
                </span>
              </div>
              <div className="glass-card rounded-xl p-4 flex flex-col items-center justify-center text-center">
                <Clock className="h-6 w-6 text-primary mb-2" />
                <span className="text-sm text-muted-foreground font-medium">Total Time Spent</span>
                <span className="text-2xl font-bold text-foreground mt-1">
                  {formatTime(data.totalTimeSeconds)}
                </span>
              </div>
              <div className="glass-card rounded-xl p-4 flex flex-col items-center justify-center text-center">
                <BookOpen className="h-6 w-6 text-amber-500 mb-2" />
                <span className="text-sm text-muted-foreground font-medium">Quiz Accuracy</span>
                <span className="text-2xl font-bold text-foreground mt-1">
                  {data.quizSummary?.totalQuizzesAttempted > 0 
                    ? Math.round((data.quizSummary.firstAttemptCorrect / data.quizSummary.totalQuizzesAttempted) * 100) 
                    : 0}%
                </span>
              </div>
            </div>

            {/* Modules Details */}
            <div className="mt-8">
              <h3 className="font-display text-lg font-bold text-foreground flex items-center gap-2 mb-4">
                <Layers className="h-5 w-5" /> Module Breakdown
              </h3>
              
              <div className="space-y-4">
                {data.modules?.map((mod: any) => {
                  const modLessons = data.lessons?.filter((l: any) => l.moduleId === mod.moduleId) || [];
                  return (
                    <div key={mod.moduleId} className="rounded-lg border border-border/50 bg-muted/20 overflow-hidden">
                      <div className="px-4 py-3 bg-muted/40 border-b border-border/50 flex justify-between items-center">
                        <span className="font-semibold text-foreground">{mod.moduleTitle}</span>
                        <span className="text-sm text-muted-foreground">
                          {mod.completedLessons} / {mod.totalLessons} Lessons
                        </span>
                      </div>
                      
                      <div className="divide-y divide-border/30">
                        {modLessons.map((lesson: any) => (
                          <div key={lesson.lessonId} className="px-4 py-3 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2 hover:bg-muted/10 transition-colors">
                            <div className="flex items-center gap-3">
                              {lesson.completed ? (
                                <CheckCircle className="h-4.5 w-4.5 text-success shrink-0" />
                              ) : (
                                <div className="h-4.5 w-4.5 rounded-full border-2 border-muted-foreground/30 shrink-0" />
                              )}
                              <span className={cn(
                                "text-sm", 
                                lesson.completed ? "text-foreground font-medium" : "text-muted-foreground"
                              )}>
                                {lesson.lessonTitle}
                              </span>
                            </div>
                            
                            <div className="flex items-center gap-4 text-xs font-mono">
                              <span className="text-muted-foreground flex items-center gap-1.5">
                                <Clock className="h-3 w-3" /> {formatTime(lesson.timeSpentSeconds)}
                              </span>
                              
                              {lesson.completionFlagged && (
                                <span className="text-destructive flex items-center gap-1 bg-destructive/10 px-2 py-0.5 rounded font-sans font-medium text-[10px]">
                                  <AlertTriangle className="h-3 w-3" /> 
                                  Too Fast
                                </span>
                              )}
                            </div>
                          </div>
                        ))}
                        {modLessons.length === 0 && (
                          <div className="px-4 py-3 text-sm text-muted-foreground text-center">
                            No lessons in this module.
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
                {data.modules?.length === 0 && (
                  <p className="text-sm text-muted-foreground text-center py-4">No modules found.</p>
                )}
              </div>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
