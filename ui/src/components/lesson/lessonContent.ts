export interface LessonHeading {
  type: "heading";
  content: string;
}

export interface LessonText {
  type: "text";
  content: string;
}

export interface LessonList {
  type: "list";
  content: string[];
}

export interface LessonTable {
  type: "table";
  content: {
    headers: string[];
    rows: string[][];
  };
}

export interface LessonQuiz {
  type: "quiz";
  content: {
    question: string;
    options: string[];
    correctIndex: number;
    explanation?: string;
  };
}

export interface LessonCode {
  type: "code";
  content: {
    language: string;
    code: string;
  };
}

export interface LessonYoutube {
  type: "youtube";
  content: {
    url: string;
    title?: string;
  };
}

export type LessonBlock =
  | LessonHeading
  | LessonText
  | LessonList
  | LessonTable
  | LessonQuiz
  | LessonCode
  | LessonYoutube;

export interface LessonData {
  id: string;
  title: string;
  content: LessonBlock[];
  new?: boolean;
  enriched?: boolean;
}
