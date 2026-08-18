import { useCourseBuilder } from "@/context/CourseBuilderContext";
import type { Quiz, QuizQuestion } from "@/types/course-builder";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { BrainCircuit, Filter, Plus, Trash2 } from "lucide-react";
import { useState } from "react";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

export function StepQuiz() {
  const { state, dispatch } = useCourseBuilder();
  const { course } = state;

  const [activeQuiz, setActiveQuiz] = useState<Quiz | null>(course.finalExam || null);

  const handleCreateFinalExam = () => {
    const newExam: Quiz = {
      id: crypto.randomUUID(),
      title: "Comprehensive Quiz",
      questions: [],
      settings: { passingScore: 80, randomize: false }
    };
    dispatch({ type: "UPDATE_METADATA", payload: { finalExam: newExam } });
    setActiveQuiz(newExam);
  };

  const addQuestion = () => {
    if (!activeQuiz) return;
    const newQ: QuizQuestion = {
      id: crypto.randomUUID(),
      type: "mcq",
      text: "",
      options: ["", "", "", ""],
      correctAnswer: 0,
      points: 10,
      difficulty: "medium",
      explanation: ""
    };

    const updatedQuiz = {
      ...activeQuiz,
      questions: [...activeQuiz.questions, newQ]
    };

    setActiveQuiz(updatedQuiz);
    dispatch({ type: "UPDATE_METADATA", payload: { finalExam: updatedQuiz } });
  };

  const updateQuestion = (index: number, updates: Partial<QuizQuestion>) => {
    if (!activeQuiz) return;
    const newQs = [...activeQuiz.questions];
    newQs[index] = { ...newQs[index], ...updates };
    
    const updatedQuiz = {
      ...activeQuiz,
      questions: newQs
    };
    setActiveQuiz(updatedQuiz);
    dispatch({ type: "UPDATE_METADATA", payload: { finalExam: updatedQuiz } });
  };

  const updateOption = (qIndex: number, optIndex: number, val: string) => {
    if (!activeQuiz) return;
    const q = activeQuiz.questions[qIndex];
    if (!q.options) return;
    const newOpts = [...q.options];
    newOpts[optIndex] = val;
    updateQuestion(qIndex, { options: newOpts });
  };

  if (!course.finalExam) {
    return (
      <div className="p-6 space-y-6 max-w-4xl mx-auto flex flex-col items-center justify-center h-[calc(100vh-200px)] animate-fade-in">
        <BrainCircuit className="w-16 h-16 text-muted-foreground opacity-30 mb-4" />
        <h2 className="text-2xl font-bold">Assess & Certify</h2>
        <p className="text-muted-foreground text-center max-w-md">
          Assessments measure student comprehension. You can add quizzes inline within lessons, or create a comprehensive quiz here.
        </p>
        <Button onClick={handleCreateFinalExam} size="lg" className="mt-4">
          <Plus className="w-4 h-4 mr-2" /> Create Quiz
        </Button>
      </div>
    );
  }

  return (
    <div className="p-6 h-[calc(100vh-160px)] flex gap-6 max-w-7xl mx-auto w-full animate-fade-in">
      <div className="w-80 shrink-0 bg-muted/10 border rounded-lg p-4 space-y-4">
        <h3 className="font-semibold pb-2 border-b">Assessments</h3>
        <div 
          className="p-3 bg-primary/10 border rounded-md cursor-pointer border-primary font-medium"
        >
          Comprehensive Quiz
        </div>
        <p className="text-xs text-muted-foreground mt-4">
          Note: Inline lesson quizzes are edited directly inside the Structure builder.
        </p>
      </div>

      <div className="flex-1 min-w-0 bg-background border rounded-lg shadow-sm p-6 overflow-y-auto">
        <div className="flex items-center justify-between mb-8 pb-4 border-b">
          <h2 className="text-2xl font-bold">Quiz Configuration</h2>
          <Button variant="outline" onClick={addQuestion}>
            <Plus className="w-4 h-4 mr-2" /> Add Question
          </Button>
        </div>

        <div className="space-y-8">
          {activeQuiz?.questions.length === 0 ? (
            <div className="text-center p-12 bg-muted/20 border-2 border-dashed rounded-lg">
              <p className="text-muted-foreground">No questions added yet.</p>
            </div>
          ) : (
            activeQuiz?.questions.map((q, qIndex) => (
              <div key={q.id} className="p-6 border rounded-lg shadow-sm space-y-4 relative group hover:border-primary/30 transition-colors">
                <div className="absolute top-4 right-4 flex opacity-0 group-hover:opacity-100 transition-opacity">
                  <Button 
                    variant="ghost" 
                    size="icon"
                    className="text-destructive h-8 w-8"
                    onClick={() => {
                        const newQs = activeQuiz.questions.filter((_, i) => i !== qIndex);
                        const updatedQuiz = { ...activeQuiz, questions: newQs };
                        setActiveQuiz(updatedQuiz);
                        dispatch({ type: "UPDATE_METADATA", payload: { finalExam: updatedQuiz } });
                    }}
                  >
                    <Trash2 className="w-4 h-4" />
                  </Button>
                </div>
                
                <div className="flex items-center gap-4">
                  <span className="bg-primary/20 text-primary font-bold w-8 h-8 flex items-center justify-center rounded-full">
                    {qIndex + 1}
                  </span>
                  <div className="flex-1">
                     <Textarea 
                       value={q.text} 
                       onChange={(e) => updateQuestion(qIndex, { text: e.target.value })}
                       placeholder="Enter question text..."
                       className="font-medium text-lg resize-none min-h-[60px]"
                     />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4 mt-4 ml-12">
                  <div className="space-y-1">
                    <label className="text-xs text-muted-foreground font-medium">Question Type</label>
                    <Select value={q.type} onValueChange={(val: any) => updateQuestion(qIndex, { type: val })}>
                      <SelectTrigger><SelectValue/></SelectTrigger>
                      <SelectContent>
                        <SelectItem value="mcq">Multiple Choice</SelectItem>
                        <SelectItem value="true-false">True / False</SelectItem>
                        <SelectItem value="short-answer">Short Answer</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-1">
                    <label className="text-xs text-muted-foreground font-medium">Difficulty</label>
                    <Select value={q.difficulty} onValueChange={(val: any) => updateQuestion(qIndex, { difficulty: val })}>
                      <SelectTrigger><SelectValue/></SelectTrigger>
                      <SelectContent>
                        <SelectItem value="easy">Easy</SelectItem>
                        <SelectItem value="medium">Medium</SelectItem>
                        <SelectItem value="hard">Hard</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                </div>

                {q.type === "mcq" && q.options && (
                  <div className="ml-12 mt-6 space-y-3">
                    <label className="text-sm font-medium">Options</label>
                    {q.options.map((opt, optIdx) => (
                      <div key={optIdx} className="flex items-center gap-3">
                        <input 
                          type="radio" 
                          name={`correct-${q.id}`}
                          checked={Number(q.correctAnswer) === optIdx}
                          onChange={() => updateQuestion(qIndex, { correctAnswer: optIdx })}
                          className="w-4 h-4 text-primary"
                        />
                        <Input 
                          value={opt}
                          onChange={(e) => updateOption(qIndex, optIdx, e.target.value)}
                          placeholder={`Option ${optIdx + 1}`}
                          className={Number(q.correctAnswer) === optIdx ? "border-primary bg-primary/5" : ""}
                        />
                      </div>
                    ))}
                  </div>
                )}
                
                {q.type === "true-false" && (
                  <div className="ml-12 mt-6 space-y-3">
                    <Select value={String(q.correctAnswer)} onValueChange={(v) => updateQuestion(qIndex, { correctAnswer: v })}>
                      <SelectTrigger><SelectValue placeholder="Select correct answer" /></SelectTrigger>
                      <SelectContent>
                        <SelectItem value="true">True</SelectItem>
                        <SelectItem value="false">False</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                )}

                <div className="ml-12 mt-6 space-y-2">
                  <label className="text-sm font-medium text-muted-foreground flex items-center gap-2">
                    Explanation (Optional)
                  </label>
                  <Textarea 
                    value={q.explanation || ""}
                    onChange={(e) => updateQuestion(qIndex, { explanation: e.target.value })}
                    placeholder="Explain why the correct answer is correct..."
                    className="resize-none min-h-[60px]"
                  />
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
