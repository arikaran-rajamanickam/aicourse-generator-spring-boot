import { useEffect, useState } from "react";
import { CourseBuilderProvider, useCourseBuilder } from "../context/CourseBuilderContext";
import { WizardStepper } from "../components/course-builder/WizardStepper";
import { StepMetadata } from "../components/course-builder/StepMetadata";
import { StepStructure } from "../components/course-builder/StepStructure";
import { StepQuiz } from "../components/course-builder/StepQuiz";
import { StepPreview } from "../components/course-builder/StepPreview";
import { Button } from "../components/ui/button";
import { Card } from "../components/ui/card";
import { Input } from "../components/ui/input";
import { saveBuiltCourse, createCourse } from "../services/courseApi";
import { addCourseToProject, getProjectById } from "../services/projectApi";
import { toast } from "sonner";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Sparkles, Library, PenLine, ChevronRight, Loader2 } from "lucide-react";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";

// --- Original Power User Wizard (moved to a component) ---
function CourseBuilderWizard({ onBack }: { onBack: () => void }) {
  const { state, dispatch } = useCourseBuilder();
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();

  const handleNext = async () => {
    if (state.currentStep < 3) {
      dispatch({ type: "SET_STEP", payload: state.currentStep + 1 });
    } else {
      try {
        setSubmitting(true);
        toast.loading("Publishing built course...", { id: "publish" });
        const createdCourse = await saveBuiltCourse(state.course);
        toast.success("Course published successfully!", { id: "publish" });
        navigate(`/courses/${createdCourse.id}`);
      } catch (e: any) {
        toast.error(`Publish failed: ${e.message}`, { id: "publish" });
      } finally {
        setSubmitting(false);
      }
    }
  };

  const handleBack = () => {
    if (state.currentStep > 0) {
      dispatch({ type: "SET_STEP", payload: state.currentStep - 1 });
    } else {
      onBack();
    }
  };

  const renderCurrentStep = () => {
    switch (state.currentStep) {
      case 0: return <StepMetadata />;
      case 1: return <StepStructure />;
      case 2: return <StepQuiz />;
      case 3: return <StepPreview />;
      default: return <StepMetadata />;
    }
  };

  return (
    <div className="flex flex-col min-h-[calc(100vh-4rem)] bg-background animate-fade-in w-full">
      <header className="sticky top-0 z-10 bg-background/80 backdrop-blur-md border-b">
        <WizardStepper />
      </header>
      <main className="flex-1 overflow-auto">
        {renderCurrentStep()}
      </main>
      <footer className="sticky bottom-0 z-10 bg-background/80 backdrop-blur-md border-t p-4">
        <div className="max-w-4xl mx-auto flex items-center justify-between">
          <Button variant="outline" onClick={handleBack}>
            {state.currentStep === 0 ? "Cancel" : "Back"}
          </Button>
          <div className="space-x-4">
            <Button variant="secondary" onClick={() => toast.info("Draft saved locally.")}>
              Save Draft
            </Button>
            <Button onClick={handleNext} disabled={submitting}>
              {state.currentStep === 3 ? (submitting ? "Publishing..." : "Publish Course") : "Next"}
            </Button>
          </div>
        </div>
      </footer>
    </div>
  );
}

