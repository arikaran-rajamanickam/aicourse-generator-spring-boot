import { formatDistanceToNow } from "date-fns";
import { useNavigate } from "react-router-dom";
import { MessageSquare, Play, Trash2, Clock, CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import { useDeletePrompt, useUsePrompt } from "@/lib/queries/projects";
import type { ProjectPrompt } from "@/services/projectApi";
import { Badge } from "@/components/ui/badge";

export function SavedPromptsList({ projectId, prompts }: { projectId: string; prompts: ProjectPrompt[] }) {
  const rm = useDeletePrompt(projectId);
  const useP = useUsePrompt(projectId);
  const navigate = useNavigate();

  if (!prompts.length) {
    return (
      <div className="flex flex-col items-center justify-center p-10 mt-6 rounded-2xl border border-dashed border-border bg-card/50 text-center">
        <MessageSquare className="h-10 w-10 text-muted-foreground/30 mb-4" />
        <h3 className="font-semibold text-lg mb-1">No saved prompts yet</h3>
        <p className="text-sm text-muted-foreground max-w-sm">
          Saved prompts act as templates so you can easily generate similar courses or iterate on your ideas later.
        </p>
      </div>
    );
  }

  return (
    <ul className="mt-6 grid grid-cols-1 md:grid-cols-2 gap-4">
      {prompts.map((p) => {
        const isPending = p.id.startsWith("tmp-");
        return (
          <li
            key={p.id}
            data-pending={isPending || undefined}
            className="group flex flex-col rounded-xl border border-border bg-card p-5 transition-all hover:border-primary/30 hover:shadow-md data-[pending]:opacity-50"
          >
            <div className="flex-1">
              <div className="flex items-start justify-between gap-2 mb-3">
                <p className="text-sm text-foreground line-clamp-4 leading-relaxed font-body whitespace-pre-wrap">{p.text}</p>
                <TooltipProvider>
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => rm.mutate(p.id)}
                        disabled={isPending || rm.isPending}
                        className="h-8 w-8 text-muted-foreground hover:text-destructive opacity-0 group-hover:opacity-100 transition-opacity flex-shrink-0"
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </TooltipTrigger>
                    <TooltipContent>Delete prompt</TooltipContent>
                  </Tooltip>
                </TooltipProvider>
              </div>
              
              {p.relatedCourseTitle && (
                <div className="flex items-center gap-1.5 mt-2 mb-3">
                  <Badge variant="outline" className="text-xs font-normal text-muted-foreground bg-background">
                    Linked: <span className="font-medium ml-1 truncate max-w-[120px]">{p.relatedCourseTitle}</span>
                  </Badge>
                </div>
              )}
            </div>

            <div className="mt-auto pt-4 flex items-center justify-between border-t border-border/50">
              <div className="flex items-center text-xs text-muted-foreground gap-3">
                <span className="flex items-center gap-1">
                  <Clock className="h-3 w-3" />
                  {formatDistanceToNow(p.createdAt ? new Date(p.createdAt) : new Date())} ago
                </span>
                {p.lastUsedAt && p.lastUsedAt !== p.createdAt && (
                  <span className="flex items-center gap-1 text-primary/70">
                    <CheckCircle2 className="h-3 w-3" />
                    Used recently
                  </span>
                )}
              </div>
              
              <Button
                size="sm"
                variant="secondary"
                onClick={() => {
                  useP.mutate(p.id);
                  navigate(`/create-course?projectId=${projectId}&topic=${encodeURIComponent(p.text)}`);
                }}
                disabled={isPending || useP.isPending}
                className="h-8 gap-1.5"
              >
                <Play className="h-3 w-3" /> Reuse
              </Button>
            </div>
          </li>
        );
      })}
    </ul>
  );
}
