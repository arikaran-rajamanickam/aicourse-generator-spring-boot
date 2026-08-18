import { Plus, FolderGit2, BookCopy, CalendarClock } from "lucide-react";
import { Button } from "@/components/ui/button";

interface Props {
  totalProjects: number;
  totalCourses: number;
  updatedThisWeek: number;
  onCreate(): void;
}

export function ProjectsHeader({ totalProjects, totalCourses, updatedThisWeek, onCreate }: Props) {
  return (
    <div className="flex flex-col gap-6 md:flex-row md:items-end md:justify-between">
      <div className="flex-1">
        <h1 className="text-display mb-6 bg-clip-text text-transparent bg-gradient-to-r from-foreground to-foreground/70">
          Projects Hub
        </h1>
        <div className="grid grid-cols-3 gap-3 md:w-auto md:max-w-xl">
          <div className="p-4 rounded-xl bg-card border border-border shadow-sm flex flex-col items-start gap-2">
            <div className="bg-primary/10 p-2 rounded-lg text-primary">
              <FolderGit2 className="h-4 w-4" />
            </div>
            <div>
              <p className="text-xl font-bold text-foreground">{totalProjects}</p>
              <p className="text-xs text-muted-foreground font-medium">Total Projects</p>
            </div>
          </div>
          
          <div className="p-4 rounded-xl bg-card border border-border shadow-sm flex flex-col items-start gap-2">
            <div className="bg-blue-500/10 p-2 rounded-lg text-blue-500">
              <BookCopy className="h-4 w-4" />
            </div>
            <div>
              <p className="text-xl font-bold text-foreground">{totalCourses}</p>
              <p className="text-xs text-muted-foreground font-medium">Generated Courses</p>
            </div>
          </div>
          
          <div className="p-4 rounded-xl bg-card border border-border shadow-sm flex flex-col items-start gap-2">
            <div className="bg-emerald-500/10 p-2 rounded-lg text-emerald-500">
              <CalendarClock className="h-4 w-4" />
            </div>
            <div>
              <p className="text-xl font-bold text-foreground">{updatedThisWeek}</p>
              <p className="text-xs text-muted-foreground font-medium">Updated this week</p>
            </div>
          </div>
        </div>
      </div>
      
      <Button onClick={onCreate} size="lg" className="hidden md:flex gap-2">
        <Plus className="h-4 w-4" /> New project
      </Button>
    </div>
  );
}
