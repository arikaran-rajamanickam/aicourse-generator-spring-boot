import { useCourseBuilder } from "@/context/CourseBuilderContext";
import type { Module, Lesson } from "@/types/course-builder";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ScrollArea } from "@/components/ui/scroll-area";
import { ChevronDown, ChevronRight, File, Folder, MoreVertical, Plus, Trash2, ArrowUp, ArrowDown, Edit2, Sparkles, Loader2 } from "lucide-react";
import { useState } from "react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { toast } from "sonner";
import { batchGenerateLessons } from "@/services/courseApi";

export function ModuleLessonTree() {
  const { state, dispatch } = useCourseBuilder();
  const { course, selectedLessonId } = state;
  const [expandedModules, setExpandedModules] = useState<Record<string, boolean>>({});
  const [editingModuleId, setEditingModuleId] = useState<string | null>(null);
  const [editingLessonId, setEditingLessonId] = useState<string | null>(null);
  const [editTitle, setEditTitle] = useState("");
  const [batchGeneratingModuleId, setBatchGeneratingModuleId] = useState<string | null>(null);

  const handleBatchGenerate = async (moduleId: string) => {
    const limitStr = window.prompt("Enter max number of AI lessons to generate simultaneously (1-10):", "3");
    if (!limitStr) return;
    const limit = parseInt(limitStr, 10);
    if (isNaN(limit) || limit < 1 || limit > 10) {
      toast.error("Please enter a valid number between 1 and 10.");
      return;
    }

    setBatchGeneratingModuleId(moduleId);
    toast.loading(`Batch generating up to ${limit} lessons...`, { id: `batch-${moduleId}` });
    try {
      const generatedLessons = await batchGenerateLessons(course.id, moduleId, limit);
      toast.success(`Successfully generated ${generatedLessons.length} lessons!`, { id: `batch-${moduleId}` });
      // We could optionally dispatch an action to update the UI or refetch course,
      // but reloading the course or letting the user click them works since it's saved to DB.
      // Easiest is to just update state if we want, but since they are already in the UI (as pending),
      // we don't strictly have to do anything except let the user know they are ready!
    } catch (error: any) {
      toast.error(`Batch generation failed: ${error.message}`, { id: `batch-${moduleId}` });
    } finally {
      setBatchGeneratingModuleId(null);
    }
  };

  const toggleModule = (id: string) => {
    setExpandedModules(prev => ({ ...prev, [id]: !prev[id] }));
  };

  const addModule = () => {
    const newModule: Module = {
      id: crypto.randomUUID(),
      title: "New Module",
      description: "",
      learningObjectives: [],
      lessons: [],
      order: course.modules.length,
    };
    dispatch({ type: "ADD_MODULE", payload: newModule });
    setExpandedModules(prev => ({ ...prev, [newModule.id]: true }));
  };

  const addLesson = (moduleId: string) => {
    const moduleItem = course.modules.find(m => m.id === moduleId);
    if (!moduleItem) return;
    
    const newLesson: Lesson = {
      id: crypto.randomUUID(),
      title: "New Lesson",
      contentBlocks: [],
      order: moduleItem.lessons.length,
    };
    dispatch({ type: "ADD_LESSON", payload: { moduleId, lesson: newLesson } });
    if (!expandedModules[moduleId]) toggleModule(moduleId);
    dispatch({ type: "SELECT_LESSON", payload: newLesson.id });
  };

  return (
    <div className="flex flex-col h-full bg-background border rounded-lg shadow-sm">
      <div className="p-4 border-b flex items-center justify-between bg-muted/30">
        <h3 className="font-semibold text-sm">Course Structure</h3>
        <Button variant="ghost" size="icon" onClick={addModule}>
          <Plus className="w-4 h-4" />
        </Button>
      </div>

      <ScrollArea className="flex-1">
        <div className="p-2 space-y-1">
          {course.modules.length === 0 ? (
            <div className="text-center p-6 text-sm text-muted-foreground border-2 border-dashed rounded-lg m-2">
              <Folder className="w-8 h-8 mx-auto mb-2 opacity-20" />
              <p>No modules yet</p>
              <Button variant="outline" size="sm" className="mt-4" onClick={addModule}>
                Add Module
              </Button>
            </div>
          ) : (
            course.modules.map(m => (
              <div key={m.id} className="space-y-1">
                {/* Module Item */}
                <div 
                  className="flex items-center group rounded-md hover:bg-muted/50 p-1"
                >
                  <Button 
                    variant="ghost" 
                    size="icon" 
                    className="h-6 w-6 shrink-0"
                    onClick={() => toggleModule(m.id)}
                  >
                    {expandedModules[m.id] ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
                  </Button>
                  
                  {editingModuleId === m.id ? (
                    <Input 
                      className="h-7 text-sm ml-1 flex-1"
                      autoFocus
                      value={editTitle}
                      onChange={e => setEditTitle(e.target.value)}
                      onBlur={() => {
                        dispatch({ type: "UPDATE_MODULE", payload: { moduleId: m.id, updates: { title: editTitle } } });
                        setEditingModuleId(null);
                      }}
                      onKeyDown={e => {
                        if (e.key === 'Enter') e.currentTarget.blur();
                      }}
                    />
                  ) : (
                    <span 
                      className="text-sm font-medium ml-1 flex-1 truncate cursor-pointer select-none"
                      onDoubleClick={() => {
                        setEditTitle(m.title);
                        setEditingModuleId(m.id);
                      }}
                    >
                      {m.title}
                    </span>
                  )}

                  <div className="flex items-center opacity-0 group-hover:opacity-100 transition-opacity">
                    <Button variant="ghost" size="icon" className="h-6 w-6" onClick={() => addLesson(m.id)}>
                      <Plus className="h-3 w-3" />
                    </Button>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon" className="h-6 w-6">
                          {batchGeneratingModuleId === m.id ? (
                            <Loader2 className="h-3 w-3 animate-spin text-primary" />
                          ) : (
                            <MoreVertical className="h-3 w-3" />
                          )}
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={() => {
                          setEditTitle(m.title);
                          setEditingModuleId(m.id);
                        }}><Edit2 className="h-4 w-4 mr-2" /> Rename</DropdownMenuItem>
                        <DropdownMenuItem onClick={() => dispatch({ type: "MOVE_MODULE_UP", payload: m.id })}><ArrowUp className="h-4 w-4 mr-2" /> Move Up</DropdownMenuItem>
                        <DropdownMenuItem onClick={() => dispatch({ type: "MOVE_MODULE_DOWN", payload: m.id })}><ArrowDown className="h-4 w-4 mr-2" /> Move Down</DropdownMenuItem>
                        <DropdownMenuItem onClick={() => dispatch({ type: "DELETE_MODULE", payload: m.id })} className="text-destructive"><Trash2 className="h-4 w-4 mr-2" /> Delete</DropdownMenuItem>
                        <DropdownMenuItem 
                          disabled={batchGeneratingModuleId === m.id}
                          onClick={() => handleBatchGenerate(m.id)}
                          className="text-primary font-medium border-t mt-1 pt-1"
                        >
                          {batchGeneratingModuleId === m.id ? <Loader2 className="h-4 w-4 mr-2 animate-spin" /> : <Sparkles className="h-4 w-4 mr-2" />}
                          Batch Generate AI Lessons
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </div>
                </div>

                {/* Lessons */}
                {expandedModules[m.id] && m.lessons.map(l => (
                  <div 
                    key={l.id} 
                    className={`flex items-center group rounded-md ml-6 pl-2 p-1 border-l-2 cursor-pointer
                      ${selectedLessonId === l.id ? 'bg-primary/5 border-primary text-primary' : 'hover:bg-muted/50 border-transparent text-muted-foreground'}`}
                    onClick={() => dispatch({ type: "SELECT_LESSON", payload: l.id })}
                  >
                    <File className="h-3.5 w-3.5 shrink-0 mr-2" />
                    
                    {editingLessonId === l.id ? (
                      <Input 
                        className="h-7 text-sm flex-1"
                        autoFocus
                        value={editTitle}
                        onChange={e => setEditTitle(e.target.value)}
                        onBlur={() => {
                          dispatch({ type: "UPDATE_LESSON", payload: { moduleId: m.id, lessonId: l.id, updates: { title: editTitle } } });
                          setEditingLessonId(null);
                        }}
                        onKeyDown={e => {
                          if (e.key === 'Enter') e.currentTarget.blur();
                        }}
                        onClick={e => e.stopPropagation()}
                      />
                    ) : (
                      <span 
                        className="text-sm flex-1 truncate select-none"
                        onDoubleClick={(e) => {
                          e.stopPropagation();
                          setEditTitle(l.title);
                          setEditingLessonId(l.id);
                        }}
                      >
                        {l.title}
                      </span>
                    )}

                    <div className="flex items-center opacity-0 group-hover:opacity-100 transition-opacity" onClick={e => e.stopPropagation()}>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="icon" className="h-6 w-6">
                            <MoreVertical className="h-3 w-3" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => {
                            setEditTitle(l.title);
                            setEditingLessonId(l.id);
                          }}><Edit2 className="h-4 w-4 mr-2" /> Rename</DropdownMenuItem>
                          <DropdownMenuItem onClick={() => dispatch({ type: "MOVE_LESSON_UP", payload: { moduleId: m.id, lessonId: l.id } })}><ArrowUp className="h-4 w-4 mr-2" /> Move Up</DropdownMenuItem>
                          <DropdownMenuItem onClick={() => dispatch({ type: "MOVE_LESSON_DOWN", payload: { moduleId: m.id, lessonId: l.id } })}><ArrowDown className="h-4 w-4 mr-2" /> Move Down</DropdownMenuItem>
                          <DropdownMenuItem className="text-destructive" onClick={() => dispatch({ type: "DELETE_LESSON", payload: { moduleId: m.id, lessonId: l.id } })}><Trash2 className="h-4 w-4 mr-2" /> Delete</DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                  </div>
                ))}
              </div>
            ))
          )}
        </div>
      </ScrollArea>
    </div>
  );
}
