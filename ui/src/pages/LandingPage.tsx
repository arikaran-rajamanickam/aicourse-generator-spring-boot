import { useEffect } from "react";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import {
  ArrowRight,
  Bot,
  CheckCircle2,
  FolderKanban,
  GraduationCap,
  LayoutGrid,
  LineChart,
  ShieldCheck,
  Share2,
  Sparkles,
  Star,
  Trophy,
  Wand2,
  Zap,
} from "lucide-react";
import { AmbientBackground } from "../components/AmbientBackground";
import { Header } from "../components/marketing/Header";
import { Footer } from "../components/marketing/Footer";
import { Button } from "../components/ui/button";
import { cn } from "../lib/utils";
import { landingContentQueryOptions } from "../lib/queries/marketing";
import { fallbackLandingContent } from "../data/marketingContent";
import { getGlobalLeaderboard } from "../services/leaderboardApi";

import heroImage from "@/assets/hero-mockup.jpg";

export default function LandingPage() {
  const { data: content = fallbackLandingContent } = useQuery(landingContentQueryOptions());

  // Force dark mode for landing page to maintain premium cosmic aesthetic
  useEffect(() => {
    const root = document.documentElement;
    const wasLight = root.classList.contains("light");
    root.classList.remove("light");
    root.classList.add("dark");
    return () => {
      if (wasLight) {
        root.classList.remove("dark");
        root.classList.add("light");
      }
    };
  }, []);

  const { data: leaderboardPreview = [] } = useQuery({
    queryKey: ["landing", "leaderboard-preview"],
    queryFn: async () => {
      const raw = await getGlobalLeaderboard(0, 3);
      const list = raw?.data?.content ?? raw?.content ?? raw?.data ?? (Array.isArray(raw) ? raw : []);
      return Array.isArray(list) ? list.slice(0, 3) : [];
    },
    staleTime: 2 * 60_000,
    retry: 0,
  });

  const leaderboardRows = leaderboardPreview.length
    ? leaderboardPreview.map((entry: any) => ({
      n: entry.displayName ?? entry.username ?? entry.handle ?? "Learner",
      v: `${(entry.totalPoints ?? entry.points ?? 0).toLocaleString()} XP`,
    }))
    : [
      { n: "Priya S.", v: "1,240 XP" },
      { n: "Marco D.", v: "1,180 XP" },
      { n: "Aiko T.", v: "1,055 XP" },
    ];

  return (
    <div className="relative min-h-screen bg-background text-foreground selection:bg-primary/30">
      <Header navLabels={content.header.nav} />

      <main>
        {/* ---------- HERO ---------- */}
        <section className="relative overflow-hidden pt-24 pb-24 md:pt-32 md:pb-40">
          <AmbientBackground showParticles particleInfluence={120} particleDensity={0.00007} />

          <div className="relative z-10 mx-auto max-w-7xl px-6 md:px-8">
            <div className="grid items-center gap-16 lg:grid-cols-12">
              <div className="lg:col-span-7 animate-fade-up">
                <div className="inline-flex items-center gap-2 rounded-full glass px-3.5 py-1.5 text-xs mb-8 border border-white/10 group cursor-default">
                  <span className="grid h-5 w-5 place-items-center rounded-full bg-gradient-cta shadow-[0_0_12px_rgba(168,85,247,0.5)]">
                    <Sparkles className="h-3 w-3 text-primary-foreground animate-pulse" />
                  </span>
                  <span className="text-white/80 font-medium uppercase tracking-[0.05em]">
                    {content.hero.badge}
                  </span>
                  <ArrowRight className="h-3.5 w-3.5 text-white/40 group-hover:translate-x-0.5 transition-transform" />
                </div>

                <h1 className="font-display text-5xl md:text-7xl lg:text-8xl font-bold leading-[1.02] tracking-tight">
                  {content.hero.titlePrefix}
                  <br />
                  <span className="text-gradient">{content.hero.titleHighlight}</span>
                </h1>

                <p className="mt-8 max-w-xl text-lg md:text-xl text-muted-foreground leading-relaxed font-medium">
                  {content.hero.description}
                </p>

                <div className="mt-10 flex flex-wrap items-center gap-4">
                  <Button asChild variant="hero" size="xl" className="group btn-shine">
                    <Link to="/login">
                      {content.hero.primaryCta} <ArrowRight className="h-4 w-4 ml-2 group-hover:translate-x-1 transition-transform" />
                    </Link>
                  </Button>
                  <Button asChild variant="glass" size="xl" className="border-white/10 hover:border-white/20">
                    <a href="#how">{content.hero.secondaryCta}</a>
                  </Button>
                </div>

                <div className="mt-10 flex flex-wrap items-center gap-x-8 gap-y-4 text-xs font-bold uppercase tracking-widest text-muted-foreground/60">
                  {content.hero.trust.map((item) => (
                    <span key={item} className="inline-flex items-center gap-2">
                      <CheckCircle2 className="h-4 w-4 text-accent" /> {item}
                    </span>
                  ))}
                </div>
              </div>

              <div className="lg:col-span-5 relative">
                <div className="relative animate-float-slow">
                  <div className="absolute -inset-10 rounded-[3rem] bg-primary/20 blur-[100px] opacity-40 mix-blend-screen" />
                  <div className="absolute -inset-10 rounded-[3rem] bg-accent/20 blur-[100px] opacity-30 mix-blend-screen translate-x-20" />

                  <div className="relative overflow-hidden rounded-[2rem] border border-white/10 bg-background/50 backdrop-blur-3xl shadow-2xl aspect-[4/5] lg:aspect-[4/3] flex items-center justify-center group">
                    <img
                      src={heroImage}
                      alt="AI CourseGen Dashboard"
                      className="relative z-10 w-full h-full object-cover transition-transform duration-700 group-hover:scale-105"
                    />
                    <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_50%_120%,oklch(0.68_0.22_295_/_30%),transparent_60%)]" />
                  </div>

                  <FloatingCard
                    className="absolute -left-8 top-10 hidden md:block"
                    icon={<Bot className="h-4 w-4 text-accent" />}
                    title="AI generated 8 modules"
                    subtitle="From a single prompt"
                  />
                  <FloatingCard
                    className="absolute -right-6 bottom-12 hidden md:block"
                    icon={<Trophy className="h-4 w-4 text-secondary-foreground" />}
                    title="92% completion"
                    subtitle="vs. 41% baseline"
                  />
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* ---------- SOCIAL PROOF ---------- */}
        <section className="relative border-y border-white/5 bg-background/40 py-12 overflow-hidden">
          <div className="mx-auto max-w-7xl px-6 md:px-8">
            <div className="grid items-center gap-12 md:grid-cols-[auto_1fr]">
              <p className="text-xs font-bold uppercase tracking-widest text-muted-foreground/60 leading-relaxed">
                Trusted by teams shipping<br className="hidden md:block" /> learning programs.
              </p>
              <div className="relative group overflow-hidden">
                <div className="flex w-max animate-marquee gap-16 pr-16 group-hover:[animation-play-state:paused]">
                  {[...content.logos, ...content.logos].map((l, i) => (
                    <span
                      key={i}
                      className="font-display text-2xl font-bold tracking-tighter text-white/20 hover:text-white/60 transition-all duration-300"
                    >
                      {l}
                    </span>
                  ))}
                </div>
                <div className="pointer-events-none absolute inset-y-0 left-0 w-32 bg-gradient-to-r from-background to-transparent" />
                <div className="pointer-events-none absolute inset-y-0 right-0 w-32 bg-gradient-to-l from-background to-transparent" />
              </div>
            </div>
          </div>
        </section>

        {/* ---------- FEATURES ---------- */}
        <section id="features" className="relative py-32 md:py-48">
          <div className="mx-auto max-w-7xl px-6 md:px-8">
            <Eyebrow>Platform</Eyebrow>
            <SectionTitle>
              Everything you need to <span className="text-gradient">create, learn, and ship</span> knowledge.
            </SectionTitle>
            <p className="mt-6 max-w-2xl text-lg md:text-xl text-muted-foreground leading-relaxed">
              A single workspace for prompt-driven course creation, structured learning, and team-grade
              collaboration.
            </p>

            <div className="mt-20 grid gap-4 md:grid-cols-2 lg:grid-cols-4">
              {FEATURE_ICONS.map((Icon, index) => {
                const feature = content.features[index];
                if (!feature) return null;
                return <FeatureCard key={feature.title} icon={Icon} title={feature.title} desc={feature.desc} />;
              })}
            </div>
          </div>
        </section>

        {/* ---------- HOW IT WORKS ---------- */}
        <section id="how" className="relative py-24 md:py-32">
          <div className="absolute inset-x-0 top-0 mx-auto h-px max-w-5xl divider-glow" />
          <div className="mx-auto max-w-7xl px-6 md:px-8">
            <Eyebrow>How it works</Eyebrow>
            <SectionTitle>
              From a blank page to a <span className="text-gradient">launch-ready course</span>.
            </SectionTitle>

            <div className="mt-14 grid gap-6 md:grid-cols-3">
              {content.steps.map((s, i) => (
                <div key={s.n} className="relative glass rounded-2xl p-7">
                  <div className="flex items-center gap-3">
                    <span className="font-display text-xs text-muted-foreground">STEP</span>
                    <span className="font-display text-3xl font-semibold text-gradient">{s.n}</span>
                  </div>
                  <h3 className="mt-5 font-display text-xl font-semibold tracking-tight text-white">{s.title}</h3>
                  <p className="mt-2 text-sm text-muted-foreground leading-relaxed">{s.desc}</p>
                  {i < content.steps.length - 1 && (
                    <ArrowRight className="absolute -right-3 top-1/2 hidden h-5 w-5 -translate-y-1/2 text-muted-foreground md:block" />
                  )}
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* ---------- SHOWCASE ---------- */}
        <section className="relative py-24 md:py-32">
          <div className="mx-auto max-w-7xl px-6 md:px-8">
            <div className="grid items-center gap-12 lg:grid-cols-2">

              {/* ---- Left: copy ---- */}
              <div>
                <Eyebrow>Inside the product</Eyebrow>
                <SectionTitle>
                  A workspace that <span className="text-gradient">feels alive</span>.
                </SectionTitle>
                <p className="mt-4 text-lg text-muted-foreground leading-relaxed">
                  Generate a course, browse modules, and chat with the AI coach — all in one fluid
                  workspace designed for focus and flow.
                </p>
                <ul className="mt-6 space-y-3">
                  {[
                    "Drag, reorder, regenerate any module instantly",
                    "Linked projects keep related courses in sync",
                    "Per-lesson AI coaching, scoped to context",
                    "Shareable links with view, edit, or admin roles",
                  ].map((t) => (
                    <li key={t} className="flex items-start gap-3 text-sm text-foreground/90">
                      <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-[oklch(0.84_0.16_200)]" />
                      {t}
                    </li>
                  ))}
                </ul>
                <div className="mt-8">
                  <Button asChild variant="neon" size="lg">
                    <Link to="/login">Try the workspace <ArrowRight className="h-4 w-4" /></Link>
                  </Button>
                </div>
              </div>

              {/* ---- Right: 6-col mock UI grid ---- */}
              <div className="relative">
                <div className="absolute -inset-6 rounded-[2rem] bg-gradient-cta opacity-25 blur-3xl" />
                <div className="relative grid grid-cols-6 gap-4">

                  {/* Course card — col-span-4 */}
                  <MockCard className="col-span-4">
                    <div className="flex items-center justify-between">
                      <div>
                        <div className="text-xs text-muted-foreground">Course</div>
                        <div className="font-display text-lg font-semibold">Intro to Vector DBs</div>
                      </div>
                      <span className="rounded-full bg-gradient-cta px-2.5 py-1 text-[10px] font-medium text-primary-foreground">
                        AI-generated
                      </span>
                    </div>
                    <div className="mt-4 space-y-2">
                      {["Foundations of embeddings", "Indexing strategies", "Hybrid search patterns"].map((m, i) => (
                        <div key={m} className="flex items-center gap-3 rounded-lg bg-white/[0.04] p-3 text-sm">
                          <span className="grid h-7 w-7 place-items-center rounded-md bg-gradient-cta text-[11px] font-semibold text-primary-foreground">
                            {i + 1}
                          </span>
                          <span className="flex-1">{m}</span>
                          <span className="text-xs text-muted-foreground">{6 + i} lessons</span>
                        </div>
                      ))}
                    </div>
                  </MockCard>

                  {/* AI Coach card — col-span-2 */}
                  <MockCard className="col-span-2">
                    <div className="text-xs text-muted-foreground">AI Coach</div>
                    <div className="mt-3 space-y-2.5 text-sm">
                      <div className="rounded-lg bg-white/[0.05] p-2.5">Explain HNSW like I'm a junior dev.</div>
                      <div className="rounded-lg bg-gradient-cta/30 p-2.5 text-foreground border border-white/10">
                        Think layered shortcuts on a graph — fast at the top, precise at the bottom.
                      </div>
                    </div>
                  </MockCard>

                  {/* Engagement card — col-span-3 */}
                  <MockCard className="col-span-3">
                    <div className="text-xs text-muted-foreground">Engagement</div>
                    <div className="mt-2 font-display text-3xl font-semibold">+38%</div>
                    <div className="text-xs text-[oklch(0.84_0.16_200)]">vs. last month</div>
                    <div className="mt-3 flex h-12 items-end gap-1">
                      {[30, 45, 38, 60, 52, 70, 84, 76, 88, 95].map((v, i) => (
                        <div
                          key={i}
                          className="w-full rounded-sm bg-gradient-to-t from-[oklch(0.72_0.21_285)] to-[oklch(0.78_0.18_200)]"
                          style={{ height: `${v}%` }}
                        />
                      ))}
                    </div>
                  </MockCard>

                  {/* Leaderboard card — col-span-3 */}
                  <MockCard className="col-span-3">
                    <div className="text-xs text-muted-foreground">Leaderboard</div>
                    <ul className="mt-3 space-y-2 text-sm">
                      {leaderboardRows.map((p, i) => (
                        <li key={p.n} className="flex items-center gap-3">
                          <span className="grid h-6 w-6 place-items-center rounded-full bg-white/[0.06] text-[11px]">
                            {i + 1}
                          </span>
                          <span className="flex-1">{p.n}</span>
                          <span className="text-xs text-muted-foreground">{p.v}</span>
                        </li>
                      ))}
                    </ul>
                  </MockCard>

                </div>
              </div>

            </div>
          </div>
        </section>


        {/* ---------- USE CASES ---------- */}
        <section id="use-cases" className="relative py-24 md:py-32">
          <div className="mx-auto max-w-7xl px-6 md:px-8">
            <Eyebrow>Use cases</Eyebrow>
            <SectionTitle>
              Built for the way <span className="text-gradient">modern teams learn</span>.
            </SectionTitle>

            <div className="mt-14 grid gap-4 md:grid-cols-2 lg:grid-cols-4">
              {USE_CASE_ICONS.map((Icon, index) => {
                const useCase = content.useCases[index];
                if (!useCase) return null;
                return <div key={useCase.title} className="glass rounded-2xl p-8 transition-all hover:-translate-y-0.5 hover:border-white/10 border border-white/5 bg-white/[0.02]">
                  <span className="inline-flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-cta shadow-glow">
                    <Icon className="h-6 w-6 text-white" />
                  </span>
                  <h3 className="mt-6 font-display text-xl font-bold tracking-tight text-white">{useCase.title}</h3>
                  <p className="mt-2 text-sm text-muted-foreground leading-relaxed font-medium">{useCase.desc}</p>
                </div>;
              })}
            </div>
          </div>
        </section>


        {/* ---------- TESTIMONIALS ---------- */}
        <section className="relative py-24 md:py-32">
          <div className="mx-auto max-w-7xl px-6 md:px-8">
            <Eyebrow>Outcomes</Eyebrow>
            <SectionTitle>
              Loved by teams who care about <span className="text-gradient">real results</span>.
            </SectionTitle>

            <div className="mt-16 grid gap-6 md:grid-cols-3">
              {content.quotes.map((q) => (
                <figure key={q.name} className="glass-strong rounded-[2.5rem] p-10 border border-white/5 relative group hover:border-white/15 transition-all">
                  <div className="flex gap-1 mb-6">
                    {Array.from({ length: 5 }).map((_, i) => (
                      <Star key={i} className="h-4 w-4 fill-accent text-accent animate-pulse" />
                    ))}
                  </div>
                  <blockquote className="text-lg leading-relaxed text-white font-medium italic">
                    “{q.quote}”
                  </blockquote>
                  <figcaption className="mt-10 flex items-center gap-4">
                    <div className="grid h-12 w-12 place-items-center rounded-2xl bg-gradient-cta text-sm font-bold text-white">
                      {q.name.charAt(0)}
                    </div>
                    <div>
                      <div className="text-sm font-bold text-white tracking-tight">{q.name}</div>
                      <div className="text-[10px] uppercase tracking-widest font-bold text-muted-foreground mt-1">{q.role}</div>
                    </div>
                  </figcaption>
                </figure>
              ))}
            </div>
          </div>
        </section>

        {/* ---------- FINAL CTA ---------- */}
        <section id="pricing" className="relative py-24 md:py-48">
          <div className="mx-auto max-w-6xl px-6 md:px-8">
            <div className="relative overflow-hidden rounded-[4rem] border border-white/10 p-12 md:p-24 bg-background/50 backdrop-blur-3xl shadow-glow">
              <div className="absolute inset-0 bg-aurora opacity-20" />
              <div className="absolute -top-48 -right-48 w-96 h-96 bg-primary/20 blur-[120px] rounded-full" />
              <div className="absolute -bottom-48 -left-48 w-96 h-96 bg-accent/20 blur-[120px] rounded-full" />

              <div className="relative text-center max-w-4xl mx-auto">
                <Eyebrow center>Get started</Eyebrow>
                <h2 className="mt-8 font-display text-5xl md:text-7xl font-bold leading-[1] tracking-tight text-white">
                  {content.finalCta.titlePrefix} <span className="text-gradient">{content.finalCta.titleHighlight}</span>
                </h2>
                <p className="mt-8 text-xl text-muted-foreground font-medium max-w-2xl mx-auto">
                  {content.finalCta.description}
                </p>
                <div className="mt-12 flex flex-wrap justify-center gap-6">
                  <Button asChild variant="hero" size="xl" className="h-16 px-10 rounded-2xl font-bold uppercase tracking-widest text-xs btn-shine">
                    <Link to="/login">
                      {content.finalCta.primary} <ArrowRight className="h-4 w-4 ml-2" />
                    </Link>
                  </Button>
                  <Button asChild variant="glass" size="xl" className="h-16 px-10 rounded-2xl font-bold uppercase tracking-widest text-xs bg-white/[0.03] border-white/10 hover:bg-white/10">
                    <a href="#features">{content.finalCta.secondary}</a>
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </section>
      </main>

      <Footer content={content.footer} />
    </div>
  );
}

