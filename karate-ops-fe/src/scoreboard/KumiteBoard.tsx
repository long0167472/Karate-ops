// =============================================================================
// scoreboard/KumiteBoard.tsx — KUMITE spectator board (inner content only).
// The parent ScoreboardFrame provides the frame wrapper, header, footer and
// winner overlay. This file renders: AKA side | CENTER | AO side.
// =============================================================================

import { motion, AnimatePresence } from "framer-motion";
import { Crown, Flag, AlertTriangle, Sparkles } from "lucide-react";

import { sideLabel, cx, formatMs, liveRemaining, winnerText } from "../utils";
import type { Side, MatchState, Competitor } from "../types";
import { useNow, C, type BoardVariant, type BoardProps } from "./contract";

type Point = MatchState["kumite"]["lastPoint"];

// -----------------------------------------------------------------------------
// KumitePenalties — WKF penalty ladder: C1 C2 C3 -> HC -> DQ lamp.
// -----------------------------------------------------------------------------
function KumitePenalties({
  side,
  penalties,
}: {
  side: Side;
  penalties: Competitor["penalties"];
}) {
  const dq = penalties.hansoku || penalties.shikkaku || penalties.kiken;
  const chuiCount = penalties.chui;

  return (
    <div className={cx(C.penaltyLadder, side)}>
      {["C1", "C2", "C3"].map((label, index) => (
        <span
          key={label}
          className={cx(
            C.penaltyCell,
            chuiCount > index && C.penaltyActive
          )}
        >
          {label}
        </span>
      ))}
      <span
        className={cx(C.penaltyCell, penalties.hansokuChui && C.penaltyActive)}
      >
        HC
      </span>
      <span className={cx(C.penaltyCell, dq && C.penaltyDanger)}>DQ</span>
    </div>
  );
}

// -----------------------------------------------------------------------------
// KumiteSide — one competitor column (huge score, name/meta, point pop, ladder).
// -----------------------------------------------------------------------------
function KumiteSide({
  side,
  competitor,
  point,
  variant,
}: {
  side: Side;
  competitor: Competitor;
  point: Point;
  variant: BoardVariant;
}) {
  const isPreview = variant === "preview";
  const scoreSpring = isPreview
    ? { type: "spring" as const, stiffness: 600, damping: 30 }
    : { type: "spring" as const, stiffness: 400, damping: 22 };
  const popDuration = isPreview ? 0.9 : 1.6;
  const showPop = point != null && point.side === side;

  return (
    <div className={cx(C.side, side)}>
      <div className={cx(C.sideBadge, side)}>
        <Flag size={isPreview ? 14 : 18} />
        {sideLabel(side)}
        {competitor.senshu && (
          <span className={C.senshuBadge}>
            <Crown size={isPreview ? 12 : 14} /> 先 SENSHU
          </span>
        )}
      </div>

      <div className={C.scoreStage}>
        <AnimatePresence mode="popLayout">
          <motion.span
            key={competitor.score}
            className={C.scoreValue}
            initial={{ scale: 0.4, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0.4, opacity: 0 }}
            transition={scoreSpring}
          >
            {competitor.score}
          </motion.span>
        </AnimatePresence>

        <AnimatePresence>
          {showPop && point && (
            <motion.div
              key={point.id}
              className={C.pointPop}
              initial={{ y: isPreview ? 20 : 40, opacity: 0, scale: 0.8 }}
              animate={{ y: 0, opacity: 1, scale: 1 }}
              exit={{ y: isPreview ? -20 : -48, opacity: 0 }}
              transition={{ duration: popDuration * 0.4, ease: "easeOut" }}
            >
              <strong>+{point.points}</strong>
              <em>{point.label}</em>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      <div className={C.athleteName}>{competitor.name || "—"}</div>
      <div className={C.athleteMeta}>
        {competitor.club || "—"}
        {competitor.bib ? ` / #${competitor.bib}` : ""}
      </div>

      <KumitePenalties side={side} penalties={competitor.penalties} />
    </div>
  );
}

// -----------------------------------------------------------------------------
// KumiteCenter — status chip, big timer, caption, contextual tags.
// -----------------------------------------------------------------------------
function KumiteCenter({
  match,
  remaining,
}: {
  match: MatchState;
  remaining: number;
}) {
  const warning = remaining <= 15000 && remaining > 0;
  const decision = match.kumite.decision;

  const caption = warning
    ? "ATOSHIBARAKU"
    : match.kumite.hantei
      ? "HANTEI"
      : decision?.winType === "HIKIWAKE"
        ? "HIKIWAKE"
      : "WKF 2026";

  const senshuSide: Side | null = match.competitors.aka.senshu
    ? "aka"
    : match.competitors.ao.senshu
      ? "ao"
      : null;

  return (
    <div className={C.center}>
      <div className={cx(C.statusChip, match.status)}>
        {match.status || "—"}
      </div>

      <div className={cx(C.timer, warning && C.timerWarning)}>
        {formatMs(remaining)}
      </div>

      <div className={C.centerCaption}>{caption}</div>

      <div className={C.centerTags}>
        {senshuSide && (
          <span className={cx(C.smallChip, senshuSide)}>
            <Crown size={12} /> 先 SENSHU {sideLabel(senshuSide)}
          </span>
        )}
        {decision && (
          <span className={cx(C.smallChip, "gold")}>
            <Sparkles size={12} />
            {winnerText(decision.side, decision.reasonText || decision.winType || undefined)}
          </span>
        )}
        {match.kumite.medical.active && (
          <span className={cx(C.smallChip, "gold")}>
            <AlertTriangle size={12} /> Medical
          </span>
        )}
        {match.kumite.hantei && (
          <span className={cx(C.smallChip, "gold")}>
            <AlertTriangle size={12} /> HANTEI
          </span>
        )}
      </div>
    </div>
  );
}

// -----------------------------------------------------------------------------
// KumiteBoard — top-level inner content for the kumite mode.
// -----------------------------------------------------------------------------
export function KumiteBoard({ payload, variant }: BoardProps) {
  const match = payload.match;
  const now = useNow(120);
  const remaining = liveRemaining(match.timer, payload.receivedAt, now);
  const point = match.kumite.lastPoint;

  return (
    <div className={cx(C.boardMain, C.kumiteLayout)}>
      <KumiteSide
        side="aka"
        competitor={match.competitors.aka}
        point={point}
        variant={variant}
      />
      <KumiteCenter match={match} remaining={remaining} />
      <KumiteSide
        side="ao"
        competitor={match.competitors.ao}
        point={point}
        variant={variant}
      />
    </div>
  );
}
