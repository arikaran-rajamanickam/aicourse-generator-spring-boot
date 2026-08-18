import { useCourseBuilder } from "@/context/CourseBuilderContext";
import { Check } from "lucide-react";

const STEPS = [
  "Metadata",
  "Structure",
  "Assessments",
  "Preview"
];

export function WizardStepper() {
  const { state } = useCourseBuilder();

  return (
    <div className="w-full py-4 border-b">
      <div className="max-w-4xl mx-auto flex items-center justify-between px-4">
        {STEPS.map((step, index) => {
          const isCompleted = state.currentStep > index;
          const isCurrent = state.currentStep === index;
          
          return (
            <div key={step} className="flex items-center">
              <div className="flex flex-col items-center gap-2">
                <div 
                  className={`w-8 h-8 rounded-full flex items-center justify-center transition-colors
                    ${isCompleted ? 'bg-primary text-primary-foreground' : 
                      isCurrent ? 'bg-primary/20 text-primary border-2 border-primary' : 
                      'bg-muted text-muted-foreground'}`
                  }
                >
                  {isCompleted ? <Check className="w-4 h-4" /> : index + 1}
                </div>
                <span className={`text-xs font-medium ${isCurrent ? 'text-primary' : 'text-muted-foreground'}`}>
                  {step}
                </span>
              </div>
              {index < STEPS.length - 1 && (
                <div className={`h-[2px] w-12 sm:w-24 md:w-32 mx-2 ${isCompleted ? 'bg-primary' : 'bg-muted'}`} />
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
