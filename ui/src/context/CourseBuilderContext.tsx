import React, { createContext, useContext, useReducer, ReactNode } from "react";
import type { Course, Module, Lesson, ContentBlock, Quiz } from "@/types/course-builder";

type CourseBuilderState = {
  course: Course;
  currentStep: number;
  selectedLessonId: string | null;
};

type Action =
  | { type: "SET_STEP"; payload: number }
  | { type: "UPDATE_METADATA"; payload: Partial<Course> }
  // Module Actions
  | { type: "ADD_MODULE"; payload: Module }
  | { type: "UPDATE_MODULE"; payload: { moduleId: string; updates: Partial<Module> } }
  | { type: "DELETE_MODULE"; payload: string }
  | { type: "REORDER_MODULES"; payload: { startIndex: number; endIndex: number } }
  | { type: "MOVE_MODULE_UP"; payload: string }
  | { type: "MOVE_MODULE_DOWN"; payload: string }
  // Lesson Actions
  | { type: "ADD_LESSON"; payload: { moduleId: string; lesson: Lesson } }
  | { type: "UPDATE_LESSON"; payload: { moduleId: string; lessonId: string; updates: Partial<Lesson> } }
  | { type: "DELETE_LESSON"; payload: { moduleId: string; lessonId: string } }
  | { type: "SELECT_LESSON"; payload: string | null }
  | { type: "MOVE_LESSON_UP"; payload: { moduleId: string; lessonId: string } }
  | { type: "MOVE_LESSON_DOWN"; payload: { moduleId: string; lessonId: string } }
  // Content Block Actions
  | { type: "ADD_CONTENT_BLOCK"; payload: { moduleId: string; lessonId: string; block: ContentBlock } }
  | { type: "UPDATE_CONTENT_BLOCK"; payload: { moduleId: string; lessonId: string; blockId: string; updates: Partial<ContentBlock> } }
  | { type: "DELETE_CONTENT_BLOCK"; payload: { moduleId: string; lessonId: string; blockId: string } }
  | { type: "REORDER_CONTENT_BLOCKS"; payload: { moduleId: string; lessonId: string; startIndex: number; endIndex: number } }
  | { type: "MOVE_BLOCK_UP"; payload: { moduleId: string; lessonId: string; blockId: string } }
  | { type: "MOVE_BLOCK_DOWN"; payload: { moduleId: string; lessonId: string; blockId: string } }
  // Load whole course
  | { type: "LOAD_COURSE"; payload: Course }
  // AI populate
  | { type: "SET_OUTLINE"; payload: Module[] };

const initialState: CourseBuilderState = {
  course: {
    id: crypto.randomUUID(),
    title: "",
    description: "",
    category: "",
    tags: [],
    difficulty: "beginner",
    estimatedDuration: { value: 2, unit: "hours" },
    modules: [],
    status: "draft",
    settings: { visibility: "private", enrollmentType: "open" },
  },
  currentStep: 0,
  selectedLessonId: null,
};

