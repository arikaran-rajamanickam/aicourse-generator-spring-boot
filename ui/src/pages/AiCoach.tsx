import { FormEvent, useMemo, useState, useRef, useEffect } from "react";
import { Link, useParams } from "react-router-dom";
import { Bot, ChevronLeft, Send, Sparkles, User, RotateCw, CheckCircle2, XCircle, Clock, BookOpen, GraduationCap, ExternalLink, Menu, Plus, MessageSquare, X, PanelLeftClose, PanelLeft, MoreHorizontal, Database } from "lucide-react";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { toast } from "sonner";
import { getCoachResponse, streamCoachResponse } from "../services/coachApi";
import { USE_MCP_CLIENT } from "../constants";
import {
  CoachBlock,
  CoachCitation,
  CoachFlashcardContent,
  CoachQuizCardContent,
  CoachResponse,
  CoachStudyPlanContent,
  CoachTextContent,
  CoachChatMessage,
} from "../types/coach";
import { cn } from "../lib/utils";

type ChatMessage = {
  role: "user" | "assistant";
  text?: string;
  textStream?: string;
  payload?: CoachResponse;
};

function extractJson(text: string): { payload: any, textBefore: string } | null {
  const trimmed = text.trim();
  
  // 1. Try direct parse of the whole thing
  try {
    const p = JSON.parse(trimmed);
    if (p && typeof p === 'object') return { payload: p, textBefore: "" };
  } catch (e) {}

  // 2. Try grabbing content between ```json and ```
  const jsonMatch = text.match(/```json\s*([\s\S]*?)\s*```/);
  if (jsonMatch && jsonMatch[1]) {
    try {
      const p = JSON.parse(jsonMatch[1].trim());
      if (p && typeof p === 'object') {
        const textBefore = text.substring(0, jsonMatch.index).trim();
        return { payload: p, textBefore };
      }
    } catch (e) {}
  }

  // 3. Try finding the first { and last }
  const firstBrace = text.indexOf('{');
  const lastBrace = text.lastIndexOf('}');
  if (firstBrace !== -1 && lastBrace !== -1 && lastBrace > firstBrace) {
    const potentialJson = text.substring(firstBrace, lastBrace + 1);
    try {
      const p = JSON.parse(potentialJson);
      if (p && typeof p === 'object') {
        const textBefore = text.substring(0, firstBrace).trim();
        return { payload: p, textBefore };
      }
    } catch (e) {}
  }

  return null;
}

function normalizeCoachBlocks(payload?: any): CoachBlock[] {
  if (!payload) return [];
  
  // 1. If it's already an array of blocks
  if (Array.isArray(payload)) {
    return payload;
  }

  // 2. If it has a blocks field
  if (Array.isArray(payload.blocks)) {
    return payload.blocks;
  }

  // 3. If it has a data field that is an array
  if (Array.isArray(payload.data)) {
    return payload.data;
  }
  
  // 4. If it has a data field that has blocks
  if (payload.data && Array.isArray(payload.data.blocks)) {
    return payload.data.blocks;
  }

  // 5. Fallback: If it's a raw string or has a message/text field, wrap it in a text block
  const rawText = typeof payload === "string" ? payload : payload.message ?? payload.text ?? payload.body ?? "";
  if (rawText) {
    return [{
      type: "text",
      content: { body: rawText }
    }];
  }

  // 6. Final safety fallback: Just show the raw JSON so the bubble isn't blank
  if (typeof payload === "object") {
    return [{
      type: "text",
      content: { body: "```json\n" + JSON.stringify(payload, null, 2) + "\n```" }
    }];
  }

  return [];
}

interface ChatSession {
  id: string;
  title: string;
  updatedAt: number;
  messages: ChatMessage[];
}

function extractChatHistory(messages: ChatMessage[]): CoachChatMessage[] {
  const recentMessages = messages.slice(-5);
  return recentMessages.map(msg => {
    if (msg.role === "user") {
      return { role: "user", text: msg.text || "" };
    } else {
      let assistantText = "";
      const blocks = normalizeCoachBlocks(msg.payload);
      if (blocks.length > 0) {
        blocks.forEach(block => {
          if (block.type === "text") {
            const textContent = block.content as CoachTextContent;
            if (textContent.title) assistantText += `**${textContent.title}**\n`;
            if (textContent.body) assistantText += textContent.body + "\n";
          }
        });
      }
      return { role: "assistant", text: assistantText.trim() };
    }
  });
}

