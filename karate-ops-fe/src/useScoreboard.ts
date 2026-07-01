import { Client } from "@stomp/stompjs";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { apiGetOptional, apiPost, authHeaders, errorMessage, getApiBase } from "./apiClient";
import type {
  BackendSide,
  Competitor,
  DeviceRecord,
  EventRecord,
  MatchEventResponse,
  MatchResponse,
  MatchState,
  ScoreboardAction,
  Side,
  StatePayload
} from "./types";

export { apiDelete, apiGet, apiPatch, apiPost, apiPut, errorMessage, getAuthToken, setAuthToken } from "./apiClient";

interface ConnectionState {
  connected: boolean;
  socketId: string | null;
  latencyMs: number | null;
  lastError: string | null;
}

export function useScoreboard(role: string) {
  const params = new URLSearchParams(window.location.search);
  const tatamiId = params.get("tatamiId");
  const [payload, setPayload] = useState<StatePayload | null>(null);
  const [connection, setConnection] = useState<ConnectionState>({
    connected: false,
    socketId: null,
    latencyMs: null,
    lastError: null
  });
  const matchRef = useRef<MatchResponse | null>(null);
  const clientRef = useRef<Client | null>(null);

  const applyMatch = useCallback((match: MatchResponse | null) => {
    matchRef.current = match;
    if (!match) {
      setPayload(null);
      return;
    }
    setPayload(toStatePayload(match, role));
  }, [role]);

  const refresh = useCallback(async () => {
    if (!tatamiId) {
      setPayload(null);
      return;
    }
    try {
      const match = await getCurrentMatch(tatamiId);
      applyMatch(match);
      setConnection((current) => ({ ...current, lastError: null }));
    } catch (error) {
      setConnection((current) => ({ ...current, lastError: errorMessage(error) }));
    }
  }, [applyMatch, tatamiId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  useEffect(() => {
    if (!tatamiId) return undefined;
    const client = new Client({
      brokerURL: wsUrl("/ws"),
      connectHeaders: authHeaders(),
      reconnectDelay: 1000,
      heartbeatIncoming: 8000,
      heartbeatOutgoing: 8000,
      onConnect: () => {
        setConnection((current) => ({ ...current, connected: true, socketId: "stomp", lastError: null }));
        client.subscribe(`/topic/tatamis/${tatamiId}`, (message) => {
          applyMatch(JSON.parse(message.body) as MatchResponse);
        });
      },
      onDisconnect: () => {
        setConnection((current) => ({ ...current, connected: false }));
      },
      onStompError: (frame) => {
        setConnection((current) => ({ ...current, lastError: frame.headers.message || "STOMP error" }));
      },
      onWebSocketClose: () => {
        setConnection((current) => ({ ...current, connected: false }));
      }
    });
    clientRef.current = client;
    client.activate();
    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [applyMatch, tatamiId]);

  const send = useCallback(async (action: ScoreboardAction) => {
    const current = matchRef.current;
    if (!current?.id) return { ok: false, error: "No current match selected" };
    try {
      const updated = await sendBackendAction(current, action);
      applyMatch(updated);
      return { ok: true };
    } catch (error) {
      const message = errorMessage(error);
      setConnection((state) => ({ ...state, lastError: message }));
      return { ok: false, error: message };
    }
  }, [applyMatch]);

  return useMemo(
    () => ({
      payload,
      match: payload?.match ?? null,
      events: payload?.events ?? [],
      devices: payload?.devices ?? [],
      connection,
      send,
      refresh
    }),
    [connection, payload, refresh, send]
  );
}

async function getCurrentMatch(tatamiId: string) {
  return apiGetOptional<MatchResponse>(`/api/tatamis/${tatamiId}/current-match`);
}

async function sendBackendAction(currentMatch: MatchResponse, action: ScoreboardAction) {
  const matchId = currentMatch.id;
  const payload = action.payload || {};
  switch (action.type) {
    case "SCORE_DELTA":
      return apiPost<MatchResponse>(`/api/matches/${matchId}/events`, {
        type: "SCORE_DELTA",
        side: toBackendSide(payload.side),
        points: payload.points
      });
    case "SCORE_EXCHANGE": {
      const exchangeId = String(payload.exchangeId || `exchange-${Date.now()}`);
      return apiPost<MatchResponse>(`/api/matches/${matchId}/events`, {
        type: "SCORE_EXCHANGE",
        exchangeId,
        payloadJson: JSON.stringify({
          exchangeId,
          akaPoints: Number(payload.akaPoints || 0),
          aoPoints: Number(payload.aoPoints || 0)
        })
      });
    }
    case "TIMER_START":
    case "TIMER_STOP":
      return apiPost<MatchResponse>(`/api/matches/${matchId}/events`, { type: action.type });
    case "TIMER_SET_DURATION":
      return apiPost<MatchResponse>(`/api/matches/${matchId}/events`, {
        type: "TIMER_SET",
        timerMs: payload.durationMs
      });
    case "TIMER_ADJUST": {
      const current = currentRemaining(currentMatch);
      return apiPost<MatchResponse>(`/api/matches/${matchId}/events`, {
        type: "TIMER_SET",
        timerMs: Math.max(0, current + Number(payload.deltaMs || 0))
      });
    }
    case "TIMER_RESET":
      return apiPost<MatchResponse>(`/api/matches/${matchId}/events`, {
        type: "TIMER_SET",
        timerMs: currentMatch.kumite?.durationMs ?? 180000
      });
    case "PENALTY_LEVEL":
      return apiPost<MatchResponse>(`/api/matches/${matchId}/events`, {
        type: "PENALTY",
        side: toBackendSide(payload.side),
        penaltyLevel: payload.level,
        penaltyReasonCode: payload.reasonCode
      });
    case "DIRECT_PENALTY":
      return apiPost<MatchResponse>(`/api/matches/${matchId}/events`, {
        type: "PENALTY",
        side: toBackendSide(payload.side),
        penaltyCode: payload.reasonCode
      });
    case "SENSHU_REVOKE":
      return apiPost<MatchResponse>(`/api/matches/${matchId}/events`, {
        type: "SENSHU_REVOKE",
        side: payload.side ? toBackendSide(payload.side) : undefined,
        penaltyReasonCode: payload.reasonCode
      });
    case "HANTEI_DECISION":
      return apiPost<MatchResponse>(`/api/matches/${matchId}/events`, {
        type: "HANTEI_DECISION",
        side: toBackendSide(payload.side)
      });
    case "RESUME_REVIEW":
      return apiPost<MatchResponse>(`/api/matches/${matchId}/events`, {
        type: "STATUS_CHANGE",
        status: "REVIEW"
      });
    case "VR_REQUEST":
      return apiPost<MatchResponse>(`/api/matches/${matchId}/events`, {
        type: "VIDEO_REVIEW_REQUEST",
        side: toBackendSide(payload.side)
      });
    case "VR_RESOLVE":
      return apiPost<MatchResponse>(`/api/matches/${matchId}/events`, {
        type: "VIDEO_REVIEW_RESOLVE",
        resolution: payload.resolution,
        resolutionSide: payload.side ? toBackendSide(payload.side) : undefined,
        resolutionPoints: payload.points,
        reasonCode: payload.reasonCode,
        reasonText: payload.reasonText
      });
    case "MEDICAL_START":
      return apiPost<MatchResponse>(`/api/matches/${matchId}/events`, {
        type: "MEDICAL_START",
        side: toBackendSide(payload.side)
      });
    case "MEDICAL_RESOLVE":
      return apiPost<MatchResponse>(`/api/matches/${matchId}/events`, {
        type: "MEDICAL_RESOLVE",
        medicalOutcome: payload.outcome
      });
    case "KATA_VOTE":
      if (payload.side === "clear") throw new Error("Backend does not support clearing Kata votes yet");
      return apiPost<MatchResponse>(`/api/matches/${matchId}/events`, {
        type: "KATA_VOTE",
        judgeNumber: payload.judge,
        voteSide: toBackendSide(payload.side)
      });
    case "DRAW_CONFIRM":
      return apiPost<MatchResponse>(`/api/matches/${matchId}/result`, {
        winnerSide: null,
        winType: "HIKIWAKE",
        reason: String(payload.reason || "HIKIWAKE")
      });
    case "WINNER_CONFIRM":
    case "KATA_CONFIRM_WINNER":
      return apiPost<MatchResponse>(`/api/matches/${matchId}/result`, {
        winnerSide: toBackendSide(payload.side),
        winType: winType(String(payload.winType || (action.type === "KATA_CONFIRM_WINNER" ? "KATA_VOTES" : "MANUAL"))),
        reason: String(payload.reason || payload.winType || action.type)
      });
    default:
      throw new Error(`${action.type} is not supported by the Spring backend yet`);
  }
}

function toStatePayload(match: MatchResponse, role: string): StatePayload {
  return {
    type: "state",
    serverTime: Date.now(),
    receivedAt: Date.now(),
    match: toMatchState(match),
    events: match.recentEvents.map(toEventRecord),
    devices: [{
      id: "spring-backend",
      role: "backend",
      name: "Spring backend",
      connectedAt: Date.now(),
      lastSeen: Date.now(),
      address: role
    }]
  };
}

function toMatchState(match: MatchResponse): MatchState {
  const aka = competitor(match, "AKA");
  const ao = competitor(match, "AO");
  const kumite = match.kumite;
  const kataVotes = Object.fromEntries(match.kataVotes.map((vote) => [String(vote.judgeNumber), toUiSide(vote.side)]));
  const judgeCount = match.kataVotes.length > 5 ? 7 : 5;
  const result = kataResult(kataVotes, judgeCount);
  const hasDecision = Boolean(
    kumite?.decision
    && (kumite.decision.winnerSide || kumite.decision.winType || kumite.decision.reasonText || kumite.decision.frozen || kumite.decision.confirmable)
  );
  const decision = hasDecision && kumite?.decision
    ? {
        side: kumite.decision.winnerSide ? toUiSide(kumite.decision.winnerSide) : null,
        winType: kumite.decision.winType || null,
        reasonCode: kumite.decision.reasonCode || null,
        reasonText: kumite.decision.reasonText || null,
        frozen: Boolean(kumite.decision.frozen),
        confirmable: Boolean(kumite.decision.confirmable)
      }
    : null;
  return {
    schemaVersion: 3,
    id: match.id,
    tournamentId: match.tournamentId,
    categoryId: match.categoryId,
    tatamiId: match.tatamiId || null,
    mode: match.mode.includes("KATA") ? "kata" : "kumite",
    tatami: match.tatamiNo ? `Tatami ${match.tatamiNo}` : "Unassigned tatami",
    category: match.categoryName,
    round: match.roundName || `Round ${match.roundNumber || "-"}`,
    matchNo: match.matchNumber ? `M-${match.matchNumber}` : match.id.slice(0, 8),
    status: match.status.toLowerCase(),
    winner: winnerSide(match),
    winType: match.winType || null,
    nextMatch: "Next match from backend draw",
    timer: {
      durationMs: kumite?.durationMs ?? 180000,
      remainingMs: kumite?.remainingMs ?? 180000,
      running: Boolean(kumite?.timerRunning),
      startedAt: kumite?.timerStartedAt ? new Date(kumite.timerStartedAt).getTime() : null
    },
    competitors: {
      aka: withKumite(aka, kumite, "aka"),
      ao: withKumite(ao, kumite, "ao")
    },
    kumite: {
      hantei: match.status === "HANTEI",
      lastPoint: lastPoint(match.recentEvents),
      decision,
      senshuState: {
        holderSide: kumite?.senshu?.holderSide ? toUiSide(kumite.senshu.holderSide) : null,
        awardedAt: kumite?.senshu?.awardedAt ? new Date(kumite.senshu.awardedAt).getTime() : null,
        revoked: Boolean(kumite?.senshu?.revoked),
        revokedAt: kumite?.senshu?.revokedAt ? new Date(kumite.senshu.revokedAt).getTime() : null,
        revocationReasonCode: kumite?.senshu?.revocationReasonCode || null
      },
      vr: {
        aka: {
          card: kumite?.videoReview ? kumite.videoReview.akaCardAvailable : true,
          active: kumite?.videoReview?.status === "REQUESTED" && kumite.videoReview.activeRequestSide === "AKA",
          result: kumite?.videoReview?.lastResolution === "DENIED"
            ? "denied"
            : kumite?.videoReview?.lastResolution === "MIENAI" || kumite?.videoReview?.lastResolution === "TECHNICAL_PROBLEM"
              ? "inconclusive"
            : kumite?.videoReview?.status === "REQUESTED"
              ? "active"
              : kumite?.videoReview?.lastResolution
                ? "accepted"
                : "ready"
        },
        ao: {
          card: kumite?.videoReview ? kumite.videoReview.aoCardAvailable : true,
          active: kumite?.videoReview?.status === "REQUESTED" && kumite.videoReview.activeRequestSide === "AO",
          result: kumite?.videoReview?.lastResolution === "DENIED"
            ? "denied"
            : kumite?.videoReview?.lastResolution === "MIENAI" || kumite?.videoReview?.lastResolution === "TECHNICAL_PROBLEM"
              ? "inconclusive"
            : kumite?.videoReview?.status === "REQUESTED"
              ? "active"
              : kumite?.videoReview?.lastResolution
                ? "accepted"
                : "ready"
        }
      },
      medical: {
        injuredSide: kumite?.medical?.injuredSide ? toUiSide(kumite.medical.injuredSide) : null,
        startedAt: kumite?.medical?.startedAt ? new Date(kumite.medical.startedAt).getTime() : null,
        deadlineAt: kumite?.medical?.deadlineAt ? new Date(kumite.medical.deadlineAt).getTime() : null,
        active: kumite?.medical?.status === "ACTIVE",
        lastOutcome: kumite?.medical?.lastOutcome || null
      }
    },
    kata: {
      phase: match.status.toLowerCase(),
      judgeCount,
      reveal: match.status === "LOCKED",
      votes: kataVotes,
      countdown: { durationMs: 35000, remainingMs: 35000, running: false, startedAt: null },
      result
    }
  };
}

function competitor(match: MatchResponse, side: BackendSide): Competitor {
  const participant = match.participants.find((item) => item.side === side);
  return {
    name: participant?.athleteName || `${side} pending`,
    club: participant?.delegationName || "No delegation",
    bib: participant?.athleteId?.slice(0, 8) || "-",
    kataName: "Kata",
    kataNo: participant?.entryId?.slice(0, 6) || "-",
    score: 0,
    senshu: false,
    penalties: {
      chui: 0,
      hansokuChui: false,
      penaltyLevel: "NONE",
      reasonCode: null,
      category1Level: "NONE",
      category2Level: "NONE",
      hansoku: false,
      shikkaku: false,
      kiken: false
    }
  };
}

function withKumite(competitor: Competitor, kumite: MatchResponse["kumite"], side: Side): Competitor {
  if (!kumite) return competitor;
  const penalties = side === "aka" ? kumite.penalties?.aka : kumite.penalties?.ao;
  return {
    ...competitor,
    score: side === "aka" ? kumite.akaScore : kumite.aoScore,
    senshu: side === "aka" ? kumite.akaSenshu : kumite.aoSenshu,
    penalties: {
      chui: side === "aka" ? kumite.akaChui : kumite.aoChui,
      hansokuChui: side === "aka" ? Boolean(kumite.akaHansokuChui) : Boolean(kumite.aoHansokuChui),
      penaltyLevel: penalties?.penaltyLevel || penalties?.category1Level || "NONE",
      reasonCode: penalties?.reasonCode || null,
      category1Level: penalties?.category1Level || "NONE",
      category2Level: penalties?.category2Level || "NONE",
      hansoku: penalties?.hansoku ?? (side === "aka" ? kumite.akaHansoku : kumite.aoHansoku),
      shikkaku: penalties?.shikkaku ?? (side === "aka" ? kumite.akaShikkaku : kumite.aoShikkaku),
      kiken: penalties?.kiken ?? (side === "aka" ? kumite.akaKiken : kumite.aoKiken)
    }
  };
}

function toEventRecord(event: MatchEventResponse): EventRecord {
  return {
    id: event.id,
    at: event.occurredAt,
    clientId: "spring-backend",
    type: event.type,
    label: eventLabel(event),
    payload: event as unknown as Record<string, unknown>
  };
}

function eventLabel(event: MatchEventResponse) {
  if (event.type === "SCORE_DELTA") return `${event.side} ${event.points && event.points > 0 ? "+" : ""}${event.points}`;
  if (event.type === "PENALTY") return `${event.side} ${event.penaltyCode}`;
  if (event.type === "KATA_VOTE") return `Judge ${event.judgeNumber} voted ${event.voteSide}`;
  if (event.type === "RESULT_CONFIRMED") return `${event.side} winner confirmed`;
  if (event.type === "VIDEO_REVIEW_REQUEST") return `${event.side} video review`;
  if (event.type === "VIDEO_REVIEW_RESOLVE") return "Video review resolved";
  if (event.type === "MEDICAL_START") return `${event.side} medical timeout`;
  if (event.type === "MEDICAL_RESOLVE") return "Medical workflow resolved";
  if (event.type === "HANTEI_DECISION") return `${event.side} hantei decision`;
  if (event.type === "SENSHU_REVOKE") return `${event.side || ""} senshu revoked`.trim();
  return event.type.replace(/_/g, " ").toLowerCase();
}

function lastPoint(events: MatchEventResponse[]): MatchState["kumite"]["lastPoint"] {
  const event = [...events].reverse().find((item) => item.type === "SCORE_DELTA" && item.side && item.points && item.points > 0);
  if (!event || !event.side || !event.points) return null;
  return {
    id: event.id,
    side: toUiSide(event.side),
    points: event.points,
    label: event.points === 3 ? "Ippon" : event.points === 2 ? "Waza-ari" : "Yuko",
    at: new Date(event.occurredAt).getTime()
  };
}

function kataResult(votes: Record<string, Side>, judgeCount: 5 | 7) {
  const aka = Object.values(votes).filter((side) => side === "aka").length;
  const ao = Object.values(votes).filter((side) => side === "ao").length;
  const needed = Math.floor(judgeCount / 2) + 1;
  return { aka, ao, needed, side: aka >= needed ? "aka" as const : ao >= needed ? "ao" as const : null, complete: aka + ao >= judgeCount };
}

function winnerSide(match: MatchResponse): Side | null {
  if (!match.winnerEntryId) return null;
  const winner = match.participants.find((item) => item.entryId === match.winnerEntryId);
  return winner ? toUiSide(winner.side) : null;
}

function toBackendSide(value: unknown): BackendSide {
  if (value === "ao" || value === "AO") return "AO";
  return "AKA";
}

function toUiSide(value: BackendSide): Side {
  return value === "AO" ? "ao" : "aka";
}

function winType(value: string) {
  const normalized = value.toUpperCase().replace(/ /g, "_");
  const allowed = new Set([
    "POINTS",
    "EIGHT_POINT_LEAD",
    "TIME_UP",
    "SENSHU",
    "HANTEI",
    "HIKIWAKE",
    "KIKEN",
    "HANSOKU",
    "SHIKKAKU",
    "TEN_SECOND_RULE",
    "KATA_VOTES",
    "MANUAL"
  ]);
  if (allowed.has(normalized)) return normalized;
  if (normalized.includes("KATA")) return "KATA_VOTES";
  if (normalized.includes("10") || normalized.includes("TEN_SECOND")) return "TEN_SECOND_RULE";
  if (normalized.includes("8")) return "EIGHT_POINT_LEAD";
  if (normalized.includes("SENSHU")) return "SENSHU";
  if (normalized.includes("HANTEI")) return "HANTEI";
  if (normalized.includes("TIME")) return "TIME_UP";
  if (normalized.includes("POINT")) return "POINTS";
  return "MANUAL";
}

function wsUrl(path: string) {
  const apiBase = getApiBase();
  if (apiBase) {
    const url = new URL(apiBase);
    url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
    url.pathname = path;
    return url.toString();
  }
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  return `${protocol}//${window.location.host}${path}`;
}

function currentRemaining(match: MatchResponse) {
  const state = match.kumite;
  if (!state) return 180000;
  if (!state.timerRunning || !state.timerStartedAt) return state.remainingMs;
  return Math.max(0, state.remainingMs - (Date.now() - new Date(state.timerStartedAt).getTime()));
}
