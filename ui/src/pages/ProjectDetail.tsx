import { useQuery } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { ChevronRight, Settings } from "lucide-react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../components/ui/tabs";
import { projectQueryOptions, promptsQueryOptions } from "../lib/queries/projects";
import { PromptComposer } from "../components/projects/PromptComposer";
import { SavedPromptsList } from "../components/projects/SavedPromptsList";
import { LinkedCoursesList } from "../components/projects/LinkedCoursesList";
import { ProjectSettings } from "../components/projects/ProjectSettings";
import { Badge } from "../components/ui/badge";

export default function ProjectDetail() {
  const { projectId } = useParams<{ projectId: string }>();
  if (!projectId) throw new Error("projectId param is required");

  const projectQ = useQuery(projectQueryOptions(projectId));
  const promptsQ = useQuery(promptsQueryOptions(projectId));

  if (projectQ.isLoading) {
    return (
      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="animate-pulse space-y-8">
          <div className="h-12 w-1/3 bg-card rounded" />
          <div className="h-96 w-full bg-card rounded-2xl" />
        </div>
      </main>
    );
  }

  const project = projectQ.data;
  if (!project) {
    return (
      <main className="mx-auto max-w-7xl px-4 py-16 text-center">
        <h2 className="text-2xl font-bold">Project not found</h2>
        <Link to="/projects" className="text-primary hover:underline mt-4 inline-block">
          &larr; Back to projects
        </Link>
      </main>
    );
  }

  const prompts = promptsQ.data ?? [];
  const courses = project.courses ?? [];

  return (
    <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8 animate-fade-in">
      <nav aria-label="Breadcrumb" className="mb-6 flex items-center gap-2 text-sm text-muted-foreground">
        <Link to="/projects" className="hover:text-foreground transition-colors">
          Projects
        </Link>
        <ChevronRight className="h-4 w-4" />
        <span className="font-medium text-foreground">{project.name}</span>
      </nav>

      <div className="mb-8">
        <h1 className="text-display text-4xl font-extrabold tracking-tight text-foreground sm:text-5xl">
          {project.name}
        </h1>
        {project.description && (
          <p className="mt-3 text-lg text-muted-foreground max-w-2xl">{project.description}</p>
        )}
      </div>

      <Tabs defaultValue="overview" className="space-y-8">
        <div className="border-b border-border">
          <TabsList className="bg-transparent h-auto p-0 flex gap-6 justify-start">
            <TabsTrigger
              value="overview"
              className="rounded-none border-b-2 border-transparent px-2 py-4 font-medium data-[state=active]:border-primary data-[state=active]:bg-transparent data-[state=active]:text-primary data-[state=active]:shadow-none!"
            >
              Overview & Courses
              <Badge variant="secondary" className="ml-2 rounded-full px-2 py-0.5 text-xs bg-secondary/50">
                {courses.length}
              </Badge>
            </TabsTrigger>
            <TabsTrigger
              value="composer"
              className="rounded-none border-b-2 border-transparent px-2 py-4 font-medium data-[state=active]:border-primary data-[state=active]:bg-transparent data-[state=active]:text-primary data-[state=active]:shadow-none!"
            >
              Prompt Engine
            </TabsTrigger>
            <TabsTrigger
              value="settings"
              className="rounded-none border-b-2 border-transparent px-2 py-4 font-medium data-[state=active]:border-primary data-[state=active]:bg-transparent data-[state=active]:text-primary data-[state=active]:shadow-none!"
            >
              <Settings className="mr-2 h-4 w-4" />
              Settings
            </TabsTrigger>
          </TabsList>
        </div>

        <TabsContent value="overview" className="mt-6 focus-visible:outline-none focus-visible:ring-2 ring-ring rounded-lg">
          <section>
            <div className="flex items-center justify-between">
              <h2 className="text-xl font-bold text-foreground">Linked Courses</h2>
            </div>
            <LinkedCoursesList projectId={project.id} courses={courses} />
          </section>
        </TabsContent>

        <TabsContent value="composer" className="mt-6 space-y-10 focus-visible:outline-none focus-visible:ring-2 ring-ring rounded-lg">
          <PromptComposer projectId={project.id} courses={courses} />

          <section>
            <div className="flex items-center justify-between">
              <h3 className="text-xl font-bold text-foreground">Prompt History</h3>
              <Badge variant="outline" className="font-normal">{prompts.length} Saved</Badge>
            </div>
            <SavedPromptsList projectId={project.id} prompts={prompts} />
          </section>
        </TabsContent>

        <TabsContent value="settings" className="mt-6 focus-visible:outline-none focus-visible:ring-2 ring-ring rounded-lg">
          <ProjectSettings project={project} />
        </TabsContent>
      </Tabs>
    </main>
  );
}
