import { Link, useLocation, useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { Eye, EyeOff, Github, Mail, Sparkles } from "lucide-react";
import { AmbientBackground } from "../components/AmbientBackground";
import { Logo } from "../components/Logo";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { useAuth } from "../auth/AuthContext";
import { toast } from "sonner";
import { login as apiLogin } from "../services/authApi";
import { fallbackLoginContent } from "../data/marketingContent";
import { loginContentQueryOptions } from "../lib/queries/marketing";

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const auth = useAuth();
  const [showPassword, setShowPassword] = useState(false);
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const { data: content = fallbackLoginContent } = useQuery(loginContentQueryOptions());

  // SEO setup from MD
  useEffect(() => {
    document.title = content.metaTitle;
  }, [content.metaTitle]);

  const redirectTarget = (() => {
    const redirect = new URLSearchParams(location.search).get("redirect");
    if (!redirect || !redirect.startsWith("/") || redirect.startsWith("//")) {
      return "/dashboard";
    }
    return redirect;
  })();

  useEffect(() => {
    if (!auth.loading && auth.isAuthenticated) {
      navigate(redirectTarget, { replace: true });
    }
  }, [auth.loading, auth.isAuthenticated, navigate, redirectTarget]);

  // Force dark mode for login page
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

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const resp = await apiLogin({ username: identifier.trim(), password });
      const data = resp?.data ?? resp;

      if (data && data.token) {
        auth.login(data.token, data.user);
        toast.success("Welcome back!");
        navigate(redirectTarget, { replace: true });
      } else {
        throw new Error("Invalid response");
      }
    } catch (error) {
      toast.error("Invalid credentials. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="relative min-h-screen overflow-hidden bg-background text-foreground">
      {/* Fixed layered backdrop: aurora glow + grid */}
      <div aria-hidden className="pointer-events-none fixed inset-0 -z-10">
        <div className="absolute inset-0 bg-aurora opacity-50" />
        <div className="absolute inset-0 bg-grid opacity-30" />
      </div>

      <AmbientBackground showParticles particleDensity={0.00006} particleInfluence={110} />

      <div className="relative z-10 grid min-h-screen lg:grid-cols-2">
        {/* Left: brand panel */}
        <div className="hidden lg:flex flex-col justify-between p-10 xl:p-14">
          <Logo />

          <div className="max-w-lg space-y-8">
            <div className="inline-flex items-center gap-2 rounded-full glass px-3 py-1.5 text-xs">
              <Sparkles className="h-3.5 w-3.5 text-accent animate-pulse" />
              <span className="text-muted-foreground font-medium uppercase tracking-[0.05em] text-[10px]">{content.left.chip}</span>
            </div>
            <h1 className="font-display text-4xl xl:text-6xl font-bold leading-[1.05] tracking-tight text-white">
              {content.left.headlineTop} <br />
              <span className="bg-[linear-gradient(120deg,oklch(0.9_0.12_205),oklch(0.84_0.16_200),oklch(0.78_0.14_185))] bg-clip-text text-transparent">{content.left.headlineMiddle}</span> <br />
              <span className="bg-[linear-gradient(120deg,#8B5CF6,#EC4899)] bg-clip-text text-transparent italic inline-block">{content.left.headlineEmphasis}</span><span>, {content.left.headlineSuffix}</span>
            </h1>
            <p className="text-muted-foreground text-lg leading-relaxed max-w-md">
              {content.left.description}
            </p>

            <div className="grid grid-cols-3 gap-4 pt-4">
              {content.left.stats.map((stat) => (
                <Stat key={stat.label} value={stat.value} label={stat.label} />
              ))}
            </div>
          </div>

          <p className="text-xs text-muted-foreground font-medium opacity-60 italic">
            "{content.left.testimonial}"
          </p>
        </div>

        {/* Right: form */}
        <div className="flex items-center justify-center p-6 sm:p-10 relative">
          <div className="w-full max-w-md relative z-10">
            <div className="lg:hidden mb-12 flex justify-center sticky top-0">
              <Logo />
            </div>

            <div className="glass-strong rounded-[2.5rem] p-8 sm:p-12 shadow-2xl border border-white/5 relative overflow-hidden backdrop-blur-2xl">
              <div className="absolute -top-24 -right-24 w-48 h-48 bg-primary/10 blur-3xl rounded-full" />

              <div className="space-y-2 relative z-10">
                <h2 className="font-display text-3xl font-bold tracking-tight text-white">{content.right.title}</h2>
                <p className="text-sm text-muted-foreground font-medium">
                  {content.right.subtitle}
                </p>
              </div>

              {/* Social login moved below form */}




              <form onSubmit={onSubmit} className="space-y-5 relative z-10">
                <div className="space-y-2">
                  <Label htmlFor="identifier" className="text-[10px] uppercase font-bold tracking-widest text-muted-foreground pl-1">Email or User ID</Label>
                  <div className="relative">
                    <Mail className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-black/40" />
                    <Input
                      id="identifier"
                      type="text"
                      autoComplete="username"
                      required
                      value={identifier}
                      onChange={(e) => setIdentifier(e.target.value)}
                      placeholder="you@company.com or your_user_id"
                      className="h-12 pl-11 bg-white text-black border border-white/10 rounded-2xl focus-visible:ring-primary/20 transition-all font-medium"
                    />
                  </div>
                </div>
                <div className="space-y-2">
                  <div className="flex items-center justify-between pl-1 pr-1">
                    <Label htmlFor="password" className="text-[10px] uppercase font-bold tracking-widest text-muted-foreground">{content.right.passwordLabel}</Label>
                  </div>
                  <div className="relative">
                    <Input
                      id="password"
                      type={showPassword ? "text" : "password"}
                      autoComplete="current-password"
                      required
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="••••••••"
                      className="h-12 pr-12 bg-white text-black border border-white/10 rounded-2xl focus-visible:ring-primary/20 transition-all"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword((v) => !v)}
                      aria-label={showPassword ? "Hide password" : "Show password"}
                      className="absolute right-3 top-1/2 -translate-y-1/2 inline-flex h-9 w-9 items-center justify-center rounded-xl text-muted-foreground hover:bg-white/5 hover:text-white transition-all"
                    >
                      {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                  <div className="flex justify-start mt-2">
                    <a href="#" className="text-[10px] uppercase font-bold tracking-widest text-muted-foreground/60 hover:text-white transition-colors">
                      {content.right.forgot}
                    </a>
                  </div>
                </div>

                <Button
                  type="submit"
                  variant="hero"
                  size="lg"
                  className="w-full h-14 rounded-2xl font-bold uppercase tracking-widest text-xs mt-4 shadow-[0_4px_20px_-5px_rgba(168,85,247,0.4)]"
                  disabled={loading}
                >
                  {loading ? (
                    <div className="flex items-center gap-2">
                      <div className="h-4 w-4 animate-spin rounded-full border-2 border-primary-foreground border-t-transparent" />
                      {content.right.loading}
                    </div>
                  ) : content.right.submit}
                </Button>

                <div className="pt-4 space-y-6">
                  <div className="flex items-center gap-4 text-[10px] font-bold uppercase tracking-[0.2em] text-muted-foreground relative z-10">
                    <div className="h-px flex-1 bg-white/10" />
                    {content.right.divider}
                    <div className="h-px flex-1 bg-white/10" />
                  </div>

                  <div className="grid grid-cols-2 gap-4 relative z-10">
                    <Button 
                      variant="glass" 
                      type="button" 
                      className="h-12 border-white/10 hover:bg-white/5 transition-all font-bold text-xs uppercase tracking-widest"
                      onClick={() => window.location.href = "/oauth2/authorization/google"}
                    >
                      <GoogleIcon /> Google
                    </Button>
                    <Button 
                      variant="glass" 
                      type="button" 
                      className="h-12 border-white/10 hover:bg-white/5 transition-all font-bold text-xs uppercase tracking-widest"
                      onClick={() => window.location.href = "/oauth2/authorization/github"}
                    >
                      <Github className="h-4 w-4" /> GitHub
                    </Button>
                  </div>
                </div>
              </form>

              <p className="mt-10 text-center text-xs font-bold uppercase tracking-widest text-muted-foreground relative z-10">
                {content.right.newTo}{" "}
                <Link to="/register" className="text-white hover:text-primary transition-colors">
                  {content.right.create}
                </Link>
              </p>
            </div>

            <p className="mt-8 text-center text-[10px] font-bold uppercase tracking-widest text-muted-foreground/40">
              <Link to="/" className="hover:text-white transition-colors flex items-center justify-center gap-2">
                ← {content.right.back}
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

function Stat({ value, label }: { value: string; label: string }) {
  return (
    <div className="glass-strong rounded-2xl p-4 border-white/5 hover:border-white/10 transition-all group">
      <div className="font-display text-2xl font-bold text-white group-hover:text-primary transition-colors leading-none">{value}</div>
      <div className="text-[10px] text-muted-foreground font-bold uppercase tracking-widest mt-2 leading-none">{label}</div>
    </div>
  );
}

function GoogleIcon() {
  return (
    <svg viewBox="0 0 24 24" className="h-4 w-4 mr-2" aria-hidden="true">
      <path fill="#EA4335" d="M12 10.2v3.9h5.5c-.2 1.4-1.7 4.1-5.5 4.1-3.3 0-6-2.7-6-6.1s2.7-6.1 6-6.1c1.9 0 3.1.8 3.8 1.5l2.6-2.5C16.9 3.5 14.7 2.5 12 2.5 6.7 2.5 2.5 6.7 2.5 12s4.2 9.5 9.5 9.5c5.5 0 9.1-3.9 9.1-9.3 0-.6-.1-1.1-.2-1.6H12z" />
    </svg>
  );
}
