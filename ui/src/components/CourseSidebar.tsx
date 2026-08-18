import { Link } from "react-router-dom";
import { CheckCircle2, Circle, PlayCircle } from "lucide-react";
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from "./ui/sidebar";
import { useEffect, useState, useMemo } from "react";
import { getCourseById } from "../services/courseApi";
import { getCompletedLessonIds } from "../services/progressApi";
import { cn } from "../lib/utils";

export function CourseSidebar({ courseId, activeLessonId }: { courseId: string, activeLessonId?: string }) {
  const [course, setCourse] = useState<any>(null);
  const [completedSet, setCompletedSet] = useState<Set<string>>(new Set());

  // Keep hook order stable across renders; derive modules even when course is not loaded yet.
  const modules = useMemo(() => {
    if (!Array.isArray(course?.modules)) return [];
    return [...course.modules]
      .sort((a, b) => (a.order || 0) - (b.order || 0))
      .map(m => ({
        ...m,
        lessons: Array.isArray(m.lessons)
          ? [...m.lessons].sort((a, b) => (a.order || 0) - (b.order || 0))
          : []
      }));
  }, [course]);

  useEffect(() => {
    let mounted = true;
    async function load() {
      if (!courseId) return;
      try {
        const [courseData, completedIds] = await Promise.all([
          getCourseById(courseId),
          getCompletedLessonIds(courseId).catch(() => [] as string[])
        ]);
        if (mounted) {
          setCourse(courseData);
          setCompletedSet(new Set(completedIds));
        }
      } catch (e) {
        console.error(e);
      }
    }
    load();
    return () => {
      mounted = false;
    };
  }, [courseId, activeLessonId]);

  if (!course) return <Sidebar className="border-r" />;

  return (
    <Sidebar className="border-r z-40 bg-sidebar" collapsible="icon">
      <SidebarContent>
        {modules.map((module: any, mIdx: number) => {
          const lessons = Array.isArray(module.lessons) ? module.lessons : [];
          const hasActiveLesson = lessons.some((l: any) => l.id === activeLessonId);

          return (
            <SidebarGroup key={module.id}>
              <SidebarGroupLabel className="flex items-center justify-between font-bold text-[10px] uppercase tracking-wider text-muted-foreground mt-2">
                <span className="truncate">{module.title || `Module ${mIdx + 1}`}</span>
                {module.moduleLevel && (
                  <span className="ml-2 rounded-sm bg-primary/10 px-1.5 py-0.5 text-[8px] text-primary shrink-0">
                    {module.moduleLevel}
                  </span>
                )}
              </SidebarGroupLabel>
              <SidebarGroupContent>
                <SidebarMenu>
                  {lessons.map((lesson: any) => {
                    const isActive = lesson.id === activeLessonId;
                    const isCompleted = completedSet.has(lesson.id);

                    return (
                      <div key={lesson.id} className="flex flex-col">
                        <SidebarMenuItem>
                          <SidebarMenuButton asChild isActive={isActive} tooltip={lesson.title}>
                            <Link to={`/courses/${courseId}/lessons/${lesson.id}?moduleId=${module.id}`}>
                              {isCompleted ? (
                                <CheckCircle2 className="h-4 w-4 text-success shrink-0" />
                              ) : isActive ? (
                                <PlayCircle className="h-4 w-4 text-primary shrink-0" />
                              ) : (
                                <Circle className="h-4 w-4 text-muted-foreground shrink-0" />
                              )}
                              <span className={cn("truncate", isActive && "font-medium text-foreground")}>
                                {lesson.title || "Untitled"}
                              </span>
                            </Link>
                          </SidebarMenuButton>
                        </SidebarMenuItem>
                        
                        {Array.isArray(lesson.subLessons) && lesson.subLessons.length > 0 && (
                          <div className="ml-4 border-l-2 border-border pl-2 my-1 space-y-1">
                            {lesson.subLessons.map((sub: any) => {
                              const isSubActive = sub.id === activeLessonId;
                              const isSubCompleted = completedSet.has(sub.id);
                              return (
                                <SidebarMenuItem key={sub.id}>
                                  <SidebarMenuButton asChild isActive={isSubActive} tooltip={sub.title} size="sm">
                                    <Link to={`/courses/${courseId}/lessons/${sub.id}?moduleId=${module.id}`}>
                                      {isSubCompleted ? (
                                        <CheckCircle2 className="h-3.5 w-3.5 text-success shrink-0" />
                                      ) : isSubActive ? (
                                        <PlayCircle className="h-3.5 w-3.5 text-primary shrink-0" />
                                      ) : (
                                        <Circle className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
                                      )}
                                      <span className={cn("truncate text-sm", isSubActive && "font-medium text-foreground")}>
                                        {sub.title || "Untitled"}
                                      </span>
                                    </Link>
                                  </SidebarMenuButton>
                                </SidebarMenuItem>
                              );
                            })}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </SidebarMenu>
              </SidebarGroupContent>
            </SidebarGroup>
          );
        })}
      </SidebarContent>
    </Sidebar>
  );
}
