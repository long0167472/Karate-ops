// =============================================================================
// scoreboard/OverlayView.tsx — OBS lower-third strip overlay.
// A compact mirror-layout strip: aka side · timer + meta · ao side.
// =============================================================================

import { motion } from "framer-motion";

import { formatMs, liveRemaining, sideLabel } from "../utils";
import { C, useNow, type OverlayViewProps } from "./contract";

export function OverlayView({ payload }: OverlayViewProps) {
  const { match, receivedAt } = payload;
  const now = useNow(120);

  const remaining =
    match.mode === "kata"
      ? liveRemaining(match.kata.countdown, receivedAt, now)
      : liveRemaining(match.timer, receivedAt, now);

  const aka = match.competitors.aka;
  const ao = match.competitors.ao;
  const detail = match.kumite.medical.active
    ? "MEDICAL"
    : match.kumite.decision?.reasonText || match.kumite.decision?.winType || null;

  return (
    <main className={C.overlayPage}>
      <motion.div
        className={C.overlayStrip}
        initial={{ y: 64, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ type: "spring", stiffness: 260, damping: 28 }}
      >
        <div className={`${C.overlaySide} aka`}>
          <span className="sb-overlay-label">{sideLabel("aka")}</span>
          <span className="sb-overlay-name">{aka.name}</span>
          <span className="sb-overlay-value">
            {match.mode === "kata" ? aka.kataName : aka.score}
          </span>
        </div>

        <div className={C.overlayCenter}>
          <div className={C.overlayTimer}>{formatMs(remaining)}</div>
          <div className={C.overlayMeta}>
            {match.tatami} / {match.category}
          </div>
          {detail ? <div className={C.overlayMeta}>{detail}</div> : null}
        </div>

        <div className={`${C.overlaySide} ao`}>
          <span className="sb-overlay-label">{sideLabel("ao")}</span>
          <span className="sb-overlay-name">{ao.name}</span>
          <span className="sb-overlay-value">
            {match.mode === "kata" ? ao.kataName : ao.score}
          </span>
        </div>
      </motion.div>
    </main>
  );
}