function FloatingCard({
  className = "",
  icon,
  title,
  subtitle,
}: {
  className?: string;
  icon: React.ReactNode;
  title: string;
  subtitle: string;
}) {
  return (
    <div className={"glass-strong rounded-2xl px-5 py-4 shadow-glow border border-white/10 backdrop-blur-3xl animate-fade-in " + className}>
      <div className="flex items-center gap-4">
        <span className="grid h-10 w-10 place-items-center rounded-xl bg-white/[0.06] border border-white/10">{icon}</span>
        <div>
          <div className="text-sm font-bold text-white tracking-tight">{title}</div>
          <div className="text-[10px] uppercase tracking-widest font-bold text-muted-foreground mt-1">{subtitle}</div>
        </div>
      </div>
    </div>
  );
}

function MockCard({ children, className = "" }: { children: React.ReactNode; className?: string }) {
  return <div className={"glass-strong rounded-2xl p-5 shadow-soft " + className}>{children}</div>;
}

function Eyebrow({ children, center }: { children: React.ReactNode; center?: boolean }) {
  return (
    <div className={cn("flex mb-4", center ? "justify-center" : "justify-start")}>
      <span className="inline-flex items-center gap-2 rounded-full glass px-4 py-1.5 text-[10px] font-bold uppercase tracking-[0.2em] text-muted-foreground border border-white/10 shadow-[0_0_20px_rgba(34,211,238,0.1)]">
        <span className="h-2 w-2 rounded-full bg-accent shadow-[0_0_12px_rgba(34,211,238,0.8)] animate-pulse" />
        {children}
      </span>
    </div>
  );
}