function extractPreviousQuizQuestions(messages: ChatMessage[]): string[] {
  const seen = new Set<string>();
  const questions: string[] = [];

  for (const message of messages) {
    const blocks = message.role === "assistant" ? normalizeCoachBlocks(message.payload) : [];
    if (blocks.length === 0) {
      continue;
    }

    for (const block of blocks) {
      if (block.type !== "quiz_card") {
        continue;
      }
      const content = block.content as CoachQuizCardContent;
      const question = (content?.question ?? "").trim();
      if (!question) {
        continue;
      }
      const key = question.toLowerCase();
      if (seen.has(key)) {
        continue;
      }
      seen.add(key);
      questions.push(question);
    }
  }

  return questions.slice(-8);
}

function CitationList({ citations }: { citations: CoachCitation[] }) {
  if (!citations.length) {
    return null;
  }

  return (
    <div className="mt-2 rounded-lg border border-border/60 p-4 bg-muted/20">
      <h5 className="text-sm font-semibold text-foreground mb-3">References</h5>
      <div className="space-y-3">
        {citations.map((citation, index) => (
          <a
            key={`${citation.url}-${index}`}
            href={citation.url}
            target="_blank"
            rel="noopener noreferrer"
            className="block rounded-md border border-border/60 px-3.5 py-3 hover:bg-muted/40 transition-colors"
          >
            <div className="flex items-start justify-between gap-2">
              <p className="text-[14px] font-medium text-foreground leading-snug">{citation.title}</p>
              <ExternalLink className="w-3.5 h-3.5 mt-0.5 text-muted-foreground shrink-0" />
            </div>
            {citation.description && (
              <p className="text-[13px] text-muted-foreground mt-1 leading-relaxed">{citation.description}</p>
            )}
            {citation.source && (
              <p className="text-[11px] uppercase tracking-wide text-muted-foreground/90 mt-2">Source: {citation.source}</p>
            )}
          </a>
        ))}
      </div>
    </div>
  );
}

const quickPrompts = [
  "Give me a 5-question quiz from this lesson",
  "Create flashcards from the key ideas",
  "Explain this lesson in simple terms",
  "Make a 30-minute study plan",
];

const CHAT_STORAGE_PREFIX = "ai-coach-chat-v2";
const MAX_PERSISTED_MESSAGES = 100;

function getChatStorageKey(courseId?: string, lessonId?: string) {
  if (!courseId) return `${CHAT_STORAGE_PREFIX}:global`;
  return lessonId
    ? `${CHAT_STORAGE_PREFIX}:${courseId}:${lessonId}`
    : `${CHAT_STORAGE_PREFIX}:${courseId}`;
}

function toPersistedMessages(value: unknown): ChatMessage[] {
  if (!Array.isArray(value)) return [];

  return value.filter((item): item is ChatMessage => {
    if (!item || typeof item !== "object" || !("role" in item)) return false;
    const role = (item as { role?: unknown }).role;
    if (role === "user") return typeof (item as { text?: unknown }).text === "string";
    if (role === "assistant") {
      const payload = (item as { payload?: unknown }).payload;
      return !!payload && typeof payload === "object";
    }
    return false;
  });
}

function generateTitle(messages: ChatMessage[]) {
  const firstUserMsg = messages.find(m => m.role === 'user');
  if (!firstUserMsg) return "New Chat";
  const text = firstUserMsg.text || "";
  return text.length > 35 ? text.substring(0, 35) + "..." : text;
}

function migrateToSessions(raw: string): ChatSession[] {
  try {
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) {
      if (parsed.length > 0 && ('role' in parsed[0])) {
        return [{
          id: Date.now().toString(),
          title: generateTitle(parsed),
          updatedAt: Date.now(),
          messages: toPersistedMessages(parsed)
        }];
      }
      return parsed as ChatSession[];
    }
  } catch {
    // Ignore malformed local cache and start with a clean session list.
  }
  return [];
}

