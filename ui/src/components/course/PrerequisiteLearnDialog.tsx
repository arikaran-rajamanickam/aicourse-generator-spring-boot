import { useState, useCallback, useRef, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import { Label } from "@/components/ui/label";
import {
  BookOpen,
  GraduationCap,
  Sparkles,
  Loader2,
  ChevronRight,
  Lightbulb,
  CheckCircle2,
  RefreshCw,
  Settings2,
  ExternalLink,
} from "lucide-react";
import { getCoachResponse } from "@/services/coachApi";
import {
  getCachedPrereqExplanation,
  savePrereqExplanation,
} from "@/services/prerequisiteApi";
import { cn } from "@/lib/utils";

// ─── Configuration ──────────────────────────────────────────────
export interface PrerequisiteFlowConfig {
  enableQuickLearn: boolean;
  enableCreateCourse: boolean;
  excludeReferences: boolean;
  excludeQuizCards: boolean;
  includeStudyPlan: boolean;
  promptPrefix: string;
  explanationDepth: "brief" | "standard" | "detailed";
}

export const DEFAULT_PREREQ_CONFIG: PrerequisiteFlowConfig = {
  enableQuickLearn: true,
  enableCreateCourse: true,
  excludeReferences: false,
  excludeQuizCards: false,
  includeStudyPlan: true,
  promptPrefix: "",
  explanationDepth: "standard",
};

// ─── In-memory cache so we don't re-call AI for the same prerequisite ─────
interface CachedResult {
  blocks: any[];
  suggestions: string[];
}
const prereqCache = new Map<string, CachedResult>();

function buildCacheKey(
  prerequisite: string,
  courseId?: string,
  depth?: string
): string {
  return `${prerequisite}::${courseId || "none"}::${depth || "standard"}`;
}

// ─── Lightweight Markdown renderer ──────────────────────────────
function MarkdownContent({ text }: { text: string }) {
  const rendered = useMemo(() => parseMarkdown(text), [text]);
  return <div className="prereq-md space-y-2.5">{rendered}</div>;
}

function parseMarkdown(raw: string): React.ReactNode[] {
  const lines = raw.split("\n");
  const nodes: React.ReactNode[] = [];
  let key = 0;
  let listItems: string[] = [];
  let listOrdered = false;

  const flushList = () => {
    if (listItems.length === 0) return;
    const Tag = listOrdered ? "ol" : "ul";
    const cls = listOrdered
      ? "list-decimal pl-5 space-y-1 text-sm text-muted-foreground"
      : "list-disc pl-5 space-y-1 text-sm text-muted-foreground";
    nodes.push(
      <Tag key={key++} className={cls}>
        {listItems.map((li, j) => (
          <li key={j}>
            <InlineMarkdown text={li} />
          </li>
        ))}
      </Tag>
    );
    listItems = [];
  };

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];

    // Headings
    const headingMatch = line.match(/^(#{1,4})\s+(.+)$/);
    if (headingMatch) {
      flushList();
      const level = headingMatch[1].length;
      const headingText = headingMatch[2];
      const cls =
        level === 1
          ? "text-lg font-bold text-foreground mt-3"
          : level === 2
            ? "text-base font-semibold text-foreground mt-2.5"
            : level === 3
              ? "text-sm font-semibold text-foreground mt-2"
              : "text-sm font-medium text-foreground mt-1.5";
      nodes.push(
        <div key={key++} className={cls}>
          <InlineMarkdown text={headingText} />
        </div>
      );
      continue;
    }

    // Unordered list items (- or *)
    const ulMatch = line.match(/^\s*[-*]\s+(.+)$/);
    if (ulMatch) {
      if (listOrdered && listItems.length) flushList();
      listOrdered = false;
      listItems.push(ulMatch[1]);
      continue;
    }

    // Ordered list items (1. 2. etc.)
    const olMatch = line.match(/^\s*\d+[.)]\s+(.+)$/);
    if (olMatch) {
      if (!listOrdered && listItems.length) flushList();
      listOrdered = true;
      listItems.push(olMatch[1]);
      continue;
    }

    // Empty line
    if (line.trim() === "") {
      flushList();
      continue;
    }

    // Horizontal rule
    if (/^---+$/.test(line.trim())) {
      flushList();
      nodes.push(<hr key={key++} className="border-border/50 my-2" />);
      continue;
    }

    // Regular paragraph
    flushList();
    nodes.push(
      <p key={key++} className="text-sm text-muted-foreground leading-relaxed">
        <InlineMarkdown text={line} />
      </p>
    );
  }
  flushList();
  return nodes;
}

