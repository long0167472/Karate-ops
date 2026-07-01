// =============================================================================
// scoreboard/BoardChrome.tsx — shared chrome for the tatami spectator board.
// Header, footer, winner overlay, and a connection pill. Every visual class is
// referenced through the contract's class-name map `C`.
// =============================================================================

import { Trophy, Wifi, WifiOff } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

import { cx, sideLabel } from "../utils";
import type { MatchState, Side } from "../types";
import {
  C,
  type BoardHeaderProps,
  type BoardFooterProps,
  type WinnerOverlayProps,
} from "./contract";

// -----------------------------------------------------------------------------
// Header — category (left) · tatami pill (center) · round/match (right).
// -----------------------------------------------------------------------------
export function BoardHeader({ match }: BoardHeaderProps) {
  return (
    <header className={C.header}>
      <div className={C.headerCategory} title={match.category}>
        {match.category}
      </div>
      <div className={C.headerTatami}>{match.tatami}</div>
      <div className={C.headerRound}>
        {match.round} / {match.matchNo}
      </div>
    </header>
  );
}

// -----------------------------------------------------------------------------
// Penalty summary — compact per-side terminal-state digest used in the footer.
// -----------------------------------------------------------------------------
function penaltySummary(match: MatchState, side: Side): string {
  const p = match.competitors[side].penalties;
  const parts: string[] = [];
  if (p.penaltyLevel !== "NONE") parts.push(p.penaltyLevel);
  if (p.reasonCode) parts.push(p.reasonCode);
  if (p.hansokuChui) parts.push("HC");
  if (p.hansoku) parts.push("HANSOKU");
  if (p.shikkaku) parts.push("SHIKKAKU");
  if (p.kiken) parts.push("KIKEN");
  return parts.length ? parts.join(" · ") : "—";
}

// -----------------------------------------------------------------------------
// Footer — penalty summary per side (kumite only) + center next-match ticker.
// -----------------------------------------------------------------------------
export function BoardFooter({ match }: BoardFooterProps) {
  const showPenalties = match.mode !== "kata";
  return (
    <footer className={C.footer}>
      {showPenalties ? (
        <span className={cx(C.sideBadge, "aka")}>
          {sideLabel("aka")} {penaltySummary(match, "aka")}
        </span>
      ) : (
        <span />
      )}

      <span className={C.nextMatch}>Next: {match.nextMatch || "—"}</span>

      {showPenalties ? (
        <span className={cx(C.sideBadge, "ao")}>
          {sideLabel("ao")} {penaltySummary(match, "ao")}
        </span>
      ) : (
        <span />
      )}
    </footer>
  );
}

// -----------------------------------------------------------------------------
// Winner overlay — celebratory dim + tinted card when match.winner is set.
// -----------------------------------------------------------------------------
export function WinnerOverlay({ match }: WinnerOverlayProps) {
  const winner = match.winner;
  return (
    <AnimatePresence>
      {winner && (
        <motion.div
          key="winner-overlay"
          className={C.winnerOverlay}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.25 }}
        >
          <motion.div
            className={cx(C.winnerCard, winner)}
            initial={{ scale: 0.7, y: 24, opacity: 0 }}
            animate={{ scale: 1, y: 0, opacity: 1 }}
            exit={{ scale: 0.85, opacity: 0 }}
            transition={{ type: "spring", stiffness: 320, damping: 24 }}
          >
            <Trophy size={48} strokeWidth={1.75} />
            <div className="sb-winner-title">Winner</div>
            <div className="sb-winner-name">
              {sideLabel(winner)} / {match.competitors[winner].name}
            </div>
            {match.winType && (
              <div className="sb-winner-type">{match.winType}</div>
            )}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

// -----------------------------------------------------------------------------
// ConnectionPill — small online/offline indicator with latency.
// -----------------------------------------------------------------------------
export function ConnectionPill({
  connected,
  latency,
}: {
  connected: boolean;
  latency?: number | null;
}) {
  return (
    <span className={cx(C.smallChip, connected ? "is-online" : "is-offline")}>
      {connected ? <Wifi size={14} /> : <WifiOff size={14} />}
      {connected
        ? `Online${latency != null ? ` /${latency}ms` : ""}`
        : "Offline"}
    </span>
  );
}
