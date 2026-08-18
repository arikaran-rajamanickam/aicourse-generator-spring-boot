import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Save, AlertTriangle, Trash2 } from "lucide-react";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import { useUpdateProject } from "@/lib/queries/projects";
import type { Project } from "@/services/projectApi";
import { DeleteProjectDialog } from "./DeleteProjectDialog";
import { toast } from "sonner";

const schema = z.object({
  name: z.string().trim().min(3, "Name must be at least 3 characters.").max(80, "Max 80 characters."),
  description: z.string().trim().max(500, "Max 500 characters.").optional(),
});
type Values = z.infer<typeof schema>;

export function ProjectSettings({ project }: { project: Project }) {
  const update = useUpdateProject(project.id);
  const [deleteOpen, setDeleteOpen] = useState(false);

  const form = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: project.name,
      description: project.description || "",
    },
  });

  async function onSubmit(values: Values) {
    try {
      await update.mutateAsync(values);
      toast.success("Project updated.");
      form.reset(values);
    } catch (err: any) {
      if (err?.errorCode === "DUPLICATE_NAME") {
        form.setError("name", { message: "A project with this name already exists." });
      }
    }
  }

  const isDirty = form.formState.isDirty;

  return (
    <div className="space-y-8 max-w-3xl animate-fade-in">
      <section className="rounded-2xl border border-border bg-card p-6">
        <h2 className="text-xl font-bold mb-6 text-foreground">Project Details</h2>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-5">
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Project Name</FormLabel>
                  <FormControl>
                    <Input {...field} className="bg-background max-w-md" />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Description</FormLabel>
                  <FormControl>
                    <Textarea
                      {...field}
                      value={field.value || ""} // Ensure controlled input with "" not undefined
                      placeholder="Describe this project..."
                      className="bg-background max-w-md h-32"
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <div className="flex">
              <Button type="submit" disabled={!isDirty || update.isPending} className="gap-2">
                <Save className="h-4 w-4" /> 
                {update.isPending ? "Saving..." : "Save Changes"}
              </Button>
            </div>
          </form>
        </Form>
      </section>

      <section className="rounded-2xl border border-destructive/20 bg-destructive/5 p-6 relative overflow-hidden">
        <div className="absolute top-0 left-0 w-1 h-full bg-destructive/50" />
        <h2 className="text-xl font-bold mb-2 text-destructive flex items-center gap-2">
          <AlertTriangle className="h-5 w-5" /> Danger Zone
        </h2>
        <p className="text-sm text-muted-foreground mb-6">
          Permanently delete this project. You can choose to cascade this deletion to all linked courses.
        </p>
        <Button variant="destructive" onClick={() => setDeleteOpen(true)} className="gap-2">
          <Trash2 className="h-4 w-4" /> Delete Project
        </Button>
      </section>

      <DeleteProjectDialog
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        projectId={project.id}
        projectName={project.name}
        redirectAfter={true}
      />
    </div>
  );
}
