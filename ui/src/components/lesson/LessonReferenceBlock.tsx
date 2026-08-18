import { LessonReference } from "@/types/lessonContent";
import { ExternalLink } from "lucide-react";

interface Props {
  block: LessonReference;
}

export default function LessonReferenceBlock({ block }: Props) {
  return (
    <div className="my-8 rounded-lg border border-border bg-card p-6">
      <div className="flex items-center gap-2.5 mb-4">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/15">
          <ExternalLink className="h-4.5 w-4.5 text-primary" />
        </div>
        <span className="font-display text-sm font-semibold uppercase tracking-wider text-primary">
          References & Resources
        </span>
      </div>

      <div className="space-y-3">
        {block.content.map((ref, idx) => (
          <a
            key={idx}
            href={ref.url}
            target="_blank"
            rel="noopener noreferrer"
            className="block rounded-md border border-border/50 p-3 transition-all hover:border-primary/50 hover:bg-muted/50"
          >
            <div className="flex items-start gap-2">
              <ExternalLink className="h-4 w-4 mt-0.5 text-primary shrink-0" />
              <div className="flex-1 min-w-0">
                <p className="font-medium text-foreground text-sm truncate hover:underline">
                  {ref.title}
                </p>
                {ref.description && (
                  <p className="text-xs text-muted-foreground mt-1">
                    {ref.description}
                  </p>
                )}
                <p className="text-xs text-muted-foreground mt-1 truncate">
                  {ref.url}
                </p>
              </div>
            </div>
          </a>
        ))}
      </div>
    </div>
  );
}

