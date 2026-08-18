import { useEffect, useRef, useState } from "react";
import { LayoutGrid, List, Search, SlidersHorizontal } from "lucide-react";
import { Input } from "@/components/ui/input";
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";

export type Sort = "updated" | "created" | "name" | "courses";
export type View = "grid" | "table";
export type Filter = "all" | "has" | "empty" | "recent";

interface Props {
  q: string;
  sort: Sort;
  view: View;
  filter: Filter;
  onChange(patch: Partial<{ q: string; sort: Sort; view: View; filter: Filter }>): void;
}

const sortLabel: Record<Sort, string> = {
  updated: "Recently updated",
  created: "Newly created",
  name: "Name (A→Z)",
  courses: "Most courses",
};

const filters: { value: Filter; label: string }[] = [
  { value: "all", label: "All" },
  { value: "has", label: "Has courses" },
  { value: "empty", label: "Empty" },
  { value: "recent", label: "Updated this week" },
];

export function ProjectsToolbar({ q, sort, view, filter, onChange }: Props) {
  const [local, setLocal] = useState(q);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const t = window.setTimeout(() => {
      if (local !== q) onChange({ q: local });
    }, 300);
    return () => window.clearTimeout(t);
  }, [local, q, onChange]);

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        inputRef.current?.focus();
      }
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, []);

  return (
    <div className="sticky top-0 z-10 -mx-6 border-b border-border bg-background/80 px-6 py-3 backdrop-blur mb-6">
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-[16rem]">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            ref={inputRef}
            value={local}
            onChange={(e) => setLocal(e.target.value)}
            placeholder="Search projects… (⌘K)"
            aria-label="Search projects"
            className="pl-9 bg-card"
          />
        </div>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="outline" size="sm" className="gap-2 bg-card">
              <SlidersHorizontal className="h-4 w-4" />
              {sortLabel[sort]}
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuLabel>Sort by</DropdownMenuLabel>
            {(Object.keys(sortLabel) as Sort[]).map((s) => (
              <DropdownMenuCheckboxItem
                key={s}
                checked={sort === s}
                onCheckedChange={() => onChange({ sort: s })}
              >
                {sortLabel[s]}
              </DropdownMenuCheckboxItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>

        <ToggleGroup
          type="single"
          value={view}
          onValueChange={(v) => v && onChange({ view: v as View })}
          aria-label="View mode"
          className="bg-card border border-border rounded-md px-1 py-0.5"
        >
          <ToggleGroupItem value="grid" aria-label="Grid view" className="h-8 px-2 data-[state=on]:bg-secondary">
            <LayoutGrid className="h-4 w-4" />
          </ToggleGroupItem>
          <ToggleGroupItem value="table" aria-label="Table view" className="h-8 px-2 data-[state=on]:bg-secondary">
            <List className="h-4 w-4" />
          </ToggleGroupItem>
        </ToggleGroup>
      </div>

      <div className="mt-4 flex flex-wrap gap-2">
        {filters.map((f) => (
          <button
            key={f.value}
            type="button"
            onClick={() => onChange({ filter: f.value })}
            className="outline-none focus-[visible]:ring-2 rounded-full"
          >
            <Badge
              variant={filter === f.value ? "default" : "secondary"}
              className="cursor-pointer px-3 py-1 font-medium bg-secondary"
            >
              {f.label}
            </Badge>
          </button>
        ))}
      </div>
    </div>
  );
}
