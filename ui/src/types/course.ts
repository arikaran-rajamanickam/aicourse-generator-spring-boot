export interface Course {
  id: string;
  title: string;
  description?: string;
  topic?: string;
  difficulty?: string;
  duration?: string;
  moduleCount?: number;
  lessonCount?: number;
  modules?: any[];
  active?: boolean;
  progress?: number; // UI only field for progress bars
  createdAt?: string;
  updatedAt?: string;
  [key: string]: any;
}

export interface CourseCreatePayload {
  topic: string;
  difficulty?: string;
  duration?: string;
  projectId?: string;
}
