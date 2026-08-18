export interface CoachTextContent {
  title?: string;
  body: string;
}

export interface CoachQuizCardContent {
  question: string;
  options: string[];
  correctIndex: number;
  explanation?: string;
}

export interface CoachFlashcardContent {
  front: string;
  back: string;
}

export interface CoachStudyPlanStep {
  title: string;
  task: string;
  time: string;
}

export interface CoachStudyPlanContent {
  goal: string;
  duration: string;
  steps: CoachStudyPlanStep[];
}

export interface CoachBlock {
  type: "text" | "quiz_card" | "flashcard" | "study_plan";
  content: CoachTextContent | CoachQuizCardContent | CoachFlashcardContent | CoachStudyPlanContent;
}

export interface CoachCitation {
  title: string;
  url: string;
  description?: string;
  source?: string;
}

export interface CoachResponse {
  intent: string;
  blocks: CoachBlock[];
  suggestions: string[];
  citations?: CoachCitation[];
}

export interface CoachChatMessage {
  role: "user" | "assistant";
  text: string;
}

export interface CoachRequest {
  courseId?: string;
  lessonId?: string;
  message: string;
  previousQuizQuestions?: string[];
  chatHistory?: CoachChatMessage[];
}

