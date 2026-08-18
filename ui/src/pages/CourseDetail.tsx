import { useEffect, useMemo, useState, useCallback } from "react";
import { useParams, Link } from "react-router-dom";
import { ChevronLeft, ChevronRight, Share2, Trash2, Play, CheckCircle2, Sparkles, Pencil, Loader2, LayoutGrid, Settings2 } from "lucide-react";
import PrerequisiteLearnDialog, { type PrerequisiteFlowConfig } from "../components/course/PrerequisiteLearnDialog";
import { Button } from "../components/ui/button";
import { Progress } from "../components/ui/progress";
import { Input } from "../components/ui/input";
import { 
  deleteCourse, 
  getCourseById, 
  addModule, 
  addLesson, 
  renameModule, 
  renameLesson 
} from "../services/courseApi";
import { getCompletedLessonIds } from "../services/progressApi";
import { toast } from "sonner";
import { useAuth } from "../auth/AuthContext";
import { cn } from "../lib/utils";

export default function CourseDetail() {
  const { courseId } = useParams();
  const { user } = useAuth();
  const [isEditMode, setIsEditMode] = useState(false);

  const [course, setCourse] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  
  // Inline editing state
  const [editingModuleId, setEditingModuleId] = useState<string | null>(null);
  const [editingLessonId, setEditingLessonId] = useState<string | null>(null);
  const [editValue, setEditValue] = useState("");

  // Prerequisite learn dialog state
  const [prereqDialogOpen, setPrereqDialogOpen] = useState(false);
  const [selectedPrereq, setSelectedPrereq] = useState("");
  const [prereqConfig, setPrereqConfig] = useState<Partial<PrerequisiteFlowConfig>>({});

  const handleLearnPrereq = useCallback((item: string) => {
    setSelectedPrereq(item);
    setPrereqDialogOpen(true);
  }, []);

  const isCreator = useMemo(() => {
    if (!course || !user) return false;
    return String(course.creator) === String(user.id);
  }, [course, user]);

  const handleAddModule = async () => {
    if (!courseId) return;
    try {
      const newModule = await addModule(courseId, "New Module");
      setCourse((prev: any) => ({
        ...prev,
        modules: [...prev.modules, { ...newModule, lessons: [] }]
      }));
      toast.success("Module added");
    } catch (e) {
      toast.error("Failed to add module");
    }
  };

  const handleAddLesson = async (moduleId: string) => {
    if (!courseId) return;
    try {
      const newLesson = await addLesson(courseId, moduleId, "New Lesson");
      setCourse((prev: any) => ({
        ...prev,
        modules: prev.modules.map((m: any) => 
          m.id === moduleId ? { ...m, lessons: [...m.lessons, newLesson] } : m
        )
      }));
      toast.success("Lesson added");
    } catch (e) {
      toast.error("Failed to add lesson");
    }
  };

  const handleRenameModule = async (moduleId: string) => {
    if (!courseId) return;
    try {
      await renameModule(courseId, moduleId, editValue);
      setCourse((prev: any) => ({
        ...prev,
        modules: prev.modules.map((m: any) => m.id === moduleId ? { ...m, title: editValue } : m)
      }));
      setEditingModuleId(null);
      toast.success("Module renamed");
    } catch (e) {
      toast.error("Rename failed");
    }
  };

  const handleRenameLesson = async (moduleId: string, lessonId: string) => {
    if (!courseId) return;
    try {
      await renameLesson(courseId, moduleId, lessonId, editValue);
      setCourse((prev: any) => ({
        ...prev,
        modules: prev.modules.map((m: any) => 
          m.id === moduleId ? {
            ...m,
            lessons: m.lessons.map((l: any) => l.id === lessonId ? { ...l, title: editValue } : l)
          } : m
        )
      }));
      setEditingLessonId(null);
      toast.success("Lesson renamed");
    } catch (e) {
      toast.error("Rename failed");
    }
  };


  useEffect(() => {
    let mounted = true;

    async function loadCourse() {
      if (!courseId) return;
      setLoading(true);
      try {
        const [data, completedIds] = await Promise.all([
          getCourseById(courseId),
          getCompletedLessonIds(courseId).catch(() => [] as string[]),
        ]);
        const completedSet = new Set(completedIds);
        const rawModules = Array.isArray(data?.modules) ? data.modules : [];

        let completedLessons = 0;
        const modulesWithCompletion = rawModules.map((module: any) => {
          const rawLessons = Array.isArray(module?.lessons) ? module.lessons : [];
          const lessons = rawLessons.map((lesson: any) => {
            const isCompleted = completedSet.has(String(lesson?.id));
            if (isCompleted) completedLessons += 1;
            return {
              ...lesson,
              completed: isCompleted,
            };
          });
          return {
            ...module,
            lessons,
          };
        });

        if (mounted) {
          if (!data || !data.id) {
            console.error("CourseDetail: Course data is missing or invalid", data);
            setCourse(null);
            return;
          }
          setCourse({
            ...data,
            modules: modulesWithCompletion,
            completedLessons,
          });
        }
      } catch (error) {
        console.error("Failed to load course:", error);
        if (mounted) setCourse(null);
      } finally {
        if (mounted) setLoading(false);
      }
    }

    loadCourse();
    return () => {
      mounted = false;
    };
  }, [courseId]);

  const modules = useMemo(() => {
    if (!Array.isArray(course?.modules)) return [];
    
    // Sort modules by order
    return [...course.modules]
      .sort((a: any, b: any) => (a.order || 0) - (b.order || 0))
      .map((module: any) => ({
        ...module,
        // Sort lessons within each module by order
        lessons: Array.isArray(module.lessons) 
          ? [...module.lessons].sort((a: any, b: any) => (a.order || 0) - (b.order || 0))
          : []
      }));
  }, [course]);

  const handleDelete = async () => {
    if (!courseId) return;
    try {
      await deleteCourse(courseId);
      toast.success("Course deleted");
      window.location.href = "/";
    } catch (error) {
      console.error("Failed to delete course:", error);
      toast.error(error instanceof Error ? error.message : "Failed to delete course");
    }
  };

  if (loading) {
    return <div className="p-8 text-muted-foreground">Loading course...</div>;
  }

  if (!course) {
    return <div className="p-8 text-muted-foreground">Course not found.</div>;
  }

  const totalLessons = course.totalLessons ?? course.lessonCount ?? modules.reduce((sum: number, module: any) => sum + (Array.isArray(module?.lessons) ? module.lessons.length : 0), 0);
  const completedLessons = course.completedLessons ?? 0;

  const progress = totalLessons > 0
    ? Math.round((completedLessons / totalLessons) * 100)
    : 0;

  return (
    <div className="animate-fade-in p-8">
      {/* Top bar */}
      <div className="flex items-center justify-between">
        <Link to="/">
          <Button variant="outline" size="sm" className="gap-2">
            <ChevronLeft className="h-4 w-4" />
            Back to Dashboard
          </Button>
        </Link>
        <div className="flex items-center gap-3">
          <Link to={`/courses/${course.id || courseId}/coach`}>
            <Button variant="outline" className="gap-2">
              <Sparkles className="h-4 w-4 text-purple-500" />
              AI Coach
            </Button>
          </Link>
          {isCreator && (
            <Link to={`/courses/${course.id || courseId}/share`}>
              <Button variant="ghost" className="gap-2">
                <Share2 className="h-4 w-4" />
                Share
              </Button>
            </Link>
          )}
        </div>
      </div>

      {/* Course Info */}
      <div className="mt-8">
        <div className="h-3 w-3 rounded-full bg-muted-foreground" />
        <h1 className="mt-4 font-display text-4xl font-bold text-foreground">{course.title || course.topic || "Untitled Course"}</h1>
        <p className="mt-3 max-w-3xl text-muted-foreground leading-relaxed">
          Topic: {course.description || course.topic || "No description"}
        </p>

        {/* Progress */}
        <div className="mt-6 flex items-center justify-between">
          <span className="text-sm text-muted-foreground">Course Progress</span>
          <span className="text-sm font-medium text-primary">
            {progress}% ({completedLessons} / {totalLessons})
          </span>
        </div>
        <Progress value={progress} className="mt-2 h-2" />
      </div>

      {course.overview && (
        <div className="mt-8 border-t border-border pt-8 animate-fade-in">
          <div className="glass-card rounded-2xl p-6 md:p-8 space-y-8">
            <div>
              <h2 className="font-display text-2xl font-bold text-foreground mb-4">Course Overview</h2>
              <p className="text-muted-foreground">{course.overview.learningFlow}</p>
            </div>
            
            <div className="grid md:grid-cols-2 gap-8">
              <div className="space-y-4">
                <h3 className="font-semibold text-lg flex items-center gap-2"><CheckCircle2 className="w-5 h-5 text-primary" /> What you'll learn</h3>
                <div className="flex flex-wrap gap-2">
                  {course.overview.whatYouWillLearn?.map((item: string, i: number) => (
                    <span key={i} className="px-3 py-1 rounded-full bg-primary/10 text-primary text-sm font-medium border border-primary/20">
                      {item}
                    </span>
                  ))}
                </div>
              </div>
              
              <div className="space-y-4">
                <h3 className="font-semibold text-lg flex items-center gap-2"><LayoutGrid className="w-5 h-5 text-blue-500" /> Topics covered</h3>
                <ul className="space-y-2">
                  {course.overview.topicsCovered?.map((item: string, i: number) => (
                    <li key={i} className="flex items-start gap-2 text-sm text-muted-foreground">
                      <div className="w-1.5 h-1.5 rounded-full bg-blue-500/50 mt-1.5 shrink-0" />
                      <span>{item}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            <div className="grid md:grid-cols-2 gap-8 pt-6 border-t border-border/50">
              <div className="space-y-4">
                <h3 className="font-semibold text-lg text-foreground">Prerequisites</h3>
                <ul className="space-y-3">
                  {course.overview.prerequisites?.map((item: string, i: number) => (
                    <li key={i} className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-sm text-muted-foreground p-2 rounded-lg border border-border/50 bg-background/50">
                      <div className="flex items-start gap-2 mt-0.5">
                        <div className="w-1.5 h-1.5 rounded-full bg-muted-foreground/50 mt-1.5 shrink-0" />
                        <span className="font-medium text-foreground">{item}</span>
                      </div>
                      <Button
                        variant="secondary"
                        size="sm"
                        className="h-7 text-xs font-medium w-full sm:w-auto"
                        onClick={() => handleLearnPrereq(item)}
                      >
                        Learn this
                      </Button>
                    </li>
                  ))}
                </ul>
              </div>
              <div className="space-y-4">
                <h3 className="font-semibold text-lg text-foreground">Real-world use cases</h3>
                <ul className="space-y-2">
                  {course.overview.realWorldUseCases?.map((item: string, i: number) => (
                    <li key={i} className="flex items-start gap-2 text-sm text-muted-foreground">
                      <div className="w-1.5 h-1.5 rounded-full bg-purple-500/50 mt-1.5 shrink-0" />
                      <span>{item}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            {modules.length > 0 && modules[0].lessons?.length > 0 && (
              <div className="pt-4 flex justify-end">
                <Link to={`/courses/${course.id || courseId}/lessons/${modules[0].lessons[0].id}?moduleId=${modules[0].id}`}>
                  <Button size="lg" className="gap-2">
                    Start with Module 1 <ChevronRight className="w-4 h-4" />
                  </Button>
                </Link>
              </div>
            )}
          </div>
        </div>
      )}

      <div className="mt-8 border-t border-border pt-8">
        <div className="flex items-center justify-between mb-6">
          <h2 className="font-display text-2xl font-bold text-foreground">Course Modules</h2>
          {isEditMode && (
            <Button size="sm" variant="secondary" className="gap-2" onClick={handleAddModule}>
              <Pencil className="h-4 w-4" />
              Add Module
            </Button>
          )}
        </div>

        <div className="mt-6 space-y-4">
          {modules.map((module: any) => (
            <div key={module.id} className="glass-card rounded-xl overflow-hidden group">
              {/* Module header */}
              <div className="flex items-center justify-between p-5 border-b border-border/50">
                <div className="flex items-center gap-4 flex-1">
                  <span className="rounded-md gradient-primary px-3 py-1 text-xs font-bold text-primary-foreground uppercase tracking-wider">
                    Module {module.number || module.moduleNumber || "-"}
                  </span>
                  {editingModuleId === module.id ? (
                    <div className="flex items-center gap-2 flex-1">
                      <Input 
                        value={editValue} 
                        onChange={(e) => setEditValue(e.target.value)}
                        className="h-8 max-w-sm"
                        autoFocus
                        onKeyDown={(e) => {
                          if (e.key === "Enter") handleRenameModule(module.id);
                          if (e.key === "Escape") setEditingModuleId(null);
                        }}
                      />
                      <Button size="sm" onClick={() => handleRenameModule(module.id)}>Save</Button>
                      <Button size="sm" variant="ghost" onClick={() => setEditingModuleId(null)}>Cancel</Button>
                    </div>
                  ) : (
                    <h3 className="font-display font-semibold text-foreground">{module.title || module.name || "Untitled Module"}</h3>
                  )}
                </div>
                {isEditMode && editingModuleId !== module.id && (
                  <Button 
                    variant="ghost" 
                    size="icon" 
                    className="opacity-0 group-hover:opacity-100 transition-opacity"
                    onClick={() => {
                      setEditingModuleId(module.id);
                      setEditValue(module.title || "");
                    }}
                  >
                    <Pencil className="h-4 w-4 text-muted-foreground" />
                  </Button>
                )}
              </div>

              {/* Lessons */}
              <div className="divide-y divide-border/30">
                {(Array.isArray(module.lessons) ? module.lessons : []).map((lesson: any) => (
                  <div key={lesson.id} className="group/lesson flex items-center justify-between px-6 py-4 hover:bg-secondary/50 transition-colors">
                    <div className="flex items-center gap-3 w-full">
                      {lesson.completed ? (
                        <CheckCircle2 className="h-5 w-5 text-success" />
                      ) : (
                        <div className="h-5 w-5 rounded border border-border" />
                      )}
                      
                      {editingLessonId === lesson.id ? (
                        <div className="flex items-center gap-2 flex-1">
                          <Input 
                            value={editValue} 
                            onChange={(e) => setEditValue(e.target.value)}
                            className="h-8 max-w-sm"
                            autoFocus
                            onKeyDown={(e) => {
                              if (e.key === "Enter") handleRenameLesson(module.id, lesson.id);
                              if (e.key === "Escape") setEditingLessonId(null);
                            }}
                          />
                          <Button size="sm" onClick={() => handleRenameLesson(module.id, lesson.id)}>Save</Button>
                          <Button size="sm" variant="ghost" onClick={() => setEditingLessonId(null)}>Cancel</Button>
                        </div>
                      ) : (
                        <span className="text-sm text-foreground flex-1">{lesson.title || lesson.name || "Untitled Lesson"}</span>
                      )}

                      {isEditMode && editingLessonId !== lesson.id && (
                         <Button 
                          variant="ghost" 
                          size="icon" 
                          className="opacity-0 group-hover/lesson:opacity-100 transition-opacity mx-2"
                          onClick={() => {
                            setEditingLessonId(lesson.id);
                            setEditValue(lesson.title || "");
                          }}
                        >
                           <Pencil className="h-4 w-4 text-muted-foreground" />
                         </Button>
                      )}
                    </div>
                    <Link to={`/courses/${course.id || courseId}/lessons/${lesson.id}?moduleId=${module.id}`}>
                      <Button variant={isEditMode ? "outline" : "default"} size="sm" className="gap-1.5 shrink-0">
                        {isEditMode ? "Edit Content" : <> <Play className="h-3 w-3" /> Start </>}
                      </Button>
                    </Link>
                  </div>
                ))}
                
                {isEditMode && (
                  <div className="px-6 py-3 bg-muted/30">
                    <Button 
                      variant="ghost" 
                      size="sm" 
                      className="text-xs text-muted-foreground hover:text-foreground gap-2"
                      onClick={() => handleAddLesson(module.id)}
                    >
                      + Add Lesson
                    </Button>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Prerequisite Learn Dialog */}
      <PrerequisiteLearnDialog
        open={prereqDialogOpen}
        onOpenChange={setPrereqDialogOpen}
        prerequisite={selectedPrereq}
        courseId={courseId}
        courseTitle={course?.title || course?.topic}
        config={prereqConfig}
      />
    </div>
  );
}
