export const queryKeys = {
  marketing: {
    all: ["marketing"] as const,
    page: (page: "landing" | "login") => [...queryKeys.marketing.all, page] as const,
  },
  projects: {
    all: ["projects"] as const,
    list: () => [...queryKeys.projects.all, "list"] as const,
    detail: (id: string) => [...queryKeys.projects.all, "detail", id] as const,
    prompts: (id: string) => [...queryKeys.projects.all, "detail", id, "prompts"] as const,
  },
};
