import { useCourseBuilder } from "@/context/CourseBuilderContext";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { BookOpen, AlertCircle, FileText, Code, Video, Link, ArrowRight } from "lucide-react";

export function StepPreview() {
  const { state } = useCourseBuilder();
  const { course } = state;

  return (
    <div className="p-6 h-[calc(100vh-160px)] flex flex-col md:flex-row gap-6 max-w-7xl mx-auto w-full animate-fade-in">
      <div className="md:w-1/3 shrink-0 bg-muted/10 border rounded-lg p-6 flex flex-col">
        <h3 className="text-xl font-bold mb-2">{course.title || "Untitled Course"}</h3>
        <p className="text-sm text-muted-foreground mb-6 line-clamp-3">
          {course.description || "No description provided."}
        </p>

        <div className="space-y-4 mb-8">
          <div className="flex items-center text-sm">
            <span className="font-medium w-24">Difficulty:</span>
            <span className="capitalize">{course.difficulty}</span>
          </div>
          <div className="flex items-center text-sm">
            <span className="font-medium w-24">Duration:</span>
            <span>{course.estimatedDuration.value} {course.estimatedDuration.unit}</span>
          </div>
          <div className="flex items-center text-sm">
            <span className="font-medium w-24">Modules:</span>
            <span>{course.modules.length}</span>
          </div>
        </div>
        
        <h4 className="font-semibold text-sm mb-4 uppercase tracking-wider text-muted-foreground pb-2 border-b">Preview Content</h4>
        <ScrollArea className="flex-1">
          <div className="space-y-6 pr-4">
             {course.modules.length === 0 ? (
                <div className="text-sm text-muted-foreground">No modules added.</div>
             ) : (
                course.modules.map((m, idx) => (
                  <div key={m.id} className="space-y-2">
                    <h5 className="font-medium text-sm flex items-center gap-2">
                      <span className="bg-primary/20 text-primary w-6 h-6 rounded-full flex items-center justify-center text-xs">{idx +1}</span>
                      {m.title}
                    </h5>
                    <div className="pl-8 space-y-1">
                       {m.lessons.map((l) => (
                          <div key={l.id} className="text-sm text-muted-foreground flex items-center gap-2">
                            <FileText className="w-3 h-3" /> {l.title}
                          </div>
                       ))}
                    </div>
                  </div>
                ))
             )}
          </div>
        </ScrollArea>
      </div>

      <div className="flex-1 bg-background border rounded-lg shadow-sm p-8 overflow-y-auto">
        <div className="border-2 border-primary/20 bg-primary/5 rounded-lg p-6 mb-8 text-center max-w-lg mx-auto">
          <BookOpen className="w-12 h-12 text-primary mx-auto mb-4" />
          <h2 className="text-2xl font-bold text-foreground">You are now previewing {course.title}</h2>
          <p className="text-muted-foreground mt-2 text-sm">This is a summary of what the students will see.</p>
        </div>

        <div className="space-y-8">
          {course.modules.map(m => (
            <div key={m.id} className="border-l-2 border-muted pl-6 space-y-4">
              <h3 className="text-xl font-bold">{m.title}</h3>
              {m.description && <p className="text-muted-foreground">{m.description}</p>}
              
              <div className="grid gap-4 mt-6">
                {m.lessons.map(l => (
                   <div key={l.id} className="p-4 border rounded-md bg-card shadow-sm hover:shadow-md transition-shadow">
                     <h4 className="font-medium flex items-center gap-2">
                        {l.title}
                     </h4>
                     <div className="flex gap-4 mt-4 text-xs text-muted-foreground flex-wrap">
                        {l.contentBlocks.map(b => {
                           let Icon = FileText;
                           let label = b.type;
                           if(b.type === "video") { Icon = Video; label = "Video"; }
                           if(b.type === "code") { Icon = Code; label = "Code snippet"; }
                           if(b.type === "link") { Icon = Link; label = "External Link"; }

                           return (
                             <span key={b.id} className="flex items-center gap-1 bg-muted px-2 py-1 rounded">
                                <Icon className="w-3 h-3" /> {label}
                             </span>
                           )
                        })}
                     </div>
                   </div>
                ))}
              </div>
            </div>
          ))}

          {course.finalExam && course.finalExam.questions.length > 0 && (
             <div className="border-t-2 border-dashed pt-8 mt-8 flex items-center justify-between">
                <div>
                  <h3 className="text-xl font-bold flex items-center gap-2"><AlertCircle className="w-5 h-5 text-accent-foreground" /> Final Exam Included</h3>
                  <p className="text-muted-foreground text-sm mt-1">Contains {course.finalExam.questions.length} questions.</p>
                </div>
                <Button variant="outline">Preview Exam <ArrowRight className="w-4 h-4 ml-2" /></Button>
             </div>
          )}
        </div>
      </div>
    </div>
  );
}
