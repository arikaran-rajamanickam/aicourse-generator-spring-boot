export type Course = {
  id: string;
  title: string;
  description: string;
  category: string;
  tags: string[];
  difficulty: "beginner" | "intermediate" | "advanced" | "BEGINNER_TO_ADVANCED";
  estimatedDuration: { value: number; unit: "hours" | "weeks" };
  thumbnailUrl?: string;
  modules: Module[];
  finalExam?: Quiz;
  status: "draft" | "published";
  settings: { visibility: "public" | "private" | "unlisted"; enrollmentType: "open" | "invite" };
};

export type Module = {
  id: string;
  title: string;
  description: string;
  learningObjectives: string[];
  lessons: Lesson[];
  assessment?: Quiz;
  order: number;
};

export type Lesson = {
  id: string;
  title: string;
  contentBlocks: ContentBlock[];
  order: number;
};

export type ExtractedContent = {
  title: string;
  summary: string;
  sections: { heading: string; content: string }[];
  keyTopics: string[];
  suggestedQuizQuestions: Partial<QuizQuestion>[];
};

export type ContentBlock =
  | { id: string; type: "text"; content: string; order: number }
  | { id: string; type: "video"; url: string; title?: string; thumbnail?: string; order: number }
  | { id: string; type: "link"; url: string; title?: string; description?: string; note?: string; order: number }
  | { id: string; type: "file"; fileName: string; fileUrl: string; extractedContent?: ExtractedContent; order: number }
  | { id: string; type: "quiz"; quiz: Quiz; order: number }
  | { id: string; type: "code"; language: string; code: string; order: number }
  | { id: string; type: "image"; url: string; alt: string; prompt: string; order: number }
  | { id: string; type: "ai-generated"; prompt: string; content: string; order: number };

export type Quiz = {
  id: string;
  title: string;
  questions: QuizQuestion[];
  settings: { timeLimit?: number; passingScore: number; randomize: boolean; maxRetakes?: number };
};

export type QuizQuestion = {
  id: string;
  type: "mcq" | "true-false" | "short-answer" | "code-challenge";
  text: string;
  options?: string[];
  correctAnswer: number | string;
  explanation?: string;
  points: number;
  difficulty: "easy" | "medium" | "hard";
};
