import { LessonYoutube } from "@/types/lessonContent";
import { Play } from "lucide-react";
import { AspectRatio } from "@/components/ui/aspect-ratio";

interface Props {
  block: LessonYoutube;
}

function extractYoutubeId(url: string): string | null {
  const patterns = [
    /(?:youtube\.com\/watch\?v=)([a-zA-Z0-9_-]{11})/,
    /(?:youtu\.be\/)([a-zA-Z0-9_-]{11})/,
    /(?:youtube\.com\/embed\/)([a-zA-Z0-9_-]{11})/,
  ];
  for (const p of patterns) {
    const match = url.match(p);
    if (match) return match[1];
  }
  return null;
}

export default function LessonYoutubeBlock({ block }: Props) {
  const { url, title } = block.content;
  const videoId = extractYoutubeId(url);

  if (!videoId) {
    return (
      <div className="my-6 rounded-xl border border-border bg-card p-6 text-center">
        <Play className="mx-auto mb-2 h-8 w-8 text-muted-foreground" />
        <p className="text-muted-foreground text-sm">
          Unable to load video.{" "}
          <a
            href={url}
            target="_blank"
            rel="noopener noreferrer"
            className="text-primary underline"
          >
            Watch on YouTube
          </a>
        </p>
      </div>
    );
  }

  return (
    <div className="my-6">
      {title && (
        <p className="mb-2 flex items-center gap-2 text-sm font-medium text-foreground">
          <Play className="h-4 w-4 text-primary" />
          {title}
        </p>
      )}
      <div className="overflow-hidden rounded-xl border border-border shadow-sm">
        <AspectRatio ratio={16 / 9}>
          <iframe
            src={`https://www.youtube.com/embed/${videoId}`}
            title={title || "YouTube Video"}
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
            allowFullScreen
            className="h-full w-full"
          />
        </AspectRatio>
      </div>
    </div>
  );
}
