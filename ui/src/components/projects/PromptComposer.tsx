import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { useNavigate } from "react-router-dom";
import { Play, Save, Sparkles, Wand2, Loader2, Info } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { useCreatePrompt } from "@/lib/queries/projects";
import { getCoachResponse } from "@/services/coachApi";
import type { ProjectCourseSummary } from "@/services/projectApi";
import { cn } from "@/lib/utils";

const schema = z.object({
  text: z.string().trim().min(5, "Prompt must be at least 5 characters.").max(3000, "Max 3000 characters."),
  relatedCourseId: z.string().optional(),
});

type Values = z.infer<typeof schema>;

export function PromptComposer({
  projectId,
  courses,
}: {
  projectId: string;
  courses: ProjectCourseSummary[];
}) {
  const navigate = useNavigate();
  const create = useCreatePrompt(projectId);
  const [isEnhancing, setIsEnhancing] = useState(false);

  const form = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { text: "", relatedCourseId: undefined },
  });

  const text = form.watch("text") ?? "";

  const handleEnhance = async () => {
    if (text.length < 5) {
      toast.error("Please enter a few words before enhancing.");
      return;
    }

    setIsEnhancing(true);
    try {
      const response = await getCoachResponse({
        message: `Act as a senior curriculum designer. Rewrite and enhance the following course topic/prompt to be more structured, detailed, and clear for an AI course generator. Keep it concise but improve depth. Prompt: "${text}"`,
      });

      const textBlock = response.blocks?.find(b => b.type === "text");
      const enhancedText = textBlock ? (textBlock.content as any).body : "";

      if (enhancedText) {
        // Clean up any AI prefixing if present
        let cleaned = enhancedText.replace(/^(Enhanced Prompt:|Here is an enhanced version:)/i, "").trim();
        form.setValue("text", cleaned);
        toast.success("Prompt enhanced by AI");
      }
    } catch (err) {
      toast.error("Failed to enhance prompt. Please try again.");
    } finally {
      setIsEnhancing(false);
    }
  };

  async function onSubmit(values: Values) {
    const relatedCourseTitle = courses.find((c) => c.id === values.relatedCourseId)?.title;
    try {
      await create.mutateAsync({
        text: values.text,
        relatedCourseId: values.relatedCourseId,
        relatedCourseTitle
      });
      form.reset({ text: "", relatedCourseId: undefined });
      toast.success("Prompt template saved to project.");
    } catch (err) {
      console.error("Save failed:", err);
    }
  }

  return (
    <div className="glass-strong rounded-[2.5rem] p-8 md:p-10 border border-border relative overflow-hidden group shadow-2xl">
      {/* Decorative Glows */}
      <div className="absolute -top-32 -right-32 w-80 h-80 bg-primary/10 blur-[100px] rounded-full pointer-events-none group-hover:bg-primary/15 transition-all duration-700" />
      <div className="absolute -bottom-32 -left-32 w-80 h-80 bg-accent/5 blur-[100px] rounded-full pointer-events-none" />

      <div className="relative z-10">
        <header className="mb-6 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-muted border border-border flex items-center justify-center text-primary">
              <Sparkles className="h-5 w-5 animate-pulse" />
            </div>
            <div>
              <h2 className="text-2xl font-bold text-foreground tracking-tight">Project Engine</h2>
              <p className="text-sm text-muted-foreground font-medium">Draft detailed prompts for your course fleet</p>
            </div>
          </div>
          <div className="hidden sm:flex items-center gap-2 text-[10px] uppercase font-bold tracking-widest text-muted-foreground/60 bg-muted px-3 py-1.5 rounded-full border border-border/50">
            <Info className="h-3 w-3" />
            Context-Aware Generation
          </div>
        </header>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
            <FormField
              control={form.control}
              name="text"
              render={({ field }) => (
                <FormItem>
                  <FormControl>
                    <div className="relative">
                      <Textarea
                        rows={6}
                        placeholder='e.g., "A comprehensive guide to building responsive React applications with TailwindCSS, including modern hooks and state management..."'
                        className="bg-white text-black border-none shadow-xl focus:ring-primary/20 text-lg p-6 resize-none min-h-[220px] rounded-3xl transition-all placeholder:text-slate-400 leading-relaxed overflow-hidden scrollbar-hide"
                        {...field}
                      />

                      <div className="absolute bottom-4 right-4 flex items-center gap-3">
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          className="h-10 px-4 rounded-xl border-border hover:border-primary/30 transition-all group/btn"
                          onClick={handleEnhance}
                          disabled={isEnhancing || !text}
                        >
                          {isEnhancing ? (
                            <Loader2 className="h-4 w-4 animate-spin text-primary" />
                          ) : (
                            <Wand2 className="h-4 w-4 group-hover/btn:scale-110 transition-transform" />
                          )}
                          <span className="ml-2 font-bold text-[10px] uppercase tracking-widest">Enhance Prompt</span>
                        </Button>
                        <div className="h-8 w-px bg-border" />
                        <span className="text-[10px] font-mono font-bold text-muted-foreground/40 uppercase tracking-widest">{text.length} / 3000</span>
                      </div>
                    </div>
                  </FormControl>
                  <FormMessage className="text-xs font-bold text-destructive/80 mt-2 px-2" />
                </FormItem>
              )}
            />

            <div className="grid sm:grid-cols-2 gap-6 items-end">
              <FormField
                control={form.control}
                name="relatedCourseId"
                render={({ field }) => (
                  <FormItem className="space-y-2">
                    <FormLabel className="text-[10px] uppercase font-bold tracking-widest text-muted-foreground pl-1">Reference existing content</FormLabel>
                    <Select onValueChange={field.onChange} value={field.value ?? undefined}>
                      <FormControl>
                        <SelectTrigger className="h-12 bg-muted/50 border-border rounded-2xl focus:ring-primary/20">
                          <SelectValue placeholder="Add to existing course..." />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent className="bg-popover border-border rounded-2xl">
                        <SelectItem value="none" className="text-muted-foreground">New course</SelectItem>
                        {courses.map((c) => (
                          <SelectItem key={c.id} value={c.id}>
                            {c.title}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </FormItem>
                )}
              />

              <div className="flex gap-3 h-12">
                <Button
                  type="button"
                  variant="outline"
                  className="flex-1 rounded-2xl border-border font-bold text-[11px] uppercase tracking-widest"
                  onClick={form.handleSubmit(onSubmit)}
                  disabled={create.isPending || !text}
                >
                  <Save className="h-4 w-4 mr-2" />
                  {create.isPending ? "Saving..." : "Save Template"}
                </Button>

                <Button
                  type="button"
                  variant="hero"
                  className="flex-[1.5] rounded-2xl font-bold text-[11px] uppercase tracking-widest group shadow-glow"
                  disabled={!text}
                  onClick={() => navigate(`/create-course?projectId=${projectId}&topic=${encodeURIComponent(text)}`)}
                >
                  <Sparkles className="h-4 w-4 mr-2 group-hover:scale-125 transition-transform" />
                  Generate Magic
                </Button>
              </div>
            </div>
          </form>
        </Form>
      </div>
    </div>
  );
}
