import { queryOptions, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { 
  Project, 
  ProjectPrompt,
  getMyProjects,
  getProjectById,
  createProject as apiCreateProject,
  updateProject,
  deleteProject as apiDeleteProject,
  removeCourseFromProject,
  listProjectPrompts,
  saveProjectPrompt,
  markPromptUsed,
  deleteProjectPrompt
} from "../../services/projectApi";
import { queryKeys } from "./keys";

export const projectsQueryOptions = () =>
  queryOptions({
    queryKey: queryKeys.projects.list(),
    queryFn: () => getMyProjects(),
    staleTime: 30_000,
  });

export const projectQueryOptions = (id: string) =>
  queryOptions({
    queryKey: queryKeys.projects.detail(id),
    queryFn: () => getProjectById(id),
    staleTime: 60_000,
  });

export const promptsQueryOptions = (id: string) =>
  queryOptions({
    queryKey: queryKeys.projects.prompts(id),
    queryFn: () => listProjectPrompts(id),
    staleTime: 15_000,
  });

export function useCreateProject() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: apiCreateProject,
    onMutate: async (vars) => {
      await qc.cancelQueries({ queryKey: queryKeys.projects.list() });
      const prev = qc.getQueryData<Project[]>(queryKeys.projects.list());
      const optimistic: Project = {
        id: `tmp-${Date.now()}`,
        name: vars.name,
        description: vars.description ?? undefined,
        creatorId: "self",
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        courses: [],
      };
      qc.setQueryData<Project[]>(queryKeys.projects.list(), (old = []) => [optimistic, ...old]);
      return { prev };
    },
    onError: (err, _vars, ctx) => {
      qc.setQueryData(queryKeys.projects.list(), ctx?.prev);
      toast.error(err instanceof Error ? err.message : "Couldn't create project.");
    },
    onSuccess: () => toast.success("Project created."),
    onSettled: () => qc.invalidateQueries({ queryKey: queryKeys.projects.list() }),
  });
}

export function useUpdateProject(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { name?: string; description?: string }) => updateProject(id, body),
    onMutate: async (body) => {
      await qc.cancelQueries({ queryKey: queryKeys.projects.detail(id) });
      const prevDetail = qc.getQueryData<Project>(queryKeys.projects.detail(id));
      const prevList = qc.getQueryData<Project[]>(queryKeys.projects.list());
      if (prevDetail) {
        qc.setQueryData<Project>(queryKeys.projects.detail(id), { ...prevDetail, ...body, updatedAt: new Date().toISOString() });
      }
      if (prevList) {
        qc.setQueryData<Project[]>(
          queryKeys.projects.list(),
          prevList.map((p) => (p.id === id ? { ...p, ...body, updatedAt: new Date().toISOString() } : p)),
        );
      }
      return { prevDetail, prevList };
    },
    onError: (err, _vars, ctx) => {
      qc.setQueryData(queryKeys.projects.detail(id), ctx?.prevDetail);
      qc.setQueryData(queryKeys.projects.list(), ctx?.prevList);
      toast.error(err instanceof Error ? err.message : "Couldn't update project.");
    },
    onSettled: () => {
      qc.invalidateQueries({ queryKey: queryKeys.projects.list() });
      qc.invalidateQueries({ queryKey: queryKeys.projects.detail(id) });
    },
  });
}

export function useDeleteProject() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, deleteCourses }: { id: string; deleteCourses: boolean }) =>
      apiDeleteProject(id, deleteCourses),
    onMutate: async ({ id }) => {
      await qc.cancelQueries({ queryKey: queryKeys.projects.list() });
      const prev = qc.getQueryData<Project[]>(queryKeys.projects.list());
      qc.setQueryData<Project[]>(queryKeys.projects.list(), (old = []) => old.filter((p) => p.id !== id));
      qc.removeQueries({ queryKey: queryKeys.projects.detail(id) });
      return { prev };
    },
    onError: (err, _vars, ctx) => {
      qc.setQueryData(queryKeys.projects.list(), ctx?.prev);
      toast.error(err instanceof Error ? err.message : "Couldn't delete project.");
    },
    onSuccess: () => toast.success("Project deleted."),
    onSettled: () => qc.invalidateQueries({ queryKey: queryKeys.projects.list() }),
  });
}

