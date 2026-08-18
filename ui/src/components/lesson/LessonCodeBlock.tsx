import { useState } from "react";
import { LessonCode } from "@/types/lessonContent";
import { Copy, Check } from "lucide-react";

interface Props {
  block: LessonCode;
}

export default function LessonCodeBlock({ block }: Props) {
  const { language, code } = block.content;
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    await navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="my-6 overflow-hidden rounded-xl border border-border bg-muted">
      {/* Header bar */}
      <div className="flex items-center justify-between border-b border-border bg-muted/80 px-4 py-2">
        <span className="text-xs font-mono font-medium uppercase tracking-wider text-muted-foreground">
          {language}
        </span>
        <button
          onClick={handleCopy}
          className="flex items-center gap-1.5 rounded-md px-2.5 py-1 text-xs text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground"
        >
          {copied ? (
            <>
              <Check className="h-3.5 w-3.5 text-success" />
              Copied
            </>
          ) : (
            <>
              <Copy className="h-3.5 w-3.5" />
              Copy
            </>
          )}
        </button>
      </div>

      {/* Code content */}
      <div className="overflow-x-auto p-4">
        <pre className="text-sm leading-relaxed">
          <code className="font-mono text-foreground/90">{code}</code>
        </pre>
      </div>
    </div>
  );
}