// Minimalist, ChatGPT-style widgets
function QuizCard({ content }: { content: CoachQuizCardContent }) {
  const [selected, setSelected] = useState<number | null>(null);
  const isCorrect = selected !== null && selected === content.correctIndex;

  return (
    <div className="rounded-lg border border-border/60 bg-transparent p-5 my-4">
      <div className="flex gap-3 mb-4">
        <BookOpen className="w-5 h-5 text-muted-foreground shrink-0 mt-0.5" />
        <p className="font-medium text-foreground leading-relaxed">{content.question}</p>
      </div>
      <div className="grid gap-2">
        {content.options.map((option, index) => {
          const isSelected = selected === index;
          const isThisCorrect = index === content.correctIndex;

          let buttonClass = "border-border/50 bg-transparent hover:bg-muted/50 text-foreground";
          let Icon = null;

          if (selected !== null) {
            if (isThisCorrect) {
              buttonClass = "border-emerald-500/50 bg-emerald-50/50 dark:bg-emerald-950/20 text-emerald-900 dark:text-emerald-200";
              Icon = <CheckCircle2 className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />;
            } else if (isSelected) {
              buttonClass = "border-rose-500/50 bg-rose-50/50 dark:bg-rose-950/20 text-rose-900 dark:text-rose-200";
              Icon = <XCircle className="w-4 h-4 text-rose-600 dark:text-rose-400" />;
            } else {
              buttonClass = "border-border/30 bg-transparent opacity-60";
            }
          }

          return (
            <button
              key={`${option}-${index}`}
              onClick={() => selected === null && setSelected(index)}
              disabled={selected !== null}
              className={`flex items-center justify-between rounded-md border px-4 py-3 text-left text-[15px] transition-all duration-200 ${buttonClass}`}
            >
              <span className="pr-2 leading-snug">{option}</span>
              {Icon && <div className="shrink-0">{Icon}</div>}
            </button>
          );
        })}
      </div>

      {selected !== null && (
        <div
          className={`mt-4 rounded-md p-4 text-[15px] flex gap-3 items-start ${isCorrect
              ? "bg-emerald-50/50 dark:bg-emerald-950/20 text-emerald-900 dark:text-emerald-200"
              : "bg-rose-50/50 dark:bg-rose-950/20 text-rose-900 dark:text-rose-200"
            }`}
        >
          <div className="leading-relaxed">
            <span className="font-semibold mr-2">
              {isCorrect ? "Correct." : "Incorrect."}
            </span>
            <span className="opacity-90">{content.explanation}</span>
          </div>
        </div>
      )}
    </div>
  );
}