function SectionTitle({ children }: { children: React.ReactNode }) {
  return (
    <h2 className="font-display text-4xl md:text-6xl font-bold tracking-tight leading-[1.1] text-white">
      {children}
    </h2>
  );
}

function FeatureCard({ icon: Icon, title, desc }: { icon: any, title: string, desc: string }) {
  return (
    <div className="group relative overflow-hidden rounded-2xl glass p-6 transition-all hover:-translate-y-0.5 hover:border-white/15">
      <div className="absolute -top-20 -right-20 h-40 w-40 rounded-full bg-[oklch(0.72_0.21_285/25%)] opacity-0 blur-3xl transition-opacity group-hover:opacity-100" />
      <div className="relative">
        <span className="inline-flex h-11 w-11 items-center justify-center rounded-xl bg-white/[0.05] ring-1 ring-white/10">
          <Icon className="h-5 w-5 text-[oklch(0.84_0.16_200)]" />
        </span>
        <h3 className="mt-5 font-display text-lg font-semibold tracking-tight">{title}</h3>
        <p className="mt-1.5 text-sm text-muted-foreground leading-relaxed">{desc}</p>
      </div>
    </div>
  );
}

const FEATURE_ICONS = [Wand2, FolderKanban, LayoutGrid, Bot, Share2, LineChart, ShieldCheck, Zap] as const;

const USE_CASE_ICONS = [GraduationCap, Bot, Sparkles, Share2] as const;
