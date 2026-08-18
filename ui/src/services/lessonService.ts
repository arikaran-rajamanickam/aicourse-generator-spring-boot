import { LessonData } from "../types/lessonContent";
import { generateLessonContent, getCourseById, getLessonById } from "./courseApi";
import { normalizeLessonData, needsLessonGeneration } from "../lib/lessonMapper";

/**
 * Lesson service: fetches and ensures lessons have generated content
 * Always returns normalized LessonData with LessonBlock[]
 */

export interface LessonServiceOptions {
  courseId: string;
  lessonId: string;
  moduleId?: string;
}

/**
 * Get lesson with auto-generation if needed
 * Returns normalized LessonData ready for rendering
 */
export async function getLessonWithGeneration(
  options: LessonServiceOptions
): Promise<LessonData> {
  const { courseId, lessonId, moduleId: initialModuleId } = options;

  // Fetch raw lesson
  let rawLesson = await getLessonById(lessonId);
  let normalized = normalizeLessonData(rawLesson);

  // Check if generation is needed (backend sets enriched=true after generation)
  if (needsLessonGeneration(rawLesson)) {
    // Infer moduleId if not provided
    let moduleId = initialModuleId;
    if (!moduleId) {
      try {
        const course = await getCourseById(courseId);
        const modules = Array.isArray(course?.modules) ? course.modules : [];
        const matched = modules.find((module: any) =>
          Array.isArray(module?.lessons)
            ? module.lessons.some((lesson: any) => String(lesson?.id) === String(lessonId))
            : false
        );
        moduleId = matched?.id ? String(matched.id) : null;
      } catch (error) {
        console.warn("Failed to infer moduleId for generation:", error);
      }
    }

    // Generate content if we have moduleId
    if (moduleId) {
      try {
        const generated = await generateLessonContent(courseId, moduleId, lessonId);
        // Backend now parses and returns lesson with enriched=true
        let generatedData = generated?.data ?? generated ?? rawLesson;
        normalized = normalizeLessonData(generatedData);
        
        // If the backend generates asynchronously, we need to poll until it's enriched
        let attempts = 0;
        while (needsLessonGeneration(generatedData) && attempts < 60) {
          // Wait 3 seconds
          await new Promise(resolve => setTimeout(resolve, 3000));
          const check = await getLessonById(lessonId);
          generatedData = check?.data ?? check;
          normalized = normalizeLessonData(generatedData);
          attempts++;
        }
      } catch (error) {
        console.error("Failed to generate lesson content:", error);
        // Continue with un-enriched content
      }
    }
  }

  return normalized;
}

/**
 * Get just the normalized lesson data (without generation)
 * Useful for previews or when you know content is ready
 */
export async function getNormalizedLesson(lessonId: string): Promise<LessonData> {
  const rawLesson = await getLessonById(lessonId);
  return normalizeLessonData(rawLesson);
}

