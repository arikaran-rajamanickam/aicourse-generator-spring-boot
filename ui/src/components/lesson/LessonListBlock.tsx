import { LessonList } from "@/types/lessonContent";
import RichText from "./RichText";

interface Props {
  block: LessonList;
}

export default function LessonListBlock({ block }: Props) {
  return (
    <ul className="my-4 ml-5 space-y-2.5">
      {block.content.map((item, i) => (
        <li
          key={i}
          className="flex items-start gap-3 text-muted-foreground leading-relaxed"
        >
          <span className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-primary" />
          <span>
            <RichText text={item} />
          </span>
        </li>
      ))}
    </ul>
  );
}
