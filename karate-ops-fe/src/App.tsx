import { AnimatePresence, motion } from "framer-motion";
import gsap from "gsap";
import { useGSAP } from "@gsap/react";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import {
  Activity,
  AlertTriangle,
  ArrowLeftRight,
  Award,
  CircleDot,
  Clock,
  Download,
  Eye,
  Flag,
  Gauge,
  History,
  ListChecks,
  Megaphone,
  Monitor,
  Move,
  Pause,
  Pin,
  PinOff,
  Play,
  Plus,
  Radio,
  RefreshCcw,
  RotateCcw,
  Search,
  Settings2,
  Maximize2,
  Minimize2,
  Shield,
  Swords,
  Trophy,
  Undo2,
  Users,
  Vote,
  Wifi,
  WifiOff,
  X
} from "lucide-react";
import { type FormEvent, type ReactNode, useCallback, useEffect, useRef, useState } from "react";
import ClubManagementPage from "./ClubManagementPage";
import { apiDelete, apiGet, apiPatch, apiPost, getAuthToken, setAuthToken } from "./apiClient";
import { LeaveRequestForm, type LeaveRequestFormData } from "./features/clubs/components/LeaveRequestForm";
import {
  JOIN_REQUEST_STATUS_LABELS,
  LEAVE_REQUEST_STATUS_LABELS,
  LEAVE_REQUEST_TYPE_LABELS,
  TOURNAMENT_STATUS_LABELS
} from "./features/clubs/clubConstants";
import { useScoreboard } from "./useScoreboard";
import type {
  AuthResponse,
  AuthUserResponse,
  AthleteResponse,
  AttendanceRecordResponse,
  AttendanceSessionResponse,
  CategoryResponse,
  ClubAnnouncementResponse,
  ClubMemberResponse,
  ClubRosterResponse,
  Competitor,
  DashboardOverviewResponse,
  DeviceRecord,
  EntryResponse,
  EventRecord,
  MatchResponse,
  MatchState,
  MedalTableRow,
  MemberAttendanceSummaryResponse,
  MemberClubProfileResponse,
  MemberFeeSummaryResponse,
  OrganizationResponse,
  PersonResponse,
  PublicClubLookupResponse,
  ScoreboardAction,
  Side,
  StatePayload,
  TatamiResponse,
  TatamiDashboardRow,
  TournamentJoinRequestResponse,
  TournamentParticipantResponse,
  TournamentResponse,
  LeaveRequestResponse
} from "./types";
import { cx, durationFromPreset, formatClockTime, formatMs, liveRemaining, sideLabel, winnerText } from "./utils";

gsap.registerPlugin(useGSAP, ScrollTrigger);

const SIDES: Side[] = ["aka", "ao"];
const POINTS = [
  { points: 1, label: "Yuko" },
  { points: 2, label: "Waza-ari" },
  { points: 3, label: "Ippon" }
];
const PREVIEW_FLOAT_KEY = "karate-ops.floatingPreviewRect";
const PREVIEW_VISIBLE_KEY = "karate-ops.previewVisible";
const PREVIEW_PINNED_KEY = "karate-ops.previewPinned";
const LAST_TOURNAMENT_KEY = "karate-ops.lastTournamentId";
const LAST_TATAMI_KEY = "karate-ops.lastTatamiId";
const VIEW_AS_ROLE_KEY = "karate-ops.viewAsRole";
const PREVIEW_MIN_WIDTH = 360;
const PREVIEW_MAX_WIDTH = 920;
type TournamentAdminTab = "setup" | "delegations" | "tatamis" | "categories" | "entries";

export default function App() {
  const route = window.location.pathname;
  const [authUser, setAuthUser] = useState<AuthUserResponse | null>(null);
  const [authReady, setAuthReady] = useState(false);
  const [viewAsRole, setViewAsRole] = useState(() => window.localStorage.getItem(VIEW_AS_ROLE_KEY) || "ACTUAL");
  const isPublicHome = route === "/";
  const role = route.includes("display")
    ? "display"
    : route.includes("overlay")
      ? "overlay"
      : route.includes("judge")
        ? "judge"
        : route.includes("control")
          ? "control"
          : "home";
  const isTatamiRoute = role !== "home";
  const hasTatamiId = Boolean(new URLSearchParams(window.location.search).get("tatamiId"));
  const api = useScoreboard(role);
  const manualApi = useManualTatami(role);

  useEffect(() => {
    if (!getAuthToken()) {
      setAuthReady(true);
      return;
    }
    apiGet<AuthUserResponse>("/api/auth/me")
      .then(setAuthUser)
      .catch(() => setAuthToken(null))
      .finally(() => setAuthReady(true));
  }, []);

  if (route.includes("login")) return <LoginPage onAuth={setAuthUser} />;
  if (route.includes("register")) return <RegisterPage onAuth={setAuthUser} />;
  if (isPublicHome) return <PublicHomePage authReady={authReady} user={authUser} />;
  if (!authReady) return <LoadingScreen connected={api.connection.connected} />;
  if (!authUser && !isTatamiRoute) return <LoginRedirect />;
  const effectiveUser = authUser ? effectiveRoleUser(authUser, viewAsRole) : null;
  const setViewAs = (role: string) => {
    setViewAsRole(role);
    window.localStorage.setItem(VIEW_AS_ROLE_KEY, role);
  };
  if (route.includes("member")) return <MemberPortalPage user={effectiveUser!} actualUser={authUser!} viewAsRole={viewAsRole} setViewAsRole={setViewAs} />;
  if (route.includes("dashboard/clubs")) return <ClubDashboardRedirect user={effectiveUser!} />;
  if (route.includes("dashboard/tournaments")) return <TournamentDashboardPage user={effectiveUser!} />;
  if (route.includes("clubs")) return <ClubManagementPage user={effectiveUser!} />;
  if (route.includes("tournaments")) return <TournamentManagementPage user={effectiveUser!} />;
  if (route.includes("app") || (!route.includes("display") && !route.includes("overlay") && !route.includes("judge") && !route.includes("control"))) {
    if (!effectiveUser) return <LoginRedirect />;
    if (!canManageClub(effectiveUser) && !canManageTournament(effectiveUser)) {
      return <MemberPortalPage user={effectiveUser} actualUser={authUser!} viewAsRole={viewAsRole} setViewAsRole={setViewAs} />;
    }
    return <HomePage connected={api.connection.connected} user={effectiveUser} actualUser={authUser!} viewAsRole={viewAsRole} setViewAsRole={setViewAs} />;
  }
  const tatamiApi = hasTatamiId ? api : manualApi;
  if (hasTatamiId && !api.match) return route.includes("control") && authUser ? <AssignMatchPage refresh={api.refresh} /> : <LoadingScreen connected={api.connection.connected} />;
  if (route.includes("display")) return <DisplayPage payload={tatamiApi.payload!} />;
  if (route.includes("overlay")) return <OverlayPage payload={tatamiApi.payload!} />;
  if (route.includes("judge")) return <JudgePage payload={tatamiApi.payload!} send={tatamiApi.send} />;
  if (route.includes("control")) return <ControlPage api={tatamiApi} />;
  return effectiveUser ? <HomePage connected={api.connection.connected} user={effectiveUser} actualUser={authUser!} viewAsRole={viewAsRole} setViewAsRole={setViewAs} /> : <LoginRedirect />;
}

function useManualTatami(role: string) {
  const [payload, setPayload] = useState<StatePayload>(() => createManualPayload(role));

  const send = useCallback(async (action: ScoreboardAction) => {
    setPayload((current) => applyManualAction(current, action));
    return { ok: true };
  }, []);

  const refresh = useCallback(async () => undefined, []);
  const match = payload.match;
  const events = payload.events;
  const devices = payload.devices;

  return {
    payload,
    match,
    events,
    devices,
    connection: {
      connected: true,
      socketId: "manual",
      latencyMs: 0,
      lastError: null
    },
    send,
    refresh,
    manual: true
  };
}

function createManualPayload(role: string): StatePayload {
  const now = Date.now();
  return {
    type: "state",
    serverTime: now,
    receivedAt: now,
    match: {
      schemaVersion: 3,
      id: "manual-tatami",
      tournamentId: null,
      categoryId: null,
      tatamiId: null,
      mode: "kumite",
      tatami: "Tatami Demo",
      category: "Kumite Senior Male -67kg",
      round: "Manual bout",
      matchNo: "TRY-001",
      status: "paused",
      winner: null,
      winType: null,
      nextMatch: "Manual free tatami",
      timer: {
        durationMs: 180000,
        remainingMs: 180000,
        running: false,
        startedAt: null
      },
      competitors: {
        aka: createManualCompetitor("AKA Athlete", "Red Corner Karate Club", "A01"),
        ao: createManualCompetitor("AO Athlete", "Blue Corner Dojo", "B02")
      },
      kumite: {
        hantei: false,
        lastPoint: null,
        suggestion: null,
        vr: {
          aka: { card: true, active: false, result: "ready" },
          ao: { card: true, active: false, result: "ready" }
        }
      },
      kata: {
        phase: "call",
        judgeCount: 5,
        reveal: true,
        votes: {},
        countdown: { durationMs: 35000, remainingMs: 35000, running: false, startedAt: null },
        result: { aka: 0, ao: 0, needed: 3, side: null, complete: false }
      }
    },
    events: [{
      id: "manual-ready",
      at: new Date(now).toISOString(),
      clientId: "manual",
      type: "MANUAL_READY",
      label: "Manual tatami ready",
      payload: { role }
    }],
    devices: [{
      id: "manual-browser",
      role: "guest",
      name: "Manual browser",
      connectedAt: now,
      lastSeen: now,
      address: role
    }]
  };
}

function createManualCompetitor(name: string, club: string, bib: string): Competitor {
  return {
    name,
    club,
    bib,
    kataName: "Kanku Dai",
    kataNo: "-",
    score: 0,
    senshu: false,
    penalties: { chui: 0, hansokuChui: false, hansoku: false, shikkaku: false, kiken: false }
  };
}

function applyManualAction(current: StatePayload, action: ScoreboardAction): StatePayload {
  const now = Date.now();
  const match = cloneManualMatch(current.match);
  const payload = action.payload || {};
  const side = payload.side === "ao" ? "ao" : "aka";

  if (action.type === "SCORE_DELTA") {
    const points = Number(payload.points || 0);
    match.competitors[side].score = Math.max(0, match.competitors[side].score + points);
    if (points > 0) {
      match.kumite.lastPoint = {
        id: `manual-point-${now}`,
        side,
        points,
        label: String(payload.label || "Point"),
        at: now
      };
    }
  }

  if (action.type === "TIMER_START") {
    match.timer.remainingMs = manualRemaining(match, current.receivedAt, now);
    match.timer.running = true;
    match.timer.startedAt = now;
    match.status = "running";
  }

  if (action.type === "TIMER_STOP") {
    match.timer.remainingMs = manualRemaining(match, current.receivedAt, now);
    match.timer.running = false;
    match.timer.startedAt = null;
    match.status = "paused";
  }

  if (action.type === "TIMER_RESET") {
    match.timer.remainingMs = match.timer.durationMs;
    match.timer.running = false;
    match.timer.startedAt = null;
    match.status = "paused";
  }

  if (action.type === "TIMER_SET_DURATION") {
    const durationMs = Number(payload.durationMs || 180000);
    match.timer.durationMs = durationMs;
    match.timer.remainingMs = durationMs;
    match.timer.running = false;
    match.timer.startedAt = null;
  }

  if (action.type === "TIMER_ADJUST") {
    match.timer.remainingMs = Math.max(0, manualRemaining(match, current.receivedAt, now) + Number(payload.deltaMs || 0));
    match.timer.running = false;
    match.timer.startedAt = null;
  }

  if (action.type === "SET_SENSHU") {
    const value = Boolean(payload.value);
    match.competitors.aka.senshu = side === "aka" ? value : false;
    match.competitors.ao.senshu = side === "ao" ? value : false;
  }

  if (action.type === "SET_CHUI") {
    match.competitors[side].penalties.chui = clampNumber(Number(payload.value || 0), 0, 3);
  }

  if (action.type === "TOGGLE_PENALTY") {
    const penalty = String(payload.penalty || "");
    const value = Boolean(payload.value);
    if (penalty === "hansokuChui") {
      match.competitors[side].penalties.hansokuChui = value;
    } else if (penalty === "hansoku") {
      match.competitors[side].penalties.hansoku = value;
    } else if (penalty === "shikkaku") {
      match.competitors[side].penalties.shikkaku = value;
    } else if (penalty === "kiken") {
      match.competitors[side].penalties.kiken = value;
    }
  }

  if (action.type === "SET_HANTEI") {
    match.kumite.hantei = Boolean(payload.value);
    match.status = match.kumite.hantei ? "hantei" : "paused";
  }

  if (action.type === "WINNER_CONFIRM" || action.type === "KATA_CONFIRM_WINNER") {
    match.winner = side;
    match.winType = String(payload.winType || "manual");
    match.status = "completed";
  }

  if (action.type === "KATA_VOTE") {
    const judge = String(payload.judge || "1");
    match.kata.votes[judge] = side;
    match.kata.result = manualKataResult(match.kata.votes, match.kata.judgeCount);
  }

  if (action.type === "MANUAL_SET_COMPETITOR") {
    const field = String(payload.field || "");
    if (["name", "club", "bib", "kataName", "kataNo"].includes(field)) {
      match.competitors[side] = { ...match.competitors[side], [field]: String(payload.value || "") };
    }
  }

  if (action.type === "MANUAL_SET_META") {
    const field = String(payload.field || "");
    if (["tatami", "matchNo", "category", "round", "nextMatch"].includes(field)) {
      Object.assign(match, { [field]: String(payload.value || "") });
    }
  }

  return {
    ...current,
    serverTime: now,
    receivedAt: now,
    match,
    events: [manualEvent(action, now), ...current.events].slice(0, 12),
    devices: current.devices.map((device) => ({ ...device, lastSeen: now }))
  };
}

function cloneManualMatch(match: MatchState): MatchState {
  return {
    ...match,
    timer: { ...match.timer },
    competitors: {
      aka: { ...match.competitors.aka, penalties: { ...match.competitors.aka.penalties } },
      ao: { ...match.competitors.ao, penalties: { ...match.competitors.ao.penalties } }
    },
    kumite: {
      ...match.kumite,
      lastPoint: match.kumite.lastPoint ? { ...match.kumite.lastPoint } : null,
      vr: { aka: { ...match.kumite.vr.aka }, ao: { ...match.kumite.vr.ao } }
    },
    kata: {
      ...match.kata,
      votes: { ...match.kata.votes },
      countdown: { ...match.kata.countdown },
      result: match.kata.result ? { ...match.kata.result } : undefined
    }
  };
}

function manualRemaining(match: MatchState, receivedAt: number, now: number) {
  return liveRemaining(match.timer, receivedAt, now);
}

function manualKataResult(votes: Record<string, Side>, judgeCount: 5 | 7) {
  const aka = Object.values(votes).filter((vote) => vote === "aka").length;
  const ao = Object.values(votes).filter((vote) => vote === "ao").length;
  const needed = Math.floor(judgeCount / 2) + 1;
  return { aka, ao, needed, side: aka >= needed ? "aka" as const : ao >= needed ? "ao" as const : null, complete: aka + ao >= judgeCount };
}

function manualEvent(action: ScoreboardAction, now: number): EventRecord {
  return {
    id: `manual-${now}`,
    at: new Date(now).toISOString(),
    clientId: "manual",
    type: action.type,
    label: action.type.replace(/_/g, " ").toLowerCase(),
    payload: action.payload || {}
  };
}