function StudyPlanCard({ content }: { content: CoachStudyPlanContent }) {
  return (
    <div className="rounded-lg border border-border/60 bg-transparent p-5 my-4">
      <div className="flex items-center gap-3 border-b border-border/50 pb-4 mb-4">
        <GraduationCap className="w-5 h-5 text-muted-foreground" />
        <div>
          <h4 className="font-semibold text-foreground text-base">{content.goal}</h4>
          <p className="text-xs text-muted-foreground flex items-center gap-1.5 mt-1">
            <Clock className="w-3.5 h-3.5" />
            {content.duration}
          </p>
        </div>
      </div>
      <div className="space-y-4">
        {content.steps?.map((step, idx) => (
          <div key={`${step.title}-${idx}`} className="flex gap-4">
            <div className="flex flex-col items-center mt-1 text-muted-foreground font-mono text-sm">
              <span className="w-6 h-6 rounded-full bg-muted/80 flex items-center justify-center shrink-0">{idx + 1}</span>
              {idx !== (content.steps?.length ?? 0) - 1 && (
                <div className="w-[1px] h-full bg-border/60 mt-1 mb-1"></div>
              )}
            </div>
            <div className="pb-2 flex-1">
              <div className="flex items-start justify-between gap-2 mb-1">
                <p className="text-[15px] font-semibold text-foreground">{step.title}</p>
                <span className="text-xs text-muted-foreground whitespace-nowrap bg-muted/50 px-2 py-0.5 rounded-sm">
                  {step.time}
                </span>
              </div>
              <p className="text-[15px] text-muted-foreground leading-relaxed">{step.task}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function Flashcard({ content }: { content: CoachFlashcardContent }) {
  const [flipped, setFlipped] = useState(false);

  return (
    <div className="group h-48 w-full max-w-sm [perspective:1000px] my-4 mx-auto" onClick={() => setFlipped(!flipped)}>
      <div
        className={`relative h-full w-full rounded-lg transition-transform duration-500 [transform-style:preserve-3d] cursor-pointer border border-border/60 hover:border-border ${flipped ? "[transform:rotateY(180deg)]" : ""
          }`}
      >
        {/* Front */}
        <div className="absolute inset-0 flex flex-col items-center justify-center rounded-lg bg-card p-6 [backface-visibility:hidden]">
          <div className="absolute top-3 left-3 text-muted-foreground text-[11px] font-semibold uppercase tracking-wider">
            Flashcard
          </div>
          <p className="text-center font-medium text-foreground text-lg leading-snug">{content.front}</p>
          <div className="absolute bottom-3 right-3 text-[11px] text-muted-foreground flex items-center gap-1.5 opacity-0 group-hover:opacity-100 transition-opacity">
            <RotateCw className="w-3 h-3 group-hover:rotate-180 transition-transform duration-500" />
            Click to flip
          </div>
        </div>
        {/* Back */}
        <div className="absolute inset-0 flex flex-col items-center justify-center rounded-lg bg-muted/30 p-6 text-center [transform:rotateY(180deg)] [backface-visibility:hidden]">
          <div className="absolute top-3 left-3 text-muted-foreground text-[11px] font-semibold uppercase tracking-wider">
            Answer
          </div>
          <p className="text-[15px] font-medium text-foreground leading-relaxed">{content.back}</p>
        </div>
      </div>
    </div>
  );
}

// Basic bold markdown parser
function renderBody(body: string) {
  if (!body) return null;
  const parts = body.split(/(\*\*.*?\*\*)/g);
  return parts.map((part, i) => {
    if (part.startsWith('**') && part.endsWith('**')) {
      return <span key={i} className="font-bold">{part.slice(2, -2)}</span>;
    }
    return <span key={i}>{part}</span>;
  });
}

function TypewriterText({ text, onComplete }: { text: string, onComplete?: () => void }) {
  const [displayed, setDisplayed] = useState("");

  useEffect(() => {
    let i = 0;
    const interval = setInterval(() => {
      // reveal 4 characters at a time for faster speed
      i += 4;
      if (i >= text.length) {
        setDisplayed(text);
        clearInterval(interval);
        if (onComplete) onComplete();
      } else {
        setDisplayed(text.substring(0, i));
      }
    }, 10);

    return () => clearInterval(interval);
  }, [text, onComplete]);

  return <>{renderBody(displayed)}</>;
}

function CoachBlockRenderer({
  block,
  animate,
  onComplete
}: {
  block: CoachBlock,
  animate?: boolean,
  onComplete?: () => void
}) {
  if (block.type === "text") {
    const text = block.content as CoachTextContent;


    return (
      <div className="mb-4 text-foreground">
        {text.title && (
          <h4 className="font-semibold text-foreground mb-2 text-base">
            {text.title}
          </h4>
        )}
        <div className="text-[15px] leading-relaxed whitespace-pre-wrap">
          {animate ? (
            <TypewriterText
              text={text.body}
              onComplete={onComplete}
            />
          ) : (
            renderBody(text.body)
          )}
        </div>
      </div>
    );
  }

  // If this block is supposed to be animating (meaning it's the active one), 
  // but it's not a text block (e.g. quiz), we just show it and trigger completion 
  // immediately so the next block can start.
  if (animate && onComplete) {
    onComplete();
  }

  if (block.type === "quiz_card") {
    return <QuizCard content={block.content as CoachQuizCardContent} />;
  }

  if (block.type === "study_plan") {
    return <StudyPlanCard content={block.content as CoachStudyPlanContent} />;
  }

  if (block.type === "flashcard") {
    return <Flashcard content={block.content as CoachFlashcardContent} />;
  }

  return null;
}

export default function AiCoach() {
  const { courseId, lessonId } = useParams();

  const [sessions, setSessions] = useState<ChatSession[]>([]);
  const [currentSessionId, setCurrentSessionId] = useState<string | null>(null);

  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  // track which message index is currently animating so we don't animate old messages
  const [animatingMessageIndex, setAnimatingMessageIndex] = useState<number | null>(null);
  // track which block within the latest message is currently being revealed
  const [activeBlockIndex, setActiveBlockIndex] = useState(0);

  const [isHydrated, setIsHydrated] = useState(false);
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const chatStorageKey = useMemo(() => getChatStorageKey(courseId, lessonId), [courseId, lessonId]);

  const currentSession = useMemo(() => sessions.find(s => s.id === currentSessionId), [sessions, currentSessionId]);
  const messages = currentSession?.messages || [];

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "instant" });
  };

  useEffect(() => {
    if (messages.length > 0) {
      scrollToBottom();
    }
  }, [messages.length, sending, currentSession?.messages]); // Trigger scroll on message content updates

  const handleInput = () => {
    const el = textareaRef.current;
    if (el) {
      el.style.height = "auto"; // reset
      el.style.height = `${Math.min(el.scrollHeight, 200)}px`;
    }
  };

  useEffect(() => {
    setIsHydrated(false);

    if (!chatStorageKey) {
      setSessions([]);
      setCurrentSessionId(null);
      setIsHydrated(true);
      return;
    }

    try {
      const raw = localStorage.getItem(chatStorageKey);
      const loadedSessions = migrateToSessions(raw || "[]");
      loadedSessions.sort((a, b) => b.updatedAt - a.updatedAt);

      setSessions(loadedSessions);
      if (loadedSessions.length > 0) {
        setCurrentSessionId(loadedSessions[0].id);
      } else {
        setCurrentSessionId(null);
      }
    } catch {
      localStorage.removeItem(chatStorageKey);
      setSessions([]);
      setCurrentSessionId(null);
    } finally {
      setIsHydrated(true);
    }
  }, [chatStorageKey]);

  useEffect(() => {
    if (!isHydrated || !chatStorageKey) return;
    localStorage.setItem(chatStorageKey, JSON.stringify(sessions));
  }, [sessions, chatStorageKey, isHydrated]);

  const submitMessage = async (message: string) => {
    if (!message.trim() || sending) return;

    const numericIdPattern = /^\d+$/;
    if (courseId && !numericIdPattern.test(courseId)) {
      toast.error("Invalid course context for AI Coach");
      return;
    }
    if (lessonId && !numericIdPattern.test(lessonId)) {
      toast.error("Invalid lesson context for AI Coach");
      return;
    }

    let targetSessionId = currentSessionId;
    if (!targetSessionId) {
      targetSessionId = Date.now().toString();
      setCurrentSessionId(targetSessionId);
    }

    const userMessage: ChatMessage = { role: "user", text: message.trim() };

    // 1. Add user message and empty assistant placeholder atomically
    setSessions((prev) => {
      let updatedSessions = [...prev];
      let sessionIndex = updatedSessions.findIndex((s) => s.id === targetSessionId);
      
      if (sessionIndex === -1) {
        // Create new session
        const newSession: ChatSession = {
          id: targetSessionId as string,
          title: message.trim().substring(0, 35) + "...",
          updatedAt: Date.now(),
          messages: [
            userMessage,
            { role: "assistant", textStream: "" } as ChatMessage
          ],
        };
        updatedSessions = [newSession, ...updatedSessions];
      } else {
        // Update existing session
        const newMessages = [...updatedSessions[sessionIndex].messages, userMessage, { role: "assistant", textStream: "" } as ChatMessage];
        updatedSessions[sessionIndex] = {
          ...updatedSessions[sessionIndex],
          updatedAt: Date.now(),
          messages: newMessages.slice(-MAX_PERSISTED_MESSAGES),
        };
        updatedSessions.sort((a, b) => b.updatedAt - a.updatedAt);
      }
      return updatedSessions;
    });

    setInput("");
    if (textareaRef.current) {
      textareaRef.current.style.height = "auto";
    }
    setSending(true);

    // Get context from current state safely
    const activeSession = sessions.find((s) => s.id === targetSessionId);
    const previousQuizQuestions = extractPreviousQuizQuestions(activeSession?.messages ?? []);
    const chatHistory = extractChatHistory(activeSession?.messages ?? []);

    try {

        // 2. Start streaming
        let fullText = "";
        await streamCoachResponse(
          {
            courseId,
            lessonId,
            message: message.trim(),
            previousQuizQuestions,
            chatHistory,
          },
          (token) => {
            fullText += token;

            // Try to extract JSON at each token
            const result = extractJson(fullText);
            if (result) {
              setSessions((prev) => {
                const updated = [...prev];
                const idx = updated.findIndex((s) => s.id === targetSessionId);
                if (idx >= 0) {
                  const msgs = [...updated[idx].messages];
                  // If we found JSON, set it as payload and preserve any text before it
                  msgs[msgs.length - 1] = { 
                    role: "assistant", 
                    payload: result.payload, 
                    textStream: result.textBefore 
                  };
                  updated[idx] = { ...updated[idx], messages: msgs };
                  setAnimatingMessageIndex(msgs.length - 1);
                  setActiveBlockIndex(0);
                }
                return updated;
              });
              return;
            }

            setSessions((prev) => {
              const updated = [...prev];
              const idx = updated.findIndex((s) => s.id === targetSessionId);
              if (idx >= 0) {
                const msgs = [...updated[idx].messages];
                const lastMsg = msgs[msgs.length - 1];

                // If no JSON found yet, update textStream.
                // If it looks like we're starting a JSON object/block, use buffering id to show loader.
                const trimmed = fullText.trim();
                const isLikelyJsonStart = trimmed.startsWith("{") || trimmed.startsWith("[") || trimmed.startsWith("```json");
                
                const currentText = isLikelyJsonStart 
                  ? `__DATA_STREAMING__:${fullText.length}` 
                  : fullText;

                if (lastMsg.textStream !== currentText) {
                  msgs[msgs.length - 1] = { ...lastMsg, textStream: currentText };
                  updated[idx] = { ...updated[idx], messages: msgs };
                }
              }
              return updated;
            });
          },
          () => {
            // Final check on complete
            const result = extractJson(fullText);
            if (result) {
              setSessions((prev) => {
                const updated = [...prev];
                const idx = updated.findIndex((s) => s.id === targetSessionId);
                if (idx >= 0) {
                  const msgs = [...updated[idx].messages];
                  msgs[msgs.length - 1] = { 
                    role: "assistant", 
                    payload: result.payload, 
                    textStream: result.textBefore 
                  };
                  updated[idx] = { ...updated[idx], messages: msgs };
                }
                return updated;
              });
            } else {
              // If not JSON even at the end, just keep text as is
              setSessions((prev) => {
                const updated = [...prev];
                const idx = updated.findIndex((s) => s.id === targetSessionId);
                if (idx >= 0) {
                  const msgs = [...updated[idx].messages];
                  msgs[msgs.length - 1] = { ...msgs[msgs.length - 1], textStream: fullText };
                  updated[idx] = { ...updated[idx], messages: msgs };
                }
                return updated;
              });
            }
          },
          (error) => {
            throw error;
          }
        );
      } catch (error) {
        console.error("AI coach failed:", error);

        // Try non-streaming fallback as a last resort
        try {
          const fallbackResponse = await getCoachResponse({
            courseId,
            lessonId,
            message: message.trim(),
            previousQuizQuestions,
            chatHistory,
          });

          if (fallbackResponse) {
            setSessions((prev) => {
              const updated = [...prev];
              const idx = updated.findIndex((s) => s.id === targetSessionId);
              if (idx >= 0) {
                const msgs = [...updated[idx].messages];
                msgs[msgs.length - 1] = { role: "assistant", payload: fallbackResponse, textStream: "" };
                updated[idx] = { ...updated[idx], messages: msgs };
              }
              return updated;
            });
            return;
          }
        } catch (fallbackError) {
          console.error("Fallback also failed:", fallbackError);
        }

        toast.error(error instanceof Error ? error.message : "Failed to get coach response");
      } finally {
        setSending(false);
      }
    };

    const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        submitMessage(input);
      }
    };

    const onSubmit = async (e: FormEvent) => {
      e.preventDefault();
      await submitMessage(input);
    };

    const startNewChat = () => {
      setCurrentSessionId(null);
      setInput("");
      if (textareaRef.current) {
        textareaRef.current.style.height = "auto";
      }
      // Mobile only: close sidebar on new chat to jump immediately into typing
      if (window.innerWidth < 768) {
        setIsSidebarOpen(false);
      }
    };

    // Group sessions by "Today", "Previous" visually (simplified to just a list here)
    return (
      <div className="flex h-full bg-background text-foreground font-sans w-full relative overflow-hidden">

        {/* Mobile Sidebar Overlay */}
        {isSidebarOpen && (
          <div
            className="fixed inset-0 bg-black/60 md:hidden z-40 transition-opacity"
            onClick={() => setIsSidebarOpen(false)}
          />
        )}

        {/* Sidebar - ChatGPT Style */}
        <div
          className={cn(
            "fixed md:relative inset-y-0 left-0 w-64 lg:w-72 bg-muted/40 dark:bg-muted/10 border-r border-border/50 z-50 flex flex-col transition-all duration-300 ease-in-out",
            !isSidebarOpen && "-translate-x-full md:translate-x-0 md:w-0 md:hidden"
          )}
        >
          <div className="p-4 flex items-center justify-between">
            <Button
              onClick={startNewChat}
              variant="outline"
              className="w-full justify-start gap-2 border-dashed border-border hover:border-primary/50"
            >
              <Plus className="w-4 h-4" />
              New Chat
            </Button>
            <Button
              variant="ghost"
              size="icon"
              className="md:hidden ml-2"
              onClick={() => setIsSidebarOpen(false)}
            >
              <PanelLeftClose className="w-5 h-5" />
            </Button>
          </div>

          <div className="flex-1 overflow-y-auto px-3 pb-3 space-y-1 scrollbar-thin">
            <div className="text-[11px] uppercase tracking-widest font-bold text-muted-foreground/60 mt-4 mb-2 px-2">History</div>
            {sessions.length === 0 ? (
              <div className="text-[13px] text-muted-foreground/60 px-2 py-4 italic">No chat history yet</div>
            ) : (
              sessions.map(session => (
                <button
                  key={session.id}
                  onClick={() => {
                    setCurrentSessionId(session.id);
                    if (window.innerWidth < 768) setIsSidebarOpen(false);
                  }}
                  className={cn(
                    "w-full text-left px-3 py-2.5 rounded-lg text-[14px] flex items-center gap-3 transition-all duration-200 group relative",
                    currentSessionId === session.id
                      ? 'bg-secondary text-secondary-foreground font-semibold shadow-sm'
                      : 'hover:bg-muted/50 text-muted-foreground hover:text-foreground'
                  )}
                >
                  <MessageSquare className={cn("w-4 h-4 shrink-0", currentSessionId === session.id ? "text-primary" : "text-muted-foreground/50")} />
                  <div className="truncate flex-1">{session.title}</div>
                  {currentSessionId === session.id && (
                    <div className="shrink-0 text-primary"><div className="h-1.5 w-1.5 rounded-full bg-primary animate-pulse" /></div>
                  )}
                </button>
              ))
            )}
          </div>

          <div className="p-4 border-t border-border/50">
            {courseId && (
              <Link to={lessonId ? `/courses/${courseId}/lessons/${lessonId}` : `/courses/${courseId}`}>
                <Button variant="ghost" className="w-full justify-start gap-2 text-muted-foreground hover:text-foreground">
                  <ChevronLeft className="w-4 h-4" /> Back to course
                </Button>
              </Link>
            )}
          </div>
        </div>

        {/* Main Chat Area */}
        <div className="flex-1 flex flex-col relative min-w-0 transition-all duration-300">

          {/* Top Header - especially for mobile to toggle sidebar */}
          <header className="h-14 flex items-center gap-3 px-4 flex-none border-b border-border/20">
            {!isSidebarOpen && (
              <Button variant="ghost" size="icon" className="h-10 w-10 shrink-0 text-muted-foreground hover:text-foreground" onClick={() => setIsSidebarOpen(true)}>
                <PanelLeft className="w-5 h-5" />
              </Button>
            )}
            <div className="font-semibold text-foreground md:hidden mx-auto pr-10">AI Coach</div>
            <div className="ml-auto hidden md:flex items-center gap-2">
              <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full border bg-background/50 text-xs font-medium text-muted-foreground">
                <Database className="w-3.5 h-3.5" />
                Transport: {USE_MCP_CLIENT ? "MCP" : "Legacy"}
              </div>
              {courseId && (
                <Link to={lessonId ? `/courses/${courseId}/lessons/${lessonId}` : `/courses/${courseId}`}>
                  <Button variant="ghost" size="sm" className="h-8 gap-2 text-muted-foreground">
                    <ChevronLeft className="w-4 h-4" /> Back to course
                  </Button>
                </Link>
              )}
            </div>
          </header>

          {/* Scrollable Conversation */}
          <div className="flex-1 overflow-y-auto px-4 pb-44" id="chat-container">

            {messages.length === 0 ? (
              <div className="h-full flex flex-col items-center justify-center max-w-2xl mx-auto w-full px-4 pt-10 pb-20">
                <div className="w-16 h-16 rounded-full bg-foreground flex items-center justify-center mb-6">
                  <Bot className="w-8 h-8 text-background" />
                </div>
                <h2 className="text-2xl font-semibold mb-12 text-center">How can I help you learn today?</h2>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 w-full max-w-3xl">
                  {quickPrompts.map((prompt) => (
                    <button
                      key={prompt}
                      onClick={() => submitMessage(prompt)}
                      disabled={sending}
                      className="border border-border/80 hover:bg-muted/50 p-4 rounded-xl text-left transition-colors text-[14px] text-foreground/80 flex justify-between items-center group shadow-sm bg-transparent"
                    >
                      <span className="pr-2">{prompt}</span>
                      <div className="p-1 rounded-md opacity-0 group-hover:opacity-100 bg-background shadow-xs transition-opacity border border-border">
                        <Send className="w-3.5 h-3.5" />
                      </div>
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              <div className="w-full flex-col flex items-center">
                {messages.map((message, index) => (
                  <div key={index} className={`w-full ${message.role === "assistant" ? "" : ""}`}>
                    <div className="max-w-3xl mx-auto flex gap-4 md:gap-5 py-6 px-4">
                      {/* Avatar */}
                      <div className="shrink-0 flex flex-col relative items-center">
                        <div className={`w-7 h-7 rounded-sm flex items-center justify-center text-xs font-semibold ${message.role === "assistant" ? "bg-emerald-600 text-white" : "bg-muted text-foreground ring-1 ring-border"
                          }`}>
                          {message.role === "assistant" ? <Sparkles className="w-4 h-4" /> : <User className="w-4 h-4" />}
                        </div>
                      </div>

                      {/* Content */}
                      <div className="flex-1 min-w-0 pt-0.5 text-foreground">
                        {message.role === "user" ? (
                          <div className="whitespace-pre-wrap text-[15px] leading-relaxed">
                            {message.text}
                          </div>
                        ) : (
                          <div className="space-y-4">
                            {/* 1. Show readable text stream */}
                            {message.textStream && (
                              <div className={cn(
                                "text-[15px] leading-relaxed whitespace-pre-wrap",
                                message.textStream.startsWith("__DATA_STREAMING__") && "opacity-0 h-0 overflow-hidden"
                              )}>
                                {renderBody(message.textStream)}
                              </div>
                            )}

                            {/* 2. Show progress state if buffering JSON or just started */}
                            {(message.textStream?.startsWith("__DATA_STREAMING__") || (!message.textStream && !message.payload && message.role === "assistant")) && (
                              <div className="flex flex-col gap-2 py-2">
                                <div className="flex items-center gap-2">
                                  <div className="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-pulse" />
                                  <span className="text-[14px] text-emerald-700 dark:text-emerald-400 font-medium animate-pulse">
                                    Coach is responding...
                                  </span>
                                </div>
                                <div className="h-1 w-32 bg-emerald-200 dark:bg-emerald-900/40 rounded-full overflow-hidden">
                                  <div className="h-full bg-emerald-500/40 animate-[shimmer_2s_infinite]" style={{ width: '100%', backgroundSize: '200% 100%', backgroundImage: 'linear-gradient(90deg, transparent, rgba(255,255,255,0.4), transparent)' }} />
                                </div>
                              </div>
                            )}

                            {/* 3. Show final structured blocks */}
                            {message.payload && (
                              <>
                                {normalizeCoachBlocks(message.payload).map((block, blockIndex) => {
                                  const isLatestMessage = index === animatingMessageIndex;

                                  // Condition 1: If it's the latest message, only show blocks up to activeBlockIndex
                                  if (isLatestMessage && blockIndex > activeBlockIndex) {
                                    return null;
                                  }

                                  return (
                                    <CoachBlockRenderer
                                      key={`${index}-${blockIndex}`}
                                      block={block}
                                      animate={isLatestMessage && blockIndex === activeBlockIndex}
                                      onComplete={() => {
                                        if (isLatestMessage && blockIndex === activeBlockIndex) {
                                          setActiveBlockIndex(prev => prev + 1);
                                        }
                                      }}
                                    />
                                  );
                                })}

                                {(!(index === animatingMessageIndex && activeBlockIndex < normalizeCoachBlocks(message.payload).length)) &&
                                  message.payload.citations &&
                                  message.payload.citations.length > 0 && (
                                    <CitationList citations={message.payload.citations} />
                                  )}

                                {/* Only show suggestions when everything is done animating */}
                                {(!(index === animatingMessageIndex && activeBlockIndex < normalizeCoachBlocks(message.payload).length)) &&
                                  message.payload.suggestions && message.payload.suggestions.length > 0 && (
                                    <div className="flex flex-wrap gap-2 pt-4">
                                      {message.payload.suggestions.map((suggestion) => (
                                        <button
                                          key={suggestion}
                                          onClick={() => submitMessage(suggestion)}
                                          disabled={sending}
                                          className="text-[13px] px-3.5 py-1.5 rounded-full border border-border/80 bg-background text-foreground/80 hover:bg-muted font-medium transition-colors"
                                        >
                                          {suggestion}
                                        </button>
                                      ))}
                                    </div>
                                  )}
                              </>
                            )}
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                ))}

                {sending && (
                  <div className="w-full">
                    <div className="max-w-3xl mx-auto flex gap-4 md:gap-5 py-6 px-4">
                      <div className="shrink-0 flex flex-col relative items-center opacity-0">
                        {/* Ghost avatar to maintain alignment */}
                        <div className="w-7 h-7 rounded-sm bg-muted" />
                      </div>
                      <div className="flex-1 flex items-center h-7 gap-1">
                        <span className="w-1.5 h-1.5 bg-foreground/30 rounded-full animate-bounce [animation-delay:-0.3s]"></span>
                        <span className="w-1.5 h-1.5 bg-foreground/30 rounded-full animate-bounce [animation-delay:-0.15s]"></span>
                        <span className="w-1.5 h-1.5 bg-foreground/30 rounded-full animate-bounce"></span>
                      </div>
                    </div>
                  </div>
                )}
                <div ref={messagesEndRef} className="h-6" />
              </div>
            )}
          </div>

          {/* Input Area (ChatGPT Style - fixed at bottom) */}
          <div className="flex-none w-full pt-4 pb-6 px-4 bg-background border-t border-border/30">
            <div className="max-w-3xl mx-auto w-full relative">
              <form
                onSubmit={onSubmit}
                className="relative flex items-center bg-card shadow-[0_0_15px_rgba(0,0,0,0.05)] border border-border/80 rounded-2xl overflow-hidden focus-within:ring-1 focus-within:ring-border transition-all"
              >
                <textarea
                  ref={textareaRef}
                  value={input}
                  onChange={(e) => {
                    setInput(e.target.value);
                    handleInput();
                  }}
                  onKeyDown={handleKeyDown}
                  placeholder="Message AI Coach..."
                  rows={1}
                  className="w-full max-h-48 resize-none bg-transparent py-3.5 pl-4 pr-12 focus:outline-none placeholder:text-muted-foreground/60 text-[15px] text-foreground leading-relaxed scrollbar-thin"
                  disabled={sending}
                />
                <div className="absolute right-2 bottom-1.5">
                  <Button
                    type="submit"
                    size="icon"
                    className={`h-8 w-8 rounded-lg transition-all ${sending || !input.trim()
                        ? 'bg-muted text-muted-foreground shadow-none'
                        : 'bg-foreground text-background shadow-sm hover:opacity-90'
                      }`}
                    disabled={sending || !input.trim()}
                  >
                    <Send className="h-4 w-4" />
                  </Button>
                </div>
              </form>
              <div className="text-center mt-2.5">
                <span className="text-xs text-muted-foreground">
                  AI Coach can make mistakes. Consider verifying important information.
                </span>
              </div>
            </div>
          </div>

        </div>
      </div>
    );
  }
