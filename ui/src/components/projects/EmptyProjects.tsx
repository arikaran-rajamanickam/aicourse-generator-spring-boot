import { FolderOpen } from "lucide-react";
import { Button } from "@/components/ui/button";

export function EmptyProjects({ onCreate }: { onCreate: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center p-12 text-center rounded-2xl border border-dashed border-border/60 bg-card/40">
      <div className="h-16 w-16 bg-primary/10 rounded-full flex items-center justify-center mb-6">
        <FolderOpen className="h-8 w-8 text-primary/60" />
      </div>
      <h3 className="text-xl font-bold text-foreground mb-2">No projects yet</h3>
      <p className="text-muted-foreground max-w-sm mb-8">
        Group your AI-generated courses into projects to keep things tidy and manage your prompts.
      </p>
      <Button onClick={onCreate} size="lg" className="shadow-primary/20 shadow-lg">
        Create your first project
      </Button>
    </div>
  );
}
