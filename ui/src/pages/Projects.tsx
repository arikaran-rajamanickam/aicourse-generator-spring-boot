import { useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Button } from "../components/ui/button";
import { projectsQueryOptions } from "../lib/queries/projects";
import { ProjectsHeader } from "../components/projects/ProjectsHeader";
import { ProjectsToolbar, type Sort, type View, type Filter } from "../components/projects/ProjectsToolbar";
import { ProjectsGrid } from "../components/projects/ProjectsGrid";
import { ProjectsTable } from "../components/projects/ProjectsTable";
import { EmptyProjects } from "../components/projects/EmptyProjects";
import { CreateProjectDialog } from "../components/projects/CreateProjectDialog";
import { MobileNewProjectFab } from "../components/projects/MobileNewProjectFab";
import { FolderGit2 } from "lucide-react";

export default function Projects() {
  const [searchParams, setSearchParams] = useSearchParams();
  const q = searchParams.get("q") ?? "";
  const sort = (searchParams.get("sort") as Sort) ?? "updated";
  const view = (searchParams.get("view") as View) ?? "grid";
  const filter = (searchParams.get("filter") as Filter) ?? "all";

  const { data: projects = [], isLoading, isError, refetch } = useQuery(projectsQueryOptions());
  const [createOpen, setCreateOpen] = useState(false);

  const filtered = useMemo(() => {
    const term = q.trim().toLowerCase();
    let list = projects.filter((p) => {
      const matchesTerm =
        !term ||
        p.name.toLowerCase().includes(term) ||
        (p.description ?? "").toLowerCase().includes(term);
      if (!matchesTerm) return false;
      if (filter === "has") return p.courses.length > 0;
      if (filter === "empty") return p.courses.length === 0;
      if (filter === "recent") {
        const week = 7 * 24 * 3600 * 1000;
        return Date.now() - new Date(p.updatedAt).getTime() <= week;
      }
      return true;
    });
    list = [...list].sort((a, b) => {
      if (sort === "name") return a.name.localeCompare(b.name);
      if (sort === "courses") return b.courses.length - a.courses.length;
      if (sort === "created") return +(a.createdAt ? new Date(a.createdAt) : 0) - +(b.createdAt ? new Date(b.createdAt) : 0);
      return +(b.updatedAt ? new Date(b.updatedAt) : 0) - +(a.updatedAt ? new Date(a.updatedAt) : 0);
    });
    return list;
  }, [projects, q, sort, filter]);

  const stats = {
    totalProjects: projects.length,
    totalCourses: projects.reduce((acc, p) => acc + p.courses.length, 0),
    updatedThisWeek: projects.filter(
      (p) => Date.now() - new Date(p.updatedAt).getTime() <= 7 * 24 * 3600 * 1000,
    ).length,
  };

  const handleParamsChange = (patch: Partial<{ q: string; sort: Sort; view: View; filter: Filter }>) => {
    const next = new URLSearchParams(searchParams);
    Object.entries(patch).forEach(([key, value]) => {
      if (value) next.set(key, value);
      else next.delete(key);
    });
    setSearchParams(next, { replace: true });
  };

  if (isLoading) {
    return (
      <main className="mx-auto max-w-7xl px-6 py-8 md:px-8 lg:px-10">
        <div className="animate-pulse space-y-6">
          <div className="h-24 bg-card rounded-2xl w-full" />
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mt-6">
            {[1, 2, 3, 4, 5, 6, 7, 8].map((i) => (
              <div key={i} className="h-44 bg-card rounded-xl" />
            ))}
          </div>
        </div>
      </main>
    );
  }

  if (isError) {
    return (
      <main className="mx-auto max-w-7xl px-6 py-8 md:px-8 lg:px-10 flex flex-col items-center justify-center min-h-[50vh]">
        <FolderGit2 className="h-10 w-10 text-destructive mb-4" />
        <h2 className="text-xl font-bold">Failed to load projects</h2>
        <Button variant="outline" className="mt-4" onClick={() => refetch()}>Try Again</Button>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-7xl px-8 py-10 animate-fade-in">
      <ProjectsHeader {...stats} onCreate={() => setCreateOpen(true)} />

      <div className="mt-6">
        <ProjectsToolbar
          q={q}
          sort={sort}
          view={view}
          filter={filter}
          onChange={handleParamsChange}
        />
      </div>

      <div className="mt-6">
        {projects.length === 0 ? (
          <EmptyProjects onCreate={() => setCreateOpen(true)} />
        ) : filtered.length === 0 ? (
          <div className="rounded-lg border border-border bg-card p-10 text-center">
            <p className="text-sm text-muted-foreground">
              Nothing matches "<span className="text-foreground">{q}</span>".
            </p>
            <Button
              variant="ghost"
              className="mt-3"
              onClick={() => handleParamsChange({ q: "", sort: "updated", view, filter: "all" })}
            >
              Clear filters
            </Button>
          </div>
        ) : view === "grid" ? (
          <ProjectsGrid projects={filtered} />
        ) : (
          <ProjectsTable projects={filtered} />
        )}
      </div>

      <CreateProjectDialog open={createOpen} onOpenChange={setCreateOpen} />
      <MobileNewProjectFab onClick={() => setCreateOpen(true)} />
    </main>
  );
}
