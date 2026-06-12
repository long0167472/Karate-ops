// =============================================================================
// scoreboard/contract.ts — SHARED CONTRACT for the tatami spectator display
// -----------------------------------------------------------------------------
// Every build agent in the parallel redesign imports from THIS file. It is the
// single source of truth for prop shapes, the ticking clock, the board variant,
// and the CSS class-name map. Components never hard-code class strings — they
// reference them through `C`, and the CSS agent implements EXACTLY those strings.
//
// Module layout the build agents will create:
//
//   src/scoreboard/
//     contract.ts        - this file (shared types, useNow, class-name map C)
//     scoreboard.css     - all visual styling (CSS agent)
//     BoardChrome.tsx    - BoardHeader, BoardFooter, WinnerOverlay, ConnectionPill
//     KumiteBoard.tsx    - kumite spectator board (+ its own sub-components)
//     KataBoard.tsx      - kata spectator board (+ its own sub-components)
//     ScoreboardFrame.tsx- dispatches kumite vs kata by match.mode
//     DisplayView.tsx    - full-screen spectator page
//     OverlayView.tsx    - OBS lower-third overlay
//     index.ts           - barrel exports
// =============================================================================

import { useEffect, useState } from "react";

// -----------------------------------------------------------------------------
// 1) Re-export shared domain types for convenience. Build agents may import
//    these straight from "./contract" instead of reaching back into "../types".
// -----------------------------------------------------------------------------
export type { Side, Mode, MatchState, Competitor, StatePayload } from "../types";

// Local imports for use in the prop interfaces below.
import type { StatePayload, MatchState, Competitor, Side } from "../types";

// -----------------------------------------------------------------------------
// 2) useNow — the shared ticking clock.
//    Holds the current epoch-millisecond timestamp in state, updates it every
//    `intervalMs` via window.setInterval, and clears the interval on unmount.
//    All live-remaining-time math derives from this single hook so the whole
//    board ticks in lockstep:
//      kumite:  liveRemaining(match.timer,          payload.receivedAt, now)
//      kata:    liveRemaining(match.kata.countdown, payload.receivedAt, now)
// -----------------------------------------------------------------------------
export function useNow(intervalMs: number): number {
  const [now, setNow] = useState<number>(() => Date.now());

  useEffect(() => {
    const id = window.setInterval(() => setNow(Date.now()), intervalMs);
    return () => window.clearInterval(id);
  }, [intervalMs]);

  return now;
}

// -----------------------------------------------------------------------------
// 3) Board variant. "display" = full-screen spectator/projector rendering;
//    "preview" = a scaled-down embed (operator console preview pane).
// -----------------------------------------------------------------------------
export type BoardVariant = "display" | "preview";

// -----------------------------------------------------------------------------
// 4) Prop interfaces for every component the build agents will create.
// -----------------------------------------------------------------------------

/** KumiteBoard, KataBoard — a full mode-specific board. */
export interface BoardProps {
  payload: StatePayload;
  variant: BoardVariant;
}

/** ScoreboardFrame — dispatches to KumiteBoard/KataBoard by match.mode. */
export interface FrameProps {
  payload: StatePayload;
  variant: BoardVariant;
}

/** DisplayView — full-screen spectator page. */
export interface DisplayViewProps {
  payload: StatePayload;
}

/** OverlayView — OBS lower-third overlay. */
export interface OverlayViewProps {
  payload: StatePayload;
}

/** BoardHeader — category / tatami / round chrome at the top of the board. */
export interface BoardHeaderProps {
  match: MatchState;
}

/** BoardFooter — status / next-match chrome at the bottom of the board. */
export interface BoardFooterProps {
  match: MatchState;
}

/** WinnerOverlay — celebratory overlay shown when match.winner is set. */
export interface WinnerOverlayProps {
  match: MatchState;
}

