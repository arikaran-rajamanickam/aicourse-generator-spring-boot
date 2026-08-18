import { LessonBlock } from "@/types/lessonContent";
import LessonHeadingBlock from "./LessonHeadingBlock";
import LessonTextBlock from "./LessonTextBlock";
import LessonHtmlBlock from "./LessonHtmlBlock";
import LessonListBlock from "./LessonListBlock";
import LessonTableBlock from "./LessonTableBlock";
import LessonQuizBlock from "./LessonQuizBlock";
import LessonCodeBlock from "./LessonCodeBlock";
import LessonYoutubeBlock from "./LessonYoutubeBlock";
import LessonReferenceBlock from "./LessonReferenceBlock";
import LessonImageBlock from "./LessonImageBlock";

interface Props {
  blocks: LessonBlock[];
  courseId?: string;
  lessonId?: string;
}

export default function LessonContentRenderer({ blocks, courseId, lessonId }: Props) {
  return (
    <article className="lesson-content">
      {blocks.map((block, index) => {
        switch (block.type) {
          case "heading":
            return <LessonHeadingBlock key={index} block={block} />;
          case "text":
            return <LessonTextBlock key={index} block={block} />;
          case "html":
            return <LessonHtmlBlock key={index} block={block} />;
          case "list":
            return <LessonListBlock key={index} block={block} />;
          case "table":
            return <LessonTableBlock key={index} block={block} />;
          case "quiz":
            return <LessonQuizBlock key={index} block={block} courseId={courseId} lessonId={lessonId} quizIndex={index} />;
          case "code":
            return <LessonCodeBlock key={index} block={block} />;
          case "youtube":
            return <LessonYoutubeBlock key={index} block={block} />;
          case "reference":
            return <LessonReferenceBlock key={index} block={block} />;
          case "image":
            return <LessonImageBlock key={index} block={block} />;
          default:
            return null;
        }
      })}
    </article>
  );
}
