import { LessonImage } from "@/types/lessonContent";

interface Props {
  block: LessonImage;
}

export default function LessonImageBlock({ block }: Props) {
  const { url, alt } = block.content;

  if (!url) {
    return (
      <div className="my-8 p-12 border-2 border-dashed rounded-xl flex flex-col items-center justify-center bg-muted/30 text-muted-foreground">
        <div className="w-12 h-12 rounded-full bg-muted flex items-center justify-center mb-4">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-image"><rect width="18" height="18" x="3" y="3" rx="2" ry="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/></svg>
        </div>
        <p className="text-sm font-medium">Visual representation</p>
        <p className="text-xs opacity-60 mt-1 max-w-xs text-center">{alt || "An image will appear here soon"}</p>
      </div>
    );
  }

  return (
    <figure className="my-8 group animate-fade-in">
      <div className="overflow-hidden rounded-xl border shadow-lg bg-muted transition-transform duration-500 hover:shadow-xl">
        <img 
          src={url} 
          alt={alt} 
          className="w-full h-auto max-h-[600px] object-contain mx-auto"
          loading="lazy"
        />
      </div>
      {alt && (
        <figcaption className="mt-3 text-center text-sm text-muted-foreground italic px-4">
          {alt}
        </figcaption>
      )}
    </figure>
  );
}
