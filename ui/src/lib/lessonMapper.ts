import { LessonBlock, LessonData } from "../types/lessonContent";

/**
 * Maps a raw block from backend into a typed LessonBlock
 * Handles multiple payload shapes and detects HTML vs plain text
 */
function coerceBlock(raw: any): LessonBlock | null {
  if (!raw) return null;

  if (typeof raw === "string") {
    const isHtml = /<\/?[a-z][\s\S]*>/i.test(raw);
    return isHtml ? { type: "html", content: raw } : { type: "text", content: raw };
  }

  const type = raw.type;
  const content = raw.content;

  // Typed blocks: direct mapping
  if (type === "heading" && typeof content === "string") {
    return { type: "heading", content };
  }

  if (type === "text" && typeof content === "string") {
    const isHtml = /<\/?[a-z][\s\S]*>/i.test(content);
    return isHtml ? { type: "html", content } : { type: "text", content };
  }

  if (type === "html" && typeof content === "string") {
    return { type: "html", content };
  }

  if (type === "list") {
    const items = Array.isArray(content)
      ? content.map((item) => String(item))
      : typeof content === "string"
        ? content.split("\n").filter(Boolean)
        : [];
    return items.length > 0 ? { type: "list", content: items } : null;
  }

  if (type === "table" && content?.headers && content?.rows) {
    const headers = Array.isArray(content.headers) ? content.headers.map(String) : [];
    const rows = Array.isArray(content.rows)
      ? content.rows.map((row: any) => (Array.isArray(row) ? row.map(String) : []))
      : [];
    return headers.length > 0 || rows.length > 0 ? { type: "table", content: { headers, rows } } : null;
  }

  if (type === "quiz" && content?.question) {
    return {
      type: "quiz",
      content: {
        question: String(content.question),
        options: Array.isArray(content.options) ? content.options.map(String) : [],
        correctIndex: typeof content.correctIndex === "number" ? content.correctIndex : 0,
        explanation: typeof content.explanation === "string" ? content.explanation : undefined,
      },
    };
  }

  if (type === "code") {
    const codeContent =
      typeof content === "string"
        ? { language: "text", code: content }
        : {
            language: typeof content?.language === "string" ? content.language : "text",
            code: typeof content?.code === "string" ? content.code : "",
          };
    return codeContent.code ? { type: "code", content: codeContent } : null;
  }

  if (type === "youtube" && content?.url) {
    return {
      type: "youtube",
      content: {
        url: String(content.url),
        title: typeof content.title === "string" ? content.title : undefined,
      },
    };
  }

  if (type === "video") {
    const url = typeof content?.url === "string" ? content.url : typeof raw?.url === "string" ? raw.url : "";
    if (!url) return null;
    return {
      type: "youtube",
      content: {
        url,
        title: typeof content?.title === "string" ? content.title : typeof raw?.title === "string" ? raw.title : undefined,
      },
    };
  }

  if (type === "image") {
    const url = typeof content?.url === "string" ? content.url : typeof raw?.url === "string" ? raw.url : "";
    if (!url) return null;
    return {
      type: "image",
      content: {
        url,
        alt:
          typeof content?.alt === "string"
            ? content.alt
            : typeof raw?.alt === "string"
              ? raw.alt
              : "Lesson image",
        prompt:
          typeof content?.prompt === "string"
            ? content.prompt
            : typeof raw?.prompt === "string"
              ? raw.prompt
              : undefined,
      },
    };
  }

  if (type === "reference" && Array.isArray(content)) {
    const refs = content
      .map((item: any) => ({
        title: typeof item?.title === "string" ? item.title : "Untitled",
        url: typeof item?.url === "string" ? item.url : "",
        description: typeof item?.description === "string" ? item.description : undefined,
      }))
      .filter((item) => item.url);

    return refs.length > 0 ? { type: "reference", content: refs } : null;
  }

  // Heuristics for non-typed blocks
  if (typeof raw.heading === "string") {
    return { type: "heading", content: raw.heading };
  }

  if (typeof raw.text === "string") {
    const isHtml = /<\/?[a-z][\s\S]*>/i.test(raw.text);
    return isHtml ? { type: "html", content: raw.text } : { type: "text", content: raw.text };
  }

  if (Array.isArray(raw.list)) {
    const items = raw.list.map(String);
    return items.length > 0 ? { type: "list", content: items } : null;
  }

  return null;
}

/**
 * Parses raw lesson content (from backend) into normalized LessonBlock[]
 * Handles strings, JSON strings, arrays, and nested objects
 */
export function parseLessonBlocks(raw: unknown): LessonBlock[] {
  if (!raw) return [];

  let source: unknown = raw;

  // If string, try JSON parse
  if (typeof source === "string") {
    try {
      source = JSON.parse(source);
    } catch {
      // If parse fails, treat as plain text content
      const isHtml = /<\/?[a-z][\s\S]*>/i.test(source);
      return [isHtml ? { type: "html", content: source } : { type: "text", content: source }];
    }
  }

  // If already an array of blocks
  if (Array.isArray(source)) {
    return source.map(coerceBlock).filter((item): item is LessonBlock => Boolean(item));
  }

  // If object with blocks array
  if (typeof source === "object") {
    const obj = source as Record<string, unknown>;
    if (Array.isArray(obj.blocks)) {
      return obj.blocks.map(coerceBlock).filter((item): item is LessonBlock => Boolean(item));
    }

    // Single block as object
    const single = coerceBlock(obj);
    return single ? [single] : [];
  }

  return [];
}

/**
 * Normalizes raw lesson response from backend into typed LessonData
 */
export function normalizeLessonData(raw: any): LessonData {
  let contentSource: any = raw;

  if (raw && typeof raw === "object" && !Array.isArray(raw)) {
    contentSource =
      raw.content ??
      raw.contentBlocks ??
      raw.blocks ??
      raw.contentMd ??
      raw.content_md ??
      raw.markdown ??
      raw;
  }

  const blocks = parseLessonBlocks(contentSource);

  return {
    id: String(raw?.id || ""),
    title: String(raw?.title || "Untitled Lesson"),
    content: blocks,
    new: Boolean(raw?.new),
    enriched: Boolean(raw?.enriched ?? raw?.isEnriched ?? raw?.is_enriched ?? blocks.length > 0),
  };
}

/**
 * Checks if lesson needs content generation
 */
export function needsLessonGeneration(lesson: any): boolean {
  let contentSource: any = lesson;

  if (lesson && typeof lesson === "object" && !Array.isArray(lesson)) {
    contentSource =
      lesson.content ??
      lesson.contentBlocks ??
      lesson.blocks ??
      lesson.contentMd ??
      lesson.content_md ??
      lesson.markdown ??
      lesson;
  }
  
  if (typeof contentSource === "string" && contentSource.trim().length > 0) {
    return false; // raw string content is ready
  }

  const blocks = parseLessonBlocks(contentSource);
  if (blocks.length > 0) return false;
  
  return lesson?.enriched === false || lesson?.new === true;
}

