import { Client } from "@stomp/stompjs";
import { Award, ChevronLeft, Trophy, Users } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import { apiGet, authHeaders, getApiBase } from "./apiClient";
import type { AthleteRanking, ClubStanding, TournamentResponse } from "./types";

// ─── WS helper (mirrors useNotifications.ts pattern) ────────────────────────

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

// ─── Medal circle components ─────────────────────────────────────────────────

function GoldCircle({ count }: { count: number }) {
  return (
    <span
      className="inline-flex items-center justify-center w-7 h-7 rounded-full text-xs font-bold"
      style={{ background: "#f59e0b", color: "#1a1a1a" }}
      title="Huy chương vàng"
    >
      {count}
    </span>
  );
}

function SilverCircle({ count }: { count: number }) {
  return (
    <span
      className="inline-flex items-center justify-center w-7 h-7 rounded-full text-xs font-bold"
      style={{ background: "#9ca3af", color: "#1a1a1a" }}
      title="Huy chương bạc"
    >
      {count}
    </span>
  );
}

function BronzeCircle({ count }: { count: number }) {
  return (
    <span
      className="inline-flex items-center justify-center w-7 h-7 rounded-full text-xs font-bold"
      style={{ background: "#b45309", color: "#fff" }}
      title="Huy chương đồng"
    >
      {count}
    </span>
  );
}

// ─── Rank badge ───────────────────────────────────────────────────────────────

function RankBadge({ rank }: { rank: number }) {
  if (rank === 1) {
    return (
      <span
        className="inline-flex items-center justify-center w-8 h-8 rounded-full font-bold text-sm"
        style={{ background: "#f59e0b", color: "#1a1a1a" }}
      >
        1
      </span>
    );
  }
  if (rank === 2) {
    return (
      <span
        className="inline-flex items-center justify-center w-8 h-8 rounded-full font-bold text-sm"
        style={{ background: "#9ca3af", color: "#1a1a1a" }}
      >
        2
      </span>
    );
  }
  if (rank === 3) {
    return (
      <span
        className="inline-flex items-center justify-center w-8 h-8 rounded-full font-bold text-sm"
        style={{ background: "#b45309", color: "#fff" }}
      >
        3
      </span>
    );
  }
  return (
    <span className="inline-flex items-center justify-center w-8 h-8 text-sm font-semibold text-gray-400">
      {rank}
    </span>
  );
}

// ─── Club Points Bar (relative to max) ───────────────────────────────────────

function PointsBar({ value, max }: { value: number; max: number }) {
  const pct = max > 0 ? Math.round((value / max) * 100) : 0;
  return (
    <div className="flex items-center gap-2 flex-1 min-w-0">
      <div className="flex-1 h-2 rounded-full bg-gray-700 overflow-hidden">
        <div
          className="h-full rounded-full transition-all duration-500"
          style={{ width: `${pct}%`, background: "linear-gradient(90deg, #6366f1, #8b5cf6)" }}
        />
      </div>
      <span className="text-sm font-semibold text-indigo-300 w-10 text-right tabular-nums">
        {value}
      </span>
    </div>
  );
}

// ─── Tab type ────────────────────────────────────────────────────────────────

type Tab = "club-points" | "medals" | "athletes";

// ─── Main page ────────────────────────────────────────────────────────────────

interface Props {
  tournamentId: string;
}

