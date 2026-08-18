import { Link } from "react-router-dom";
import { Trash2, ToggleLeft, ToggleRight, ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { Course } from "@/types/course";

interface CourseCardProps {
  course: Course;
  index: number;
  onDelete: (id: string, title: string) => void;
  onToggleActive: (course: Course) => void;
  togglingId: string | null;
}

export function CourseCard({ course, index, onDelete, onToggleActive, togglingId }: CourseCardProps) {
  // Cycle through some nice gradients for fallback placeholders
  const gradients = [
    "from-purple-500/80 to-blue-500/80",
    "from-cyan-500/80 to-teal-500/80",
    "from-pink-500/80 to-violet-500/80"
  ];
  const accent = gradients[index % gradients.length];
  const progress = course.progress || (index === 0 ? 64 : index === 1 ? 28 : 91);
  const tag = index === 0 ? "INTERVIEW PREP" : index === 1 ? "TEAM ONBOARDING" : "PERSONAL LEARNING";

  return (
    <article className="group relative overflow-hidden rounded-3xl border border-border bg-card p-6 transition-all duration-500 hover:border-primary/50 hover:shadow-glow hover:-translate-y-1">
      <div className={cn("h-40 w-full rounded-2xl bg-gradient-to-br relative overflow-hidden shadow-inner", accent)}>
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_30%_30%,rgba(255,255,255,0.2),transparent_60%)]" />
        <div className="absolute inset-0 bg-noise opacity-30 mix-blend-overlay" />
        <div className="absolute bottom-4 right-4 h-10 w-10 rounded-full border border-white/20 bg-white/10 backdrop-blur-md flex items-center justify-center text-white opacity-0 group-hover:opacity-100 transition-all translate-y-4 group-hover:translate-y-0">
          <ArrowRight className="h-5 w-5" />
        </div>
      </div>

      <div className="mt-6 flex items-start justify-between gap-4">
        <h3 className="font-display text-xl font-bold text-foreground leading-tight flex-1">{course.title || "Untitled Course"}</h3>
        <span className="shrink-0 rounded-full border border-border bg-muted px-2.5 py-1 text-[9px] font-bold uppercase tracking-widest text-muted-foreground whitespace-nowrap">
          {tag}
        </span>
      </div>

      <div className="mt-3 text-xs font-medium text-muted-foreground flex items-center gap-2">
        {course.modules?.length || 0} modules
        <span className="h-1 w-1 rounded-full bg-border" />
        AI Generated
      </div>

      <div className="mt-6 relative">
        <div className="flex items-center justify-between text-[11px] mb-2">
          <span className="text-muted-foreground font-bold">{progress}% COMPLETE</span>
          <button
            className="text-muted-foreground hover:text-destructive transition-colors"
            onClick={(e) => {
              e.preventDefault();
              onDelete(course.id, course.title);
            }}
          >
            <Trash2 className="h-3 w-3" />
          </button>
        </div>
        <div className="h-2 w-full overflow-hidden rounded-full bg-muted border border-border">
          <div
            className="h-full rounded-full bg-gradient-cta shadow-glow"
            style={{ width: `${progress}%` }}
          />
        </div>
      </div>

      <div className="mt-6 flex gap-2 pt-2">
        <Link to={`/courses/${course.id}`} className="flex-1">
          <Button variant="outline" className="w-full text-xs h-10 font-bold">
            VIEW COURSE
          </Button>
        </Link>
        <Button
          variant="outline"
          size="icon"
          className="h-10 w-10 text-muted-foreground"
          onClick={() => onToggleActive(course)}
          disabled={togglingId === course.id}
        >
          {course.active === false ? <ToggleLeft className="h-5 w-5" /> : <ToggleRight className="h-5 w-5 text-primary" />}
        </Button>
      </div>
    </article>
  );
}
