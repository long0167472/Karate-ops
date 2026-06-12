import { Check } from "lucide-react";

const DEFAULT_STEPS = [
  "Khởi tạo",
  "Duyệt CLB",
  "Duyệt VDV",
  "Tạo sigma",
  "Đang thi đấu",
];

interface TournamentStepIndicatorProps {
  currentStep: number;
  steps?: string[];
}

export function TournamentStepIndicator({
  currentStep,
  steps = DEFAULT_STEPS,
}: TournamentStepIndicatorProps) {
  return (
    <div style={{ width: "100%" }}>
      <div className="step-indicator">
        {steps.map((label, index) => {
          const isDone = index < currentStep;
          const isActive = index === currentStep;

          return (
            <div key={index} className="step-item">
              <div style={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
                <div
                  className={`step-circle ${
                    isDone
                      ? "step-circle--done"
                      : isActive
                      ? "step-circle--active"
                      : "step-circle--inactive"
                  }`}
                >
                  {isDone ? <Check size={14} strokeWidth={2.5} /> : index + 1}
                </div>
                <span className="step-label">{label}</span>
              </div>
              {index < steps.length - 1 && (
                <div
                  className="step-line"
                  style={{
                    background: index < currentStep ? "var(--ao)" : "var(--line)",
                    marginBottom: "1.1rem",
                  }}
                />
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
