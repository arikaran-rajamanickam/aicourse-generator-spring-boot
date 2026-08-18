import { useState } from "react";
import { Link } from "react-router-dom";
import { formatDistanceToNow } from "date-fns";
import { MoreHorizontal, BookOpen, Pencil, Copy, Trash2 } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import type { Project } from "@/services/projectApi";
import { DeleteProjectDialog } from "./DeleteProjectDialog";

export function ProjectCard({ project }: { project: Project }) {
  const [deleteOpen, setDeleteOpen] = useState(false);
  const isPending = project.id.startsWith("tmp-");
  const fallbackDate = project.updatedAt ? new Date(project.updatedAt) : new Date();
  const updated = formatDistanceToNow(fallbackDate, { addSuffix: true });

  return (
    <article
      data-pending={isPending || undefined}
      className="group relative rounded-[var(--radius)] border border-border bg-card p-5 transition hover:-translate-y-0.5 hover:shadow-[var(--shadow-elevated)] focus-within:ring-2 focus-within:ring-ring data-[pending]:opacity-70 data-[pending]:ring-1 data-[pending]:ring-primary/40"
    >
      <div className="flex items-start justify-between gap-3">
        <Link
          to={`/projects/${project.id}`}
          className="flex-1 outline-none"
        >
          <h3 className="line-clamp-1 text-base font-semibold text-card-foreground">{project.name}</h3>
        </Link>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              className="h-8 w-8 opacity-0 transition group-hover:opacity-100 focus:opacity-100"
              aria-label="Project actions"
              disabled={isPending}
            >
              <MoreHorizontal className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem asChild>
              <Link to={`/projects/${project.id}`}>
                <BookOpen className="mr-2 h-4 w-4" /> Open
              </Link>
            </DropdownMenuItem>
            <DropdownMenuItem>
              <Pencil className="mr-2 h-4 w-4" /> Rename
            </DropdownMenuItem>
            <DropdownMenuItem>
              <Copy className="mr-2 h-4 w-4" /> Duplicate
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem className="text-destructive focus:text-destructive" onSelect={() => setDeleteOpen(true)}>
              <Trash2 className="mr-2 h-4 w-4" /> Delete
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      <p className="mt-2 line-clamp-2 min-h-[2.6rem] text-sm text-muted-foreground">
        {project.description ?? "No description yet."}
      </p>

      <div className="mt-4 flex items-center justify-between text-xs text-muted-foreground">
        <Badge variant="secondary" className="font-medium">
          {project.courses.length} course{project.courses.length === 1 ? "" : "s"}
        </Badge>
        <TooltipProvider delayDuration={200}>
          <Tooltip>
            <TooltipTrigger asChild>
              <span>Updated {updated}</span>
            </TooltipTrigger>
            <TooltipContent>{new Date(project.updatedAt).toLocaleString()}</TooltipContent>
          </Tooltip>
        </TooltipProvider>
      </div>

      <DeleteProjectDialog
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        projectId={project.id}
        projectName={project.name}
      />
    </article>
  );
}
