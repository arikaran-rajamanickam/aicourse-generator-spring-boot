import { useEffect, useRef } from "react";

type Particle = {
  x: number; y: number; vx: number; vy: number;
  baseX: number; baseY: number;
  r: number; hue: number; alpha: number;
};

interface ParticleFieldProps {
  density?: number;
  className?: string;
  influence?: number;
}

export function ParticleField({ density = 0.00009, className, influence = 140 }: ParticleFieldProps) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const rafRef = useRef<number | null>(null);
  const runningRef = useRef(true);
  const pointerTickRef = useRef(0);
  const pointer = useRef({ x: -9999, y: -9999, active: false });

  useEffect(() => {
    runningRef.current = true;
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d", { alpha: true });
    if (!ctx) return;

    const reduceMotion =
      typeof window !== "undefined" &&
      window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;

    const dpr = Math.min(window.devicePixelRatio || 1, 1.5);
    let width = 0, height = 0;
    let particles: Particle[] = [];
    let bgGradient: CanvasGradient | null = null;
    let lastFrameTs = 0;
    const targetFrameMs = reduceMotion ? 50 : 33;
    const linkDist = 110;
    const linkStep = reduceMotion ? 4 : 2;
    const palette = [265, 285, 305, 200, 230, 330]; // violet → cyan → pink hues

    const init = () => {
      const rect = canvas.getBoundingClientRect();
      width = rect.width; height = rect.height;
      canvas.width = Math.floor(width * dpr);
      canvas.height = Math.floor(height * dpr);
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      const adjustedDensity = reduceMotion ? density * 0.55 : density;
      const count = Math.max(18, Math.min(reduceMotion ? 64 : 96, Math.floor(width * height * adjustedDensity)));
      bgGradient = ctx.createRadialGradient(
        width * 0.5,
        height * 0.2,
        0,
        width * 0.5,
        height * 0.2,
        Math.max(width, height) * 0.7
      );
      bgGradient.addColorStop(0, "rgba(140, 110, 255, 0.08)");
      bgGradient.addColorStop(1, "rgba(0, 0, 0, 0)");
      particles = Array.from({ length: count }, () => {
        const x = Math.random() * width;
        const y = Math.random() * height;
        return {
          x, y, baseX: x, baseY: y,
          vx: (Math.random() - 0.5) * 0.18,
          vy: (Math.random() - 0.5) * 0.18,
          r: Math.random() * 1.6 + 0.6,
          hue: palette[Math.floor(Math.random() * palette.length)],
          alpha: Math.random() * 0.5 + 0.35,
        };
      });
    };

    const onResize = () => init();
    const onMove = (e: PointerEvent) => {
      const now = performance.now();
      if (now - pointerTickRef.current < 24) {
        return;
      }
      pointerTickRef.current = now;
      const rect = canvas.getBoundingClientRect();
      pointer.current.x = e.clientX - rect.left;
      pointer.current.y = e.clientY - rect.top;
      pointer.current.active = true;
    };
    const onLeave = () => {
      pointer.current.active = false;
      pointer.current.x = -9999; pointer.current.y = -9999;
    };

    init();
    const onVisibilityChange = () => {
      runningRef.current = document.visibilityState === "visible";
      if (runningRef.current && rafRef.current === null) {
        rafRef.current = requestAnimationFrame(draw);
      }
    };

    window.addEventListener("resize", onResize);
    window.addEventListener("pointermove", onMove, { passive: true });
    window.addEventListener("pointerdown", onMove, { passive: true });
    window.addEventListener("pointerleave", onLeave);
    document.addEventListener("visibilitychange", onVisibilityChange);

    const draw = (ts: number) => {
      if (ts - lastFrameTs < targetFrameMs) {
        rafRef.current = requestAnimationFrame(draw);
        return;
      }
      lastFrameTs = ts;

      ctx.clearRect(0, 0, width, height);

      if (!runningRef.current) {
        rafRef.current = null;
        return;
      }

      if (bgGradient) {
        ctx.fillStyle = bgGradient;
      }
      ctx.fillRect(0, 0, width, height);

      const px = pointer.current.x;
      const py = pointer.current.y;
      const active = pointer.current.active;

      for (let i = 0; i < particles.length; i++) {
        const p = particles[i];
        p.x += p.vx; p.y += p.vy;
        p.vx += (p.baseX - p.x) * 0.0008;
        p.vy += (p.baseY - p.y) * 0.0008;

        if (active) {
          const dx = p.x - px, dy = p.y - py;
          const dist2 = dx * dx + dy * dy;
          const inf2 = influence * influence;
          if (dist2 < inf2) {
            const dist = Math.sqrt(dist2) || 1;
            const force = (1 - dist / influence) * 1.6;
            p.vx += (dx / dist) * force * 0.15;
            p.vy += (dy / dist) * force * 0.15;
          }
        }
        p.vx *= 0.96; p.vy *= 0.96;

        if (p.x < -20) p.x = width + 20;
        if (p.x > width + 20) p.x = -20;
        if (p.y < -20) p.y = height + 20;
        if (p.y > height + 20) p.y = -20;

        ctx.beginPath();
        ctx.fillStyle = `hsla(${p.hue}, 90%, 70%, ${p.alpha})`;
        ctx.shadowColor = `hsla(${p.hue}, 95%, 65%, 0.9)`;
        ctx.shadowBlur = 12;
        ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
        ctx.fill();
      }
      ctx.shadowBlur = 0;

      const drawLinks = width >= 768;
      if (drawLinks) {
        for (let i = 0; i < particles.length; i++) {
          const a = particles[i];
          for (let j = i + 1; j < particles.length; j += linkStep) {
          const b = particles[j];
          const dx = a.x - b.x, dy = a.y - b.y;
          const d2 = dx * dx + dy * dy;
          if (d2 < linkDist * linkDist) {
            const t = 1 - Math.sqrt(d2) / linkDist;
            ctx.strokeStyle = `hsla(${(a.hue + b.hue) / 2}, 90%, 70%, ${t * 0.18})`;
            ctx.lineWidth = 0.6;
            ctx.beginPath();
            ctx.moveTo(a.x, a.y);
            ctx.lineTo(b.x, b.y);
              ctx.stroke();
            }
          }
        }
      }
      rafRef.current = requestAnimationFrame(draw);
    };

    rafRef.current = requestAnimationFrame(draw);

    return () => {
      if (rafRef.current) cancelAnimationFrame(rafRef.current);
      runningRef.current = false;
      window.removeEventListener("resize", onResize);
      window.removeEventListener("pointermove", onMove);
      window.removeEventListener("pointerdown", onMove);
      window.removeEventListener("pointerleave", onLeave);
      document.removeEventListener("visibilitychange", onVisibilityChange);
    };
  }, [density, influence]);

  return (
    <canvas ref={canvasRef} aria-hidden="true"
      className={"pointer-events-none absolute inset-0 h-full w-full " + (className ?? "")} />
  );
}