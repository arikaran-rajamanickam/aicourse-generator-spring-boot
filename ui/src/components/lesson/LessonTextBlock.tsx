import { LessonText } from "@/types/lessonContent";
import RichText from "./RichText";

interface Props {
  block: LessonText;
}

export default function LessonTextBlock({ block }: Props) {
  return (
    <p className="my-4 text-muted-foreground leading-relaxed text-[1.05rem]">
      <RichText text={block.content} />
    </p>
  );
}
