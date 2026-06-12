// =============================================================================
// scoreboard/KataBoard.tsx — KATA spectator board (flag/vote model)
// -----------------------------------------------------------------------------
// Renders ONLY the kata-specific main area. The parent ScoreboardFrame supplies
// the header, footer and winner overlay. Layout: performer | center | performer.
//
// Kata in this build is a FLAG/VOTE model: 5 or 7 judges each raise an AKA or AO
// flag. Before reveal the flags are neutral; on reveal each flips to its side's
// color with a staggered animation. needed = floor(judgeCount / 2) + 1.
// =============================================================================

import { motion, AnimatePresence } from "framer-motion";
import { Flag, Hourglass, Crown, Users } from "lucide-react";

import type { MatchState, Competitor, Side } from "../types";
import { cx, formatMs, liveRemaining, sideLabel } from "../utils";
import { useNow, C, type BoardProps } from "./contract";

const SIDES: Side[] = ["aka", "ao"];

// -----------------------------------------------------------------------------
// Count flags currently cast for a given side.
// -----------------------------------------------------------------------------
function countVotes(votes: Record<string, Side>, side: Side): number {
  let n = 0;
  for (const k in votes) if (votes[k] === side) n += 1;
  return n;
}

// -----------------------------------------------------------------------------
// KataPerformer — one performer panel (badge, name, club/bib, kata, flag tally).
// -----------------------------------------------------------------------------
function KataPerformer({
  side,
  competitor,
  votes,
  reveal,
}: {
  side: Side;
  competitor: Competitor;
  votes: number;
  reveal: boolean;
}) {
  return (
    <div className={cx(C.kataPerformer, side)}>
      <div className={cx(C.sideBadge, side)}>{sideLabel(side)}</div>

      <div className={C.athleteName}>{competitor.name || "—"}</div>
      <div className={C.athleteMeta}>
        {competitor.club || "—"}
        {competitor.bib ? ` / #${competitor.bib}` : ""}
      </div>

      <div className={C.kataName}>
        <Flag size={16} aria-hidden />
        <span>{competitor.kataName || "—"}</span>
        {competitor.kataNo ? <small>{competitor.kataNo}</small> : null}
      </div>

      <AnimatePresence>
        {reveal && (
          <motion.div
            className={cx(C.senshuBadge, side)}
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0 }}
          >
            {votes} flags
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

// -----------------------------------------------------------------------------
// JudgeFlags — the row of judge flag cells. Before reveal: neutral. On reveal:
// each flips to its voted side's color, staggered by index.
// -----------------------------------------------------------------------------
function JudgeFlags({
  match,
  reveal,
  fast,
}: {
  match: MatchState;
  reveal: boolean;
  fast: boolean;
}) {
  const { judgeCount, votes } = match.kata;
  const cells = Array.from({ length: judgeCount }, (_, i) => i + 1);

  return (
    <div className={C.judgeFlags}>
      {cells.map((j, i) => {
        const voted = votes[String(j)] ?? votes[j as unknown as string];
        const revealed = reveal && !!voted;
        return (
          <motion.div
            key={j}
            className={cx(C.judgeFlag, revealed && voted)}
            initial={false}
            animate={
              revealed
                ? { rotateY: 0, scale: 1, opacity: 1 }
                : { rotateY: 0, scale: 1, opacity: 1 }
            }
            transition={{
              delay: revealed ? i * (fast ? 0.05 : 0.12) : 0,
              type: "spring",
              stiffness: 320,
              damping: 22,
            }}
          >
            <AnimatePresence mode="wait" initial={false}>
              <motion.span
                key={revealed ? `r-${voted}` : "n"}
                initial={{ rotateX: 90, opacity: 0 }}
                animate={{ rotateX: 0, opacity: 1 }}
                exit={{ rotateX: -90, opacity: 0 }}
                transition={{ delay: revealed ? i * (fast ? 0.05 : 0.12) : 0 }}
              >
                {revealed ? <Flag size={fast ? 14 : 18} aria-hidden /> : j}
              </motion.span>
            </AnimatePresence>
          </motion.div>
        );
      })}
    </div>
  );
}

// -----------------------------------------------------------------------------
// KataCenter — phase label, countdown, caption, judge flags, tally + majority.
// -----------------------------------------------------------------------------
function KataCenter({
  match,
  remaining,
  fast,
}: {
  match: MatchState;
  remaining: number;
  fast: boolean;
}) {
  const { phase, reveal, votes, judgeCount, result } = match.kata;
  const needed = Math.floor(judgeCount / 2) + 1;

  const akaVotes = result?.aka ?? countVotes(votes, "aka");
  const aoVotes = result?.ao ?? countVotes(votes, "ao");

  const seconds = Math.max(0, Math.ceil(remaining / 1000));
  const warning = remaining <= 10_000;

  return (
    <div className={C.center}>
      <div className={C.kataPhase}>{(phase || "kata").toUpperCase()}</div>

      <div className={cx(C.timer, warning && C.timerWarning)}>
        <Hourglass size={fast ? 18 : 26} aria-hidden />
        <span>{formatMs(remaining)}</span>
      </div>

      <div className={C.centerCaption}>{seconds}s call</div>

      <JudgeFlags match={match} reveal={reveal} fast={fast} />

      <div className={C.kataTally}>
        <span className="aka">{akaVotes}</span>
        <em>-</em>
        <span className="ao">{aoVotes}</span>
      </div>

      <AnimatePresence>
        {reveal && result?.side && (
          <motion.div
            className={cx(C.majorityChip, result.side)}
            initial={{ opacity: 0, scale: 0.85 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0 }}
            transition={{ delay: fast ? 0.2 : judgeCount * 0.12 }}
          >
            <Crown size={14} aria-hidden />
            {sideLabel(result.side)} majority
          </motion.div>
        )}
      </AnimatePresence>

      <div className={C.centerCaption}>
        <Users size={12} aria-hidden /> {needed} of {judgeCount} needed
      </div>
    </div>
  );
}

// -----------------------------------------------------------------------------
// KataBoard — public entry. Computes live countdown and lays out the three cols.
// -----------------------------------------------------------------------------
export function KataBoard({ payload, variant }: BoardProps) {
  const fast = variant === "preview";
  const now = useNow(120);
  const match = payload.match;

  const remaining = liveRemaining(
    match.kata.countdown,
    payload.receivedAt,
    now
  );

  const akaVotes = countVotes(match.kata.votes, "aka");
  const aoVotes = countVotes(match.kata.votes, "ao");

  return (
    <div className={cx(C.boardMain, C.kataLayout)}>
      <KataPerformer
        side="aka"
        competitor={match.competitors.aka}
        votes={akaVotes}
        reveal={match.kata.reveal}
      />
      <KataCenter match={match} remaining={remaining} fast={fast} />
      <KataPerformer
        side="ao"
        competitor={match.competitors.ao}
        votes={aoVotes}
        reveal={match.kata.reveal}
      />
    </div>
  );
}
