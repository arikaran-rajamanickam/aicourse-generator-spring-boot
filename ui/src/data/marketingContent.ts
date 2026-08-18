export interface LandingContent {
  header: {
    nav: string[];
  };
  hero: {
    badge: string;
    titlePrefix: string;
    titleHighlight: string;
    description: string;
    primaryCta: string;
    secondaryCta: string;
    trust: string[];
  };
  logos: string[];
  features: Array<{ title: string; desc: string }>;
  steps: Array<{ n: string; title: string; desc: string }>;
  showcaseBullets: string[];
  useCases: Array<{ title: string; desc: string }>;
  quotes: Array<{ quote: string; name: string; role: string }>;
  finalCta: {
    titlePrefix: string;
    titleHighlight: string;
    description: string;
    primary: string;
    secondary: string;
  };
  footer: {
    tagline: string;
    product: string[];
    company: string[];
    legal: string[];
    social: string[];
  };
}

export interface LoginContent {
  metaTitle: string;
  left: {
    chip: string;
    headlineTop: string;
    headlineMiddle: string;
    headlineEmphasis: string;
    headlineSuffix: string;
    description: string;
    testimonial: string;
    stats: Array<{ value: string; label: string }>;
  };
  right: {
    title: string;
    subtitle: string;
    divider: string;
    emailLabel: string;
    passwordLabel: string;
    forgot: string;
    submit: string;
    loading: string;
    newTo: string;
    create: string;
    back: string;
  };
}

export const fallbackLandingContent: LandingContent = {
  header: {
    nav: ["Product", "How it works", "Use cases", "Pricing"],
  },
  hero: {
    badge: "Now with multi-modal lesson generation",
    titlePrefix: "Turn a prompt into a",
    titleHighlight: "complete course.",
    description:
      "AI CourseGen drafts modules, lessons, and assessments from a single idea - then helps your team learn, share, and ship knowledge faster.",
    primaryCta: "Start building free",
    secondaryCta: "See how it works",
    trust: ["No credit card", "SOC 2 ready", "Generate in <2 min"],
  },
  logos: ["NORTHWIND", "HELIX", "LUMEN", "STRATIFY", "COBALT", "VERTEX", "ORBITAL", "ATLAS"],
  features: [
    {
      title: "AI course generation",
      desc: "From a single prompt to a full course with modules, lessons, and assessments - in under two minutes.",
    },
    {
      title: "Project organization",
      desc: "Group related goals, prompts, and courses into projects so knowledge compounds over time.",
    },
    {
      title: "Structured content",
      desc: "Beautifully laid out lessons, ready to consume - or refine with one click using powerful AI tools.",
    },
    {
      title: "Contextual AI coach",
      desc: "Always-on tutor that explains concepts, generates examples, and answers questions in context.",
    },
    {
      title: "Team collaboration",
      desc: "Invite teammates, share links, and co-author courses with granular roles and permissions.",
    },
    {
      title: "Rich analytics",
      desc: "Track engagement, completion, and standout learners with real-time, per-course leaderboards.",
    },
    {
      title: "Enterprise ready",
      desc: "SSO-friendly roles, audit logs, and granular permissions built for scaling serious teams.",
    },
    {
      title: "Prompt factory",
      desc: "Save winning prompts as templates and turn them into repeatable, high-output course factories.",
    },
  ],
  steps: [
    {
      n: "01",
      title: "Drop in a topic",
      desc: "Type any subject, paste a brief, or pick from your reusable prompt library.",
    },
    {
      n: "02",
      title: "AI builds structure",
      desc: "Modules, lessons, and assessments - fully generated and ready to refine.",
    },
    {
      n: "03",
      title: "Share & track",
      desc: "Invite learners and follow live progress and engagement with ease.",
    },
  ],
  showcaseBullets: [
    "Drag, reorder, regenerate any module instantly",
    "Linked projects keep related courses in sync",
    "Per-lesson AI coaching, scoped to context",
    "Shareable links with view, edit, or admin roles",
  ],
  useCases: [
    {
      title: "Interview prep",
      desc: "Generate focused study tracks for system design, DSA, behaviorals, and role-specific loops.",
    },
    {
      title: "Team onboarding",
      desc: "Spin up structured ramp-up courses for new hires - refreshed with one click.",
    },
    {
      title: "Personal learning",
      desc: "Turn curiosity into a curriculum. Learn anything, structured by AI, paced by you.",
    },
    {
      title: "Training content",
      desc: "Build branded courses for customers, partners, and certifications at scale.",
    },
  ],
  quotes: [
    {
      quote: "We replaced three weeks of curriculum design with an afternoon. Genuinely usable.",
      name: "Priya Shankar",
      role: "Head of L&D, Helix",
    },
    {
      quote: "Onboarding ramp-time dropped 40%. New hires actually finish the courses now.",
      name: "Marco Devereux",
      role: "VP Engineering, Stratify",
    },
    {
      quote: "The AI coach is the unlock. It is like every learner has a senior tutor for free.",
      name: "Aiko Tanaka",
      role: "Founder, Lumen Academy",
    },
  ],
  finalCta: {
    titlePrefix: "The fastest way to ship a",
    titleHighlight: "world-class course.",
    description:
      "Start free today. No credit card required. Generate your first course with the power of modern AI in under two minutes.",
    primary: "Create first course",
    secondary: "Explore features",
  },
  footer: {
    tagline:
      "The AI-first platform for generating, editing, and deploying world-class learning experiences in minutes, not months.",
    product: ["Features", "Integrations", "Pricing", "Changelog"],
    company: ["About", "Blog", "Careers", "Contact"],
    legal: ["Privacy", "Terms", "Security"],
    social: ["Twitter", "GitHub", "Discord"],
  },
};

export const fallbackLoginContent: LoginContent = {
  metaTitle: "Sign in - AI CourseGen",
  left: {
    chip: "AI-native learning platform",
    headlineTop: "Build structured",
    headlineMiddle: "courses in",
    headlineEmphasis: "minutes",
    headlineSuffix: "not weeks.",
    description:
      "Turn any prompt into modules, lessons, and a learning experience that actually ships - with an AI coach beside every learner.",
    testimonial:
      "The fastest way I have ever assembled an onboarding curriculum. - Head of Learning, Series B SaaS",
    stats: [
      { value: "12k+", label: "Courses created" },
      { value: "92%", label: "Completion lift" },
      { value: "<2 min", label: "Avg. generation" },
    ],
  },
  right: {
    title: "Welcome back",
    subtitle: "Sign in to continue building with AI CourseGen.",
    divider: "or sign in with email",
    emailLabel: "Email address",
    passwordLabel: "Password",
    forgot: "Forgot password?",
    submit: "Sign in to Dashboard",
    loading: "Signing in...",
    newTo: "New to AI CourseGen?",
    create: "Create an account",
    back: "Back to home",
  },
};

