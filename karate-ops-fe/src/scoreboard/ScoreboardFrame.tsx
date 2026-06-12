// =============================================================================
// scoreboard/ScoreboardFrame.tsx — composes the chrome around a mode-specific
// board and dispatches kumite vs kata by match.mode.
// =============================================================================

import { cx } from "../utils";
import { C, type FrameProps } from "./contract";
import { BoardHeader, BoardFooter, WinnerOverlay } from "./BoardChrome";
import { KumiteBoard } from "./KumiteBoard";
import { KataBoard } from "./KataBoard";

export function ScoreboardFrame({ payload, variant }: FrameProps) {
  const { match } = payload;
  return (
    <section className={cx(C.frame, variant, match.mode)}>
      <BoardHeader match={match} />
      {match.mode === "kata" ? (
        <KataBoard payload={payload} variant={variant} />
      ) : (
        <KumiteBoard payload={payload} variant={variant} />
      )}
      <BoardFooter match={match} />
      <WinnerOverlay match={match} />
    </section>
  );
}