function PublicHomePage({ authReady, user }: { authReady: boolean; user: AuthUserResponse | null }) {
  const root = useRef<HTMLElement | null>(null);
  const appHref = authReady && user ? "/app" : "/login";

  useGSAP(() => {
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;

    gsap.from(".public-nav", { y: -18, opacity: 0, duration: 0.7, ease: "power3.out" });
    gsap.from(".landing-hero-copy > *", {
      y: 26,
      opacity: 0,
      duration: 0.82,
      ease: "power3.out",
      stagger: 0.08
    });
    gsap.from(".landing-visual", { scale: 0.92, opacity: 0, duration: 1.05, ease: "power3.out", delay: 0.12 });
    gsap.from(".landing-bento-card", {
      y: 34,
      opacity: 0,
      duration: 0.75,
      ease: "power3.out",
      stagger: 0.07,
      scrollTrigger: { trigger: ".landing-bento", start: "top 78%" }
    });
    gsap.to(".landing-story-pin", {
      scale: 0.96,
      opacity: 0.72,
      ease: "none",
      scrollTrigger: {
        trigger: ".landing-story",
        start: "top top",
        end: "bottom 45%",
        scrub: true,
        pin: ".landing-story-pin"
      }
    });
    gsap.from(".landing-story-card", {
      y: 72,
      opacity: 0,
      scale: 0.96,
      ease: "power2.out",
      stagger: 0.12,
      scrollTrigger: { trigger: ".landing-story-track", start: "top 76%", end: "bottom 52%", scrub: 0.55 }
    });
  }, { scope: root });

  return (
    <main className="public-home" ref={root}>
      <nav className="public-nav">
        <a className="public-brand" href="/">
          <span>K</span>
          <strong>Karate Ops</strong>
        </a>
        <div className="public-nav-links">
          <a href="#workflow">Luồng vận hành</a>
          <a href="/control">Tatami miễn phí</a>
          <a href="#club-dashboard">Dashboard CLB</a>
          <a href="/register">Đăng ký CLB</a>
        </div>
        <a className="public-nav-cta" href={appHref}>{authReady && user ? "Vào hệ thống" : "Đăng nhập"}</a>
      </nav>

      <section className="landing-hero">
        <div className="landing-hero-copy">
          <span className="landing-kicker">Club to Tatami Operations</span>
          <h1>
            CLB, giải đấu và tatami trong một nhịp.
          </h1>
          <p>
            Karate Ops gom roster, lịch tập, điểm danh, đăng ký giải và bảng điểm trực tiếp thành một hệ thống rõ ràng cho HLV, quản lý CLB và ban tổ chức.
          </p>
          <div className="landing-actions">
            <a className="landing-primary" href="/control">Dùng thử tatami miễn phí</a>
            <a className="landing-secondary" href="/register">Đăng ký quản lý CLB</a>
            <a className="landing-secondary" href={appHref}>{authReady && user ? "Mở bảng điều khiển" : "Đăng nhập hệ thống"}</a>
          </div>
        </div>

        <div className="landing-visual" aria-label="Karate Ops dashboard preview">
          <div className="landing-photo-strip">
            <span>Club roster</span>
            <strong>Members - Athletes - Fees</strong>
            <small>Ready for tournament entry</small>
          </div>
          <div className="landing-console">
            <div className="landing-console-head">
              <span>Live tatami</span>
              <b>SYNC</b>
            </div>
            <div className="landing-score-row">
              <strong>AKA</strong>
              <span>08</span>
            </div>
            <div className="landing-score-row ao">
              <strong>AO</strong>
              <span>06</span>
            </div>
            <div className="landing-console-foot">
              <span>Roster checked</span>
              <span>Attendance 86%</span>
            </div>
          </div>
        </div>
      </section>

      <section className="landing-bento" id="workflow">
        <article className="landing-bento-card wide">
          <span>Roster nguồn</span>
          <h2>Một lần nhập, nhiều lần dùng.</h2>
          <p>Thành viên CLB trở thành dữ liệu gốc cho điểm danh, hồ sơ VĐV và đăng ký giải.</p>
        </article>
        <article className="landing-bento-card image">
          <div className="landing-bento-mat">
            <span>Attendance board</span>
            <strong>Present / Late / Absent / Excused</strong>
            <small>Live records per training session</small>
          </div>
        </article>
        <article className="landing-bento-card">
          <span>Điểm danh</span>
          <strong>86%</strong>
          <p>Theo dõi present, late, absent và excused theo từng buổi tập.</p>
        </article>
        <article className="landing-bento-card">
          <span>Tài chính</span>
          <strong>Thu chi</strong>
          <p>Học phí tháng, khoản thu một lần và công nợ được tách rõ.</p>
        </article>
        <article className="landing-bento-card">
          <span>Tatami</span>
          <strong>Live</strong>
          <p>Control, display, judge vote và OBS overlay dùng cùng trạng thái trận đấu.</p>
        </article>
        <article className="landing-bento-card dark">
          <span>Ban tổ chức</span>
          <h2>Từ CLB đến giải đấu không còn phải ghép Excel thủ công.</h2>
          <p>CLB chuẩn bị roster, ban tổ chức duyệt đoàn, tatami nhận trận đấu và bảng điểm phát trực tiếp.</p>
        </article>
      </section>

      <section className="landing-story" id="club-dashboard">
        <div className="landing-story-pin">
          <span className="landing-kicker">Dashboard CLB mới</span>
          <h2>Không tách màn nữa. Sức khỏe CLB nằm ngay trong workspace.</h2>
          <p>Overview của từng CLB sẽ gộp số liệu thành viên, VĐV, chuyên cần, cảnh báo và VĐV cần chú ý để quản lý thao tác nhanh hơn.</p>
        </div>
        <div className="landing-story-track">
          {[
            ["Health", "Sức khỏe vận hành", "Thành viên active, VĐV active, số buổi tập và lượt đăng ký giải xuất hiện ngay đầu tab tổng quan."],
            ["Readiness", "Cảnh báo có ngữ cảnh", "Thiếu roster, chưa có lịch tập hoặc chuyên cần thấp được gom thành danh sách việc cần xử lý."],
            ["Attendance", "Chuyên cần có hành động", "Present, late, absent, excused và nhóm VĐV chuyên cần thấp dẫn người quản lý sang tab điểm danh."]
          ].map(([tag, title, text]) => (
            <article className="landing-story-card" key={tag}>
              <b>{tag}</b>
              <h3>{title}</h3>
              <p>{text}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="landing-final">
        <div>
          <span className="landing-kicker">Ready for operation</span>
          <h2>Mở Karate Ops và đưa dữ liệu CLB vào đúng dòng chảy.</h2>
        </div>
        <a className="landing-primary" href={appHref}>{authReady && user ? "Vào hệ thống" : "Đăng nhập để dùng chức năng"}</a>
      </section>
    </main>
  );
}

function LoginRedirect() {
  useEffect(() => {
    const next = encodeURIComponent(`${window.location.pathname}${window.location.search}`);
    window.location.replace(`/login?next=${next}`);
  }, []);
  return <LoadingScreen connected={false} />;
}

function ClubDashboardRedirect({ user }: { user: AuthUserResponse }) {
  useEffect(() => {
    const organizationId = window.location.pathname.split("/").filter(Boolean).at(-1) || user.primaryOrganizationId || "";
    window.location.replace(organizationId ? `/clubs/${organizationId}?tab=overview` : "/clubs");
  }, [user.primaryOrganizationId]);
  return <LoadingScreen connected={false} />;
}

function LoadingScreen({ connected }: { connected: boolean }) {
  return (
    <main className="center-shell">
      <motion.section className="loading-card" initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }}>
        <div className="app-mark">K</div>
        <div>
          <h1>Karate Ops</h1>
          <p>{connected ? "Syncing tatami state..." : "Connecting to Spring backend..."}</p>
        </div>
      </motion.section>
    </main>
  );
}

function HomePage({
  connected,
  user,
  actualUser,
  viewAsRole,
  setViewAsRole
}: {
  connected: boolean;
  user: AuthUserResponse;
  actualUser: AuthUserResponse;
  viewAsRole: string;
  setViewAsRole: (role: string) => void;
}) {
  const [tournaments, setTournaments] = useState<TournamentResponse[]>([]);
  const [tatamis, setTatamis] = useState<TatamiResponse[]>([]);
  const [tournamentId, setTournamentId] = useState(() => window.localStorage.getItem(LAST_TOURNAMENT_KEY) || "");
  const [tatamiId, setTatamiId] = useState(() => window.localStorage.getItem(LAST_TATAMI_KEY) || "");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiGet<TournamentResponse[]>("/api/tournaments")
      .then((data) => {
        setTournaments(data);
        if (!tournamentId && data[0]) setTournamentId(data[0].id);
      })
      .catch((err) => setError(err instanceof Error ? err.message : String(err)));
  }, [tournamentId]);

  useEffect(() => {
    if (!tournamentId) return;
    window.localStorage.setItem(LAST_TOURNAMENT_KEY, tournamentId);
    apiGet<TatamiResponse[]>(`/api/tournaments/${tournamentId}/tatamis`)
      .then((data) => {
        setTatamis(data);
        if (!data.some((tatami) => tatami.id === tatamiId)) setTatamiId(data[0]?.id || "");
      })
      .catch((err) => setError(err instanceof Error ? err.message : String(err)));
  }, [tatamiId, tournamentId]);

  useEffect(() => {
    if (tatamiId) window.localStorage.setItem(LAST_TATAMI_KEY, tatamiId);
  }, [tatamiId]);

  const query = tournamentId && tatamiId ? `?tournamentId=${tournamentId}&tatamiId=${tatamiId}` : "";
  return (
    <main className="app-hub-page">
      <section className="app-hub-shell">
        <motion.header className="app-hub-hero" initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} transition={{ type: "spring", stiffness: 110, damping: 22 }}>
          <div>
            <span className="landing-kicker">Operations hub</span>
            <h1>Chào {user.displayName}, chọn luồng cần vận hành hôm nay.</h1>
            <p>{user.roles.join(", ")}{user.primaryOrganizationName ? ` - ${user.primaryOrganizationName}` : ""}</p>
          </div>
          <div className="app-hub-status">
            <ConnectionPill connected={connected} />
            {hasRole(actualUser, "GLOBAL_ADMIN") ? (
              <label className="view-as-select">
                <span>View as</span>
                <select value={viewAsRole} onChange={(event) => setViewAsRole(event.target.value)}>
                  <option value="ACTUAL">Actual</option>
                  <option value="GLOBAL_ADMIN">Global admin</option>
                  <option value="CLUB_MANAGER">Admin CLB</option>
                  <option value="MEMBER">Member</option>
                </select>
              </label>
            ) : null}
            <button className="app-hub-logout" onClick={() => {
              setAuthToken(null);
              window.location.href = "/";
            }}>Đăng xuất</button>
          </div>
        </motion.header>

        <section className="app-hub-selector">
          <div>
            <span className="landing-kicker">Tatami selector</span>
            <h2>Khóa đúng giải và sàn trước khi mở control, display hoặc judge.</h2>
          </div>
          <div className="app-hub-select-grid">
            <label className="field">
              <span>Tournament</span>
              <select value={tournamentId} onChange={(event) => setTournamentId(event.target.value)}>
                {tournaments.length === 0 ? <option value="">Chưa có giải đấu</option> : null}
                {tournaments.map((tournament) => <option key={tournament.id} value={tournament.id}>{tournament.name}</option>)}
              </select>
            </label>
            <label className="field">
              <span>Tatami</span>
              <select value={tatamiId} onChange={(event) => setTatamiId(event.target.value)}>
                {tatamis.length === 0 ? <option value="">Chưa có tatami</option> : null}
                {tatamis.map((tatami) => <option key={tatami.id} value={tatami.id}>{tatami.name || `Tatami ${tatami.tatamiNo}`}</option>)}
              </select>
            </label>
          </div>
          {error ? <p className="error-text">{error}</p> : null}
        </section>

        <section className="app-hub-grid">
          <HomeLink href="/member" icon={<Users />} title="Member Portal" text="Xem hồ sơ của mình, học phí, chuyên cần và gửi request xin nghỉ." />
          <HomeLink href="/clubs" icon={<Activity />} title="Không gian CLB" text="Quản lý thành viên, roster, học phí, chuyên cần và dashboard ngay trong workspace." disabled={!canManageClub(user)} />
          <HomeLink href={user.primaryOrganizationId ? `/clubs/${user.primaryOrganizationId}?tab=overview` : "/clubs"} icon={<Shield />} title="Dashboard CLB" text="Mở tổng quan CLB đã gộp với cảnh báo, chuyên cần và readiness." disabled={!canManageClub(user) && !user.primaryOrganizationId} />
          <HomeLink href="/tournaments" icon={<Trophy />} title="Tournament Admin" text="Tạo giải, duyệt đoàn, chia tatami, hạng cân và hồ sơ thi đấu." disabled={!canManageTournament(user)} />
          <HomeLink href={tournamentId ? `/dashboard/tournaments/${tournamentId}` : "#"} icon={<History />} title="Tournament Dashboard" text="Theo dõi tổng quan giải, huy chương, tatami và trạng thái trận." disabled={!tournamentId} />
          <HomeLink href={`/control${query}`} icon={<Gauge />} title="Control Desk" text="Bàn thư ký điều khiển điểm, đồng hồ, cảnh báo và preview." disabled={!query} />
          <HomeLink href={`/display${query}`} icon={<Monitor />} title="Display" text="Bảng điểm fullscreen cho TV, màn chiếu hoặc LED tại sàn." disabled={!query} />
          <HomeLink href={`/judge${query}`} icon={<Vote />} title="Judge Vote" text="Màn bỏ phiếu Kata cho trọng tài, nối trực tiếp vào trận hiện tại." disabled={!query} />
          <HomeLink href={`/overlay${query}`} icon={<Eye />} title="OBS Overlay" text="Score strip nền trong suốt dành cho livestream và broadcast." disabled={!query} />
        </section>
      </section>
    </main>
  );
}

function HomeLink({ href, icon, title, text, disabled }: { href: string; icon: React.ReactNode; title: string; text: string; disabled?: boolean }) {
  return (
    <a className={cx("home-link", disabled && "disabled")} href={disabled ? "#" : href}>
      {icon}
      <strong>{title}</strong>
      <span>{text}</span>
    </a>
  );
}

