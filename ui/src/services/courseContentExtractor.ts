import type { ExtractedContent } from "../types/course-builder";

// The python backend URL should come from an env config, for now we mock it
const PYTHON_BACKEND_URL = import.meta.env.VITE_PYTHON_BACKEND_URL || "http://localhost:8000";

/**
 * Mocks connecting to the Python Backend to extract content from a PDF, DOCX, PPTX
 */
export async function extractContentFromFile(fileStr: string): Promise<ExtractedContent> {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        title: "Mock Extracted Document Title",
        summary: "This is a dummy summary of the document, showing what the python backend might return.",
        sections: [
          { heading: "Section 1", content: "Extracted text content from the file..." },
          { heading: "Section 2", content: "More content..." }
        ],
        keyTopics: ["dummy-topic1", "dummy-topic2"],
        suggestedQuizQuestions: [
          { 
            text: "What is this document about?", 
            type: "mcq", 
            options: ["A", "B", "C", "D"], 
            correctAnswer: 0 
          }
        ]
      });
    }, 1500);
  });
}

/**
 * Mocks connecting to the Python Backend to extract content from a webpage.
 */
export async function extractContentFromUrl(url: string): Promise<ExtractedContent> {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        title: "Webpage Dummy Extraction",
        summary: `Summary of the content at ${url}...`,
        sections: [
          { heading: "Intro", content: "..." },
        ],
        keyTopics: ["web", "mock"],
        suggestedQuizQuestions: []
      });
    }, 1000);
  });
}

/**
 * Mocks hitting Lovable AI (or other endpoint) to generate a full module structure string.
 */
export async function generateCourseOutline(topic: string) {
  // Returns mock structure data for step 1 outline feature
  return [];
}