// -----------------------------------------------------------------------------
// 5) Frozen CSS class-name map. ALL components reference classes through `C`.
//    The CSS agent implements EXACTLY these strings (sb- prefix; state modifiers
//    use the is- convention so they compose on top of base classes).
// -----------------------------------------------------------------------------
export const C = {
  // --- Page shells -----------------------------------------------------------
  displayPage: "sb-display-page", // full-screen spectator page root (DisplayView)
  overlayPage: "sb-overlay-page", // OBS lower-third root (OverlayView)

  // --- Frame / board scaffolding ---------------------------------------------
  frame: "sb-frame", // ScoreboardFrame outer wrapper (carries variant modifier)
  boardMain: "sb-board-main", // main board region between header and footer
  kumiteLayout: "sb-kumite-layout", // KumiteBoard grid (aka | center | ao)
  kataLayout: "sb-kata-layout", // KataBoard grid (performer + judges + tally)

  // --- Header ----------------------------------------------------------------
  header: "sb-header", // BoardHeader container
  headerCategory: "sb-header-category", // category name
  headerTatami: "sb-header-tatami", // tatami label
  headerRound: "sb-header-round", // round + match number

  // --- Footer ----------------------------------------------------------------
  footer: "sb-footer", // BoardFooter container
  nextMatch: "sb-next-match", // "Next: …" line

  // --- Competitor side (kumite) ----------------------------------------------
  side: "sb-side", // one competitor column (add aka/ao color modifier)
  sideBadge: "sb-side-badge", // AKA / AO color badge
  scoreStage: "sb-score-stage", // wrapper around the big score number (for pops)
  scoreValue: "sb-score-value", // the big numeric score
  pointPop: "sb-point-pop", // transient last-point label animation
  senshuBadge: "sb-senshu", // SENSHU (first-point advantage) marker
  athleteName: "sb-athlete-name", // competitor name
  athleteMeta: "sb-athlete-meta", // club / bib line
  penaltyLadder: "sb-penalty-ladder", // penalty row container
  penaltyCell: "sb-penalty-cell", // single penalty step (chui / HC / H / etc.)
  penaltyActive: "is-active", // modifier: penalty step earned
  penaltyDanger: "is-danger", // modifier: terminal penalty (hansoku/shikkaku)

  // --- Center column (kumite) ------------------------------------------------
  center: "sb-center", // center stack: status + timer + tags
  statusChip: "sb-status-chip", // match status pill
  timer: "sb-timer", // big match clock
  timerWarning: "is-warning", // modifier: clock in low-time/warning state
  centerCaption: "sb-center-caption", // caption under the clock (e.g. HANTEI)
  centerTags: "sb-center-tags", // row of small contextual chips
  smallChip: "sb-small-chip", // generic small chip (VR, suggestion, etc.)

  // --- Kata -------------------------------------------------------------------
  kataPerformer: "sb-kata-performer", // active performer panel
  kataName: "sb-kata-name", // kata name being performed
  kataPhase: "sb-kata-phase", // phase label (perform / reveal / result)
  judgeFlags: "sb-judge-flags", // row of judge flag indicators
  judgeFlag: "sb-judge-flag", // single judge flag (add aka/ao modifier)
  kataTally: "sb-kata-tally", // aka vs ao vote tally panel
  majorityChip: "sb-majority-chip", // majority / needed-votes chip

  // --- Winner overlay --------------------------------------------------------
  winnerOverlay: "sb-winner-overlay", // full-board winner overlay backdrop
  winnerCard: "sb-winner-card", // winner announcement card

  // --- OBS lower-third overlay ----------------------------------------------
  overlayStrip: "sb-overlay-strip", // the lower-third strip container
  overlaySide: "sb-overlay-side", // one competitor block in the strip
  overlayCenter: "sb-overlay-center", // center block of the strip
  overlayTimer: "sb-overlay-timer", // compact timer in the strip
  overlayMeta: "sb-overlay-meta", // category / round meta line in the strip
} as const;

// Convenience: the type of the class-name map (e.g. keyof typeof C for lookups).
export type ClassMap = typeof C;
