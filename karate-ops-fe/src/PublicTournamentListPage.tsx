import { AnimatePresence, motion } from "framer-motion";
import { Calendar, MapPin, Trophy, Users } from "lucide-react";
import { useEffect, useState } from "react";
import { apiGet } from "./apiClient";
import { StatusBadge } from "./components";
import type { PublicTournamentSummary, TournamentPhase } from "./types";

const PAGE_SIZE = 10;

const TABS: { label: string; phase: TournamentPhase }[] = [
  { label: "Sắp diễn ra", phase: "UPCOMING" },
  { label: "Đang diễn ra", phase: "ONGOING" },
  { label: "Đã kết thúc", phase: "FINISHED" },
];

function formatDate(iso: string | null | undefined): string {
  if (!iso) return "";
  const d = new Date(iso);
  return d.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });
}

function DateRange({ startsOn, endsOn }: { startsOn: string; endsOn: string }) {
  const start = formatDate(startsOn);
  const end = formatDate(endsOn);
  if (start === end) return <>{start}</>;
  return <>{start} – {end}</>;
}

interface TournamentCardProps {
  item: PublicTournamentSummary;
}

function TournamentCard({ item }: TournamentCardProps) {
  const levels: string[] = [];
  if (item.phongTraoEnabled) levels.push("Phong trào");
  if (item.nangCaoEnabled) levels.push("Nâng cao");

  return (
    <motion.div
      className="ptl-card"
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: 8 }}
      transition={{ type: "spring", stiffness: 260, damping: 24 }}
      onClick={() => { window.location.href = `/tournaments/${item.id}`; }}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") window.location.href = `/tournaments/${item.id}`; }}
    >
      <div className="ptl-card-top">
        <div className="ptl-card-title-row">
          <span className="ptl-card-name">{item.name}</span>
          <StatusBadge status={item.status} />
        </div>
        {item.organizerName && (
          <span className="ptl-card-organizer">{item.organizerName}</span>
        )}
      </div>

      <div className="ptl-card-meta">
        {item.location && (
          <span className="ptl-card-meta-item">
            <MapPin size={13} />
            {item.location}
          </span>
        )}
        <span className="ptl-card-meta-item">
          <Calendar size={13} />
          <DateRange startsOn={item.startsOn} endsOn={item.endsOn} />
        </span>
        <span className="ptl-card-meta-item">
          <Users size={13} />
          {item.participantCount} đoàn tham dự
        </span>
      </div>

      <div className="ptl-card-footer">
        <div className="ptl-card-levels">
          {levels.map((lvl) => (
            <span key={lvl} className="ptl-level-chip">{lvl}</span>
          ))}
        </div>
        {item.status === "REGISTRATION_OPEN" && item.registrationDeadline && (
          <span className="ptl-deadline">
            Hạn đăng ký: {formatDate(item.registrationDeadline)}
          </span>
        )}
      </div>
    </motion.div>
  );
}

function SkeletonCard() {
  return (
    <div className="ptl-card ptl-card--skeleton">
      <div className="ptl-skeleton ptl-skeleton--title" />
      <div className="ptl-skeleton ptl-skeleton--line" />
      <div className="ptl-skeleton ptl-skeleton--line short" />
    </div>
  );
}

function EmptyState({ phase }: { phase: TournamentPhase }) {
  const messages: Record<TournamentPhase, string> = {
    UPCOMING: "Hiện chưa có giải thi đấu nào sắp diễn ra.",
    ONGOING: "Hiện không có giải thi đấu nào đang diễn ra.",
    FINISHED: "Chưa có giải thi đấu nào đã kết thúc.",
  };
  return (
    <div className="ptl-empty">
      <Trophy size={40} strokeWidth={1.4} />
      <p>{messages[phase]}</p>
    </div>
  );
}

interface PagedResult {
  items: PublicTournamentSummary[];
  total: number;
}

export function PublicTournamentListPage() {
  const [phase, setPhase] = useState<TournamentPhase>("UPCOMING");
  const [page, setPage] = useState(1);
  const [data, setData] = useState<PagedResult>({ items: [], total: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    const offset = (page - 1) * PAGE_SIZE;
    apiGet<PublicTournamentSummary[] | PagedResult>(
      `/api/public/tournaments?phase=${phase}&limit=${PAGE_SIZE}&offset=${offset}`
    )
      .then((res) => {
        if (cancelled) return;
        if (Array.isArray(res)) {
          const sliced = res.slice(offset, offset + PAGE_SIZE);
          setData({ items: sliced, total: res.length });
        } else {
          setData(res);
        }
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(err instanceof Error ? err.message : "Không thể tải danh sách giải.");
        setLoading(false);
      });

    return () => { cancelled = true; };
  }, [phase, page]);

  const totalPages = Math.max(1, Math.ceil(data.total / PAGE_SIZE));

  function handlePhaseChange(p: TournamentPhase) {
    setPhase(p);
    setPage(1);
  }

  return (
    <div className="ptl-root">
      {/* Header */}
      <div className="ptl-header">
        <Trophy size={28} strokeWidth={1.6} className="ptl-header-icon" />
        <h1 className="ptl-header-title">Các Giải Thi Đấu</h1>
      </div>

      {/* Tab bar */}
      <div className="ptl-tabs" role="tablist">
        {TABS.map((tab) => (
          <button
            key={tab.phase}
            className={`ptl-tab${phase === tab.phase ? " ptl-tab--active" : ""}`}
            role="tab"
            aria-selected={phase === tab.phase}
            onClick={() => handlePhaseChange(tab.phase)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Content */}
      <div className="ptl-content">
        {error && (
          <div className="ptl-error">{error}</div>
        )}

        {loading ? (
          <div className="ptl-list">
            <SkeletonCard />
            <SkeletonCard />
            <SkeletonCard />
          </div>
        ) : !error && data.items.length === 0 ? (
          <EmptyState phase={phase} />
        ) : (
          <AnimatePresence mode="wait">
            <motion.div
              key={phase + "-" + page}
              className="ptl-list"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.18 }}
            >
              {data.items.map((item) => (
                <TournamentCard key={item.id} item={item} />
              ))}
            </motion.div>
          </AnimatePresence>
        )}

        {/* Pagination */}
        {!loading && !error && totalPages > 1 && (
          <div className="ptl-pagination">
            <button
              className="ptl-page-btn"
              disabled={page <= 1}
              onClick={() => setPage((p) => Math.max(1, p - 1))}
            >
              ‹ Trước
            </button>
            <span className="ptl-page-info">
              Trang {page} / {totalPages}
            </span>
            <button
              className="ptl-page-btn"
              disabled={page >= totalPages}
              onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
            >
              Tiếp ›
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
