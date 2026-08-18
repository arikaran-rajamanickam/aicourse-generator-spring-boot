import { ModuleLessonTree } from "./ModuleLessonTree";
import { ContentEditor } from "./ContentEditor";

export function StepStructure() {
  return (
    <div className="p-6 h-[calc(100vh-160px)] flex gap-6 max-w-7xl mx-auto w-full animate-fade-in">
      <div className="w-80 shrink-0">
        <ModuleLessonTree />
      </div>
      <div className="flex-1 min-w-0">
        <ContentEditor />
      </div>
    </div>
  );
}
