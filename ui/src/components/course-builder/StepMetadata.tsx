import { ChangeEvent } from "react";
import { useCourseBuilder } from "@/context/CourseBuilderContext";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { Sparkles, UploadCloud } from "lucide-react";
import type { ContentBlock, Module } from "@/types/course-builder";
import { toast } from "sonner";
import { generateCourseOutlineAPI } from "@/services/courseApi";

export function StepMetadata() {
  const { state, dispatch } = useCourseBuilder();
  const { course } = state;

  const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    dispatch({
      type: "UPDATE_METADATA",
      payload: { [e.target.name]: e.target.value }
    });
  };

  const handleDifficultyChange = (val: "beginner" | "intermediate" | "advanced" | "BEGINNER_TO_ADVANCED") => {
    dispatch({ type: "UPDATE_METADATA", payload: { difficulty: val } });
  };

  const toLessonContentBlocks = (lesson: any): ContentBlock[] => {
    const rawBlocks = Array.isArray(lesson?.contentBlocks) ? lesson.contentBlocks : [];
    if (rawBlocks.length > 0) {
      return rawBlocks.map((block: any, idx: number) => {
        const type = block?.type;

        if (type === "video") {
          return {
            id: crypto.randomUUID(),
            type: "video",
            url: typeof block?.url === "string" ? block.url : "",
            title: typeof block?.title === "string" ? block.title : "",
            thumbnail: typeof block?.thumbnail === "string" ? block.thumbnail : "",
            order: idx,
          };
        }

        if (type === "link") {
          return {
            id: crypto.randomUUID(),
            type: "link",
            url: typeof block?.url === "string" ? block.url : "",
            title: typeof block?.title === "string" ? block.title : "",
            description: typeof block?.description === "string" ? block.description : "",
            note: typeof block?.note === "string" ? block.note : "",
            order: idx,
          };
        }

        if (type === "code") {
          return {
            id: crypto.randomUUID(),
            type: "code",
            language: typeof block?.language === "string" ? block.language : "text",
            code: typeof block?.code === "string" ? block.code : "",
            order: idx,
          };
        }

        if (type === "ai-generated") {
          return {
            id: crypto.randomUUID(),
            type: "ai-generated",
            prompt: typeof block?.prompt === "string" ? block.prompt : "",
            content: typeof block?.content === "string" ? block.content : "",
            order: idx,
          };
        }

        return {
          id: crypto.randomUUID(),
          type: "text",
          content: typeof block?.content === "string" ? block.content : "",
          order: idx,
        };
      });
    }

    const fallbackContent = [lesson?.content, lesson?.summary, lesson?.body]
      .find((value) => typeof value === "string" && value.trim().length > 0);

    if (typeof fallbackContent === "string") {
      return [{ id: crypto.randomUUID(), type: "text", content: fallbackContent, order: 0 }];
    }

    return [];
  };

  const handleGenerateOutline = async () => {
    if (!course.title) {
      toast.error("Please provide a Course Title first.");
      return;
    }
    
    try {
      toast.loading("Generating full course draft using AI...", { id: "generator" });

      const payload = {
        title: course.title,
        difficulty: course.difficulty,
        duration: `${course.estimatedDuration.value} ${course.estimatedDuration.unit}`
      };

      const responseJSON = await generateCourseOutlineAPI(payload);
      
      // Parse the JSON. Depending on how Jackson serializes it, it should just be an object.
      // E.g. { title: "...", description: "...", modules: [...] }
      
      if (responseJSON && responseJSON.description) {
         dispatch({
            type: "UPDATE_METADATA",
            payload: { description: responseJSON.description }
         });
      }

      if (responseJSON && Array.isArray(responseJSON.modules)) {
        const generatedModules: Module[] = responseJSON.modules.map((mod: any, mIdx: number) => ({
          id: crypto.randomUUID(),
          title: mod.title || `Module ${mIdx + 1}`,
          description: mod.description || "",
          learningObjectives: mod.learningObjectives || [],
          order: mIdx,
          lessons: (mod.lessons || []).map((less: any, lIdx: number) => ({
             id: crypto.randomUUID(),
             title: less.title || less || `Lesson ${lIdx + 1}`,
             contentBlocks: toLessonContentBlocks(less),
             order: lIdx
          }))
        }));

        dispatch({ type: "SET_OUTLINE", payload: generatedModules });
        toast.success("Full draft generated successfully! You can edit everything.", { id: "generator" });
        dispatch({ type: "SET_STEP", payload: 1 }); // Move to Structure
      } else {
        throw new Error("Invalid format returned from AI");
      }

    } catch (error: any) {
      toast.error(`Draft generation failed: ${error.message}`, { id: "generator" });
    }
  };

  return (
    <div className="p-6 space-y-8 max-w-4xl mx-auto">
      <div>
        <h2 className="text-2xl font-bold flex items-center gap-2">
          Course Metadata
        </h2>
        <p className="text-muted-foreground mt-1">
          Set up the basic information and let AI scaffold a complete editable course draft for you.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        <div className="md:col-span-2 space-y-6">
          <div className="space-y-2">
            <label className="text-sm font-medium">Course Title <span className="text-destructive">*</span></label>
            <Input 
              name="title" 
              placeholder="e.g. Advanced Functional React" 
              value={course.title}
              onChange={handleChange}
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium">Description</label>
            <Textarea 
              name="description" 
              placeholder="What will the students learn?" 
              value={course.description}
              onChange={handleChange}
              rows={4}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Difficulty Level</label>
              <Select value={course.difficulty} onValueChange={handleDifficultyChange}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="beginner">Beginner</SelectItem>
                  <SelectItem value="intermediate">Intermediate</SelectItem>
                  <SelectItem value="advanced">Advanced</SelectItem>
                  <SelectItem value="BEGINNER_TO_ADVANCED">Beginner → Advanced (Full Path)</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium">Estimated Duration (Hours)</label>
              <Input 
                type="number" 
                min={1} 
                value={course.estimatedDuration.value}
                onChange={(e) => dispatch({ 
                  type: "UPDATE_METADATA", 
                  payload: { estimatedDuration: { value: Number(e.target.value), unit: "hours" } } 
                })}
              />
            </div>
          </div>
        </div>

        <div className="space-y-6">
          <div className="space-y-2">
            <label className="text-sm font-medium">Thumbnail</label>
            <div className="border-2 border-dashed border-muted-foreground/30 rounded-xl p-8 flex flex-col items-center justify-center text-center hover:bg-muted/50 transition-colors cursor-pointer aspect-video bg-muted/20">
              <UploadCloud className="w-8 h-8 text-muted-foreground mb-3" />
              <p className="text-sm font-medium text-muted-foreground">Drag & Drop Image</p>
              <p className="text-xs text-muted-foreground mt-1">or click to browse</p>
            </div>
          </div>

          <div className="pt-4">
            <div className="bg-primary/5 border border-primary/20 rounded-xl p-4">
              <h4 className="font-medium text-primary flex items-center gap-2 mb-2">
                <Sparkles className="w-4 h-4" /> AI Scaffold
              </h4>
              <p className="text-xs text-muted-foreground mb-4">
                Let AI generate modules, lessons, and lesson content blocks. You can edit any part after generation.
              </p>
              <Button onClick={handleGenerateOutline} className="w-full shadow-sm">
                Generate Full Draft
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