export default function TournamentStandingsPage({ tournamentId }: Props) {
  const [activeTab, setActiveTab] = useState<Tab>("club-points");
  const [tournament, setTournament] = useState<TournamentResponse | null>(null);
  const [clubStandings, setClubStandings] = useState<ClubStanding[]>([]);
  const [athleteRankings, setAthleteRankings] = useState<AthleteRanking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [wsConnected, setWsConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);

  // ─── Data fetchers ──────────────────────────────────────────────────────────

  const fetchTournament = useCallback(async () => {
    try {
      const data = await apiGet<TournamentResponse>(`/api/tournaments/${tournamentId}`);
      setTournament(data);
    } catch {
      // non-critical
    }
  }, [tournamentId]);

  const fetchClubStandings = useCallback(async () => {
    try {
      const data = await apiGet<ClubStanding[]>(`/api/tournaments/${tournamentId}/standings/clubs`);
      setClubStandings(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không tải được dữ liệu xếp hạng CLB");
    }
  }, [tournamentId]);

  const fetchAthleteRankings = useCallback(async () => {
    try {
      const data = await apiGet<AthleteRanking[]>(`/api/tournaments/${tournamentId}/standings/athletes`);
      setAthleteRankings(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không tải được xếp hạng VĐV");
    }
  }, [tournamentId]);

  const refreshAll = useCallback(async () => {
    await Promise.all([fetchClubStandings(), fetchAthleteRankings()]);
  }, [fetchClubStandings, fetchAthleteRankings]);

  // ─── Initial load ───────────────────────────────────────────────────────────

  useEffect(() => {
    setLoading(true);
    setError(null);
    Promise.all([fetchTournament(), fetchClubStandings(), fetchAthleteRankings()]).finally(() =>
      setLoading(false)
    );
  }, [fetchTournament, fetchClubStandings, fetchAthleteRankings]);

  // ─── WebSocket subscription ─────────────────────────────────────────────────

  useEffect(() => {
    const client = new Client({
      brokerURL: wsUrl("/ws"),
      connectHeaders: authHeaders(),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setWsConnected(true);
        client.subscribe(`/topic/tournaments/${tournamentId}/dashboard`, () => {
          // re-fetch standings on any dashboard update message
          refreshAll();
        });
      },
      onDisconnect: () => {
        setWsConnected(false);
      },
    });
    clientRef.current = client;
    client.activate();
    return () => {
      client.deactivate();
      clientRef.current = null;
      setWsConnected(false);
    };
  }, [tournamentId, refreshAll]);

  // ─── Derived data ───────────────────────────────────────────────────────────

  // Club points tab — sorted by totalPoints desc
  const clubByPoints = [...clubStandings].sort((a, b) => b.totalPoints - a.totalPoints);
  const maxPoints = clubByPoints[0]?.totalPoints ?? 1;

  // Medal table tab — sorted by medalScore desc, then gold desc
  const clubByMedals = [...clubStandings].sort(
    (a, b) => b.medalScore - a.medalScore || b.goldMedals - a.goldMedals
  );

  // ─── Back navigation ────────────────────────────────────────────────────────

  const handleBack = () => {
    if (window.history.length > 1) {
      window.history.back();
    }
  };

  // ─── Render ─────────────────────────────────────────────────────────────────

  return (
    <div
      className="min-h-screen text-gray-100"
      style={{ background: "linear-gradient(135deg, #0f0f1a 0%, #1a1a2e 50%, #16213e 100%)" }}
    >
      {/* ── Header ── */}
      <header className="sticky top-0 z-10 backdrop-blur-sm border-b border-gray-700/50"
        style={{ background: "rgba(15,15,26,0.85)" }}
      >
        <div className="max-w-5xl mx-auto px-4 py-4 flex items-center gap-3">
          <button
            onClick={handleBack}
            className="p-2 rounded-lg text-gray-400 hover:text-white hover:bg-gray-700/60 transition-colors"
            aria-label="Quay lại"
          >
            <ChevronLeft size={20} />
          </button>

          <div className="flex items-center gap-2 flex-1 min-w-0">
            <Trophy size={22} className="text-indigo-400 shrink-0" />
            <div className="min-w-0">
              <h1 className="text-lg font-bold truncate leading-tight">
                {tournament?.name ?? "Xếp hạng giải đấu"}
              </h1>
              <p className="text-xs text-gray-400 truncate">Bảng thành tích tổng hợp</p>
            </div>
          </div>

          {/* WS indicator */}
          <div className="flex items-center gap-1.5 shrink-0">
            <span
              className="w-2 h-2 rounded-full"
              style={{ background: wsConnected ? "#22c55e" : "#6b7280" }}
            />
            <span className="text-xs text-gray-500 hidden sm:inline">
              {wsConnected ? "Trực tiếp" : "Offline"}
            </span>
          </div>
        </div>

        {/* ── Tab navigation ── */}
        <div className="max-w-5xl mx-auto px-4 flex gap-1 pb-0">
          <TabButton
            active={activeTab === "club-points"}
            onClick={() => setActiveTab("club-points")}
            icon={<Trophy size={15} />}
            label="Điểm CLB"
          />
          <TabButton
            active={activeTab === "medals"}
            onClick={() => setActiveTab("medals")}
            icon={<Award size={15} />}
            label="Huy chương"
          />
          <TabButton
            active={activeTab === "athletes"}
            onClick={() => setActiveTab("athletes")}
            icon={<Users size={15} />}
            label="Xếp hạng VĐV"
          />
        </div>
      </header>

      {/* ── Body ── */}
      <main className="max-w-5xl mx-auto px-4 py-6">
        {loading ? (
          <LoadingSkeleton />
        ) : error ? (
          <ErrorState message={error} onRetry={refreshAll} />
        ) : (
          <>
            {activeTab === "club-points" && (
              <ClubPointsTab rows={clubByPoints} maxPoints={maxPoints} />
            )}
            {activeTab === "medals" && (
              <MedalTableTab rows={clubByMedals} />
            )}
            {activeTab === "athletes" && (
              <AthleteRankingTab rows={athleteRankings} />
            )}
          </>
        )}
      </main>
    </div>
  );
}

// ─── Tab button ───────────────────────────────────────────────────────────────

function TabButton({
  active,
  onClick,
  icon,
  label,
}: {
  active: boolean;
  onClick: () => void;
  icon: React.ReactNode;
  label: string;
}) {
  return (
    <button
      onClick={onClick}
      className={[
        "flex items-center gap-1.5 px-4 py-2.5 text-sm font-medium transition-colors border-b-2 -mb-px",
        active
          ? "text-indigo-400 border-indigo-500"
          : "text-gray-400 border-transparent hover:text-gray-200",
      ].join(" ")}
    >
      {icon}
      {label}
    </button>
  );
}

// ─── Club Points Tab ──────────────────────────────────────────────────────────

function ClubPointsTab({ rows, maxPoints }: { rows: ClubStanding[]; maxPoints: number }) {
  if (rows.length === 0) {
    return <EmptyState message="Chưa có dữ liệu điểm CLB" />;
  }

  return (
    <div className="space-y-2">
      <SectionLabel>Bảng điểm câu lạc bộ</SectionLabel>
      {rows.map((row, idx) => {
        const rank = idx + 1;
        const isTop3 = rank <= 3;
        return (
          <div
            key={row.organizationId}
            className={[
              "flex items-center gap-3 rounded-xl px-4 py-3 transition-colors",
              isTop3
                ? "bg-gray-800/80 border border-gray-600/50"
                : "bg-gray-800/40 border border-transparent",
            ].join(" ")}
          >
            <RankBadge rank={rank} />
            <div className="flex-1 min-w-0">
              <p
                className={[
                  "font-semibold truncate text-sm",
                  isTop3 ? "text-white" : "text-gray-200",
                ].join(" ")}
              >
                {row.organizationName}
              </p>
              <div className="flex items-center gap-2 mt-1">
                <GoldCircle count={row.goldMedals} />
                <SilverCircle count={row.silverMedals} />
                <BronzeCircle count={row.bronzeMedals} />
              </div>
            </div>
            <PointsBar value={row.totalPoints} max={maxPoints} />
          </div>
        );
      })}
    </div>
  );
}

// ─── Medal Table Tab ──────────────────────────────────────────────────────────

function MedalTableTab({ rows }: { rows: ClubStanding[] }) {
  if (rows.length === 0) {
    return <EmptyState message="Chưa có dữ liệu huy chương" />;
  }

  return (
    <div>
      <SectionLabel>Bảng huy chương</SectionLabel>
      <div className="rounded-xl overflow-hidden border border-gray-700/50">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-gray-800/80 text-gray-400 text-xs uppercase tracking-wider">
              <th className="text-left px-4 py-3 w-10">#</th>
              <th className="text-left px-4 py-3">Câu lạc bộ</th>
              <th className="text-center px-3 py-3">
                <span
                  className="inline-block w-5 h-5 rounded-full align-middle"
                  style={{ background: "#f59e0b" }}
                  title="Vàng"
                />
              </th>
              <th className="text-center px-3 py-3">
                <span
                  className="inline-block w-5 h-5 rounded-full align-middle"
                  style={{ background: "#9ca3af" }}
                  title="Bạc"
                />
              </th>
              <th className="text-center px-3 py-3">
                <span
                  className="inline-block w-5 h-5 rounded-full align-middle"
                  style={{ background: "#b45309" }}
                  title="Đồng"
                />
              </th>
              <th className="text-right px-4 py-3">Điểm HCh</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row, idx) => {
              const rank = idx + 1;
              const isTop3 = rank <= 3;
              return (
                <tr
                  key={row.organizationId}
                  className={[
                    "border-t border-gray-700/40 transition-colors",
                    isTop3
                      ? "bg-gray-800/60 hover:bg-gray-700/60"
                      : "hover:bg-gray-800/40",
                  ].join(" ")}
                >
                  <td className="px-4 py-3">
                    <RankBadge rank={rank} />
                  </td>
                  <td className="px-4 py-3 font-medium text-gray-100 max-w-0 truncate">
                    {row.organizationName}
                  </td>
                  <td className="px-3 py-3 text-center">
                    {row.goldMedals > 0 ? (
                      <span
                        className="inline-flex items-center justify-center w-7 h-7 rounded-full text-xs font-bold"
                        style={{ background: "#f59e0b", color: "#1a1a1a" }}
                      >
                        {row.goldMedals}
                      </span>
                    ) : (
                      <span className="text-gray-600">—</span>
                    )}
                  </td>
                  <td className="px-3 py-3 text-center">
                    {row.silverMedals > 0 ? (
                      <span
                        className="inline-flex items-center justify-center w-7 h-7 rounded-full text-xs font-bold"
                        style={{ background: "#9ca3af", color: "#1a1a1a" }}
                      >
                        {row.silverMedals}
                      </span>
                    ) : (
                      <span className="text-gray-600">—</span>
                    )}
                  </td>
                  <td className="px-3 py-3 text-center">
                    {row.bronzeMedals > 0 ? (
                      <span
                        className="inline-flex items-center justify-center w-7 h-7 rounded-full text-xs font-bold"
                        style={{ background: "#b45309", color: "#fff" }}
                      >
                        {row.bronzeMedals}
                      </span>
                    ) : (
                      <span className="text-gray-600">—</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-right font-semibold text-indigo-300 tabular-nums">
                    {row.medalScore}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <p className="text-xs text-gray-500 mt-2 text-right">
        Điểm HCh = vàng×3 + bạc×2 + đồng×1
      </p>
    </div>
  );
}

// ─── Athlete Ranking Tab ──────────────────────────────────────────────────────

const ATHLETE_PAGE_SIZE = 30;

function AthleteRankingTab({ rows }: { rows: AthleteRanking[] }) {
  const [page, setPage] = useState(0);

  const totalPages = Math.ceil(rows.length / ATHLETE_PAGE_SIZE);
  const visible = rows.slice(page * ATHLETE_PAGE_SIZE, (page + 1) * ATHLETE_PAGE_SIZE);

  if (rows.length === 0) {
    return <EmptyState message="Chưa có dữ liệu xếp hạng VĐV" />;
  }

  return (
    <div>
      <SectionLabel>
        Xếp hạng vận động viên
        <span className="ml-2 text-gray-500 font-normal text-xs">({rows.length} VĐV)</span>
      </SectionLabel>

      <div className="rounded-xl overflow-hidden border border-gray-700/50">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-gray-800/80 text-gray-400 text-xs uppercase tracking-wider">
              <th className="text-left px-4 py-3 w-12">#</th>
              <th className="text-left px-4 py-3">Vận động viên</th>
              <th className="text-left px-4 py-3 hidden sm:table-cell">Câu lạc bộ</th>
              <th className="text-right px-4 py-3">Điểm</th>
            </tr>
          </thead>
          <tbody>
            {visible.map((row) => {
              const isTop3 = row.rank <= 3;
              return (
                <tr
                  key={row.athleteId}
                  className={[
                    "border-t border-gray-700/40 transition-colors",
                    isTop3
                      ? "bg-gray-800/60 hover:bg-gray-700/60"
                      : "hover:bg-gray-800/30",
                  ].join(" ")}
                >
                  <td className="px-4 py-3">
                    <RankBadge rank={row.rank} />
                  </td>
                  <td className="px-4 py-3">
                    <p className="font-medium text-gray-100">{row.athleteName}</p>
                    {/* Show club inline on mobile */}
                    <p className="text-xs text-gray-500 sm:hidden mt-0.5">
                      {row.organizationName}
                    </p>
                  </td>
                  <td className="px-4 py-3 text-gray-400 hidden sm:table-cell">
                    {row.organizationName}
                  </td>
                  <td className="px-4 py-3 text-right font-semibold text-indigo-300 tabular-nums">
                    {row.points}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-xs text-gray-500">
            Trang {page + 1} / {totalPages} — {rows.length} VĐV
          </p>
          <div className="flex gap-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-3 py-1.5 rounded-lg text-sm border border-gray-700 text-gray-300 hover:bg-gray-700/60 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              Trước
            </button>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page === totalPages - 1}
              className="px-3 py-1.5 rounded-lg text-sm border border-gray-700 text-gray-300 hover:bg-gray-700/60 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              Sau
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Utility sub-components ───────────────────────────────────────────────────

function SectionLabel({ children }: { children: React.ReactNode }) {
  return (
    <h2 className="text-sm font-semibold text-gray-400 uppercase tracking-wider mb-3">
      {children}
    </h2>
  );
}

function EmptyState({ message }: { message: string }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-gray-500">
      <Trophy size={40} className="mb-3 opacity-30" />
      <p className="text-sm">{message}</p>
    </div>
  );
}

function ErrorState({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-gray-500">
      <p className="text-sm text-red-400 mb-3">{message}</p>
      <button
        onClick={onRetry}
        className="px-4 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-sm transition-colors"
      >
        Thử lại
      </button>
    </div>
  );
}

function LoadingSkeleton() {
  return (
    <div className="space-y-3 animate-pulse">
      {Array.from({ length: 6 }).map((_, i) => (
        <div key={i} className="h-14 rounded-xl bg-gray-800/50" />
      ))}
    </div>
  );
}
