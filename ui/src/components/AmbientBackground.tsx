import { ParticleField } from "./ParticleField";

interface AmbientBackgroundProps {
  showGrid?: boolean;
  showParticles?: boolean;
  particleDensity?: number;
  particleInfluence?: number;
  className?: string;
}

export function AmbientBackground({
  showGrid = true,
  showParticles = false,
  particleDensity,
  particleInfluence,
  className,
}: AmbientBackgroundProps) {
  return (
    <div aria-hidden="true"
      className={"pointer-events-none absolute inset-0 overflow-hidden " + (className ?? "")}>
      {/* base aurora gradient (violet + cyan + pink radials) */}
      <div className="absolute inset-0 bg-aurora opacity-90" />

      {/* Glowing blobs:
          - top-left   = VIOLET (primary)
          - top-right  = CYAN/BLUE
          - bottom     = PINK */}
      <div className="absolute -top-32 -left-24 h-[520px] w-[520px] rounded-full bg-[oklch(0.72_0.21_285_/_28%)] blur-3xl animate-pulse-glow" />
      <div className="absolute -top-20 right-[-10%] h-[460px] w-[460px] rounded-full bg-[oklch(0.78_0.18_200_/_22%)] blur-3xl animate-pulse-glow"
        style={{ animationDelay: "1.5s" }} />
      <div className="absolute bottom-[-20%] left-1/3 h-[520px] w-[520px] rounded-full bg-[oklch(0.74_0.22_350_/_18%)] blur-3xl animate-pulse-glow"
        style={{ animationDelay: "3s" }} />

      {showGrid && <div className="absolute inset-0 bg-grid opacity-60" />}
      {showParticles && <ParticleField density={particleDensity} influence={particleInfluence} />}

      {/* Noise + vignette */}
      <div className="absolute inset-0 bg-noise opacity-[0.5] mix-blend-overlay" />
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,transparent_50%,oklch(0.08_0.02_270/_70%)_100%)]" />
    </div>
  );
}