import React from "react";

interface Props {
  text: string;
}

export default function RichText({ text }: Props) {
  // Parse **bold** and *italic* and `code` inline
  const parts: React.ReactNode[] = [];
  const regex = /(\*\*(.+?)\*\*|\*(.+?)\*|`(.+?)`)/g;
  let lastIndex = 0;
  let match: RegExpExecArray | null;
  let key = 0;

  while ((match = regex.exec(text)) !== null) {
    if (match.index > lastIndex) {
      parts.push(text.slice(lastIndex, match.index));
    }
    if (match[2]) {
      parts.push(
        <strong key={key++} className="font-semibold text-foreground">
          {match[2]}
        </strong>
      );
    } else if (match[3]) {
      parts.push(
        <em key={key++} className="italic text-foreground/80">
          {match[3]}
        </em>
      );
    } else if (match[4]) {
      parts.push(
        <code
          key={key++}
          className="rounded bg-muted px-1.5 py-0.5 text-sm font-mono text-primary"
        >
          {match[4]}
        </code>
      );
    }
    lastIndex = match.index + match[0].length;
  }

  if (lastIndex < text.length) {
    parts.push(text.slice(lastIndex));
  }

  return <>{parts}</>;
}
