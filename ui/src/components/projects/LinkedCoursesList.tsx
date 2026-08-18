import { Link } from "react-router-dom";
import { BookOpen, FolderOpen, MoreVertical, Trash2 } from "lucide-react";
import { formatDistanceToNow } from "date-fns";
import { Button } from "@/components/ui/button";
import { useUnlinkCourse } from "@/lib/queries/projects";
import type { ProjectCourseSummary } from "@/services/projectApi";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";

export function LinkedCoursesList({ projectId, courses }: { projectId: string; courses: ProjectCourseSummary[] }) {
  const unlink = useUnlinkCourse(projectId);

  if (!courses.length) {
    return (
      <div className="flex flex-col items-center justify-center p-12 mt-6 rounded-2xl border border-dashed border-border bg-card/30 text-center">
        <FolderOpen className="h-10 w-10 text-muted-foreground/30 mb-4" />
        <h3 className="font-semibold text-lg mb-1">No courses generated</h3>
        <p className="text-sm text-muted-foreground max-w-md">
          Use the AI Prompt Engine below to generate a new course. It will automatically be linked to this project.
        </p>
      </div>
    );
  }

  return (
    <div className="mt-6 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
      {courses.map((course) => (
        <article
          key={course.id}
          className="group relative rounded-xl border border-border bg-card p-5 transition-all hover:-translate-y-0.5 hover:shadow-md hover:border-primary/20"
        >
          <div className="flex items-start justify-between gap-3">
            <Link
              to={`/courses/${course.id}`}
              className="flex-1 outline-none group-focus-visible:ring-2"
            >
              <h3 className="font-semibold text-foreground line-clamp-1 leading-tight group-hover:text-primary transition-colors">
                {course.title}
              </h3>
            </Link>

            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" className="h-8 w-8 -mt-1 -mr-2 opacity-0 group-hover:opacity-100 transition-opacity text-muted-foreground">
                  <MoreVertical className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem asChild>
                  <Link to={`/courses/${course.id}`}>
                    <BookOpen className="mr-2 h-4 w-4" /> Open Course
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuItem
                  className="text-destructive focus:text-destructive"
                  onClick={() => unlink.mutate(course.id)}
                  disabled={unlink.isPending}
                >
                  <Trash2 className="mr-2 h-4 w-4" /> Unlink from Project
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
          
          <div className="mt-4 flex flex-col gap-2">
            <div className="w-full bg-secondary rounded-full h-1.5 overflow-hidden">
               <div className="bg-primary h-full rounded-full w-[33%]" />
            </div>
            <div className="flex items-center justify-between text-xs text-muted-foreground mt-1">
              <span>{course.lessonCount || 0} lessons</span>
              <span>Updated {formatDistanceToNow(new Date(course.updatedAt || Date.now()), { addSuffix: true })}</span>
            </div>
          </div>
        </article>
      ))}
    </div>
  );
}