export function useUnlinkCourse(projectId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (courseId: string) => removeCourseFromProject(projectId, courseId),
    onMutate: async (courseId) => {
      await qc.cancelQueries({ queryKey: queryKeys.projects.detail(projectId) });
      const prev = qc.getQueryData<Project>(queryKeys.projects.detail(projectId));
      if (prev) {
        qc.setQueryData<Project>(queryKeys.projects.detail(projectId), {
          ...prev,
          courses: prev.courses.filter((c) => c.id !== courseId),
        });
      }
      return { prev };
    },
    onError: (err, _vars, ctx) => {
      qc.setQueryData(queryKeys.projects.detail(projectId), ctx?.prev);
      toast.error("Couldn't unlink course.");
    },
    onSettled: () => {
      qc.invalidateQueries({ queryKey: queryKeys.projects.detail(projectId) });
      qc.invalidateQueries({ queryKey: queryKeys.projects.list() });
    },
  });
}

export function useCreatePrompt(projectId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { text: string; relatedCourseId?: string; relatedCourseTitle?: string }) =>
      saveProjectPrompt(projectId, body),
    onMutate: async (body) => {
      await qc.cancelQueries({ queryKey: queryKeys.projects.prompts(projectId) });
      const prev = qc.getQueryData<ProjectPrompt[]>(queryKeys.projects.prompts(projectId));
      const optimistic: ProjectPrompt = {
        id: `tmp-${Date.now()}`,
        projectId,
        text: body.text,
        relatedCourseId: body.relatedCourseId,
        relatedCourseTitle: body.relatedCourseTitle,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        lastUsedAt: new Date().toISOString(),
      };
      qc.setQueryData<ProjectPrompt[]>(queryKeys.projects.prompts(projectId), (old = []) => [optimistic, ...old]);
      return { prev };
    },
    onError: (_err, _vars, ctx) => {
      qc.setQueryData(queryKeys.projects.prompts(projectId), ctx?.prev);
      toast.error("Couldn't save prompt.");
    },
    onSettled: () => qc.invalidateQueries({ queryKey: queryKeys.projects.prompts(projectId) }),
  });
}

export function useUsePrompt(projectId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (promptId: string) => markPromptUsed(projectId, promptId),
    onMutate: async (promptId) => {
      await qc.cancelQueries({ queryKey: queryKeys.projects.prompts(projectId) });
      const prev = qc.getQueryData<ProjectPrompt[]>(queryKeys.projects.prompts(projectId));
      if (prev) {
        const target = prev.find((p) => p.id === promptId);
        const rest = prev.filter((p) => p.id !== promptId);
        if (target) {
          qc.setQueryData<ProjectPrompt[]>(queryKeys.projects.prompts(projectId), [
            { ...target, lastUsedAt: new Date().toISOString() },
            ...rest,
          ]);
        }
      }
      return { prev };
    },
    onError: (_err, _vars, ctx) => qc.setQueryData(queryKeys.projects.prompts(projectId), ctx?.prev),
    onSettled: () => qc.invalidateQueries({ queryKey: queryKeys.projects.prompts(projectId) }),
  });
}

export function useDeletePrompt(projectId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (promptId: string) => deleteProjectPrompt(projectId, promptId),
    onMutate: async (promptId) => {
      await qc.cancelQueries({ queryKey: queryKeys.projects.prompts(projectId) });
      const prev = qc.getQueryData<ProjectPrompt[]>(queryKeys.projects.prompts(projectId));
      qc.setQueryData<ProjectPrompt[]>(queryKeys.projects.prompts(projectId), (old = []) =>
        old.filter((p) => p.id !== promptId),
      );
      return { prev };
    },
    onError: (_err, _vars, ctx) => qc.setQueryData(queryKeys.projects.prompts(projectId), ctx?.prev),
    onSettled: () => qc.invalidateQueries({ queryKey: queryKeys.projects.prompts(projectId) }),
  });
}
