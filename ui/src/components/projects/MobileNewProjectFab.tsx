import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";

export function MobileNewProjectFab({ onClick }: { onClick: () => void }) {
  return (
    <Button
      size="icon"
      className="md:hidden fixed bottom-6 right-6 h-14 w-14 rounded-full shadow-lg z-50 hover:shadow-xl transition-all"
      onClick={onClick}
    >
      <Plus className="h-6 w-6" />
    </Button>
  );
}
