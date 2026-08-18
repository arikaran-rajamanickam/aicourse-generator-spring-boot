import { useState, useEffect } from "react";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "./ui/dialog";
import { Button } from "./ui/button";
import { Sparkles, BookOpen, Trophy, Bot, ArrowRight, Check } from "lucide-react";
import { cn } from "../lib/utils";

interface OnboardingTutorialProps {
  open: boolean;
  onComplete: () => void;
  userName?: string;
}

const steps = [
  {
    id: "welcome",
    title: "Welcome to AI CourseGen",
    description: "The fastest way to build, learn, and grow. Let's take a quick tour of what you can do here.",
    icon: Sparkles,
    color: "text-primary",
    bg: "bg-primary/10",
  },
  {
    id: "create",
    title: "Generate Courses instantly",
    description: "Use our advanced AI to turn any topic into a structured, full-length course with lessons and quizzes.",
    icon: BookOpen,
    color: "text-emerald-500",
    bg: "bg-emerald-500/10",
  },
  {
    id: "learn",
    title: "Learn & Earn",
    description: "Complete lessons, ace quizzes, build your streak, and climb the global leaderboard.",
    icon: Trophy,
    color: "text-amber-500",
    bg: "bg-amber-500/10",
  },
  {
    id: "coach",
    title: "Meet your AI Coach",
    description: "Got stuck? Your personal AI coach is available 24/7 to help you understand complex topics.",
    icon: Bot,
    color: "text-blue-500",
    bg: "bg-blue-500/10",
  },
];

export function OnboardingTutorial({ open, onComplete, userName }: OnboardingTutorialProps) {
  const [step, setStep] = useState(0);
  const [isClosing, setIsClosing] = useState(false);

  // Reset step if opened again
  useEffect(() => {
    if (open) {
      setStep(0);
      setIsClosing(false);
    }
  }, [open]);

  const handleNext = () => {
    if (step < steps.length - 1) {
      setStep((s) => s + 1);
    } else {
      setIsClosing(true);
      setTimeout(() => {
        onComplete();
      }, 300); // Wait for transition
    }
  };

  const currentStep = steps[step];
  const Icon = currentStep.icon;

  return (
    <Dialog open={open && !isClosing} onOpenChange={(val) => {
      // Prevent closing by clicking outside during the tutorial to ensure they see it,
      // but if they really want to close it, they can use Esc (handled by radix by default)
      if (!val) {
        setIsClosing(true);
        setTimeout(() => onComplete(), 300);
      }
    }}>
      <DialogContent className="sm:max-w-[480px] p-0 overflow-hidden border-border/50 bg-background/95 backdrop-blur-xl shadow-2xl glass-card rounded-3xl">
        <div className="relative p-8">
          {/* Background glow based on current step color */}
          <div className="absolute top-0 right-0 w-48 h-48 bg-primary/10 blur-3xl rounded-full opacity-50" />
          
          {/* Progress Dots */}
          <div className="flex justify-center gap-2 mb-8">
            {steps.map((_, i) => (
              <div 
                key={i} 
                className={cn(
                  "h-1.5 rounded-full transition-all duration-300",
                  i === step ? "w-6 bg-primary" : "w-1.5 bg-muted-foreground/30"
                )} 
              />
            ))}
          </div>

          <div className="flex flex-col items-center text-center space-y-6 relative z-10 animate-fade-in" key={step}>
            <div className={cn("h-20 w-20 rounded-2xl flex items-center justify-center border border-border/50 shadow-inner", currentStep.bg)}>
              <Icon className={cn("h-10 w-10", currentStep.color)} />
            </div>
            
            <div className="space-y-2">
              <DialogTitle className="font-display text-2xl font-bold text-foreground">
                {step === 0 && userName ? `Welcome, ${userName}` : currentStep.title}
              </DialogTitle>
              <DialogDescription className="text-base text-muted-foreground leading-relaxed max-w-[280px] mx-auto">
                {currentStep.description}
              </DialogDescription>
            </div>
          </div>

          <div className="mt-10 flex justify-between items-center relative z-10">
            <Button 
              variant="ghost" 
              onClick={() => {
                setIsClosing(true);
                setTimeout(() => onComplete(), 300);
              }}
              className="text-muted-foreground hover:text-foreground text-xs font-bold uppercase tracking-widest"
            >
              Skip tour
            </Button>
            
            <Button 
              onClick={handleNext}
              variant="hero"
              className="gap-2 px-6 shadow-glow"
            >
              {step === steps.length - 1 ? (
                <>Get Started <Check className="h-4 w-4" /></>
              ) : (
                <>Next <ArrowRight className="h-4 w-4" /></>
              )}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
