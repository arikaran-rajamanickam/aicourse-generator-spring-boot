import { Link } from "react-router-dom";

export function Logo({ className = "" }: { className?: string }) {
  return (
    <Link to="/" className={"inline-flex items-center gap-2.5 group " + className}>
      <span className="relative grid h-9 w-9 place-items-center rounded-xl bg-gradient-cta shadow-glow">
        <span className="absolute inset-0 rounded-xl bg-gradient-cta blur-md opacity-60 group-hover:opacity-90 transition-opacity" />
        <svg viewBox="0 0 24 24" className="relative h-5 w-5 text-primary-foreground"
          fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M4 7l8-4 8 4-8 4-8-4z" />
          <path d="M4 12l8 4 8-4" />
          <path d="M4 17l8 4 8-4" />
        </svg>
      </span>
      <span className="font-display text-lg font-semibold tracking-tight">
        AI<span className="text-gradient"> CourseGen</span>
      </span>
    </Link>
  );
}