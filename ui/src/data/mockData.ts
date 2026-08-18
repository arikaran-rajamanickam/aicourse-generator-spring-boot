export type Lesson = {
  id: string;
  title: string;
  completed: boolean;
};

export type Module = {
  id: string;
  number: number;
  title: string;
  lessons: Lesson[];
};

export type Course = {
  id: string;
  title: string;
  description: string;
  modules: Module[];
  totalLessons: number;
  completedLessons: number;
};

export type Project = {
  id: string;
  name: string;
  description?: string;
  updatedAt: string;
};

const courseSeed: Array<Omit<Course, "totalLessons" | "completedLessons">> = [
  {
    id: "course-react",
    title: "React Fundamentals",
    description: "Build modern interfaces with components, hooks, and routing.",
    modules: [
      {
        id: "m1",
        number: 1,
        title: "React Basics",
        lessons: [
          { id: "l1", title: "What is React?", completed: true },
          { id: "l2", title: "JSX and Components", completed: true },
          { id: "l3", title: "Props and State", completed: false },
        ],
      },
      {
        id: "m2",
        number: 2,
        title: "State Management",
        lessons: [
          { id: "l4", title: "useState and useEffect", completed: false },
          { id: "l5", title: "Lifting State Up", completed: false },
        ],
      },
    ],
  },
  {
    id: "course-spring",
    title: "Spring Boot API Design",
    description: "Create secure and scalable APIs with Spring Boot.",
    modules: [
      {
        id: "m3",
        number: 1,
        title: "Core Setup",
        lessons: [
          { id: "l6", title: "Project Structure", completed: true },
          { id: "l7", title: "Controllers and DTOs", completed: false },
        ],
      },
      {
        id: "m4",
        number: 2,
        title: "Persistence",
        lessons: [
          { id: "l8", title: "JPA Entities", completed: false },
          { id: "l9", title: "Repositories", completed: false },
        ],
      },
    ],
  },
];

export const courses: Course[] = courseSeed.map((course) => {
  const totalLessons = course.modules.reduce((sum, module) => sum + module.lessons.length, 0);
  const completedLessons = course.modules.reduce(
    (sum, module) => sum + module.lessons.filter((lesson) => lesson.completed).length,
    0,
  );

  return {
    ...course,
    totalLessons,
    completedLessons,
  };
});

export const projects: Project[] = [
  {
    id: "project-1",
    name: "Interview Prep",
    description: "Generate focused Java + React prep courses.",
    updatedAt: "2 hours ago",
  },
  {
    id: "project-2",
    name: "Team Onboarding",
    description: "Create quick onboarding tracks for new developers.",
    updatedAt: "Yesterday",
  },
  {
    id: "project-3",
    name: "Data Engineering Path",
    description: "Design step-by-step lessons for analytics engineers.",
    updatedAt: "3 days ago",
  },
];

export const lessonContent = `**Welcome to the lesson**

In this lesson you will learn key concepts in a practical, easy-to-follow way.

**What you will cover**

- Why this topic matters in real projects
- Common mistakes and how to avoid them
- A repeatable workflow you can use immediately

Practice by applying each concept to a small feature in your own project.`;

