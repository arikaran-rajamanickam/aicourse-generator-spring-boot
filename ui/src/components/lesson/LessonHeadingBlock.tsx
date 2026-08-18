import { LessonHeading } from "@/types/lessonContent";

interface Props {
  block: LessonHeading;
}

export default function LessonHeadingBlock({ block }: Props) {
  return (
    <h2 className="mt-10 mb-4 font-display text-xl font-bold text-foreground tracking-tight">
      {block.content}
    </h2>
  );
}