/** Inline markdown: **bold**, *italic*, `code`, [link](url) */
function InlineMarkdown({ text }: { text: string }) {
  const parts: React.ReactNode[] = [];
  const regex = /(\*\*(.+?)\*\*|\*(.+?)\*|`(.+?)`|\[([^\]]+)\]\(([^)]+)\))/g;
  let lastIndex = 0;
  let match: RegExpExecArray | null;
  let k = 0;

  while ((match = regex.exec(text)) !== null) {
    if (match.index > lastIndex) {
      parts.push(text.slice(lastIndex, match.index));
    }
    if (match[2]) {
      parts.push(
        <strong key={k++} className="font-semibold text-foreground">
          {match[2]}
        </strong>
      );
    } else if (match[3]) {
      parts.push(
        <em key={k++} className="italic text-foreground/80">
          {match[3]}
        </em>
      );
    } else if (match[4]) {
      parts.push(
        <code
          key={k++}
          className="rounded bg-muted px-1.5 py-0.5 text-xs font-mono text-primary"
        >
          {match[4]}
        </code>
      );
    } else if (match[5] && match[6]) {
      parts.push(
        <a
          key={k++}
          href={match[6]}
          target="_blank"
          rel="noopener noreferrer"
          className="text-primary underline underline-offset-2 hover:text-primary/80"
        >
          {match[5]}
        </a>
      );
    }
    lastIndex = match.index + match[0].length;
  }
  if (lastIndex < text.length) {
    parts.push(text.slice(lastIndex));
  }
  return <>{parts}</>;
}

// ─── Props ──────────────────────────────────────────────────────
interface PrerequisiteLearnDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  prerequisite: string;
  courseId?: string;
  courseTitle?: string;
  config?: Partial<PrerequisiteFlowConfig>;
}

