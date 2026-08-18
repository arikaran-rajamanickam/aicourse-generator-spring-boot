import { useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Upload, Link as LinkIcon, Sparkles, Image as ImageIcon, Loader2 } from "lucide-react";
import { toast } from "sonner";

interface LessonMediaPickerProps {
  isOpen: boolean;
  onClose: () => void;
  onSelect: (url: string) => void;
  lessonId: string;
}

export function LessonMediaPicker({ isOpen, onClose, onSelect, lessonId }: LessonMediaPickerProps) {
  const [tab, setTab] = useState("upload");
  const [uploading, setUploading] = useState(false);
  const [imageUrl, setImageUrl] = useState("");
  const [file, setFile] = useState<File | null>(null);

  const handleUpload = async () => {
    if (!file) {
      toast.error("Please select a file first");
      return;
    }

    const formData = new FormData();
    formData.append("file", file);
    formData.append("lessonId", lessonId);
    formData.append("source", "upload");

    try {
      setUploading(true);
      const response = await fetch("/api/media/upload", {
        method: "POST",
        body: formData,
      });

      if (response.ok) {
        const data = await response.json();
        onSelect(data.url);
        onClose();
        toast.success("Image uploaded successfully");
      } else {
        throw new Error("Upload failed");
      }
    } catch (e) {
      toast.error("Failed to upload image");
    } finally {
      setUploading(false);
    }
  };

  const handleUrlSubmit = () => {
    if (!imageUrl) return;
    onSelect(imageUrl);
    onClose();
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Add Lesson Media</DialogTitle>
        </DialogHeader>
        
        <Tabs value={tab} onValueChange={setTab} className="w-full">
          <TabsList className="grid w-full grid-cols-3">
            <TabsTrigger value="upload" className="gap-2">
              <Upload className="w-4 h-4" /> Upload
            </TabsTrigger>
            <TabsTrigger value="url" className="gap-2">
              <LinkIcon className="w-4 h-4" /> URL
            </TabsTrigger>
            <TabsTrigger value="ai" className="gap-2">
              <Sparkles className="w-4 h-4" /> AI
            </TabsTrigger>
          </TabsList>

          <TabsContent value="upload" className="py-4 space-y-4">
            <div className="flex flex-col items-center justify-center border-2 border-dashed rounded-lg p-8 cursor-pointer hover:bg-muted/50 transition-colors relative">
               <input 
                 type="file" 
                 className="absolute inset-0 opacity-0 cursor-pointer" 
                 accept="image/*"
                 onChange={(e) => setFile(e.target.files?.[0] || null)}
               />
               {file ? (
                 <div className="text-center">
                    <ImageIcon className="w-10 h-10 mx-auto text-primary mb-2" />
                    <p className="text-sm font-medium">{file.name}</p>
                    <p className="text-xs text-muted-foreground mt-1">Click to change</p>
                 </div>
               ) : (
                 <div className="text-center">
                    <Upload className="w-10 h-10 mx-auto text-muted-foreground mb-2" />
                    <p className="text-sm font-medium">Click or drag image to upload</p>
                    <p className="text-xs text-muted-foreground mt-1">PNG, JPG, SVG up to 5MB</p>
                 </div>
               )}
            </div>
            <Button className="w-full" onClick={handleUpload} disabled={uploading || !file}>
              {uploading ? <><Loader2 className="w-4 h-4 mr-2 animate-spin" /> Uploading...</> : "Confirm Upload"}
            </Button>
          </TabsContent>

          <TabsContent value="url" className="py-4 space-y-4">
            <div className="space-y-2">
              <Label htmlFor="url">External Image URL</Label>
              <Input 
                id="url" 
                placeholder="https://images.unsplash.com/..." 
                value={imageUrl}
                onChange={(e) => setImageUrl(e.target.value)}
              />
            </div>
            <Button className="w-full" onClick={handleUrlSubmit} disabled={!imageUrl}>
              Insert Image
            </Button>
          </TabsContent>

          <TabsContent value="ai" className="py-4 space-y-4">
            <div className="p-8 text-center bg-muted/30 rounded-lg">
               <Sparkles className="w-10 h-10 mx-auto text-purple-500 mb-2 opacity-50" />
               <p className="text-sm text-muted-foreground">
                 AI Image generation (Gemini) coming soon!
               </p>
            </div>
          </TabsContent>
        </Tabs>
      </DialogContent>
    </Dialog>
  );
}