// --- New "Start a Course" Entry Screen ---
function CourseEntryScreen() {
  const [mode, setMode] = useState<"entry" | "blank">("entry");
  const [topic, setTopic] = useState("");
  const [difficulty, setDifficulty] = useState("Beginner");
  const [generating, setGenerating] = useState(false);
  const [selectedProjectName, setSelectedProjectName] = useState<string | null>(null);
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const projectId = searchParams.get("projectId") || "";

  useEffect(() => {
    const initialTopic = searchParams.get("topic");
    if (initialTopic) {
      setTopic(initialTopic);
    }
  }, [searchParams]);

  useEffect(() => {
    let mounted = true;
    async function loadProjectName() {
      if (!projectId) return;
      try {
        const project = await getProjectById(projectId);
        if (mounted) setSelectedProjectName(project.name);
      } catch {
        if (mounted) setSelectedProjectName(null);
      }
    }
    loadProjectName();
    return () => {
      mounted = false;
    };
  }, [projectId]);

  const handleAIGenerate = async () => {
    if (!topic.trim()) {
      toast.error("Please enter a topic first!");
      return;
    }
    try {
      setGenerating(true);
      toast.loading("Generating your course...", { id: "generate" });
      const created = await createCourse({ topic: topic, difficulty: difficulty, duration: "2 Hours" });
      if (projectId) {
        await addCourseToProject(projectId, created.id);
      }
      toast.success("Course generated!", { id: "generate" });
      navigate(`/courses/${created.id}`); // View mode by default
    } catch (e: any) {
      toast.error(`Generation failed: ${e.message}`, { id: "generate" });
    } finally {
      setGenerating(false);
    }
  };

  const handleBlank = async () => {
    // Drop into the powerful wizard
    setMode("blank");
  };

  const handleTemplate = () => {
    toast.info("Template library coming soon!");
  };

  if (mode === "blank") {
    return (
      <CourseBuilderProvider>
        <CourseBuilderWizard onBack={() => setMode("entry")} />
      </CourseBuilderProvider>
    );
  }

  return (
    <div className="min-h-[calc(100vh-4rem)] flex flex-col items-center justify-center p-6 bg-gradient-to-b from-background to-muted/20">
      <div className="w-full max-w-4xl space-y-12 animate-fade-in">
        <div className="text-center space-y-4">
          <h1 className="text-4xl sm:text-5xl font-bold tracking-tight text-foreground">
            What do you want to learn or build today?
          </h1>
          {projectId && (
            <p className="text-sm text-primary font-medium">
              Generating into project: {selectedProjectName || "Selected Project"}
            </p>
          )}
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            Get started instantly with AI, pick a proven template, or start from scratch with full control.
          </p>
        </div>

        {/* AI Highlight Row */}
        <div 
          className="relative group p-1 -m-1 hover:-translate-y-1 transition-transform duration-300"
        >
          <div className="absolute inset-0 bg-gradient-to-r from-primary/20 via-blue-500/20 to-purple-500/20 rounded-2xl blur-xl opacity-50 group-hover:opacity-100 transition-opacity duration-500" />
          <Card className="relative p-6 sm:p-8 flex flex-col sm:flex-row items-center gap-6 shadow-xl border-primary/20 bg-background/95 backdrop-blur-sm">
            <div className="flex-1 w-full space-y-2">
              <div className="flex items-center gap-2 text-primary font-semibold mb-2">
                <Sparkles className="w-5 h-5" />
                <span>AI Generate</span>
              </div>
              <Input 
                value={topic}
                onChange={(e) => setTopic(e.target.value)}
                placeholder='e.g. "Learn Python for data science"'
                className="text-lg py-6 shadow-inner"
                onKeyDown={(e) => {
                  if (e.key === "Enter") handleAIGenerate();
                }}
              />
            </div>
            <div className="flex flex-col sm:flex-row gap-4 mt-4 sm:mt-8 shrink-0 w-full sm:w-auto">
              <Select value={difficulty} onValueChange={setDifficulty}>
                <SelectTrigger className="w-full sm:w-[180px] h-14 rounded-xl">
                  <SelectValue placeholder="Select difficulty" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="Beginner">Beginner</SelectItem>
                  <SelectItem value="Intermediate">Intermediate</SelectItem>
                  <SelectItem value="Advanced">Advanced</SelectItem>
                  <SelectItem value="BEGINNER_TO_ADVANCED">Beginner → Advanced</SelectItem>
                </SelectContent>
              </Select>
              <Button 
                size="lg" 
                className="w-full sm:w-auto text-md px-8 py-6 rounded-xl hover:scale-105 transition-transform h-14"
                onClick={handleAIGenerate}
                disabled={generating}
              >
                {generating ? (
                  <><Loader2 className="mr-2 h-5 w-5 animate-spin"/> Generating...</>
                ) : (
                  <>✨ Generate <ChevronRight className="ml-2 w-5 h-5" /></>
                )}
              </Button>
            </div>
          </Card>
        </div>

        <div className="text-center text-muted-foreground uppercase tracking-widest text-sm font-semibold">
          — or pick a starting point —
        </div>

        {/* Secondary Paths */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
          <div className="hover:-translate-y-1 transition-transform duration-300">
            <Card 
              className="p-6 cursor-pointer hover:shadow-lg transition-all h-full group border-muted/60"
              onClick={handleTemplate}
            >
              <Library className="w-8 h-8 text-blue-500 mb-4 group-hover:scale-110 transition-transform" />
              <h3 className="text-xl font-semibold mb-2">Template Library</h3>
              <p className="text-muted-foreground text-sm">
                Pick a proven course outline and structure to hit the ground running.
              </p>
            </Card>
          </div>

          <div className="hover:-translate-y-1 transition-transform duration-300">
            <Card 
              className="p-6 cursor-pointer hover:shadow-lg transition-all h-full group border-muted/60"
              onClick={handleBlank}
            >
              <PenLine className="w-8 h-8 text-orange-500 mb-4 group-hover:scale-110 transition-transform" />
              <h3 className="text-xl font-semibold mb-2">Blank Canvas</h3>
              <p className="text-muted-foreground text-sm">
                Start from scratch with the powerful module and lesson builder. Full control.
              </p>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function CreateCourse() {
  return <CourseEntryScreen />;
}