function MemberPortalPage({
  user,
  actualUser,
  viewAsRole,
  setViewAsRole
}: {
  user: AuthUserResponse;
  actualUser: AuthUserResponse;
  viewAsRole: string;
  setViewAsRole: (role: string) => void;
}) {
  const [profile, setProfile] = useState<MemberClubProfileResponse | null>(null);
  const [fees, setFees] = useState<MemberFeeSummaryResponse | null>(null);
  const [attendance, setAttendance] = useState<MemberAttendanceSummaryResponse | null>(null);
  const [announcements, setAnnouncements] = useState<ClubAnnouncementResponse[]>([]);
  const [myLeaveRequests, setMyLeaveRequests] = useState<LeaveRequestResponse[]>([]);
  const [joinRequests, setJoinRequests] = useState<TournamentJoinRequestResponse[]>([]);
  const [tournaments, setTournaments] = useState<TournamentResponse[]>([]);
  const [leaveModal, setLeaveModal] = useState<{ open: boolean; sessionId?: string }>({ open: false });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setError(null);
    const [nextProfile, nextFees, nextAttendance] = await Promise.all([
      apiGet<MemberClubProfileResponse>("/api/me/club-profile"),
      apiGet<MemberFeeSummaryResponse>("/api/me/fees"),
      apiGet<MemberAttendanceSummaryResponse>("/api/me/attendance")
    ]);
    setProfile(nextProfile);
    setFees(nextFees);
    setAttendance(nextAttendance);
    const [announcementResult, leaveResult, joinResult, tournamentResult] = await Promise.allSettled([
      apiGet<ClubAnnouncementResponse[]>("/api/me/announcements"),
      apiGet<LeaveRequestResponse[]>("/api/me/leave-requests"),
      apiGet<TournamentJoinRequestResponse[]>("/api/me/tournament-join-requests"),
      apiGet<TournamentResponse[]>("/api/tournaments")
    ]);
    setAnnouncements(announcementResult.status === "fulfilled" ? announcementResult.value : []);
    setMyLeaveRequests(leaveResult.status === "fulfilled" ? leaveResult.value : []);
    setJoinRequests(joinResult.status === "fulfilled" ? joinResult.value : []);
    setTournaments(tournamentResult.status === "fulfilled" ? tournamentResult.value : []);
  }, []);

  useEffect(() => {
    load().catch((err) => setError(errorMessage(err)));
  }, [load]);

  const memberships = profile?.memberships ?? [];
  const feeRows = fees?.assignments ?? [];
  const attendanceRows = attendance?.sessionRows ?? [];
  const primaryOrganizationId = memberships[0]?.organizationId;
  const pendingRequests = myLeaveRequests.filter((row) => row.status === "PENDING").length
    + joinRequests.filter((row) => row.status === "PENDING").length;
  const joinRequestByTournament = new Map(joinRequests.map((row) => [row.tournamentId, row]));
  const openTournaments = tournaments.filter((event) => event.status === "REGISTRATION_OPEN");

  async function submitLeaveRequest(data: LeaveRequestFormData) {
    setBusy(true);
    setError(null);
    try {
      await apiPost("/api/me/attendance/leave-requests", {
        requestType: data.requestType,
        sessionId: data.sessionId,
        organizationId: data.requestType === "LEAVE_LONG_TERM" ? primaryOrganizationId : undefined,
        fromDate: data.fromDate,
        toDate: data.toDate,
        reason: data.reason
      });
      setLeaveModal({ open: false });
      await load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function requestJoinTournament(tournamentId: string) {
    setBusy(true);
    setError(null);
    try {
      const created = await apiPost<TournamentJoinRequestResponse>("/api/me/tournament-join-requests", {
        tournamentId,
        organizationId: primaryOrganizationId
      });
      setJoinRequests((current) => [created, ...current.filter((row) => row.id !== created.id)]);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  const money = (value?: number) => new Intl.NumberFormat("vi-VN").format(Number(value || 0));
  const dateText = (value?: string) => value ? new Intl.DateTimeFormat("vi-VN").format(new Date(value)) : "chưa rõ";

  return (
    <main className="member-portal-page">
      <section className="member-portal-shell">
        <header className="app-hub-hero member">
          <div>
            <span className="landing-kicker">Member portal</span>
            <h1>Chào {user.displayName}, đây là dữ liệu CLB của bạn.</h1>
            <p>{user.roles.join(", ")}{user.primaryOrganizationName ? ` - ${user.primaryOrganizationName}` : ""}</p>
          </div>
          <div className="app-hub-status">
            {hasRole(actualUser, "GLOBAL_ADMIN") ? (
              <label className="view-as-select">
                <span>View as</span>
                <select value={viewAsRole} onChange={(event) => setViewAsRole(event.target.value)}>
                  <option value="ACTUAL">Actual</option>
                  <option value="GLOBAL_ADMIN">Global admin</option>
                  <option value="CLUB_MANAGER">Admin CLB</option>
                  <option value="MEMBER">Member</option>
                </select>
              </label>
            ) : null}
            <a className="app-hub-logout" href="/app">Hub</a>
            <button className="app-hub-logout" onClick={() => {
              setAuthToken(null);
              window.location.href = "/";
            }}>Đăng xuất</button>
          </div>
        </header>

        {error ? <p className="error-text">{error}</p> : null}

        <section className="member-summary-grid">
          <div className="member-summary-card">
            <span>CLB đang tham gia</span>
            <strong>{memberships.length}</strong>
          </div>
          <div className="member-summary-card">
            <span>Còn phải đóng</span>
            <strong>{money(fees?.totalRemaining)}đ</strong>
          </div>
          <div className="member-summary-card">
            <span>Buổi đã ghi nhận</span>
            <strong>{attendance?.sessions ?? 0}</strong>
          </div>
          <div className="member-summary-card">
            <span>Request chờ duyệt</span>
            <strong>{pendingRequests}</strong>
          </div>
        </section>

        <section className="member-panel member-announcements">
          <div className="member-panel-head">
            <span>Thông báo</span>
            <h2>Tin mới từ CLB của bạn</h2>
          </div>
          {announcements.length === 0 ? <MemberEmpty text="Chưa có thông báo nào từ CLB." /> : announcements.slice(0, 6).map((announcement) => (
            <article className={announcement.pinned ? "member-announcement-row pinned" : "member-announcement-row"} key={announcement.id}>
              <div className="member-announcement-icon"><Megaphone size={17} /></div>
              <div>
                <strong>
                  {announcement.pinned ? <span className="member-announcement-pin"><Pin size={12} /> Ghim</span> : null}
                  {announcement.title}
                </strong>
                <p>{announcement.content}</p>
                <small>{announcement.organizationName} - {dateText(announcement.createdAt)}</small>
              </div>
            </article>
          ))}
        </section>

        <section className="member-portal-grid">
          <article className="member-panel">
            <div className="member-panel-head">
              <span>Hồ sơ CLB</span>
              <h2>Thông tin thành viên</h2>
            </div>
            {memberships.length === 0 ? <MemberEmpty text="Tài khoản này chưa được liên kết với thành viên CLB." /> : memberships.map((member) => (
              <div className="member-row" key={member.id}>
                <div>
                  <strong>{member.personName || member.userName || user.displayName}</strong>
                  <span>{member.organizationName} / {member.role} / {member.status}</span>
                </div>
                <small>{member.phone || member.email || "Chưa có liên hệ"}</small>
              </div>
            ))}
          </article>

          <article className="member-panel">
            <div className="member-panel-head">
              <span>Tài chính</span>
              <h2>Các khoản của bạn</h2>
            </div>
            {feeRows.length === 0 ? <MemberEmpty text="Chưa có khoản phí nào được gán." /> : feeRows.map((row) => (
              <div className="member-row" key={row.id}>
                <div>
                  <strong>{row.feeItemName}</strong>
                  <span>{row.status} / hạn {row.dueDate || "chưa đặt"}</span>
                </div>
                <small>{money(Number(row.amountDue) - Number(row.paidAmount))}đ</small>
              </div>
            ))}
          </article>
        </section>

        <section className="member-portal-grid">
          <article className="member-panel">
            <div className="member-panel-head with-action">
              <div>
                <span>Giải đấu</span>
                <h2>Xin tham gia giải</h2>
              </div>
            </div>
            {openTournaments.length === 0 ? <MemberEmpty text="Hiện chưa có giải nào đang mở đăng ký." /> : openTournaments.map((event) => {
              const existing = joinRequestByTournament.get(event.id);
              return (
                <div className="member-row" key={event.id}>
                  <div>
                    <strong>{event.name}</strong>
                    <span>{TOURNAMENT_STATUS_LABELS[event.status || ""] || event.status}{event.startsOn ? ` / thi đấu ${dateText(event.startsOn)}` : ""}</span>
                  </div>
                  {existing && existing.status !== "REJECTED" ? (
                    <small className={`member-request-chip ${existing.status.toLowerCase()}`}>{JOIN_REQUEST_STATUS_LABELS[existing.status]}</small>
                  ) : (
                    <button className="member-inline-button" disabled={busy || memberships.length === 0} onClick={() => requestJoinTournament(event.id)}>
                      Xin tham gia
                    </button>
                  )}
                </div>
              );
            })}
            {joinRequests.length > 0 ? (
              <div className="member-request-history">
                <small className="member-request-history-title">Yêu cầu đã gửi</small>
                {joinRequests.slice(0, 5).map((request) => (
                  <div className="member-row compact" key={request.id}>
                    <div>
                      <strong>{request.tournamentName}</strong>
                      <span>Gửi {dateText(request.createdAt)}{request.decisionNote ? ` / ${request.decisionNote}` : ""}</span>
                    </div>
                    <small className={`member-request-chip ${request.status.toLowerCase()}`}>{JOIN_REQUEST_STATUS_LABELS[request.status]}</small>
                  </div>
                ))}
              </div>
            ) : null}
          </article>

          <article className="member-panel">
            <div className="member-panel-head with-action">
              <div>
                <span>Xin nghỉ</span>
                <h2>Yêu cầu của bạn</h2>
              </div>
              <button className="member-inline-button" disabled={busy || memberships.length === 0} onClick={() => setLeaveModal({ open: true })}>
                Tạo yêu cầu
              </button>
            </div>
            {myLeaveRequests.length === 0 ? <MemberEmpty text="Bạn chưa gửi yêu cầu xin nghỉ nào." /> : myLeaveRequests.slice(0, 6).map((request) => (
              <div className="member-row" key={request.id}>
                <div>
                  <strong>{LEAVE_REQUEST_TYPE_LABELS[request.requestType] || request.requestType}</strong>
                  <span>
                    {request.requestType === "LEAVE_LONG_TERM" && request.fromDate && request.toDate
                      ? `${dateText(request.fromDate)} - ${dateText(request.toDate)}`
                      : request.sessionName || "Buổi tập"} / {request.reason}
                  </span>
                </div>
                <small className={`member-request-chip ${request.status.toLowerCase()}`}>{LEAVE_REQUEST_STATUS_LABELS[request.status] || request.status}</small>
              </div>
            ))}
          </article>
        </section>

        <section className="member-panel attendance">
          <div className="member-panel-head">
            <span>Chuyên cần</span>
            <h2>Lịch tập và điểm danh của bạn</h2>
          </div>
          {attendanceRows.length === 0 ? <MemberEmpty text="Chưa có buổi tập nào để hiển thị." /> : attendanceRows.map((session) => {
            const leave = session.leaveRequest;
            return (
              <div className="member-attendance-row" key={session.id}>
                <div>
                  <strong>{session.name}</strong>
                  <span>{session.organizationName} / {session.scheduledDate || session.scheduledAt?.slice(0, 10) || "chưa xếp lịch"}</span>
                  <small>Điểm danh: {session.record?.status || "Chưa ghi nhận"}{leave ? ` / Xin nghỉ: ${LEAVE_REQUEST_STATUS_LABELS[leave.status] || leave.status}` : ""}</small>
                </div>
                {!leave && session.status === "OPEN" ? (
                  <button className="member-inline-button" disabled={busy} onClick={() => setLeaveModal({ open: true, sessionId: session.id })}>
                    Xin nghỉ buổi này
                  </button>
                ) : leave ? <small className="member-leave-status">{leave.reason}</small> : null}
              </div>
            );
          })}
        </section>
      </section>

      {leaveModal.open ? (
        <div className="club-modal-overlay" onClick={() => setLeaveModal({ open: false })}>
          <div className="club-modal member-leave-modal" onClick={(event) => event.stopPropagation()}>
            <h3>Tạo yêu cầu xin nghỉ</h3>
            <LeaveRequestForm
              key={leaveModal.sessionId || "new"}
              busy={busy}
              error={error}
              sessions={attendanceRows.map((session) => ({
                id: session.id,
                name: session.name,
                status: session.status,
                scheduledAt: session.scheduledAt,
                scheduledDate: session.scheduledDate
              }))}
              initialSessionId={leaveModal.sessionId}
              onSubmit={submitLeaveRequest}
            />
          </div>
        </div>
      ) : null}
    </main>
  );
}

function MemberEmpty({ text }: { text: string }) {
  return <p className="member-empty">{text}</p>;
}

function LoginPage({ onAuth }: { onAuth: (user: AuthUserResponse) => void }) {
  const [email, setEmail] = useState("admin@karate-ops.local");
  const [password, setPassword] = useState("Admin@123456");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const nextPath = safeNextPath(new URLSearchParams(window.location.search).get("next"));

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const auth = await apiPost<AuthResponse>("/api/auth/login", { email, password });
      setAuthToken(auth.accessToken);
      onAuth(auth.user);
      window.location.href = nextPath || "/app";
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="auth-page">
      <section className="auth-shell">
        <AuthVisual />
        <motion.form className="auth-panel" onSubmit={submit} initial={{ opacity: 0, x: 22 }} animate={{ opacity: 1, x: 0 }} transition={{ type: "spring", stiffness: 120, damping: 22 }}>
          <div className="auth-heading">
            <span className="auth-kicker">Karate Ops</span>
            <h1>Đăng nhập hệ thống</h1>
            <p>Dùng tài khoản quản trị toàn hệ thống hoặc tài khoản quản lý CLB đã đăng ký.</p>
          </div>
          <label className="auth-field">
            <span>Email hoặc tên tài khoản</span>
            <input value={email} onChange={(event) => setEmail(event.target.value)} required />
          </label>
          <label className="auth-field">
            <span>Mật khẩu</span>
            <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} required />
          </label>
          {error ? <p className="auth-error">{error}</p> : null}
          <button className="auth-primary" disabled={busy}>{busy ? "Đang đăng nhập..." : "Đăng nhập"}</button>
          <div className="auth-switch">
            <span>Chưa có tài khoản?</span>
            <a href="/register">Xin cấp tài khoản CLB</a>
          </div>
        </motion.form>
      </section>
    </main>
  );
}

function safeNextPath(value: string | null) {
  if (!value) return "";
  try {
    const decoded = decodeURIComponent(value);
    if (!decoded.startsWith("/") || decoded.startsWith("//")) return "";
    if (decoded.includes("/login") || decoded.includes("/register")) return "";
    return decoded;
  } catch {
    return "";
  }
}

function RegisterPage({ onAuth }: { onAuth: (user: AuthUserResponse) => void }) {
  void onAuth;
  const [clubCode, setClubCode] = useState("");
  const [club, setClub] = useState<PublicClubLookupResponse | null>(null);
  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [gender, setGender] = useState("MALE");
  const [birthDate, setBirthDate] = useState("");
  const [currentAddress, setCurrentAddress] = useState("");
  const [sent, setSent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      if (!club) {
        const found = await apiGet<PublicClubLookupResponse>(`/api/public/clubs/lookup?code=${encodeURIComponent(clubCode.trim())}`);
        setClub(found);
        return;
      }
      await apiPost("/api/account-requests", {
        organizationCode: club.code || clubCode,
        displayName,
        email,
        phone,
        gender,
        birthDate: birthDate || undefined,
        currentAddress: currentAddress || undefined
      });
      setSent(true);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="auth-page">
      <section className="auth-shell register">
        <AuthVisual />
        <motion.form className="auth-panel" onSubmit={submit} initial={{ opacity: 0, x: 22 }} animate={{ opacity: 1, x: 0 }} transition={{ type: "spring", stiffness: 120, damping: 22 }}>
          <div className="auth-heading">
            <span className="auth-kicker">Tài khoản CLB</span>
            <h1>Xin cấp tài khoản thành viên</h1>
            <p>Nhập mã CLB để gửi yêu cầu. Admin CLB sẽ duyệt và gửi thông tin đăng nhập qua email.</p>
          </div>
          <div className="auth-choice-row">
            <button type="button" className="active">Xin cấp tài khoản CLB</button>
            <button type="button" disabled title="Tạm khóa">Tạo admin/CLB mới</button>
          </div>
          <p className="auth-muted-note">Chức năng tạo admin mới đang tạm khóa. Vui lòng liên hệ quản trị hệ thống.</p>
          {sent ? (
            <div className="auth-success-box">
              <strong>Đã gửi yêu cầu cấp tài khoản</strong>
              <span>Admin của {club?.name || "CLB"} sẽ xét duyệt. Nếu được duyệt, tài khoản và mật khẩu sẽ được gửi về email của bạn.</span>
              <a href="/login">Quay lại đăng nhập</a>
            </div>
          ) : (
            <>
              <label className="auth-field">
                <span>Mã CLB</span>
                <input value={clubCode} onChange={(event) => { setClubCode(event.target.value.toUpperCase()); setClub(null); }} placeholder="TLKC" required />
              </label>
              {error ? <p className="auth-error">{error}</p> : null}
              {!club ? (
                <button className="auth-primary" disabled={busy || !clubCode.trim()}>
                  {busy ? "Đang kiểm tra..." : "Kiểm tra mã CLB"}
                </button>
              ) : (
                <motion.div className="auth-request-step" initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.2 }}>
                  <div className="auth-club-confirm">
                    <strong>{club.name}</strong>
                    <span>{[club.code, club.province, club.address].filter(Boolean).join(" - ")}</span>
                  </div>
                  <div className="auth-grid">
                    <label className="auth-field">
                      <span>Họ tên</span>
                      <input value={displayName} onChange={(event) => setDisplayName(event.target.value)} required />
                    </label>
                    <label className="auth-field">
                      <span>Email nhận tài khoản</span>
                      <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
                    </label>
                    <label className="auth-field">
                      <span>Số điện thoại</span>
                      <input value={phone} onChange={(event) => setPhone(event.target.value)} required />
                    </label>
                    <label className="auth-field">
                      <span>Giới tính</span>
                      <select value={gender} onChange={(event) => setGender(event.target.value)}>
                        <option value="MALE">Nam</option>
                        <option value="FEMALE">Nữ</option>
                        <option value="OTHER">Khác</option>
                      </select>
                    </label>
                    <label className="auth-field">
                      <span>Ngày sinh</span>
                      <input type="date" value={birthDate} onChange={(event) => setBirthDate(event.target.value)} />
                    </label>
                    <label className="auth-field">
                      <span>Địa chỉ</span>
                      <input value={currentAddress} onChange={(event) => setCurrentAddress(event.target.value)} />
                    </label>
                  </div>
                  <button className="auth-primary" disabled={busy || !displayName.trim() || !email.trim() || !phone.trim()}>
                    {busy ? "Đang gửi yêu cầu..." : "Gửi yêu cầu cấp tài khoản"}
                  </button>
                </motion.div>
              )}
            </>
          )}
          <div className="auth-switch">
            <span>Đã có tài khoản?</span>
            <a href="/login">Quay lại đăng nhập</a>
          </div>
        </motion.form>
      </section>
    </main>
  );
}