function courseBuilderReducer(state: CourseBuilderState, action: Action): CourseBuilderState {
  switch (action.type) {
    case "SET_STEP":
      return { ...state, currentStep: action.payload };
    
    case "UPDATE_METADATA":
      return {
        ...state,
        course: { ...state.course, ...action.payload },
      };
      
    case "ADD_MODULE":
      return {
        ...state,
        course: {
          ...state.course,
          modules: [...state.course.modules, action.payload],
        },
      };

    case "UPDATE_MODULE":
      return {
        ...state,
        course: {
          ...state.course,
          modules: state.course.modules.map((m) =>
            m.id === action.payload.moduleId ? { ...m, ...action.payload.updates } : m
          ),
        },
      };

    case "DELETE_MODULE":
      return {
        ...state,
        course: {
          ...state.course,
          modules: state.course.modules.filter((m) => m.id !== action.payload),
        },
      };
      
    case "MOVE_MODULE_UP": {
      const index = state.course.modules.findIndex(m => m.id === action.payload);
      if (index <= 0) return state;
      const newModules = [...state.course.modules];
      [newModules[index - 1], newModules[index]] = [newModules[index], newModules[index - 1]];
      // Update orders
      newModules.forEach((m, idx) => m.order = idx);
      return { ...state, course: { ...state.course, modules: newModules } };
    }

    case "MOVE_MODULE_DOWN": {
      const index = state.course.modules.findIndex(m => m.id === action.payload);
      if (index === -1 || index >= state.course.modules.length - 1) return state;
      const newModules = [...state.course.modules];
      [newModules[index + 1], newModules[index]] = [newModules[index], newModules[index + 1]];
      newModules.forEach((m, idx) => m.order = idx);
      return { ...state, course: { ...state.course, modules: newModules } };
    }

    case "REORDER_MODULES": {
      const result = Array.from(state.course.modules);
      const [removed] = result.splice(action.payload.startIndex, 1);
      result.splice(action.payload.endIndex, 0, removed);
      result.forEach((m, idx) => (m.order = idx));
      return {
        ...state,
        course: { ...state.course, modules: result },
      };
    }

    case "ADD_LESSON":
      return {
        ...state,
        course: {
          ...state.course,
          modules: state.course.modules.map((m) =>
            m.id === action.payload.moduleId
              ? { ...m, lessons: [...m.lessons, action.payload.lesson] }
              : m
          ),
        },
      };

    case "UPDATE_LESSON":
      return {
        ...state,
        course: {
          ...state.course,
          modules: state.course.modules.map((m) =>
            m.id === action.payload.moduleId
              ? {
                  ...m,
                  lessons: m.lessons.map((l) =>
                    l.id === action.payload.lessonId ? { ...l, ...action.payload.updates } : l
                  ),
                }
              : m
          ),
        },
      };

    case "DELETE_LESSON":
      return {
        ...state,
        course: {
          ...state.course,
          modules: state.course.modules.map((m) =>
            m.id === action.payload.moduleId
              ? { ...m, lessons: m.lessons.filter((l) => l.id !== action.payload.lessonId) }
              : m
          ),
        },
        selectedLessonId:
          state.selectedLessonId === action.payload.lessonId ? null : state.selectedLessonId,
      };

    case "SELECT_LESSON":
      return { ...state, selectedLessonId: action.payload };

    case "MOVE_LESSON_UP": {
      return {
        ...state,
        course: {
          ...state.course,
          modules: state.course.modules.map(m => {
            if (m.id !== action.payload.moduleId) return m;
            const idx = m.lessons.findIndex(l => l.id === action.payload.lessonId);
            if (idx <= 0) return m;
            const newLessons = [...m.lessons];
            [newLessons[idx - 1], newLessons[idx]] = [newLessons[idx], newLessons[idx - 1]];
            newLessons.forEach((l, i) => l.order = i);
            return { ...m, lessons: newLessons };
          })
        }
      };
    }

    case "MOVE_LESSON_DOWN": {
      return {
        ...state,
        course: {
          ...state.course,
          modules: state.course.modules.map(m => {
            if (m.id !== action.payload.moduleId) return m;
            const idx = m.lessons.findIndex(l => l.id === action.payload.lessonId);
            if (idx === -1 || idx >= m.lessons.length - 1) return m;
            const newLessons = [...m.lessons];
            [newLessons[idx + 1], newLessons[idx]] = [newLessons[idx], newLessons[idx + 1]];
            newLessons.forEach((l, i) => l.order = i);
            return { ...m, lessons: newLessons };
          })
        }
      };
    }

    case "ADD_CONTENT_BLOCK":
      return {
        ...state,
        course: {
          ...state.course,
          modules: state.course.modules.map((m) =>
            m.id === action.payload.moduleId
              ? {
                  ...m,
                  lessons: m.lessons.map((l) =>
                    l.id === action.payload.lessonId
                      ? { ...l, contentBlocks: [...l.contentBlocks, action.payload.block] }
                      : l
                  ),
                }
              : m
          ),
        },
      };

    case "UPDATE_CONTENT_BLOCK":
      return {
        ...state,
        course: {
          ...state.course,
          modules: state.course.modules.map((m) =>
            m.id === action.payload.moduleId
              ? {
                  ...m,
                  lessons: m.lessons.map((l) =>
                    l.id === action.payload.lessonId
                      ? {
                          ...l,
                          contentBlocks: l.contentBlocks.map((b) =>
                            b.id === action.payload.blockId ? { ...b, ...action.payload.updates } as ContentBlock : b
                          ),
                        }
                      : l
                  ),
                }
              : m
          ),
        },
      };

    case "DELETE_CONTENT_BLOCK":
      return {
        ...state,
        course: {
          ...state.course,
          modules: state.course.modules.map((m) =>
            m.id === action.payload.moduleId
              ? {
                  ...m,
                  lessons: m.lessons.map((l) =>
                    l.id === action.payload.lessonId
                      ? { ...l, contentBlocks: l.contentBlocks.filter((b) => b.id !== action.payload.blockId) }
                      : l
                  ),
                }
              : m
          ),
        },
      };

    case "MOVE_BLOCK_UP": {
        return {
          ...state,
          course: {
            ...state.course,
            modules: state.course.modules.map(m => {
              if (m.id !== action.payload.moduleId) return m;
              return {
                ...m,
                lessons: m.lessons.map(l => {
                  if (l.id !== action.payload.lessonId) return l;
                  const idx = l.contentBlocks.findIndex(b => b.id === action.payload.blockId);
                  if (idx <= 0) return l;
                  const newBlocks = [...l.contentBlocks];
                  [newBlocks[idx - 1], newBlocks[idx]] = [newBlocks[idx], newBlocks[idx - 1]];
                  newBlocks.forEach((b, i) => b.order = i);
                  return { ...l, contentBlocks: newBlocks };
                })
              };
            })
          }
        };
      }
  
      case "MOVE_BLOCK_DOWN": {
        return {
          ...state,
          course: {
            ...state.course,
            modules: state.course.modules.map(m => {
              if (m.id !== action.payload.moduleId) return m;
              return {
                ...m,
                lessons: m.lessons.map(l => {
                  if (l.id !== action.payload.lessonId) return l;
                  const idx = l.contentBlocks.findIndex(b => b.id === action.payload.blockId);
                  if (idx === -1 || idx >= l.contentBlocks.length - 1) return l;
                  const newBlocks = [...l.contentBlocks];
                  [newBlocks[idx + 1], newBlocks[idx]] = [newBlocks[idx], newBlocks[idx + 1]];
                  newBlocks.forEach((b, i) => b.order = i);
                  return { ...l, contentBlocks: newBlocks };
                })
              };
            })
          }
        };
      }

    case "REORDER_CONTENT_BLOCKS": {
      return {
        ...state,
        course: {
          ...state.course,
          modules: state.course.modules.map((m) =>
            m.id === action.payload.moduleId
              ? {
                  ...m,
                  lessons: m.lessons.map((l) => {
                    if (l.id === action.payload.lessonId) {
                      const result = Array.from(l.contentBlocks);
                      const [removed] = result.splice(action.payload.startIndex, 1);
                      result.splice(action.payload.endIndex, 0, removed);
                      result.forEach((b, idx) => (b.order = idx));
                      return { ...l, contentBlocks: result };
                    }
                    return l;
                  }),
                }
              : m
          ),
        },
      };
    }

    case "LOAD_COURSE":
      return {
        ...state,
        course: action.payload,
      };

    case "SET_OUTLINE":
      return {
        ...state,
        course: {
          ...state.course,
          modules: action.payload,
        },
      };

    default:
      return state;
  }
}

const CourseBuilderContext = createContext<{
  state: CourseBuilderState;
  dispatch: React.Dispatch<Action>;
} | null>(null);

export function CourseBuilderProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(courseBuilderReducer, initialState);

  return (
    <CourseBuilderContext.Provider value={{ state, dispatch }}>
      {children}
    </CourseBuilderContext.Provider>
  );
}

export function useCourseBuilder() {
  const context = useContext(CourseBuilderContext);
  if (!context) {
    throw new Error("useCourseBuilder must be used within a CourseBuilderProvider");
  }
  return context;
}
