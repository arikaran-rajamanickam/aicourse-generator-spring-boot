import { useState } from "react";
import { LessonQuiz } from "@/types/lessonContent";
import { CheckCircle, XCircle, HelpCircle } from "lucide-react";
import { cn } from "@/lib/utils";
import RichText from "./RichText";
import { recordQuizAttempt } from "@/services/progressApi";

interface Props {
  block: LessonQuiz;
  courseId?: string;
  lessonId?: string;
  quizIndex?: number;
}

export default function LessonQuizBlock({ block, courseId, lessonId, quizIndex }: Props) {
  const { question, options, correctIndex, explanation } = block.content;
  const [selected, setSelected] = useState<number | null>(null);
  const answered = selected !== null;
  const isCorrect = selected === correctIndex;

  const handleSelect = async (index: number) => {
    setSelected(index);
    if (courseId && lessonId && quizIndex !== undefined) {
      try {
        await recordQuizAttempt(lessonId, courseId, quizIndex, index === correctIndex);
      } catch (err) {
        console.error("Failed to record quiz attempt", err);
      }
    }
  };

  return (
    <div className="my-8 rounded-xl border border-border bg-card p-6">
      {/* Header */}
      <div className="flex items-center gap-2.5 mb-4">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/15">
          <HelpCircle className="h-4.5 w-4.5 text-primary" />
        </div>
        <span className="font-display text-sm font-semibold uppercase tracking-wider text-primary">
          Quick Quiz
        </span>
      </div>

      {/* Question */}
      <p className="mb-5 text-lg font-medium text-foreground leading-snug">
        <RichText text={question} />
      </p>

      {/* Options */}
      <div className="space-y-2.5">
        {options.map((opt, i) => {
          const isThis = selected === i;
          const isRight = i === correctIndex;

          let borderClass = "border-border hover:border-primary/50 hover:bg-muted/50 cursor-pointer";
          let iconSlot: React.ReactNode = (
            <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full border border-border text-xs font-semibold text-muted-foreground">
              {String.fromCharCode(65 + i)}
            </span>
          );

          if (answered) {
            borderClass = "cursor-default ";
            if (isRight) {
              borderClass += "border-success bg-success/10";
              iconSlot = (
                <CheckCircle className="h-6 w-6 shrink-0 text-success" />
              );
            } else if (isThis && !isRight) {
              borderClass += "border-destructive bg-destructive/10";
              iconSlot = (
                <XCircle className="h-6 w-6 shrink-0 text-destructive" />
              );
            } else {
              borderClass += "border-border opacity-50";
            }
          }

          return (
            <button
              key={i}
              disabled={answered}
              onClick={() => handleSelect(i)}
              className={cn(
                "flex w-full items-center gap-3 rounded-lg border px-4 py-3 text-left transition-all",
                borderClass
              )}
            >
              {iconSlot}
              <span className="text-foreground text-[0.95rem]">
                <RichText text={opt} />
              </span>
            </button>
          );
        })}
      </div>

      {/* Result feedback */}
      {answered && (
        <div
          className={cn(
            "mt-5 rounded-lg px-4 py-3 text-sm",
            isCorrect
              ? "bg-success/10 border border-success/30 text-success"
              : "bg-destructive/10 border border-destructive/30 text-destructive"
          )}
        >
          <p className="font-semibold">
            {isCorrect ? "🎉 Correct!" : "❌ Incorrect"}
          </p>
          {explanation && (
            <p className="mt-1 text-muted-foreground">
              <RichText text={explanation} />
            </p>
          )}
          {!isCorrect && (
            <p className="mt-1 text-muted-foreground">
              The correct answer is{" "}
              <strong className="text-foreground">
                {String.fromCharCode(65 + correctIndex)}. <RichText text={options[correctIndex]} />
              </strong>
            </p>
          )}
        </div>
      )}
    </div>
  );
}