function AuthVisual() {
  return (
    <motion.aside className="auth-visual" initial={{ opacity: 0, x: -24 }} animate={{ opacity: 1, x: 0 }} transition={{ type: "spring", stiffness: 100, damping: 24 }}>
      <div className="auth-brand-mark">K</div>
      <div className="auth-visual-copy">
        <span className="auth-kicker">Club to Tatami</span>
        <h2>Một nguồn dữ liệu cho CLB, giải đấu và sàn thi đấu.</h2>
        <p>Roster, lịch tập, điểm danh và đăng ký giải nằm chung một luồng để giảm nhập lại và giảm nhầm ngày thi đấu.</p>
      </div>
      <div className="auth-motion-card">
        <motion.span animate={{ x: [0, 116, 116, 0, 0], y: [0, 0, 74, 74, 0] }} transition={{ duration: 7.5, repeat: Infinity, ease: "easeInOut" }} />
        <strong>Dojo sync</strong>
        <small>Lịch tập hôm nay - điểm danh - roster thi đấu</small>
      </div>
      <div className="auth-rhythm">
        <i />
        <i />
        <i />
      </div>
    </motion.aside>
  );
}

function TournamentManagementPage({ user }: { user: AuthUserResponse }) {
  const [organizations, setOrganizations] = useState<OrganizationResponse[]>([]);
  const [tournaments, setTournaments] = useState<TournamentResponse[]>([]);
  const [participants, setParticipants] = useState<TournamentParticipantResponse[]>([]);
  const [tatamis, setTatamis] = useState<TatamiResponse[]>([]);
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [entries, setEntries] = useState<EntryResponse[]>([]);
  const [entryRoster, setEntryRoster] = useState<ClubRosterResponse[]>([]);
  const [selectedTournamentId, setSelectedTournamentId] = useState("");
  const [selectedCategoryId, setSelectedCategoryId] = useState("");
  const [entryParticipantId, setEntryParticipantId] = useState("");
  const [entryAthleteId, setEntryAthleteId] = useState("");
  const [tournamentName, setTournamentName] = useState("");
  const [organizerName, setOrganizerName] = useState("");
  const [tatamiCountDraft, setTatamiCountDraft] = useState("1");
  const [rulesetPreset, setRulesetPreset] = useState("WKF");
  const [ownerOrganizationId, setOwnerOrganizationId] = useState("");
  const [participantOrgId, setParticipantOrgId] = useState("");
  const [tatamiNo, setTatamiNo] = useState("1");
  const [categoryName, setCategoryName] = useState("");
  const [discipline, setDiscipline] = useState("KUMITE");
  const [gender, setGender] = useState("MALE");
  const [entryType, setEntryType] = useState("INDIVIDUAL");
  const [competitionLevel, setCompetitionLevel] = useState("OPEN");
  const [weightMinKg, setWeightMinKg] = useState("");
  const [weightMaxKg, setWeightMaxKg] = useState("");
  const [openWeight, setOpenWeight] = useState(false);
  const [repechageEnabled, setRepechageEnabled] = useState(true);
  const [kataJudgeCount, setKataJudgeCount] = useState("5");
  const [matchDurationSeconds, setMatchDurationSeconds] = useState("180");
  const [teamName, setTeamName] = useState("");
  const [registrationWeightKg, setRegistrationWeightKg] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [activeTab, setActiveTab] = useState<TournamentAdminTab>("setup");
  const [tournamentSearch, setTournamentSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");

  const loadBase = useCallback(async () => {
    const [orgs, events] = await Promise.all([
      apiGet<OrganizationResponse[]>("/api/organizations"),
      apiGet<TournamentResponse[]>("/api/tournaments")
    ]);
    const visibleOrgs = hasRole(user, "GLOBAL_ADMIN") ? orgs : orgs.filter((org) => org.id === user.primaryOrganizationId);
    setOrganizations(visibleOrgs);
    setTournaments(events);
    setOwnerOrganizationId((current) => current || user.primaryOrganizationId || visibleOrgs[0]?.id || "");
    setParticipantOrgId((current) => current || user.primaryOrganizationId || visibleOrgs[0]?.id || "");
    setSelectedTournamentId((current) => current || events[0]?.id || "");
  }, [user]);

  const loadTournamentData = useCallback(async (tournamentId: string) => {
    if (!tournamentId) return;
    const [nextParticipants, nextTatamis, nextCategories] = await Promise.all([
      apiGet<TournamentParticipantResponse[]>(`/api/tournaments/${tournamentId}/participants`),
      apiGet<TatamiResponse[]>(`/api/tournaments/${tournamentId}/tatamis`),
      apiGet<CategoryResponse[]>(`/api/tournaments/${tournamentId}/categories`)
    ]);
    const nextApprovedParticipants = nextParticipants.filter((participant) => participant.status === "APPROVED");
    setParticipants(nextParticipants);
    setTatamis(nextTatamis);
    setCategories(nextCategories);
    setSelectedCategoryId((current) => nextCategories.some((category) => category.id === current) ? current : nextCategories[0]?.id || "");
    setEntryParticipantId((current) => nextApprovedParticipants.some((participant) => participant.id === current) ? current : nextApprovedParticipants[0]?.id || "");
  }, []);

  const loadEntries = useCallback(async (categoryId: string) => {
    if (!categoryId) {
      setEntries([]);
      return;
    }
    setEntries(await apiGet<EntryResponse[]>(`/api/categories/${categoryId}/entries`));
  }, []);

  useEffect(() => {
    loadEntries(selectedCategoryId).catch((err) => setError(errorMessage(err)));
  }, [loadEntries, selectedCategoryId]);

  useEffect(() => {
    const participant = participants.find((item) => item.id === entryParticipantId);
    if (!participant) {
      setEntryRoster([]);
      return;
    }
    apiGet<ClubRosterResponse[]>(`/api/organizations/${participant.organizationId}/roster`)
      .then((data) => {
        const active = data.filter((item) => item.status === "ACTIVE");
        setEntryRoster(active);
        setEntryAthleteId((current) => active.some((item) => item.athleteId === current) ? current : active[0]?.athleteId || "");
      })
      .catch((err) => setError(errorMessage(err)));
  }, [entryParticipantId, participants]);

  useEffect(() => {
    loadBase().catch((err) => setError(errorMessage(err)));
  }, [loadBase]);

  useEffect(() => {
    loadTournamentData(selectedTournamentId).catch((err) => setError(errorMessage(err)));
  }, [loadTournamentData, selectedTournamentId]);

  const selectedTournament = tournaments.find((event) => event.id === selectedTournamentId);
  const selectedCategory = categories.find((category) => category.id === selectedCategoryId);
  const controlQuery = selectedTournamentId && tatamis[0] ? `?tournamentId=${selectedTournamentId}&tatamiId=${tatamis[0].id}` : "";
  const controlHref = controlQuery ? `/control${controlQuery}` : "";
  const dashboardHref = selectedTournamentId ? `/dashboard/tournaments/${selectedTournamentId}` : "";
  const approvedParticipants = participants.filter((participant) => participant.status === "APPROVED");
  const statuses = Array.from(new Set(tournaments.map((event) => event.status).filter(Boolean)));
  const visibleTournaments = tournaments.filter((event) => {
    const keyword = tournamentSearch.trim().toLowerCase();
    const haystack = `${event.name} ${event.code || ""} ${event.ownerOrganizationName || ""} ${event.status}`.toLowerCase();
    return (!keyword || haystack.includes(keyword)) && (statusFilter === "ALL" || event.status === statusFilter);
  });
  const readiness = [
    { label: "Tạo giải", done: Boolean(selectedTournament), detail: selectedTournament?.status || "Chưa chọn giải" },
    { label: "Duyệt đoàn", done: approvedParticipants.length > 0, detail: `${approvedParticipants.length}/${participants.length} đoàn approved` },
    { label: "Tatami", done: tatamis.length > 0, detail: `${tatamis.length} sàn thi đấu` },
    { label: "Hạng mục", done: categories.length > 0, detail: `${categories.length} category` },
    { label: "Đăng ký", done: entries.length > 0, detail: selectedCategory ? `${entries.length} entries trong ${selectedCategory.name}` : "Chọn hạng mục" }
  ];
  const completedSetup = readiness.filter((step) => step.done).length;
  const tournamentTabs: Array<{ id: TournamentAdminTab; label: string; hint: string; icon: ReactNode }> = [
    { id: "setup", label: "Setup", hint: "Tạo giải và checklist", icon: <Settings2 /> },
    { id: "delegations", label: "Đoàn", hint: `${participants.length} hồ sơ`, icon: <Users /> },
    { id: "tatamis", label: "Tatami", hint: `${tatamis.length} sàn`, icon: <Monitor /> },
    { id: "categories", label: "Hạng mục", hint: `${categories.length} category`, icon: <Swords /> },
    { id: "entries", label: "Entries", hint: `${entries.length} VĐV`, icon: <ListChecks /> }
  ];

  const chooseTournament = (eventId: string) => {
    setSelectedTournamentId(eventId);
    setSelectedCategoryId("");
    setEntryParticipantId("");
    setEntryAthleteId("");
    setEntryRoster([]);
    setEntries([]);
    setError(null);
  };

  const submit = async (event: FormEvent, action: () => Promise<void>) => {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await action();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="tournament-admin-page">
      <aside className="tournament-admin-sidebar">
        <div className="tournament-sidebar-head">
          <a className="tournament-mark" href="/app">K</a>
          <div>
            <span>Karate Ops</span>
            <h1>Quản lý giải</h1>
          </div>
        </div>

        <nav className="tournament-sidebar-nav">
          <a href="/app"><Gauge /> Home</a>
          <a href="/clubs"><Shield /> CLB</a>
          <a className="active" href="/tournaments"><Trophy /> Tournament</a>
        </nav>

        <div className="tournament-sidebar-tools">
          <label className="tournament-search">
            <Search />
            <input value={tournamentSearch} onChange={(event) => setTournamentSearch(event.target.value)} placeholder="Tìm giải, mã, đơn vị" />
          </label>
          <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
            <option value="ALL">Tất cả trạng thái</option>
            {statuses.map((status) => <option key={status} value={status}>{status}</option>)}
          </select>
        </div>

        <form className="tournament-create-form" onSubmit={(event) => submit(event, async () => {
          const created = await apiPost<TournamentResponse>("/api/tournaments", {
            name: tournamentName,
            code: slugCode(tournamentName),
            ownerOrganizationId,
            visibility: "PUBLIC",
            status: "DRAFT",
            rulesetVersion: "WKF_2026",
            organizerName,
            tatamiCount: Number(tatamiCountDraft) || 1,
            competitionLevels: ["PHONG_TRAO", "NANG_CAO"],
            rulesetPreset
          });
          setTournamentName("");
          setOrganizerName("");
          setTatamiCountDraft("1");
          await loadBase();
          chooseTournament(created.id);
        })}>
          <div className="tournament-panel-head compact">
            <div>
              <span>Tạo giải mới</span>
              <strong>Khởi tạo hồ sơ tournament</strong>
            </div>
            <Plus />
          </div>
          <label className="tournament-field">
            <span>Tên giải</span>
            <input value={tournamentName} onChange={(event) => setTournamentName(event.target.value)} placeholder="VD: Hanoi Open 2026" required />
          </label>
          <label className="tournament-field">
            <span>Đơn vị tổ chức</span>
            <input value={organizerName} onChange={(event) => setOrganizerName(event.target.value)} placeholder="BTC / Liên đoàn / CLB chủ nhà" />
          </label>
          <div className="tournament-form-grid">
            <label className="tournament-field">
              <span>Số tatami dự kiến</span>
              <input type="number" min="1" value={tatamiCountDraft} onChange={(event) => setTatamiCountDraft(event.target.value)} />
            </label>
            <label className="tournament-field">
              <span>Preset luật</span>
              <select value={rulesetPreset} onChange={(event) => setRulesetPreset(event.target.value)}>
                <option>WKF</option>
                <option>PHONG_TRAO</option>
                <option>NANG_CAO</option>
                <option>CUSTOM</option>
              </select>
            </label>
          </div>
          <label className="tournament-field">
            <span>Đơn vị chủ quản</span>
            <select value={ownerOrganizationId} onChange={(event) => setOwnerOrganizationId(event.target.value)}>
              {organizations.map((org) => <option key={org.id} value={org.id}>{org.name}</option>)}
            </select>
          </label>
          <button className="tournament-primary-button" disabled={busy || !tournamentName.trim() || !ownerOrganizationId}>Tạo giải</button>
        </form>

        <div className="tournament-list">
          {visibleTournaments.length === 0 ? (
            <TournamentEmptyState icon={<Trophy />} title="Chưa có giải phù hợp" text="Tạo giải mới hoặc bỏ lọc để xem toàn bộ danh sách." />
          ) : visibleTournaments.map((event) => (
            <button key={event.id} className={cx("tournament-list-row", event.id === selectedTournamentId && "active")} onClick={() => chooseTournament(event.id)}>
              <span className={cx("tournament-status-dot", event.status.toLowerCase())} />
              <div>
                <strong>{event.name}</strong>
                <small>{event.ownerOrganizationName || "Global"} / {event.code || slugCode(event.name)}</small>
              </div>
              <b>{event.status}</b>
            </button>
          ))}
        </div>
      </aside>

      <section className="tournament-admin-main">
        <header className="tournament-command-bar">
          <div className="tournament-command-title">
            <span className="tournament-kicker">WKF operations console</span>
            <h2>{selectedTournament?.name || "Chọn hoặc tạo một giải đấu"}</h2>
            <p>{selectedTournament ? `${selectedTournament.ownerOrganizationName || "Global"} / ${selectedTournament.organizerName || "BTC chưa đặt"} / ${selectedTournament.rulesetPreset || "WKF"} / ${selectedTournament.tatamiCount || 1} tatami` : "Từ màn này có thể chuẩn bị dữ liệu để dashboard và tatami control sử dụng ngay."}</p>
          </div>
          <div className="tournament-command-actions">
            <a className={cx("tournament-secondary-button", !dashboardHref && "disabled")} href={dashboardHref || "#"}><History /> Dashboard</a>
            <a className={cx("tournament-secondary-button", !selectedTournamentId && "disabled")} href={selectedTournamentId ? `/api/tournaments/${selectedTournamentId}/exports/entries.csv` : "#"}><Download /> Entries</a>
            <a className={cx("tournament-secondary-button", !selectedTournamentId && "disabled")} href={selectedTournamentId ? `/api/tournaments/${selectedTournamentId}/exports/schedule.csv` : "#"}><Download /> Lịch</a>
            <a className={cx("tournament-secondary-button", !controlHref && "disabled")} href={controlHref || "#"}><Monitor /> Tatami đầu tiên</a>
            <button className="tournament-primary-button" disabled={!selectedTournamentId || busy} onClick={() => submitNoEvent(async () => {
              await apiPatch<TournamentResponse>(`/api/tournaments/${selectedTournamentId}`, { status: selectedTournament?.status === "REGISTRATION_OPEN" ? "DRAFT" : "REGISTRATION_OPEN" });
              await loadBase();
            }, setBusy, setError)}>
              <Flag /> {selectedTournament?.status === "REGISTRATION_OPEN" ? "Về draft" : "Mở đăng ký"}
            </button>
            <button className="tournament-ghost-button" onClick={() => {
              setAuthToken(null);
              window.location.href = "/login";
            }}>Logout</button>
          </div>
        </header>

        {error ? <div className="tournament-error"><AlertTriangle /> {error}</div> : null}

        <section className="tournament-metric-strip">
          <TournamentMetric icon={<Users />} label="Delegations" value={`${approvedParticipants.length}/${participants.length}`} />
          <TournamentMetric icon={<Monitor />} label="Tatamis" value={tatamis.length} />
          <TournamentMetric icon={<Swords />} label="Categories" value={categories.length} />
          <TournamentMetric icon={<Award />} label="Entries" value={entries.length} />
          <TournamentMetric icon={<CircleDot />} label="Ready" value={`${completedSetup}/5`} />
        </section>

        <section className="tournament-readiness">
          {readiness.map((step, index) => (
            <div key={step.label} className={cx("tournament-readiness-step", step.done && "done")}>
              <span>{String(index + 1).padStart(2, "0")}</span>
              <strong>{step.label}</strong>
              <small>{step.detail}</small>
            </div>
          ))}
        </section>

        <nav className="tournament-tab-list">
          {tournamentTabs.map((tab) => (
            <button key={tab.id} className={cx(activeTab === tab.id && "active")} onClick={() => setActiveTab(tab.id)}>
              {tab.icon}
              <span>{tab.label}</span>
              <small>{tab.hint}</small>
            </button>
          ))}
        </nav>

        <section className="tournament-work-panel">
          {activeTab === "setup" ? (
            <div className="tournament-two-column">
              <section className="tournament-work-section">
                <div className="tournament-panel-head">
                  <div>
                    <span>Checklist vận hành</span>
                    <h3>Chuẩn bị giải trước khi mở tatami</h3>
                  </div>
                  <TournamentStatusBadge value={selectedTournament?.status || "NO TOURNAMENT"} />
                </div>
                <div className="tournament-check-list">
                  {readiness.map((step) => (
                    <div key={step.label} className={cx("tournament-check-row", step.done && "done")}>
                      <CircleDot />
                      <div>
                        <strong>{step.label}</strong>
                        <span>{step.detail}</span>
                      </div>
                    </div>
                  ))}
                </div>
              </section>
              <section className="tournament-work-section emphasis">
                <div className="tournament-panel-head">
                  <div>
                    <span>Command links</span>
                    <h3>Điều phối nhanh</h3>
                  </div>
                  <Eye />
                </div>
                <a className={cx("tournament-wide-action", !dashboardHref && "disabled")} href={dashboardHref || "#"}>Mở dashboard giải</a>
                <a className={cx("tournament-wide-action", !controlHref && "disabled")} href={controlHref || "#"}>Mở control tatami đầu tiên</a>
                <p className="tournament-helper-text">Cần có ít nhất một tatami để mở bàn điều khiển đầu tiên.</p>
              </section>
            </div>
          ) : null}

          {activeTab === "delegations" ? (
            <div className="tournament-two-column">
              <form className="tournament-work-section" onSubmit={(event) => submit(event, async () => {
                await apiPost<TournamentParticipantResponse>(`/api/tournaments/${selectedTournamentId}/participants`, {
                  organizationId: participantOrgId,
                  status: "APPROVED"
                });
                await loadTournamentData(selectedTournamentId);
              })}>
                <div className="tournament-panel-head">
                  <div>
                    <span>Thêm đoàn</span>
                    <h3>Duyệt CLB vào giải</h3>
                  </div>
                  <Users />
                </div>
                <label className="tournament-field">
                  <span>CLB / tổ chức</span>
                  <select value={participantOrgId} onChange={(event) => setParticipantOrgId(event.target.value)}>
                    {organizations.map((org) => <option key={org.id} value={org.id}>{org.name}</option>)}
                  </select>
                </label>
                <button className="tournament-primary-button" disabled={busy || !selectedTournamentId || !participantOrgId}>Duyệt đoàn</button>
              </form>
              <section className="tournament-work-section">
                <div className="tournament-panel-head">
                  <div>
                    <span>Danh sách đoàn</span>
                    <h3>{participants.length} hồ sơ tham gia</h3>
                  </div>
                  <TournamentStatusBadge value={`${approvedParticipants.length} APPROVED`} />
                </div>
                <div className="tournament-data-list">
                  {participants.length === 0 ? <TournamentEmptyState icon={<Users />} title="Chưa có đoàn tham gia" text="Chọn tổ chức ở bên trái để duyệt đoàn đầu tiên." /> : participants.map((participant) => (
                    <div key={participant.id} className="tournament-data-row">
                      <div>
                        <strong>{participant.displayName || participant.organizationName}</strong>
                        <span>{participant.organizationName}</span>
                      </div>
                      <TournamentStatusBadge value={participant.status} />
                      <button className={cx("tournament-text-button", participant.status === "APPROVED" && "danger")} disabled={busy} onClick={() => submitNoEvent(async () => {
                        await apiPatch<TournamentParticipantResponse>(`/api/tournaments/${selectedTournamentId}/participants/${participant.id}/status`, {
                          status: participant.status === "APPROVED" ? "WITHDRAWN" : "APPROVED"
                        });
                        await loadTournamentData(selectedTournamentId);
                      }, setBusy, setError)}>{participant.status === "APPROVED" ? "Rút đoàn" : "Approve"}</button>
                    </div>
                  ))}
                </div>
              </section>
            </div>
          ) : null}

          {activeTab === "tatamis" ? (
            <div className="tournament-two-column">
              <form className="tournament-work-section" onSubmit={(event) => submit(event, async () => {
                const no = Number(tatamiNo);
                await apiPost<TatamiResponse>(`/api/tournaments/${selectedTournamentId}/tatamis`, {
                  tatamiNo: no,
                  name: `Tatami ${no}`,
                  status: "ACTIVE"
                });
                setTatamiNo(String(no + 1));
                await loadTournamentData(selectedTournamentId);
              })}>
                <div className="tournament-panel-head">
                  <div>
                    <span>Thêm tatami</span>
                    <h3>Mở sàn thi đấu</h3>
                  </div>
                  <Monitor />
                </div>
                <label className="tournament-field">
                  <span>Số tatami</span>
                  <input type="number" min="1" value={tatamiNo} onChange={(event) => setTatamiNo(event.target.value)} required />
                </label>
                <button className="tournament-primary-button" disabled={busy || !selectedTournamentId}>Tạo tatami</button>
              </form>
              <section className="tournament-work-section">
                <div className="tournament-panel-head">
                  <div>
                    <span>Điều phối sàn</span>
                    <h3>{tatamis.length} tatami active</h3>
                  </div>
                  <Clock />
                </div>
                <div className="tournament-data-list">
                  {tatamis.length === 0 ? <TournamentEmptyState icon={<Monitor />} title="Chưa có tatami" text="Tạo tatami để mở control và phân trận." /> : tatamis.map((tatami) => (
                    <div key={tatami.id} className="tournament-data-row">
                      <div>
                        <strong>{tatami.name || `Tatami ${tatami.tatamiNo}`}</strong>
                        <span>{tatami.currentMatchId ? "Đã có trận hiện tại" : "Chưa gán trận hiện tại"}</span>
                      </div>
                      <TournamentStatusBadge value={tatami.status} />
                      <a className="tournament-text-button" href={`/control?tournamentId=${selectedTournamentId}&tatamiId=${tatami.id}`}>Control</a>
                    </div>
                  ))}
                </div>
              </section>
            </div>
          ) : null}

          {activeTab === "categories" ? (
            <div className="tournament-two-column">
              <form className="tournament-work-section" onSubmit={(event) => submit(event, async () => {
                await apiPost<CategoryResponse>(`/api/tournaments/${selectedTournamentId}/categories`, {
                  name: categoryName,
                  discipline,
                  gender,
                  entryType,
                  status: "DRAFT",
                  rulesetVersion: "WKF_2026",
                  competitionLevel,
                  weightMinKg: weightMinKg ? Number(weightMinKg) : null,
                  weightMaxKg: weightMaxKg ? Number(weightMaxKg) : null,
                  openWeight: openWeight || (!weightMinKg && !weightMaxKg && discipline.includes("KUMITE")),
                  repechageEnabled,
                  kataJudgeCount: Number(kataJudgeCount) || 5,
                  matchDurationSeconds: Number(matchDurationSeconds) || 180
                });
                setCategoryName("");
                setWeightMinKg("");
                setWeightMaxKg("");
                setOpenWeight(false);
                await loadTournamentData(selectedTournamentId);
              })}>
                <div className="tournament-panel-head">
                  <div>
                    <span>Tạo hạng mục</span>
                    <h3>Category thi đấu</h3>
                  </div>
                  <Swords />
                </div>
                <label className="tournament-field">
                  <span>Tên hạng mục</span>
                  <input value={categoryName} onChange={(event) => setCategoryName(event.target.value)} placeholder="Male Kumite U14 -45kg" required />
                </label>
                <div className="tournament-form-grid">
                  <label className="tournament-field">
                    <span>Nội dung</span>
                    <select value={discipline} onChange={(event) => setDiscipline(event.target.value)}>
                      <option>KUMITE</option>
                      <option>KATA</option>
                      <option>TEAM_KUMITE</option>
                      <option>TEAM_KATA</option>
                    </select>
                  </label>
                  <label className="tournament-field">
                    <span>Giới tính</span>
                    <select value={gender} onChange={(event) => setGender(event.target.value)}>
                      <option>MALE</option>
                      <option>FEMALE</option>
                      <option>MIXED</option>
                      <option>OPEN</option>
                    </select>
                  </label>
                </div>
                <div className="tournament-form-grid">
                  <label className="tournament-field">
                    <span>Loại đăng ký</span>
                    <select value={entryType} onChange={(event) => setEntryType(event.target.value)}>
                      <option>INDIVIDUAL</option>
                      <option>TEAM</option>
                    </select>
                  </label>
                  <label className="tournament-field">
                    <span>Trình độ</span>
                    <select value={competitionLevel} onChange={(event) => setCompetitionLevel(event.target.value)}>
                      <option>OPEN</option>
                      <option>PHONG_TRAO</option>
                      <option>NANG_CAO</option>
                    </select>
                  </label>
                </div>
                <div className="tournament-form-grid">
                  <label className="tournament-field">
                    <span>Cân từ kg</span>
                    <input type="number" step="0.1" value={weightMinKg} onChange={(event) => setWeightMinKg(event.target.value)} placeholder="Trống nếu không giới hạn" />
                  </label>
                  <label className="tournament-field">
                    <span>Cân đến kg</span>
                    <input type="number" step="0.1" value={weightMaxKg} onChange={(event) => setWeightMaxKg(event.target.value)} placeholder="VD 67" />
                  </label>
                </div>
                <div className="tournament-form-grid">
                  <label className="tournament-field inline-check">
                    <input type="checkbox" checked={openWeight} onChange={(event) => setOpenWeight(event.target.checked)} />
                    <span>Vô địch tuyệt đối</span>
                  </label>
                  <label className="tournament-field inline-check">
                    <input type="checkbox" checked={repechageEnabled} onChange={(event) => setRepechageEnabled(event.target.checked)} />
                    <span>Repechage / 2 HCĐ</span>
                  </label>
                </div>
                <div className="tournament-form-grid">
                  <label className="tournament-field">
                    <span>Thời gian Kumite giây</span>
                    <input type="number" min="30" value={matchDurationSeconds} onChange={(event) => setMatchDurationSeconds(event.target.value)} />
                  </label>
                  <label className="tournament-field">
                    <span>Số trọng tài Kata</span>
                    <select value={kataJudgeCount} onChange={(event) => setKataJudgeCount(event.target.value)}>
                      <option>5</option>
                      <option>7</option>
                    </select>
                  </label>
                </div>
                <button className="tournament-primary-button" disabled={busy || !selectedTournamentId || !categoryName.trim()}>Tạo hạng mục</button>
              </form>
              <section className="tournament-work-section">
                <div className="tournament-panel-head">
                  <div>
                    <span>Danh sách hạng mục</span>
                    <h3>Chọn category để quản lý entries</h3>
                  </div>
                  <TournamentStatusBadge value={`${categories.length} CATEGORY`} />
                </div>
                <div className="tournament-data-list">
                  {categories.length === 0 ? <TournamentEmptyState icon={<Swords />} title="Chưa có hạng mục" text="Tạo category trước khi đăng ký vận động viên." /> : categories.map((category) => {
                    const isSelected = category.id === selectedCategoryId;
                    const drawDisabled = busy || !isSelected || entries.length < 2;
                    return (
                      <div key={category.id} className={cx("tournament-data-row", isSelected && "selected")}>
                        <div>
                          <strong>{category.name}</strong>
                          <span>{category.discipline} / {category.entryType || "INDIVIDUAL"} / {category.competitionLevel || "OPEN"} / {category.openWeight ? "Vo dich tuyet doi" : category.weightLabel || "No weight"}</span>
                        </div>
                        <TournamentStatusBadge value={category.status} />
                        <button className="tournament-text-button" onClick={() => setSelectedCategoryId(category.id)}>Chọn</button>
                        <button className="tournament-secondary-button compact" disabled={drawDisabled} title={!isSelected ? "Chọn category trước khi bốc thăm" : entries.length < 2 ? "Cần ít nhất 2 entries" : "Bốc thăm"} onClick={() => submitNoEvent(async () => {
                          await apiPost(`/api/categories/${category.id}/draw`, { bracketType: category.repechageEnabled ? "REPECHAGE" : "SINGLE_ELIMINATION", shuffle: false, enableRepechage: category.repechageEnabled });
                          await loadTournamentData(selectedTournamentId);
                        }, setBusy, setError)}>Draw</button>
                      </div>
                    );
                  })}
                </div>
              </section>
            </div>
          ) : null}

          {activeTab === "entries" ? (
            <div className="tournament-two-column">
              <form className="tournament-work-section" onSubmit={(event) => submit(event, async () => {
                await apiPost<EntryResponse>(`/api/categories/${selectedCategoryId}/entries`, {
                  tournamentParticipantId: entryParticipantId,
                  athleteId: entryAthleteId,
                  status: "REGISTERED",
                  registrationWeightKg: registrationWeightKg ? Number(registrationWeightKg) : null,
                  teamName: selectedCategory?.entryType === "TEAM" ? teamName : null,
                  teamMemberAthleteIds: selectedCategory?.entryType === "TEAM" ? [entryAthleteId] : []
                });
                setTeamName("");
                setRegistrationWeightKg("");
                await loadEntries(selectedCategoryId);
              })}>
                <div className="tournament-panel-head">
                  <div>
                    <span>Đăng ký VĐV</span>
                    <h3>{selectedCategory?.name || "Chọn hạng mục"}</h3>
                  </div>
                  <Award />
                </div>
                <label className="tournament-field">
                  <span>Hạng mục</span>
                  <select value={selectedCategoryId} onChange={(event) => setSelectedCategoryId(event.target.value)}>
                    {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
                  </select>
                </label>
                <label className="tournament-field">
                  <span>Đoàn</span>
                  <select value={entryParticipantId} onChange={(event) => setEntryParticipantId(event.target.value)}>
                    {approvedParticipants.map((participant) => (
                      <option key={participant.id} value={participant.id}>{participant.displayName || participant.organizationName}</option>
                    ))}
                  </select>
                </label>
                <label className="tournament-field">
                  <span>Vận động viên</span>
                  <select value={entryAthleteId} onChange={(event) => setEntryAthleteId(event.target.value)}>
                    {entryRoster.map((item) => <option key={item.id} value={item.athleteId}>{item.athleteName}</option>)}
                  </select>
                </label>
                {selectedCategory?.entryType === "TEAM" ? (
                  <label className="tournament-field">
                    <span>Tên đội</span>
                    <input value={teamName} onChange={(event) => setTeamName(event.target.value)} placeholder="VD: Red Club Team A" />
                  </label>
                ) : null}
                <label className="tournament-field">
                  <span>Cân đăng ký kg</span>
                  <input type="number" step="0.1" value={registrationWeightKg} onChange={(event) => setRegistrationWeightKg(event.target.value)} placeholder="Để trống dùng cân hồ sơ VĐV" />
                </label>
                <button className="tournament-primary-button" disabled={busy || !selectedCategoryId || !entryParticipantId || !entryAthleteId}>Tạo entry</button>
                {!approvedParticipants.length ? <p className="tournament-helper-text">Cần duyệt ít nhất một đoàn trước khi đăng ký VĐV.</p> : null}
              </form>
              <section className="tournament-work-section">
                <div className="tournament-panel-head">
                  <div>
                    <span>Entries hiện tại</span>
                    <h3>{selectedCategory?.name || "Chưa chọn category"}</h3>
                  </div>
                  <TournamentStatusBadge value={`${entries.length} ENTRIES`} />
                </div>
                <div className="tournament-data-list">
                  {!selectedCategory ? <TournamentEmptyState icon={<ListChecks />} title="Chưa chọn hạng mục" text="Chọn một category để xem và thêm entries." /> : entries.length === 0 ? <TournamentEmptyState icon={<Award />} title="Chưa có entry" text="Chọn đoàn và VĐV ở bên trái để đăng ký." /> : entries.map((entry) => (
                    <div key={entry.id} className="tournament-data-row">
                      <div>
                        <strong>{entry.teamName || entry.athleteName || entry.participantName}</strong>
                        <span>{entry.participantName} / {entry.weighInStatus || "REVIEW"} / {entry.validationNotes || `seed ${entry.seedNo || "auto"}`}</span>
                      </div>
                      <TournamentStatusBadge value={entry.status} />
                      <button className="tournament-text-button danger" disabled={busy || selectedCategory?.status === "DRAWN"} onClick={() => submitNoEvent(async () => {
                        await apiDelete(`/api/categories/${selectedCategoryId}/entries/${entry.id}`);
                        await loadEntries(selectedCategoryId);
                      }, setBusy, setError)}>Xóa</button>
                    </div>
                  ))}
                </div>
              </section>
            </div>
          ) : null}
        </section>
      </section>
    </main>
  );
}

function TournamentMetric({ icon, label, value }: { icon: ReactNode; label: string; value: number | string }) {
  return (
    <div className="tournament-metric">
      <span>{icon}</span>
      <div>
        <small>{label}</small>
        <strong>{value}</strong>
      </div>
    </div>
  );
}

function TournamentStatusBadge({ value }: { value: string }) {
  return <span className={cx("tournament-status-badge", value.toLowerCase().replace(/[^a-z0-9]+/g, "-"))}>{value}</span>;
}

function TournamentEmptyState({ icon, title, text }: { icon: ReactNode; title: string; text: string }) {
  return (
    <div className="tournament-empty-state">
      <span>{icon}</span>
      <strong>{title}</strong>
      <p>{text}</p>
    </div>
  );
}

function ManagerHeader({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <header className="control-topbar manager-topbar">
      <div className="topbar-title">
        <div className="app-mark small">K</div>
        <div>
          <h1>{title}</h1>
          <p>{subtitle}</p>
        </div>
      </div>
      <div className="topbar-actions">
        <a className="text-button" href="/app">Home</a>
        <a className="text-button" href="/clubs">CLB</a>
        <a className="text-button" href="/tournaments">Tournament</a>
        <button className="text-button" onClick={() => {
          setAuthToken(null);
          window.location.href = "/login";
        }}>Logout</button>
      </div>
    </header>
  );
}

function TournamentHero({
  title,
  subtitle,
  status,
  primaryHref,
  primaryLabel,
  secondaryHref,
  secondaryLabel,
  stats
}: {
  title: string;
  subtitle: string;
  status: string;
  primaryHref?: string;
  primaryLabel: string;
  secondaryHref?: string;
  secondaryLabel?: string;
  stats: Array<[string, number | string]>;
}) {
  return (
    <section className="tournament-hero">
      <div className="tournament-hero-copy">
        <span className="landing-kicker">WKF operations console</span>
        <h2>{title}</h2>
        <p>{subtitle}</p>
        <div className="tournament-hero-actions">
          <a className={cx("primary-button", !primaryHref && "disabled")} href={primaryHref || "#"}>{primaryLabel}</a>
          {secondaryLabel ? <a className={cx("secondary-button", !secondaryHref && "disabled")} href={secondaryHref || "#"}>{secondaryLabel}</a> : null}
        </div>
      </div>
      <div className="tournament-hero-board">
        <div className="tournament-status-orbit">
          <span>{status}</span>
          <strong>WKF</strong>
        </div>
        <div className="tournament-stat-grid">
          {stats.map(([label, value]) => (
            <div className="tournament-stat" key={label}>
              <span>{label}</span>
              <strong>{value}</strong>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function TournamentFlow({ active }: { active: number }) {
  const steps = ["Create", "Delegations", "Tatamis", "Categories", "Entries"];
  return (
    <section className="tournament-flow">
      {steps.map((step, index) => (
        <div key={step} className={cx("tournament-flow-step", index < active && "done", index === active && "current")}>
          <span>{String(index + 1).padStart(2, "0")}</span>
          <strong>{step}</strong>
        </div>
      ))}
    </section>
  );
}

function TournamentDashboardPage({ user }: { user: AuthUserResponse }) {
  const tournamentId = window.location.pathname.split("/").filter(Boolean).at(-1) || "";
  const [overview, setOverview] = useState<DashboardOverviewResponse | null>(null);
  const [medals, setMedals] = useState<MedalTableRow[]>([]);
  const [tatamis, setTatamis] = useState<TatamiDashboardRow[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([
      apiGet<DashboardOverviewResponse>(`/api/dashboard/tournaments/${tournamentId}/overview`),
      apiGet<MedalTableRow[]>(`/api/dashboard/tournaments/${tournamentId}/medals`),
      apiGet<TatamiDashboardRow[]>(`/api/dashboard/tournaments/${tournamentId}/tatamis`)
    ])
      .then(([nextOverview, nextMedals, nextTatamis]) => {
        setOverview(nextOverview);
        setMedals(nextMedals);
        setTatamis(nextTatamis);
      })
      .catch((err) => setError(errorMessage(err)));
  }, [tournamentId]);

  return (
    <main className="manager-page tournament-page dashboard">
      <ManagerHeader title="Tournament Dashboard" subtitle={`Live tournament operations / ${user.displayName}`} />
      <TournamentHero
        title="Live tournament command"
        subtitle="Theo dõi tiến độ trận, tatami đang chạy và bảng huy chương trong cùng một màn điều phối."
        status={overview ? `${overview.completedMatches}/${overview.matches} completed` : "Loading"}
        primaryHref="/tournaments"
        primaryLabel="Quản lý giải"
        stats={[
          ["Organizations", overview?.organizations ?? 0],
          ["Athletes", overview?.athletes ?? 0],
          ["Categories", overview?.categories ?? 0],
          ["Tatamis", tatamis.length]
        ]}
      />
      <section className="manager-grid">
        <section className="manager-panel span-2">
          <PanelTitle title="Overview" value={tournamentId.slice(0, 8)} />
          <div className="metric-grid">
            <MetricCard label="Organizations" value={overview?.organizations ?? 0} />
            <MetricCard label="Athletes" value={overview?.athletes ?? 0} />
            <MetricCard label="Categories" value={overview?.categories ?? 0} />
            <MetricCard label="Completed" value={`${overview?.completedMatches ?? 0}/${overview?.matches ?? 0}`} />
          </div>
          <div className="manager-list">
            {Object.entries(overview?.matchesByStatus || {}).map(([status, count]) => (
              <div className="manager-row" key={status}><strong>{status}</strong><span>{count} matches</span></div>
            ))}
          </div>
        </section>
        <section className="manager-panel">
          <PanelTitle title="Tatamis" value={`${tatamis.length}`} />
          <div className="manager-list tall">
            {tatamis.map((tatami) => (
              <div className="manager-row" key={tatami.tatamiId}>
                <div>
                  <strong>{tatami.name || `Tatami ${tatami.tatamiNo}`}</strong>
                  <span>{tatami.running} running / {tatami.completed} completed / {tatami.scheduled} scheduled</span>
                </div>
              </div>
            ))}
          </div>
        </section>
        <section className="manager-panel span-2">
          <PanelTitle title="Medals" value={`${medals.length}`} />
          <div className="manager-list tall">
            {medals.map((row) => (
              <div className="manager-row" key={row.organizationId}>
                <strong>{row.organizationName}</strong>
                <span>G {row.gold} / S {row.silver} / B {row.bronze} / Total {row.total}</span>
              </div>
            ))}
          </div>
          {error ? <p className="error-text">{error}</p> : null}
        </section>
      </section>
    </main>
  );
}

function PanelTitle({ title, value }: { title: string; value: string }) {
  return (
    <div className="panel-heading strong">
      <span>{title}</span>
      <span className="status-mini">{value}</span>
    </div>
  );
}

function MetricCard({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="metric-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

async function markAttendance(sessionId: string, athleteId: string, record: AttendanceRecordResponse | undefined, status: string) {
  const body = {
    athleteId,
    status,
    checkInAt: status === "PRESENT" || status === "LATE" ? new Date().toISOString() : undefined
  };
  if (record) {
    await apiPatch<AttendanceRecordResponse>(`/api/attendance-sessions/${sessionId}/records/${record.id}`, body);
  } else {
    await apiPost<AttendanceRecordResponse>(`/api/attendance-sessions/${sessionId}/records`, body);
  }
}

async function submitNoEvent(action: () => Promise<void>, setBusy: (value: boolean) => void, setError: (value: string | null) => void) {
  setBusy(true);
  setError(null);
  try {
    await action();
  } catch (err) {
    setError(errorMessage(err));
  } finally {
    setBusy(false);
  }
}

function attendancePercent(records: AttendanceRecordResponse[]) {
  if (!records.length) return 0;
  const attended = records.filter((record) => record.status === "PRESENT" || record.status === "LATE").length;
  return Math.round((attended / records.length) * 100);
}

function slugCode(value: string) {
  return value
    .trim()
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "")
    .slice(0, 32) || `KO_${Date.now()}`;
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : String(error);
}

function hasRole(user: AuthUserResponse, role: string) {
  return user.roles.includes(role);
}

function effectiveRoleUser(user: AuthUserResponse, viewAsRole: string): AuthUserResponse {
  const baseRoles = Array.from(new Set([...user.roles, "MEMBER"])).sort();
  if (!user.roles.includes("GLOBAL_ADMIN") || viewAsRole === "ACTUAL") {
    return { ...user, roles: baseRoles };
  }
  if (viewAsRole === "MEMBER") {
    return { ...user, roles: ["MEMBER"] };
  }
  if (viewAsRole === "CLUB_MANAGER") {
    return { ...user, roles: ["CLUB_MANAGER", "MEMBER"] };
  }
  if (viewAsRole === "GLOBAL_ADMIN") {
    return { ...user, roles: ["GLOBAL_ADMIN", "MEMBER"] };
  }
  return { ...user, roles: baseRoles };
}

function canManageClub(user: AuthUserResponse) {
  return hasRole(user, "GLOBAL_ADMIN") || hasRole(user, "CLUB_MANAGER") || hasRole(user, "COACH");
}

function canManageTournament(user: AuthUserResponse) {
  return hasRole(user, "GLOBAL_ADMIN") || hasRole(user, "CLUB_MANAGER") || hasRole(user, "TOURNAMENT_OWNER");
}

function TatamiRequiredPage() {
  return (
    <main className="center-shell">
      <section className="home-card">
        <div className="home-head">
          <div className="app-mark">K</div>
          <div>
            <h1>Select Tatami</h1>
            <p>Open Karate Ops home and choose a tournament and tatami first.</p>
          </div>
        </div>
        <a className="primary-button" href="/">Back to selector</a>
      </section>
    </main>
  );
}

function AssignMatchPage({ refresh }: { refresh: () => Promise<void> }) {
  const params = new URLSearchParams(window.location.search);
  const tournamentId = params.get("tournamentId") || "";
  const tatamiId = params.get("tatamiId") || "";
  const [matches, setMatches] = useState<MatchResponse[]>([]);
  const [matchId, setMatchId] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!tournamentId) return;
    apiGet<MatchResponse[]>(`/api/tournaments/${tournamentId}/matches`)
      .then((data) => {
        const assignable = data.filter((match) => !["LOCKED", "COMPLETED", "CANCELLED"].includes(match.status));
        setMatches(assignable);
        setMatchId(assignable[0]?.id || "");
      })
      .catch((err) => setError(err instanceof Error ? err.message : String(err)));
  }, [tournamentId]);

  const assign = async () => {
    try {
      await apiPost<MatchResponse>(`/api/tatamis/${tatamiId}/assign-match`, { matchId });
      await refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  };

  return (
    <main className="center-shell">
      <section className="home-card">
        <div className="home-head">
          <div className="app-mark">K</div>
          <div>
            <h1>No Current Match</h1>
            <p>Assign a tournament match to this tatami to start control/display sync.</p>
          </div>
        </div>
        <label className="field">
          <span>Match</span>
          <select value={matchId} onChange={(event) => setMatchId(event.target.value)}>
            {matches.map((match) => (
              <option key={match.id} value={match.id}>{match.categoryName} / {match.roundName || match.status} / M-{match.matchNumber || match.id.slice(0, 8)}</option>
            ))}
          </select>
        </label>
        {error ? <p className="error-text">{error}</p> : null}
        <button className="primary-button" disabled={!matchId} onClick={assign}>Assign match</button>
      </section>
    </main>
  );
}

function DisplayPage({ payload }: { payload: StatePayload }) {
  return (
    <main className="display-page">
      <ScoreboardFrame payload={payload} variant="display" />
    </main>
  );
}

function OverlayPage({ payload }: { payload: StatePayload }) {
  const { match } = payload;
  const now = useNow(120);
  const remaining = match.mode === "kata"
    ? liveRemaining(match.kata.countdown, payload.receivedAt, now)
    : liveRemaining(match.timer, payload.receivedAt, now);
  return (
    <main className="overlay-page">
      <motion.section className="overlay-strip" initial={{ y: 20, opacity: 0 }} animate={{ y: 0, opacity: 1 }}>
        <OverlaySide side="aka" competitor={match.competitors.aka} mode={match.mode} />
        <div className="overlay-center">
          <div className="overlay-timer">{formatMs(remaining)}</div>
          <div className="overlay-meta">{match.tatami} / {match.category}</div>
        </div>
        <OverlaySide side="ao" competitor={match.competitors.ao} mode={match.mode} />
      </motion.section>
    </main>
  );
}

function OverlaySide({ side, competitor, mode }: { side: Side; competitor: Competitor; mode: string }) {
  return (
    <div className={cx("overlay-side", side)}>
      <span>{sideLabel(side)}</span>
      <strong>{competitor.name}</strong>
      <b>{mode === "kata" ? competitor.kataName : competitor.score}</b>
    </div>
  );
}

function ScoreboardFrame({ payload, variant }: { payload: StatePayload; variant: "display" | "preview" }) {
  const board = payload.match.mode === "kata"
    ? <KataBoard payload={payload} variant={variant} />
    : <KumiteBoard payload={payload} variant={variant} />;
  return (
    <section className={cx("scoreboard-frame", variant, payload.match.mode)}>
      {board}
    </section>
  );
}

function KumiteBoard({ payload, variant }: { payload: StatePayload; variant: "display" | "preview" }) {
  const { match } = payload;
  const now = useNow(120);
  const remaining = liveRemaining(match.timer, payload.receivedAt, now);
  const warning = remaining > 0 && remaining <= 15000;
  const suggestion = match.kumite.suggestion;
  return (
    <>
      <BoardHeader match={match} />
      <div className="board-main kumite-layout">
        <AthletePanel side="aka" competitor={match.competitors.aka} point={match.kumite.lastPoint} variant={variant} />
        <CenterStack
          status={match.status}
          timer={formatMs(remaining)}
          warning={warning}
          senshu={match.competitors.aka.senshu ? "aka" : match.competitors.ao.senshu ? "ao" : null}
          vrText={vrSummary(match)}
          suggestion={suggestion ? winnerText(suggestion.side, suggestion.reason) : null}
        />
        <AthletePanel side="ao" competitor={match.competitors.ao} point={match.kumite.lastPoint} variant={variant} />
      </div>
      <BoardFooter match={match} />
      <WinnerOverlay match={match} />
    </>
  );
}

function KataBoard({ payload, variant }: { payload: StatePayload; variant: "display" | "preview" }) {
  const { match } = payload;
  const now = useNow(120);
  const remaining = liveRemaining(match.kata.countdown, payload.receivedAt, now);
  const result = match.kata.result;
  return (
    <>
      <BoardHeader match={match} />
      <div className="board-main kata-layout">
        <KataAthlete side="aka" competitor={match.competitors.aka} votes={result?.aka ?? 0} reveal={match.kata.reveal} />
        <div className="center-stack">
          <div className="kata-phase">{match.kata.phase.toUpperCase()}</div>
          <div className={cx("timer-display", remaining <= 10000 && remaining > 0 && "warning")}>{formatMs(remaining)}</div>
          <div className="center-caption">35s call countdown</div>
          <VoteDots match={match} reveal={match.kata.reveal} />
          {result?.side && match.kata.reveal ? <div className={cx("result-chip", result.side)}>{sideLabel(result.side)} majority</div> : null}
        </div>
        <KataAthlete side="ao" competitor={match.competitors.ao} votes={result?.ao ?? 0} reveal={match.kata.reveal} />
      </div>
      <BoardFooter match={match} />
      <WinnerOverlay match={match} />
    </>
  );
}

function BoardHeader({ match }: { match: MatchState }) {
  return (
    <header className="board-header">
      <div className="truncate">{match.category}</div>
      <div className="match-pill">{match.tatami}</div>
      <div className="truncate right">{match.round} / {match.matchNo}</div>
    </header>
  );
}

function BoardFooter({ match }: { match: MatchState }) {
  return (
    <footer className="board-footer">
      <PenaltyStrip side="aka" competitor={match.competitors.aka} />
      <div className="next-match">{match.nextMatch}</div>
      <PenaltyStrip side="ao" competitor={match.competitors.ao} />
    </footer>
  );
}

function AthletePanel({
  side,
  competitor,
  point,
  variant
}: {
  side: Side;
  competitor: Competitor;
  point: MatchState["kumite"]["lastPoint"];
  variant: "display" | "preview";
}) {
  return (
    <motion.article className={cx("athlete-panel", side)} layout>
      <div className="athlete-top">
        <span className="side-badge">{sideLabel(side)}</span>
        {competitor.senshu ? <span className="senshu-badge">SENSHU</span> : null}
      </div>
      <div className="score-stage">
        <motion.div
          key={`${side}-${competitor.score}`}
          className="score-value"
          initial={{ scale: 0.92, opacity: 0.45 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ type: "spring", stiffness: 280, damping: 22 }}
        >
          {competitor.score}
        </motion.div>
        <AnimatePresence>
          {point?.side === side ? (
            <motion.div
              key={point.id}
              className={cx("point-pop", side)}
              initial={{ opacity: 0, y: 22, scale: 0.92 }}
              animate={{ opacity: 1, y: -10, scale: 1 }}
              exit={{ opacity: 0, y: -42, scale: 1.06 }}
              transition={{ duration: variant === "preview" ? 0.55 : 0.9 }}
            >
              +{point.points}
            </motion.div>
          ) : null}
        </AnimatePresence>
      </div>
      <div className="athlete-copy">
        <h2>{competitor.name}</h2>
        <p>{competitor.club} / #{competitor.bib}</p>
      </div>
    </motion.article>
  );
}

function KataAthlete({ side, competitor, votes, reveal }: { side: Side; competitor: Competitor; votes: number; reveal: boolean }) {
  return (
    <article className={cx("athlete-panel kata-athlete", side)}>
      <div className="athlete-top">
        <span className="side-badge">{sideLabel(side)}</span>
        {reveal ? <span className="senshu-badge">{votes} votes</span> : null}
      </div>
      <div className="kata-main">
        <h2>{competitor.name}</h2>
        <p>{competitor.club} / #{competitor.bib}</p>
        <strong>{competitor.kataName}</strong>
        <span>Kata No. {competitor.kataNo}</span>
      </div>
    </article>
  );
}

function CenterStack({
  status,
  timer,
  warning,
  senshu,
  vrText,
  suggestion
}: {
  status: string;
  timer: string;
  warning: boolean;
  senshu: Side | null;
  vrText: string | null;
  suggestion: string | null;
}) {
  return (
    <div className="center-stack">
      <div className={cx("status-chip", status)}>{status.toUpperCase()}</div>
      <div className={cx("timer-display", warning && "warning")}>{timer}</div>
      <div className="center-caption">{warning ? "Ato Shibaraku" : "WKF 2026 preset"}</div>
      <div className="center-tags">
        {senshu ? <span className={cx("small-chip", senshu)}>Senshu {sideLabel(senshu)}</span> : <span className="small-chip muted">No Senshu</span>}
        {vrText ? <span className="small-chip gold">{vrText}</span> : null}
      </div>
      {suggestion ? <div className="suggestion-chip">{suggestion}</div> : null}
    </div>
  );
}

function PenaltyStrip({ side, competitor }: { side: Side; competitor: Competitor }) {
  const p = competitor.penalties;
  return (
    <div className={cx("penalty-strip", side)}>
      <span>{sideLabel(side)}</span>
      {[1, 2, 3].map((level) => <b key={level} className={cx(p.chui >= level && "active")}>C{level}</b>)}
      <b className={cx(p.hansokuChui && "active")}>HC</b>
      <b className={cx((p.hansoku || p.shikkaku || p.kiken) && "danger")}>DQ</b>
    </div>
  );
}

function VoteDots({ match, reveal }: { match: MatchState; reveal: boolean }) {
  const votes = match.kata.votes;
  return (
    <div className="vote-dots">
      {Array.from({ length: match.kata.judgeCount }, (_, i) => {
        const judge = String(i + 1);
        const side = votes[judge];
        return <span key={judge} className={cx("vote-dot", reveal && side)}>{judge}</span>;
      })}
    </div>
  );
}

function WinnerOverlay({ match }: { match: MatchState }) {
  return (
    <AnimatePresence>
      {match.winner ? (
        <motion.div
          className="winner-overlay"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          <motion.div
            className={cx("winner-card", match.winner)}
            initial={{ scale: 0.9, y: 28 }}
            animate={{ scale: 1, y: 0 }}
            exit={{ scale: 0.96, y: 20 }}
            transition={{ type: "spring", stiffness: 220, damping: 24 }}
          >
            <Trophy />
            <span>Winner</span>
            <strong>{sideLabel(match.winner)} / {match.competitors[match.winner].name}</strong>
          </motion.div>
        </motion.div>
      ) : null}
    </AnimatePresence>
  );
}

function ControlPage({
  api
}: {
  api: ReturnType<typeof useScoreboard> | ReturnType<typeof useManualTatami>;
}) {
  const { payload, match, events, devices, connection, send } = api;
  const manual = "manual" in api && api.manual;
  const [compact, setCompact] = useState(false);
  const [showPreview, setShowPreview] = useState(() => window.localStorage.getItem(PREVIEW_VISIBLE_KEY) !== "false");
  const now = useNow(120);
  const remaining = match ? liveRemaining(match.timer, payload!.receivedAt, now) : 0;
  const tatamiQuery = window.location.search;

  const setPreviewVisible = useCallback((visible: boolean) => {
    setShowPreview(visible);
    window.localStorage.setItem(PREVIEW_VISIBLE_KEY, String(visible));
  }, []);

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement | null;
      if (target && ["INPUT", "SELECT", "TEXTAREA"].includes(target.tagName)) return;
      if (event.key === " ") {
        event.preventDefault();
        send({ type: match?.timer.running ? "TIMER_STOP" : "TIMER_START" });
      }
      const map: Record<string, { side: Side; points: number; label: string }> = {
        "1": { side: "aka", points: 1, label: "Yuko" },
        "2": { side: "aka", points: 2, label: "Waza-ari" },
        "3": { side: "aka", points: 3, label: "Ippon" },
        "7": { side: "ao", points: 1, label: "Yuko" },
        "8": { side: "ao", points: 2, label: "Waza-ari" },
        "9": { side: "ao", points: 3, label: "Ippon" }
      };
      const shortcut = map[event.key];
      if (shortcut) {
        send({ type: "SCORE_DELTA", payload: shortcut });
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [match?.timer.running, send]);

  if (!match || !payload) return null;

  return (
    <main className={cx("control-page", manual ? "manual-mode" : "synced-mode", compact && "compact")}>
      <header className="control-topbar">
        <div className="topbar-title">
          <div className="app-mark small">K</div>
          <div>
            <h1>Tatami Control</h1>
            <p>{manual ? "Free manual tatami" : match.tatami} / {match.category} / {match.matchNo}</p>
          </div>
        </div>
        <div className="topbar-actions">
          <ModeSwitch match={match} />
          <button className="icon-button" type="button" onClick={() => setCompact((value) => !value)} title="Toggle compact layout">
            <Activity />
          </button>
          <button
            className={cx("icon-button", showPreview && "active")}
            type="button"
            onClick={() => setPreviewVisible(!showPreview)}
            title={showPreview ? "Hide display preview" : "Show display preview"}
          >
            <Monitor />
          </button>
          <a className="icon-button" href={`/display${tatamiQuery}`} target="_blank" title="Open display"><Monitor /></a>
          <a className="icon-button" href={`/overlay${tatamiQuery}`} target="_blank" title="Open OBS overlay"><Eye /></a>
          <button className="icon-button" disabled title="Backend export endpoint is not available yet"><Download /></button>
          <ConnectionPill connected={connection.connected} latency={connection.latencyMs} />
        </div>
      </header>

      {manual ? (
        <section className="manual-tatami-banner">
          <div>
            <strong>Dùng thử tatami miễn phí</strong>
            <span>Nhập tay VĐV, điểm, phạt và đồng hồ ngay trên trình duyệt. Dashboard giải đấu, quản lý CLB và điều phối tournament cần đăng nhập.</span>
          </div>
          <a className="secondary-button" href="/login">Đăng nhập để quản lý giải</a>
        </section>
      ) : null}

      {showPreview ? <FloatingPreview payload={payload} onClose={() => setPreviewVisible(false)} /> : null}

      <section className="control-grid">
        <SideControl side="aka" match={match} send={send} manual={manual} />
        <section className="control-center">
          <TimerPanel match={match} remaining={remaining} send={send} />
          <MetaPanel match={match} send={send} manual={manual} />
          {match.mode === "kata" ? <KataControl match={match} send={send} /> : <KumiteFlow match={match} send={send} />}
          <RecoveryPanel events={events} send={send} />
          <DeviceHealth devices={devices} connection={connection} />
        </section>
        <SideControl side="ao" match={match} send={send} manual={manual} />
      </section>
    </main>
  );
}

function FloatingPreview({ payload, onClose }: { payload: StatePayload; onClose: () => void }) {
  const [rect, setRect] = useState(readFloatingPreviewRect);
  const [focus, setFocus] = useState(false);
  const [pinned, setPinned] = useState(() => window.localStorage.getItem(PREVIEW_PINNED_KEY) === "true");
  const activeRect = focus ? getFocusedPreviewRect() : rect;

  const saveRect = useCallback((nextRect: FloatingPreviewRect) => {
    setRect(nextRect);
    window.localStorage.setItem(PREVIEW_FLOAT_KEY, JSON.stringify(nextRect));
  }, []);

  const togglePinned = useCallback(() => {
    if (pinned) {
      const viewportRect = clampPreviewRect({
        ...rect,
        x: rect.x - window.scrollX,
        y: rect.y - window.scrollY
      });
      saveRect(viewportRect);
      setPinned(false);
      window.localStorage.setItem(PREVIEW_PINNED_KEY, "false");
      return;
    }

    const visualRect = focus ? getFocusedPreviewRect() : rect;
    const documentRect = clampPinnedPreviewRect({
      ...visualRect,
      x: visualRect.x + window.scrollX,
      y: visualRect.y + window.scrollY
    });
    setFocus(false);
    saveRect(documentRect);
    setPinned(true);
    window.localStorage.setItem(PREVIEW_PINNED_KEY, "true");
  }, [focus, pinned, rect, saveRect]);

  const updateRect = useCallback((nextRect: FloatingPreviewRect) => {
    const next = clampPreviewRect(nextRect);
    saveRect(next);
  }, [saveRect]);

  const startDrag = useCallback((event: React.PointerEvent<HTMLDivElement>) => {
    if (focus || pinned) return;
    event.preventDefault();
    const startX = event.clientX;
    const startY = event.clientY;
    const startRect = rect;

    const onPointerMove = (moveEvent: PointerEvent) => {
      updateRect({
        ...startRect,
        x: startRect.x + moveEvent.clientX - startX,
        y: startRect.y + moveEvent.clientY - startY
      });
    };

    const onPointerUp = () => {
      window.removeEventListener("pointermove", onPointerMove);
      window.removeEventListener("pointerup", onPointerUp);
    };

    window.addEventListener("pointermove", onPointerMove);
    window.addEventListener("pointerup", onPointerUp);
  }, [focus, pinned, rect, updateRect]);

  const startResize = useCallback((event: React.PointerEvent<HTMLButtonElement>) => {
    if (focus || pinned) return;
    event.preventDefault();
    event.stopPropagation();
    const startX = event.clientX;
    const startRect = rect;

    const onPointerMove = (moveEvent: PointerEvent) => {
      updateRect({
        ...startRect,
        width: startRect.width + moveEvent.clientX - startX
      });
    };

    const onPointerUp = () => {
      window.removeEventListener("pointermove", onPointerMove);
      window.removeEventListener("pointerup", onPointerUp);
    };

    window.addEventListener("pointermove", onPointerMove);
    window.addEventListener("pointerup", onPointerUp);
  }, [focus, pinned, rect, updateRect]);

  const snapPreview = (corner: PreviewCorner) => {
    if (pinned) return;
    setFocus(false);
    updateRect(snapFloatingPreview(rect, corner));
  };

  return (
    <aside
      className={cx("floating-preview", focus && "focus", pinned && "pinned")}
      style={{
        position: pinned ? "absolute" : "fixed",
        left: activeRect.x,
        top: activeRect.y,
        width: activeRect.width
      }}
    >
      <div className="floating-preview-header">
        <div className="floating-preview-drag" onPointerDown={startDrag} title={pinned ? "Preview is pinned" : "Drag preview"}>
          {pinned ? <Pin /> : <Move />}
          <span>{pinned ? "Display preview pinned" : "Display preview"}</span>
        </div>
        <div className="floating-preview-actions">
          <button disabled={pinned} onClick={() => snapPreview("tl")}>TL</button>
          <button disabled={pinned} onClick={() => snapPreview("tr")}>TR</button>
          <button disabled={pinned} onClick={() => snapPreview("bl")}>BL</button>
          <button disabled={pinned} onClick={() => snapPreview("br")}>BR</button>
          <button className={cx("pin-preview", pinned && "active")} onClick={togglePinned} title={pinned ? "Unpin preview" : "Pin preview"}>
            {pinned ? <PinOff /> : <Pin />}
          </button>
          <button disabled={pinned} className={cx("focus-preview", focus && "active")} onClick={() => setFocus((value) => !value)} title="Focus preview">
            {focus ? <Minimize2 /> : <Maximize2 />}
          </button>
          <button className="close-preview" onClick={onClose} title="Close preview">
            <X />
          </button>
        </div>
      </div>
      <div className="floating-preview-board">
        <ScoreboardFrame payload={payload} variant="preview" />
      </div>
      <button disabled={pinned} className="floating-preview-resize" onPointerDown={startResize} title={pinned ? "Preview is pinned" : "Resize preview"} />
    </aside>
  );
}

function ModeSwitch({ match }: { match: MatchState }) {
  return (
    <div className="segmented">
      <button className={cx(match.mode === "kumite" && "active")} disabled title="Match mode is controlled by backend category">Kumite</button>
      <button className={cx(match.mode === "kata" && "active")} disabled title="Match mode is controlled by backend category">Kata</button>
    </div>
  );
}

function SideControl({ side, match, send, manual }: { side: Side; match: MatchState; send: SendAction; manual?: boolean }) {
  const competitor = match.competitors[side];
  const penalties = competitor.penalties;
  const field = (label: string, value: string, key: "name" | "club" | "bib" | "kataName") => manual
    ? <CommitInput label={label} value={value} onCommit={(next) => send({ type: "MANUAL_SET_COMPETITOR", payload: { side, field: key, value: next } })} />
    : <ReadOnlyField label={label} value={value} />;

  return (
    <section className={cx("side-control", side)}>
      <div className="panel-heading strong">
        <span><CircleDot size={18} /> {sideLabel(side)}</span>
        <span className="score-chip">{match.mode === "kata" ? competitor.kataName : competitor.score}</span>
      </div>

      <div className="form-grid">
        {field("Athlete", competitor.name, "name")}
        {field("Club", competitor.club, "club")}
        {field("Bib", competitor.bib, "bib")}
        {field("Kata", competitor.kataName, "kataName")}
      </div>

      {match.mode === "kumite" ? (
        <>
          <div className="point-grid">
            {POINTS.map((point) => (
              <button
                key={point.points}
                className={cx("large-action", side)}
                onClick={() => send({ type: "SCORE_DELTA", payload: { side, points: point.points, label: point.label } })}
              >
                <Award />
                <span>+{point.points}</span>
                <small>{point.label}</small>
              </button>
            ))}
          </div>
          <div className="mini-grid">
            {POINTS.map((point) => (
              <button
                key={point.points}
                className="secondary-button"
                onClick={() => send({ type: "SCORE_DELTA", payload: { side, points: -point.points, label: `Undo ${point.label}` } })}
              >
                -{point.points}
              </button>
            ))}
          </div>
          <div className="two-grid">
            <button
              className={cx("secondary-button", competitor.senshu && "active")}
              onClick={() => send({ type: "SET_SENSHU", payload: { side, value: !competitor.senshu } })}
            >
              <Flag /> Senshu
            </button>
            <button className="secondary-button" disabled title="Clear Senshu is not supported by backend v1">Clear Senshu</button>
          </div>
          <div className="control-block">
            <label>Chui</label>
            <div className="segmented full">
              {[0, 1, 2, 3].map((value) => (
                <button key={value} className={cx(penalties.chui === value && "active")} onClick={() => send({ type: "SET_CHUI", payload: { side, value } })}>
                  {value}
                </button>
              ))}
            </div>
          </div>
          <div className="two-grid">
            <ToggleButton active={penalties.hansokuChui} label="Hansoku Chui" onClick={() => send({ type: "TOGGLE_PENALTY", payload: { side, penalty: "hansokuChui", value: !penalties.hansokuChui } })} />
            <ToggleButton active={penalties.hansoku} danger label="Hansoku" onClick={() => confirmAction("Confirm Hansoku?", () => send({ type: "TOGGLE_PENALTY", payload: { side, penalty: "hansoku", value: !penalties.hansoku } }))} />
            <ToggleButton active={penalties.shikkaku} danger label="Shikkaku" onClick={() => confirmAction("Confirm Shikkaku?", () => send({ type: "TOGGLE_PENALTY", payload: { side, penalty: "shikkaku", value: !penalties.shikkaku } }))} />
            <ToggleButton active={penalties.kiken} danger label="Kiken" onClick={() => confirmAction("Confirm Kiken?", () => send({ type: "TOGGLE_PENALTY", payload: { side, penalty: "kiken", value: !penalties.kiken } }))} />
          </div>
          <div className="control-block">
            <label>Video review</label>
            <div className="mini-grid">
              <button className="secondary-button" disabled title="VR state is not supported by backend v1">VR</button>
              <button className="secondary-button success" disabled title="VR state is not supported by backend v1">Accept</button>
              <button className="secondary-button danger" disabled title="VR state is not supported by backend v1">Deny</button>
            </div>
          </div>
          <button
            className="winner-button"
            onClick={() => confirmAction(`Confirm ${sideLabel(side)} winner?`, () => send({ type: "WINNER_CONFIRM", payload: { side, winType: match.kumite.suggestion?.reason || "manual" } }))}
          >
            <Trophy /> Confirm {sideLabel(side)} winner
          </button>
        </>
      ) : (
        <div className="kata-side-actions">
          <button className={cx("large-action", side)} disabled title="Kata phase state is not supported by backend v1">
            <Flag />
            <span>Set {sideLabel(side)} active</span>
            <small>{competitor.kataName}</small>
          </button>
          <button className="secondary-button" onClick={() => send({ type: "KATA_VOTE", payload: { judge: 1, side } })}>
            <Vote /> Manual J1 vote
          </button>
        </div>
      )}
    </section>
  );
}

function TimerPanel({ match, remaining, send }: { match: MatchState; remaining: number; send: SendAction }) {
  return (
    <section className="control-panel">
      <div className="panel-heading strong">
        <span><Clock size={18} /> Timer</span>
        <span className={cx("status-mini", match.status)}>{match.status}</span>
      </div>
      <div className={cx("control-clock", remaining <= 15000 && remaining > 0 && "warning")}>{formatMs(remaining)}</div>
      <div className="two-grid">
        <button className="primary-button" onClick={() => send({ type: match.timer.running ? "TIMER_STOP" : "TIMER_START" })}>
          {match.timer.running ? <Pause /> : <Play />}
          {match.timer.running ? "Stop" : "Start"}
        </button>
        <button className="secondary-button" onClick={() => send({ type: "TIMER_RESET" })}><RefreshCcw /> Reset</button>
      </div>
      <div className="mini-grid">
        <button className="secondary-button" onClick={() => send({ type: "TIMER_ADJUST", payload: { deltaMs: -1000 } })}>-1s</button>
        <button className="secondary-button" onClick={() => send({ type: "TIMER_ADJUST", payload: { deltaMs: 1000 } })}>+1s</button>
        <button className="secondary-button" onClick={() => send({ type: "TIMER_ADJUST", payload: { deltaMs: 10000 } })}>+10s</button>
      </div>
      <div className="mini-grid">
        {[90, 120, 180].map((seconds) => (
          <button key={seconds} className="secondary-button" onClick={() => send({ type: "TIMER_SET_DURATION", payload: { durationMs: durationFromPreset(seconds) } })}>
            {seconds / 60 >= 1.5 ? `${seconds / 60}m` : `${seconds}s`}
          </button>
        ))}
      </div>
    </section>
  );
}

function MetaPanel({ match, send, manual }: { match: MatchState; send: SendAction; manual?: boolean }) {
  const field = (label: string, value: string, key: "tatami" | "matchNo" | "category" | "round" | "nextMatch", wide?: boolean) => manual
    ? <CommitInput label={label} value={value} wide={wide} onCommit={(next) => send({ type: "MANUAL_SET_META", payload: { field: key, value: next } })} />
    : <ReadOnlyField label={label} value={value} wide={wide} />;

  return (
    <section className="control-panel">
      <div className="panel-heading strong">
        <span><Swords size={18} /> Match setup</span>
        <button className="text-button" disabled title="Swap sides is not supported by backend v1"><ArrowLeftRight size={16} /> Swap</button>
      </div>
      <div className="form-grid">
        {field("Tatami", match.tatami, "tatami")}
        {field("Match No.", match.matchNo, "matchNo")}
        {field("Category", match.category, "category")}
        {field("Round", match.round, "round")}
        {field("Next match", match.nextMatch, "nextMatch", true)}
      </div>
    </section>
  );
}

function KumiteFlow({ match, send }: { match: MatchState; send: SendAction }) {
  const suggestion = match.kumite.suggestion;
  return (
    <section className="control-panel">
      <div className="panel-heading strong">
        <span><Shield size={18} /> Kumite flow</span>
      </div>
      <div className="suggestion-box">
        <strong>{suggestion ? winnerText(suggestion.side, suggestion.reason) : "No automatic winner suggestion"}</strong>
        <span>Secretary still confirms after referee decision.</span>
      </div>
      <div className="two-grid">
        <button className={cx("secondary-button", match.kumite.hantei && "active")} onClick={() => send({ type: "SET_HANTEI", payload: { value: !match.kumite.hantei } })}>
          <Flag /> Hantei
        </button>
        <button className="secondary-button danger" disabled title="Reset match is not supported by backend v1">
          <RotateCcw /> Reset match
        </button>
      </div>
    </section>
  );
}

function KataControl({ match, send }: { match: MatchState; send: SendAction }) {
  const result = match.kata.result;
  return (
    <section className="control-panel">
      <div className="panel-heading strong">
        <span><Vote size={18} /> Kata control</span>
        <span className="status-mini">{result?.aka ?? 0}-{result?.ao ?? 0}</span>
      </div>
      <div className="segmented full">
        {[5, 7].map((count) => (
          <button key={count} className={cx(match.kata.judgeCount === count && "active")} disabled title="Judge count is derived from backend votes in v1">
            {count} judges
          </button>
        ))}
      </div>
      <div className="two-grid">
        <button className="primary-button" disabled title="Kata countdown state is not supported by backend v1">
          {match.kata.countdown.running ? <Pause /> : <Play />} Countdown
        </button>
        <button className="secondary-button" disabled title="Kata countdown state is not supported by backend v1"><RefreshCcw /> Reset</button>
      </div>
      <div className="segmented full wrap">
        {["call", "aka", "ao", "voting", "result"].map((phase) => (
          <button key={phase} className={cx(match.kata.phase === phase && "active")} disabled>{phase}</button>
        ))}
      </div>
      <div className="vote-control-grid">
        {Array.from({ length: match.kata.judgeCount }, (_, index) => {
          const judge = index + 1;
          const vote = match.kata.votes[String(judge)];
          return (
            <div key={judge} className="judge-mini-row">
              <span>J{judge}</span>
              <button className={cx(vote === "aka" && "aka-active")} onClick={() => send({ type: "KATA_VOTE", payload: { judge, side: "aka" } })}>AKA</button>
              <button className={cx(vote === "ao" && "ao-active")} onClick={() => send({ type: "KATA_VOTE", payload: { judge, side: "ao" } })}>AO</button>
            </div>
          );
        })}
      </div>
      <div className="two-grid">
        <button className="secondary-button" disabled title="Kata reveal state is not supported by backend v1">
          <Eye /> {match.kata.reveal ? "Hide votes" : "Reveal votes"}
        </button>
        <button className="winner-button" disabled={!result?.side} onClick={() => confirmAction("Confirm kata winner?", () => send({ type: "KATA_CONFIRM_WINNER", payload: { side: result?.side } }))}>
          <Trophy /> Confirm winner
        </button>
      </div>
      <button className="secondary-button danger" disabled title="Clearing Kata votes is not supported by backend v1">
        Clear votes
      </button>
    </section>
  );
}

function RecoveryPanel({ events, send }: { events: EventRecord[]; send: SendAction }) {
  return (
    <section className="control-panel">
      <div className="panel-heading strong">
        <span><History size={18} /> Mistake recovery</span>
        <button className="text-button" disabled title="Undo is not supported by backend v1"><Undo2 size={16} /> Undo</button>
      </div>
      <div className="timeline">
        {events.slice(-10).reverse().map((event) => (
          <div key={event.id} className="event-row">
            <span>{formatClockTime(event.at)}</span>
            <strong>{event.label}</strong>
          </div>
        ))}
      </div>
    </section>
  );
}

function DeviceHealth({
  devices,
  connection
}: {
  devices: DeviceRecord[];
  connection: { connected: boolean; latencyMs: number | null; socketId: string | null; lastError: string | null };
}) {
  const now = Date.now();
  return (
    <section className="control-panel">
      <div className="panel-heading strong">
        <span><Radio size={18} /> Device health</span>
        <ConnectionPill connected={connection.connected} latency={connection.latencyMs} />
      </div>
      <div className="device-list">
        {devices.map((device) => (
          <div key={device.id} className="device-row">
            <div>
              <strong>{device.role} / {device.name}</strong>
              <span>{device.address}</span>
            </div>
            <b>{Math.max(0, Math.round((now - device.lastSeen) / 1000))}s</b>
          </div>
        ))}
      </div>
      {connection.lastError ? <p className="error-text">{connection.lastError}</p> : null}
    </section>
  );
}

function JudgePage({ payload, send }: { payload: StatePayload; send: SendAction }) {
  const [judge, setJudge] = useState(() => Number(new URLSearchParams(window.location.search).get("judge") || "1"));
  const match = payload.match;
  const selectedVote = match.kata.votes[String(judge)];
  return (
    <main className="judge-page">
      <section className="judge-card">
        <div className="panel-heading strong">
          <span><Vote size={20} /> Kata judge</span>
          <select value={judge} onChange={(event) => setJudge(Number(event.target.value))}>
            {Array.from({ length: match.kata.judgeCount }, (_, i) => <option key={i + 1} value={i + 1}>Judge {i + 1}</option>)}
          </select>
        </div>
        <div className="judge-match">
          <strong>{match.category}</strong>
          <span>{match.tatami} / {match.round}</span>
        </div>
        <div className="judge-buttons">
          <button className={cx("judge-vote aka", selectedVote === "aka" && "selected")} onClick={() => send({ type: "KATA_VOTE", payload: { judge, side: "aka" } })}>
            <Flag /> AKA
            <span>{match.competitors.aka.name}</span>
            <small>{match.competitors.aka.kataName}</small>
          </button>
          <button className={cx("judge-vote ao", selectedVote === "ao" && "selected")} onClick={() => send({ type: "KATA_VOTE", payload: { judge, side: "ao" } })}>
            <Flag /> AO
            <span>{match.competitors.ao.name}</span>
            <small>{match.competitors.ao.kataName}</small>
          </button>
        </div>
        <button className="secondary-button" disabled title="Clearing Kata votes is not supported by backend v1">Clear my vote</button>
      </section>
    </main>
  );
}

function CommitInput({
  label,
  value,
  onCommit,
  wide
}: {
  label: string;
  value: string;
  onCommit: (value: string) => void;
  wide?: boolean;
}) {
  const [draft, setDraft] = useState(value);
  useEffect(() => setDraft(value), [value]);
  const commit = () => {
    const next = draft.trim();
    if (next !== value) onCommit(next);
  };
  return (
    <label className={cx("field", wide && "wide")}>
      <span>{label}</span>
      <input
        value={draft}
        onChange={(event) => setDraft(event.target.value)}
        onBlur={commit}
        onKeyDown={(event) => {
          if (event.key === "Enter") event.currentTarget.blur();
        }}
      />
    </label>
  );
}

function ReadOnlyField({ label, value, wide }: { label: string; value: string; wide?: boolean }) {
  return (
    <label className={cx("field", wide && "wide")}>
      <span>{label}</span>
      <input value={value} readOnly />
    </label>
  );
}

function ToggleButton({ active, danger, label, onClick }: { active: boolean; danger?: boolean; label: string; onClick: () => void }) {
  return (
    <button className={cx("secondary-button", active && "active", danger && "danger")} onClick={onClick}>
      {danger ? <AlertTriangle /> : <Shield />}
      {label}
    </button>
  );
}

function ConnectionPill({ connected, latency }: { connected: boolean; latency?: number | null }) {
  return (
    <span className={cx("connection-pill", connected ? "online" : "offline")}>
      {connected ? <Wifi size={15} /> : <WifiOff size={15} />}
      {connected ? `Online${latency != null ? ` / ${latency}ms` : ""}` : "Offline"}
    </span>
  );
}

function vrSummary(match: MatchState) {
  const active = SIDES.find((side) => match.kumite.vr[side].active);
  if (active) return `VR ${sideLabel(active)}`;
  const denied = SIDES.find((side) => match.kumite.vr[side].result === "denied");
  if (denied) return `${sideLabel(denied)} VR used`;
  return null;
}

function confirmAction(message: string, action: () => void) {
  if (window.confirm(message)) action();
}

function useNow(intervalMs: number) {
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    const id = window.setInterval(() => setNow(Date.now()), intervalMs);
    return () => window.clearInterval(id);
  }, [intervalMs]);
  return now;
}

function clampNumber(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, Math.round(value)));
}

function readFloatingPreviewRect(): FloatingPreviewRect {
  try {
    const stored = window.localStorage.getItem(PREVIEW_FLOAT_KEY);
    if (stored) {
      const parsed = JSON.parse(stored) as FloatingPreviewRect;
      return window.localStorage.getItem(PREVIEW_PINNED_KEY) === "true"
        ? clampPinnedPreviewRect(parsed)
        : clampPreviewRect(parsed);
    }
  } catch {
    return getDefaultFloatingPreviewRect();
  }
  return getDefaultFloatingPreviewRect();
}

function getDefaultFloatingPreviewRect(): FloatingPreviewRect {
  const width = clampNumber(Math.min(620, window.innerWidth - 28), Math.min(PREVIEW_MIN_WIDTH, window.innerWidth - 28), Math.min(PREVIEW_MAX_WIDTH, window.innerWidth - 28));
  return clampPreviewRect({
    x: 16,
    y: window.innerHeight - floatingPreviewHeight(width) - 18,
    width
  });
}

function getFocusedPreviewRect(): FloatingPreviewRect {
  const width = clampNumber(Math.min(1040, window.innerWidth - 32), Math.min(PREVIEW_MIN_WIDTH, window.innerWidth - 32), window.innerWidth - 32);
  return clampPreviewRect({
    x: (window.innerWidth - width) / 2,
    y: 86,
    width
  });
}

function snapFloatingPreview(rect: FloatingPreviewRect, corner: PreviewCorner): FloatingPreviewRect {
  const margin = 16;
  const width = clampPreviewRect(rect).width;
  const height = floatingPreviewHeight(width);
  return clampPreviewRect({
    width,
    x: corner.endsWith("r") ? window.innerWidth - width - margin : margin,
    y: corner.startsWith("b") ? window.innerHeight - height - margin : 92
  });
}

function clampPreviewRect(rect: FloatingPreviewRect): FloatingPreviewRect {
  const margin = 10;
  const viewportWidth = Math.max(320, window.innerWidth);
  const viewportHeight = Math.max(420, window.innerHeight);
  const maxWidth = Math.max(280, Math.min(PREVIEW_MAX_WIDTH, viewportWidth - margin * 2));
  const minWidth = Math.min(PREVIEW_MIN_WIDTH, maxWidth);
  const width = clampNumber(Number(rect.width) || 560, minWidth, maxWidth);
  const height = floatingPreviewHeight(width);
  const maxX = Math.max(margin, viewportWidth - width - margin);
  const maxY = Math.max(margin, viewportHeight - height - margin);
  return {
    width,
    x: clampNumber(Number(rect.x) || margin, margin, maxX),
    y: clampNumber(Number(rect.y) || margin, margin, maxY)
  };
}

function clampPinnedPreviewRect(rect: FloatingPreviewRect): FloatingPreviewRect {
  const margin = 10;
  const viewportWidth = Math.max(320, window.innerWidth);
  const documentHeight = Math.max(
    window.innerHeight,
    document.documentElement.scrollHeight,
    document.body.scrollHeight
  );
  const maxWidth = Math.max(280, Math.min(PREVIEW_MAX_WIDTH, viewportWidth - margin * 2));
  const minWidth = Math.min(PREVIEW_MIN_WIDTH, maxWidth);
  const width = clampNumber(Number(rect.width) || 560, minWidth, maxWidth);
  const height = floatingPreviewHeight(width);
  const maxX = Math.max(margin, window.scrollX + viewportWidth - width - margin);
  const maxY = Math.max(margin, documentHeight - height - margin);
  return {
    width,
    x: clampNumber(Number(rect.x) || margin, window.scrollX + margin, maxX),
    y: clampNumber(Number(rect.y) || margin, margin, maxY)
  };
}

function floatingPreviewHeight(width: number) {
  return Math.round((width * 9) / 16 + 70);
}

type SendAction = (action: ScoreboardAction) => Promise<{ ok: boolean; error?: string }>;
type PreviewCorner = "tl" | "tr" | "bl" | "br";
interface FloatingPreviewRect {
  x: number;
  y: number;
  width: number;
}
