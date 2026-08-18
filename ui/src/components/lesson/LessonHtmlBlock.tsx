import { LessonHtml } from "@/types/lessonContent";

interface Props {
  block: LessonHtml;
}

export default function LessonHtmlBlock({ block }: Props) {
  return (
    <div
      className="my-4 space-y-2 text-muted-foreground leading-relaxed text-[1.05rem] prose prose-sm prose-neutral dark:prose-invert"
      dangerouslySetInnerHTML={{ __html: block.content }}
    />
  );
}
