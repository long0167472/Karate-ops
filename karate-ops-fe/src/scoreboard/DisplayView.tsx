// =============================================================================
// scoreboard/DisplayView.tsx — full-screen spectator/projector page.
// =============================================================================

import { C, type DisplayViewProps } from "./contract";
import { ScoreboardFrame } from "./ScoreboardFrame";

export function DisplayView({ payload }: DisplayViewProps) {
  return (
    <main className={C.displayPage}>
      <ScoreboardFrame payload={payload} variant="display" />
    </main>
  );
}