// ─── Inline content renderer ────────────────────────────────────
function QuickLearnContent({
  blocks,
  excludeReferences,
  excludeQuizCards,
}: {
  blocks: any[];
  excludeReferences: boolean;
  excludeQuizCards: boolean;
}) {
  const filteredBlocks = blocks.filter((block) => {
    if (excludeQuizCards && block.type === "quiz_card") return false;
    return true;
  });

  return (
    <div className="space-y-5">
      {filteredBlocks.map((block, i) => {
        if (block.type === "text") {
          const content = block.content as { title?: string; body: string };
          return (
            <div key={i} className="space-y-2">
              {content.title && (
                <h4 className="font-semibold text-sm text-foreground flex items-center gap-2">
                  <Lightbulb className="w-4 h-4 text-amber-500 shrink-0" />
                  {content.title}
                </h4>
              )}
              <MarkdownContent text={content.body} />
            </div>
          );
        }

        if (block.type === "study_plan") {
          const plan = block.content as {
            goal: string;
            duration: string;
            steps: { title: string; task: string; time: string }[];
          };
          return (
            <div key={i} className="border rounded-lg p-4 bg-primary/5 space-y-3">
              <div className="flex items-center gap-2">
                <GraduationCap className="w-4 h-4 text-primary" />
                <span className="font-semibold text-sm text-foreground">
                  Study Plan
                </span>
                <span className="text-xs text-muted-foreground ml-auto">
                  {plan.duration}
                </span>
              </div>
              <p className="text-xs text-muted-foreground">{plan.goal}</p>
              <div className="space-y-2">
                {plan.steps?.map((step, j) => (
                  <div
                    key={j}
                    className="flex items-start gap-3 text-sm p-2 rounded-md bg-background/60"
                  >
                    <span className="font-mono text-xs text-primary bg-primary/10 rounded px-1.5 py-0.5 mt-0.5 shrink-0">
                      {j + 1}
                    </span>
                    <div className="flex-1 min-w-0">
                      <span className="font-medium text-foreground">
                        {step.title}
                      </span>
                      <p className="text-xs text-muted-foreground mt-0.5">
                        {step.task}
                      </p>
                    </div>
                    <span className="text-[10px] text-muted-foreground shrink-0">
                      {step.time}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          );
        }

        if (block.type === "quiz_card") {
          const quiz = block.content as {
            question: string;
            options: string[];
            correctIndex: number;
            explanation?: string;
          };
          return <QuizCard key={i} quiz={quiz} />;
        }

        return null;
      })}
    </div>
  );
}

// ─── Mini quiz card ──────────────────────────────────────────────
function QuizCard({
  quiz,
}: {
  quiz: {
    question: string;
    options: string[];
    correctIndex: number;
    explanation?: string;
  };
}) {
  const [selected, setSelected] = useState<number | null>(null);
  const isAnswered = selected !== null;
  const isCorrect = selected === quiz.correctIndex;

  return (
    <div className="border rounded-lg p-4 bg-muted/30 space-y-3">
      <div className="flex items-center gap-2">
        <Sparkles className="w-4 h-4 text-purple-500" />
        <span className="font-semibold text-sm text-foreground">
          Quick Check
        </span>
      </div>
      <p className="text-sm font-medium text-foreground">{quiz.question}</p>
      <div className="space-y-2">
        {quiz.options.map((opt, i) => {
          const isThis = selected === i;
          const isRight = i === quiz.correctIndex;

          return (
            <div
              key={i}
              className={cn(
                "border rounded-md px-3 py-2 text-sm cursor-pointer transition-all",
                isAnswered
                  ? isRight
                    ? "border-green-500/50 bg-green-500/10 text-green-600"
                    : isThis
                      ? "border-red-500/50 bg-red-500/10 text-red-500"
                      : "border-border/50 text-muted-foreground opacity-60"
                  : "border-border hover:border-primary/50 hover:bg-muted/50"
              )}
              onClick={() => !isAnswered && setSelected(i)}
            >
              {opt}
            </div>
          );
        })}
      </div>
      {isAnswered && quiz.explanation && (
        <p className="text-xs text-muted-foreground bg-background/60 rounded-md p-2 mt-2">
          {isCorrect ? "✅" : "💡"} {quiz.explanation}
        </p>
      )}
    </div>
  );
}

// ─── Settings panel ──────────────────────────────────────────────
function PrereqSettingsPanel({
  config,
  onChange,
}: {
  config: PrerequisiteFlowConfig;
  onChange: (config: PrerequisiteFlowConfig) => void;
}) {
  const toggle = (key: keyof PrerequisiteFlowConfig) => {
    onChange({ ...config, [key]: !config[key] });
  };

  return (
    <div className="space-y-4 p-4 border rounded-lg bg-muted/20">
      <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-muted-foreground">
        <Settings2 className="w-3.5 h-3.5" />
        Learning Preferences
      </div>

      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <Label htmlFor="exclude-refs" className="text-sm cursor-pointer">
            Exclude external references
          </Label>
          <Switch
            id="exclude-refs"
            checked={config.excludeReferences}
            onCheckedChange={() => toggle("excludeReferences")}
          />
        </div>
        <div className="flex items-center justify-between">
          <Label htmlFor="exclude-quiz" className="text-sm cursor-pointer">
            Exclude quizzes
          </Label>
          <Switch
            id="exclude-quiz"
            checked={config.excludeQuizCards}
            onCheckedChange={() => toggle("excludeQuizCards")}
          />
        </div>
        <div className="flex items-center justify-between">
          <Label htmlFor="include-plan" className="text-sm cursor-pointer">
            Include study plan
          </Label>
          <Switch
            id="include-plan"
            checked={config.includeStudyPlan}
            onCheckedChange={() => toggle("includeStudyPlan")}
          />
        </div>

        <div className="space-y-1.5">
          <Label className="text-sm">Explanation depth</Label>
          <div className="flex gap-2">
            {(["brief", "standard", "detailed"] as const).map((depth) => (
              <button
                key={depth}
                onClick={() => onChange({ ...config, explanationDepth: depth })}
                className={cn(
                  "flex-1 py-1.5 px-2 rounded-md text-xs font-medium capitalize transition-all",
                  config.explanationDepth === depth
                    ? "bg-primary text-primary-foreground shadow-sm"
                    : "bg-muted/50 text-muted-foreground hover:bg-muted"
                )}
              >
                {depth}
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

// ─── Main Dialog ─────────────────────────────────────────────────
export default function PrerequisiteLearnDialog({
  open,
  onOpenChange,
  prerequisite,
  courseId,
  courseTitle,
  config: configOverride,
}: PrerequisiteLearnDialogProps) {
  const navigate = useNavigate();
  const mergedConfig = { ...DEFAULT_PREREQ_CONFIG, ...configOverride };

  const [mode, setMode] = useState<"choose" | "learning" | "settings">("choose");
  const [loading, setLoading] = useState(false);
  const [learnedContent, setLearnedContent] = useState<any[] | null>(null);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [flowConfig, setFlowConfig] = useState<PrerequisiteFlowConfig>(mergedConfig);
  const [error, setError] = useState<string | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);

  const handleQuickLearn = useCallback(
    async (forceRefresh = false) => {
      setMode("learning");
      setError(null);

      const cacheKey = buildCacheKey(prerequisite, courseId, flowConfig.explanationDepth);

      // ─── Layer 1: In-memory cache ───
      if (!forceRefresh && prereqCache.has(cacheKey)) {
        const cached = prereqCache.get(cacheKey)!;
        setLearnedContent(cached.blocks);
        setSuggestions(cached.suggestions);
        return;
      }

      // ─── Layer 2: Backend / DB cache ───
      if (!forceRefresh && courseId) {
        try {
          const dbCached = await getCachedPrereqExplanation(
            courseId,
            prerequisite,
            flowConfig.explanationDepth
          );
          if (dbCached && dbCached.blocks && dbCached.blocks.length > 0) {
            // Populate in-memory cache too
            prereqCache.set(cacheKey, dbCached);
            setLearnedContent(dbCached.blocks);
            setSuggestions(dbCached.suggestions || []);
            return;
          }
        } catch {
          // Backend cache miss or error — fall through to AI generation
        }
      }

      // ─── Layer 3: AI generation (only if both caches miss) ───
      setLoading(true);
      setLearnedContent(null);

      const depthInstructions = {
        brief: "Give a brief, concise overview in 2-3 paragraphs.",
        standard:
          "Give a clear, well-structured explanation with key concepts and examples.",
        detailed:
          "Give a thorough, in-depth explanation with multiple examples, edge cases, and comparisons.",
      };

      let prompt = flowConfig.promptPrefix
        ? `${flowConfig.promptPrefix}\n\n`
        : "";
      prompt += `I'm learning "${courseTitle || "a course"}" and need to understand a prerequisite: "${prerequisite}". `;
      prompt += depthInstructions[flowConfig.explanationDepth] + " ";

      if (flowConfig.includeStudyPlan) {
        prompt += "Also include a short study plan to master this prerequisite. ";
      }
      if (flowConfig.excludeReferences) {
        prompt += "Do NOT include external references or citations. ";
      }
      if (!flowConfig.excludeQuizCards) {
        prompt += "Include a quick quiz question to test understanding. ";
      }

      try {
        const response = await getCoachResponse({
          courseId: courseId,
          message: prompt,
        });

        const result: CachedResult = {
          blocks: response.blocks || [],
          suggestions: response.suggestions || [],
        };

        // ─── Store in memory cache ───
        prereqCache.set(cacheKey, result);

        // ─── Persist to backend (fire-and-forget) ───
        if (courseId) {
          savePrereqExplanation(
            courseId,
            prerequisite,
            flowConfig.explanationDepth,
            result
          );
        }

        setLearnedContent(result.blocks);
        setSuggestions(result.suggestions);
      } catch (err: any) {
        setError(
          err.message || "Failed to generate explanation. Please try again."
        );
      } finally {
        setLoading(false);
      }
    },
    [prerequisite, courseId, courseTitle, flowConfig]
  );

  const handleCreateCourse = () => {
    onOpenChange(false);
    navigate(`/create-course?topic=${encodeURIComponent(prerequisite)}`);
  };

  const handleFollowUp = async (question: string) => {
    setLoading(true);
    setError(null);
    try {
      const response = await getCoachResponse({
        courseId: courseId,
        message: `Following up on the prerequisite "${prerequisite}": ${question}`,
        chatHistory: [
          { role: "user", text: `Explain the prerequisite: ${prerequisite}` },
          { role: "assistant", text: "I explained the prerequisite above." },
        ],
      });
      setLearnedContent((prev) => [...(prev || []), ...response.blocks]);
      setSuggestions(response.suggestions || []);

      // Scroll to bottom after new content
      requestAnimationFrame(() => {
        scrollRef.current?.scrollTo({
          top: scrollRef.current.scrollHeight,
          behavior: "smooth",
        });
      });
    } catch (err: any) {
      setError(err.message || "Failed to get follow-up.");
    } finally {
      setLoading(false);
    }
  };

  const handleReset = () => {
    setMode("choose");
    setLearnedContent(null);
    setSuggestions([]);
    setError(null);
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(val) => {
        if (!val) handleReset();
        onOpenChange(val);
      }}
    >
      {/* Bug #4 fix: bigger dialog — 780px wide, 90vh tall */}
      <DialogContent className="sm:max-w-[780px] max-h-[90vh] flex flex-col p-0 gap-0 overflow-hidden">
        <DialogHeader className="px-6 pt-6 pb-4 border-b border-border/50 shrink-0">
          <DialogTitle className="flex items-center gap-2 text-lg">
            <BookOpen className="w-5 h-5 text-primary" />
            {mode === "settings" ? "Learning Preferences" : prerequisite}
          </DialogTitle>
          <DialogDescription className="text-sm">
            {mode === "choose" && "Choose how you'd like to learn this prerequisite"}
            {mode === "learning" && (
              <>
                AI-powered quick explanation
                {prereqCache.has(
                  buildCacheKey(prerequisite, courseId, flowConfig.explanationDepth)
                ) && !loading && (
                  <span className="ml-2 inline-flex items-center gap-1 text-xs text-green-500">
                    <CheckCircle2 className="w-3 h-3" /> cached
                  </span>
                )}
              </>
            )}
            {mode === "settings" && "Customize how prerequisites are explained"}
          </DialogDescription>
        </DialogHeader>

        {/* Bug #2 fix: min-h-0 allows flex child to shrink & scroll */}
        <div className="flex-1 min-h-0 overflow-hidden flex flex-col">
          {/* Choice mode */}
          {mode === "choose" && (
            <div className="p-6 space-y-4 overflow-y-auto">
              {flowConfig.enableQuickLearn && (
                <button
                  onClick={() => handleQuickLearn()}
                  className="w-full group flex items-start gap-4 p-4 rounded-xl border border-border/60 hover:border-primary/40 hover:bg-primary/5 transition-all text-left"
                >
                  <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary/10 group-hover:bg-primary/20 transition-colors">
                    <Sparkles className="w-5 h-5 text-primary" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <h3 className="font-semibold text-foreground group-hover:text-primary transition-colors">
                      Quick Learn
                    </h3>
                    <p className="text-sm text-muted-foreground mt-1">
                      Get an AI-powered explanation right here. No need to leave
                      your course — understand the basics in minutes.
                    </p>
                  </div>
                  <ChevronRight className="w-5 h-5 text-muted-foreground group-hover:text-primary mt-2.5 shrink-0 transition-colors" />
                </button>
              )}

              {flowConfig.enableCreateCourse && (
                <button
                  onClick={handleCreateCourse}
                  className="w-full group flex items-start gap-4 p-4 rounded-xl border border-border/60 hover:border-blue-500/40 hover:bg-blue-500/5 transition-all text-left"
                >
                  <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-blue-500/10 group-hover:bg-blue-500/20 transition-colors">
                    <GraduationCap className="w-5 h-5 text-blue-500" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <h3 className="font-semibold text-foreground group-hover:text-blue-500 transition-colors">
                      Create Full Course
                    </h3>
                    <p className="text-sm text-muted-foreground mt-1">
                      Generate a complete, structured course on this topic with
                      modules, lessons, and quizzes.
                    </p>
                  </div>
                  <ExternalLink className="w-5 h-5 text-muted-foreground group-hover:text-blue-500 mt-2.5 shrink-0 transition-colors" />
                </button>
              )}

              <div className="pt-2 flex justify-end">
                <Button
                  variant="ghost"
                  size="sm"
                  className="text-xs text-muted-foreground gap-1"
                  onClick={() => setMode("settings")}
                >
                  <Settings2 className="w-3.5 h-3.5" />
                  Preferences
                </Button>
              </div>
            </div>
          )}

          {/* Settings mode */}
          {mode === "settings" && (
            <div className="p-6 space-y-4 overflow-y-auto">
              <PrereqSettingsPanel config={flowConfig} onChange={setFlowConfig} />
              <div className="flex justify-end">
                <Button variant="outline" size="sm" onClick={() => setMode("choose")}>
                  Done
                </Button>
              </div>
            </div>
          )}

          {/* Learning mode — Bug #2 fix: native overflow-y-auto with min-h-0 */}
          {mode === "learning" && (
            <>
              <div
                ref={scrollRef}
                className="flex-1 min-h-0 overflow-y-auto px-6 py-5"
              >
                {loading && !learnedContent && (
                  <div className="flex flex-col items-center justify-center py-16 gap-3">
                    <Loader2 className="w-8 h-8 text-primary animate-spin" />
                    <p className="text-sm text-muted-foreground">
                      Generating explanation for{" "}
                      <span className="font-medium text-foreground">
                        {prerequisite}
                      </span>
                      ...
                    </p>
                  </div>
                )}

                {error && (
                  <div className="flex flex-col items-center justify-center py-16 gap-3">
                    <p className="text-sm text-red-500">{error}</p>
                    <Button variant="outline" size="sm" onClick={() => handleQuickLearn()}>
                      <RefreshCw className="w-4 h-4 mr-1" />
                      Retry
                    </Button>
                  </div>
                )}

                {learnedContent && (
                  <div className="space-y-6">
                    <QuickLearnContent
                      blocks={learnedContent}
                      excludeReferences={flowConfig.excludeReferences}
                      excludeQuizCards={flowConfig.excludeQuizCards}
                    />

                    {/* Follow-up suggestions */}
                    {suggestions.length > 0 && (
                      <div className="space-y-2 pt-4 border-t border-border/50">
                        <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                          Want to know more?
                        </p>
                        <div className="flex flex-wrap gap-2">
                          {suggestions.map((s, i) => (
                            <button
                              key={i}
                              disabled={loading}
                              onClick={() => handleFollowUp(s)}
                              className="text-xs px-3 py-1.5 rounded-full border border-border/60 hover:border-primary/40 text-muted-foreground hover:text-primary transition-all disabled:opacity-50"
                            >
                              {s}
                            </button>
                          ))}
                        </div>
                      </div>
                    )}

                    {loading && (
                      <div className="flex items-center gap-2 py-3">
                        <Loader2 className="w-4 h-4 text-primary animate-spin" />
                        <span className="text-xs text-muted-foreground">
                          Loading more...
                        </span>
                      </div>
                    )}
                  </div>
                )}
              </div>

              {/* Footer actions */}
              {learnedContent && !loading && (
                <div className="px-6 py-3 border-t border-border/50 flex items-center justify-between gap-2 shrink-0 bg-background">
                  <div className="flex items-center gap-2">
                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-xs gap-1"
                      onClick={() => handleQuickLearn(true)}
                    >
                      <RefreshCw className="w-3.5 h-3.5" />
                      Regenerate
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-xs gap-1"
                      onClick={() => setMode("settings")}
                    >
                      <Settings2 className="w-3.5 h-3.5" />
                      Settings
                    </Button>
                  </div>

                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      className="text-xs gap-1"
                      onClick={handleCreateCourse}
                    >
                      <GraduationCap className="w-3.5 h-3.5" />
                      Create Full Course
                    </Button>
                    <Button
                      size="sm"
                      className="text-xs gap-1"
                      onClick={() => {
                        onOpenChange(false);
                        handleReset();
                      }}
                    >
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      Got it
                    </Button>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}




