import { useState } from "react";
import { useCourseBuilder } from "@/context/CourseBuilderContext";
import type { ContentBlock } from "@/types/course-builder";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Plus, Type, Video, Link as LinkIcon, FileText, Code, Sparkles, BrainCircuit, ArrowUp, ArrowDown, Trash2, Image as ImageIcon } from "lucide-react";
import { LessonMediaPicker } from "./LessonMediaPicker";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

export function ContentEditor() {
  const { state, dispatch } = useCourseBuilder();
  const { course, selectedLessonId } = state;
  const [isMediaPickerOpen, setIsMediaPickerOpen] = useState(false);
  const [targetBlockId, setTargetBlockId] = useState<string | null>(null);

  // Find the selected lesson deeply
  let selectedLesson = null;
  let selectedModuleId = null;

  for (const m of course.modules) {
    const lesson = m.lessons.find((l) => l.id === selectedLessonId);
    if (lesson) {
      selectedLesson = lesson;
      selectedModuleId = m.id;
      break;
    }
  }

  if (!selectedLessonId || !selectedLesson || !selectedModuleId) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-muted-foreground bg-muted/10 border rounded-lg shadow-sm p-8">
        <FileText className="w-12 h-12 mb-4 opacity-20" />
        <p>Select a lesson from the sidebar to edit its content</p>
      </div>
    );
  }

  const addBlock = (type: ContentBlock["type"]) => {
    const order = selectedLesson.contentBlocks.length;
    let newBlock: ContentBlock;

    switch(type) {
      case "text":
        newBlock = { id: crypto.randomUUID(), type, content: "", order };
        break;
      case "video":
        newBlock = { id: crypto.randomUUID(), type, url: "", title: "", order };
        break;
      case "link":
        newBlock = { id: crypto.randomUUID(), type, url: "", title: "", description: "", order };
        break;
      case "file":
        newBlock = { id: crypto.randomUUID(), type, fileName: "Upload pending...", fileUrl: "", order };
        break;
      case "code":
        newBlock = { id: crypto.randomUUID(), type, language: "javascript", code: "", order };
        break;
      case "image":
        newBlock = { id: crypto.randomUUID(), type, url: "", alt: "", prompt: "", order };
        break;
      case "ai-generated":
        newBlock = { id: crypto.randomUUID(), type, prompt: "", content: "", order };
        break;
      case "quiz":
        newBlock = { 
          id: crypto.randomUUID(), 
          type, 
          quiz: { id: crypto.randomUUID(), title: "Inline Quiz", questions: [], settings: { passingScore: 80, randomize: false } }, 
          order 
        };
        break;
      default:
        return;
    }

    dispatch({
      type: "ADD_CONTENT_BLOCK",
      payload: { moduleId: selectedModuleId, lessonId: selectedLesson.id, block: newBlock }
    });
  };

  const updateBlock = (blockId: string, updates: Partial<ContentBlock>) => {
    dispatch({
      type: "UPDATE_CONTENT_BLOCK",
      payload: { moduleId: selectedModuleId, lessonId: selectedLesson.id, blockId, updates }
    });
  };

  const renderBlock = (block: ContentBlock) => {
    return (
      <div key={block.id} className="relative group p-4 bg-background border rounded-lg shadow-sm space-y-4">
        {/* Block Header Toolbar */}
        <div className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity flex space-x-1 bg-background border shadow-sm rounded-md p-1">
           <Button variant="ghost" size="icon" className="h-6 w-6" onClick={() => dispatch({ type: "MOVE_BLOCK_UP", payload: { moduleId: selectedModuleId, lessonId: selectedLesson.id, blockId: block.id } })}><ArrowUp className="h-3 w-3" /></Button>
           <Button variant="ghost" size="icon" className="h-6 w-6" onClick={() => dispatch({ type: "MOVE_BLOCK_DOWN", payload: { moduleId: selectedModuleId, lessonId: selectedLesson.id, blockId: block.id } })}><ArrowDown className="h-3 w-3" /></Button>
           <Button variant="ghost" size="icon" className="h-6 w-6 text-destructive" onClick={() => dispatch({ type: "DELETE_CONTENT_BLOCK", payload: { moduleId: selectedModuleId, lessonId: selectedLesson.id, blockId: block.id } })}><Trash2 className="h-3 w-3" /></Button>
        </div>

        {/* Block Content Inputs */}
        <div className="pt-2">
          {block.type === "text" && (
            <div className="space-y-2">
              <div className="flex items-center gap-2 text-muted-foreground pb-2"><Type className="w-4 h-4"/> <span className="text-sm font-medium">Text Content</span></div>
              <Textarea 
                placeholder="Write your lesson content here (Markdown supported in future)..." 
                value={block.content} 
                onChange={(e) => updateBlock(block.id, { content: e.target.value })}
                rows={5}
              />
            </div>
          )}

          {block.type === "video" && (
            <div className="space-y-2">
              <div className="flex items-center gap-2 text-muted-foreground pb-2"><Video className="w-4 h-4"/> <span className="text-sm font-medium">Video URL</span></div>
              <Input 
                placeholder="YouTube or Vimeo URL" 
                value={block.url} 
                onChange={(e) => updateBlock(block.id, { url: e.target.value })}
              />
            </div>
          )}

          {block.type === "link" && (
            <div className="space-y-2">
              <div className="flex items-center gap-2 text-muted-foreground pb-2"><LinkIcon className="w-4 h-4"/> <span className="text-sm font-medium">Reference Link</span></div>
              <Input 
                placeholder="URL" 
                value={block.url} 
                onChange={(e) => updateBlock(block.id, { url: e.target.value })}
              />
              <Input 
                placeholder="Title (Optional)" 
                value={block.title} 
                onChange={(e) => updateBlock(block.id, { title: e.target.value })}
              />
            </div>
          )}

          {block.type === "code" && (
            <div className="space-y-2">
              <div className="flex items-center gap-2 text-muted-foreground pb-2"><Code className="w-4 h-4"/> <span className="text-sm font-medium">Code Snippet</span></div>
              <Textarea 
                className="font-mono bg-muted/50"
                placeholder="// Write code here..." 
                value={block.code} 
                onChange={(e) => updateBlock(block.id, { code: e.target.value })}
                rows={4}
              />
            </div>
          )}

          {block.type === "ai-generated" && (
            <div className="space-y-2">
              <div className="flex items-center gap-2 text-primary pb-2"><Sparkles className="w-4 h-4"/> <span className="text-sm font-medium">AI Generated Component</span></div>
              <Input 
                placeholder="Prompt to generate content..." 
                value={block.prompt} 
                onChange={(e) => updateBlock(block.id, { prompt: e.target.value })}
              />
              {block.content && (
                <Textarea 
                  value={block.content} 
                  onChange={(e) => updateBlock(block.id, { content: e.target.value })}
                  rows={4}
                />
              )}
            </div>
          )}

          {block.type === "file" && (
            <div className="space-y-2 border-2 border-dashed p-6 rounded-lg text-center bg-muted/10">
              <FileText className="w-8 h-8 opacity-50 mx-auto mb-2" />
              <p className="text-sm font-medium">Upload Document</p>
              <p className="text-xs text-muted-foreground mb-4">PDF, DOCX, PPTX</p>
              <Button variant="outline" size="sm">Select File</Button>
            </div>
          )}

          {block.type === "quiz" && (
            <div className="space-y-2 border p-6 rounded-lg bg-muted/10">
              <div className="flex items-center gap-2 text-accent-foreground pb-2"><BrainCircuit className="w-4 h-4"/> <span className="text-sm font-medium">Inline Quiz: {block.quiz.title}</span></div>
              <p className="text-xs text-muted-foreground">Configure in the Assessment Tab.</p>
            </div>
          )}

          {block.type === "image" && (
            <div className="space-y-4">
              <div className="flex items-center gap-2 text-muted-foreground pb-2">
                <ImageIcon className="w-4 h-4"/> 
                <span className="text-sm font-medium">Lesson Image</span>
              </div>
              
              {block.url ? (
                <div className="relative group/img rounded-lg overflow-hidden border">
                  <img src={block.url} alt={block.alt} className="w-full h-auto max-h-80 object-contain bg-muted" />
                  <div className="absolute inset-0 bg-black/40 opacity-0 group-hover/img:opacity-100 transition-opacity flex items-center justify-center">
                     <Button variant="secondary" size="sm" onClick={() => setIsMediaPickerOpen(true)}>Change Image</Button>
                  </div>
                </div>
              ) : (
                <Button 
                  variant="outline" 
                  className="w-full h-32 border-dashed border-2 flex flex-col gap-2"
                  onClick={() => {
                    setTargetBlockId(block.id);
                    setIsMediaPickerOpen(true);
                  }}
                >
                  <ImageIcon className="w-8 h-8 opacity-20" />
                  <span>Add Image</span>
                </Button>
              )}
              
              <div className="grid grid-cols-1 gap-4 mt-2">
                <div className="space-y-1">
                  <Label className="text-xs">Alt Text (Accessibility)</Label>
                  <Input 
                    placeholder="Describe this image..." 
                    value={block.alt} 
                    onChange={(e) => updateBlock(block.id, { alt: e.target.value })}
                  />
                </div>
                <div className="space-y-1">
                  <Label className="text-xs">Generation Prompt (Reference)</Label>
                  <Textarea 
                    placeholder="What should this image show?" 
                    value={block.prompt} 
                    onChange={(e) => updateBlock(block.id, { prompt: e.target.value })}
                    rows={2}
                  />
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    );
  };

  return (
    <div className="flex flex-col h-full bg-background border rounded-lg shadow-sm">
      <div className="p-4 border-b flex items-center justify-between">
        <h3 className="font-semibold">{selectedLesson.title}</h3>
      </div>

      <ScrollArea className="flex-1 p-4">
        <div className="space-y-4 max-w-3xl mx-auto pb-20">
          {selectedLesson.contentBlocks.length === 0 ? (
             <div className="text-center p-12 text-sm text-muted-foreground border-2 border-dashed rounded-lg">
               <p>No content in this lesson yet.</p>
               <p className="mt-1">Add a block below to get started.</p>
             </div>
          ) : (
            selectedLesson.contentBlocks.sort((a, b) => a.order - b.order).map(renderBlock)
          )}
          
          <div className="flex justify-center mt-8">
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="outline" className="shadow-sm rounded-full gap-2">
                  <Plus className="w-4 h-4" /> Add Block
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="center" className="w-56">
                <DropdownMenuItem onClick={() => addBlock("text")}><Type className="w-4 h-4 mr-2"/> Text Block</DropdownMenuItem>
                <DropdownMenuItem onClick={() => addBlock("video")}><Video className="w-4 h-4 mr-2"/> Video</DropdownMenuItem>
                <DropdownMenuItem onClick={() => addBlock("link")}><LinkIcon className="w-4 h-4 mr-2"/> Reference Link</DropdownMenuItem>
                <DropdownMenuItem onClick={() => addBlock("file")}><FileText className="w-4 h-4 mr-2"/> Document / File</DropdownMenuItem>
                <DropdownMenuItem onClick={() => addBlock("code")}><Code className="w-4 h-4 mr-2"/> Code Snippet</DropdownMenuItem>
                <DropdownMenuItem onClick={() => addBlock("image")}><ImageIcon className="w-4 h-4 mr-2"/> Image / Diagram</DropdownMenuItem>
                <DropdownMenuItem onClick={() => addBlock("quiz")}><BrainCircuit className="w-4 h-4 mr-2"/> Inline Quiz</DropdownMenuItem>
                <DropdownMenuItem onClick={() => addBlock("ai-generated")} className="text-primary"><Sparkles className="w-4 h-4 mr-2"/> AI Content</DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      </ScrollArea>

      <LessonMediaPicker 
        isOpen={isMediaPickerOpen}
        onClose={() => setIsMediaPickerOpen(false)}
        lessonId={selectedLesson.id}
        onSelect={(url) => {
          if (targetBlockId) {
            updateBlock(targetBlockId, { url });
          }
          setIsMediaPickerOpen(false);
        }}
      />
    </div>
  );
}
