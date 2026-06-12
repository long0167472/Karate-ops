import { AnimatePresence, motion } from "framer-motion";
import {
  ArrowLeft,
  Award,
  Calendar,
  CalendarX,
  Clock,
  ExternalLink,
  MapPin,
  Radio,
  Trophy,
  Users
} from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { apiGet, errorMessage } from "./apiClient";
import type { TournamentExtended } from "./types";
import { TournamentRegistrationModal } from "./TournamentRegistrationModal";

interface TournamentPublicDetailPageProps {
  tournamentId: string;
}

function formatDate(iso: string | null | undefined) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });
}

function formatDateRange(startsOn: string, endsOn: string) {
  const start = new Date(startsOn);
  const end = new Date(endsOn);
  if (startsOn === endsOn) return formatDate(startsOn);
  if (start.getMonth() === end.getMonth() && start.getFullYear() === end.getFullYear()) {
    return `${start.getDate()}–${end.getDate()}/${String(end.getMonth() + 1).padStart(2, "0")}/${end.getFullYear()}`;
  }
  return `${formatDate(startsOn)} – ${formatDate(endsOn)}`;
}

function formatMoney(amount: number) {
  if (amount === 0) return "Miễn phí";
  return new Intl.NumberFormat("vi-VN").format(amount) + "đ";
}

function getCountdown(deadline: string | null) {
  if (!deadline) return null;
  const diff = new Date(deadline).getTime() - Date.now();
  if (diff <= 0) return null;
  const days = Math.floor(diff / 86400000);
  const hours = Math.floor((diff % 86400000) / 3600000);
  if (days > 0) return `${days} ngày ${hours} giờ`;
  return `${hours} giờ`;
}

interface StatusConfig {
  label: string;
  cls: string;
}

const STATUS_MAP: Record<string, StatusConfig> = {
  DRAFT: { label: "Bản nháp", cls: "badge--draft" },
  REGISTRATION_OPEN: { label: "Đang nhận đăng ký", cls: "badge--open" },
  REGISTRATION_CLOSED: { label: "Đã đóng đăng ký", cls: "badge--closed" },
  RUNNING: { label: "Đang thi đấu", cls: "badge--running" },
  COMPLETED: { label: "Đã kết thúc", cls: "badge--completed" },
  ARCHIVED: { label: "Lưu trữ", cls: "badge--archived" }
};

function StatusBadge({ status }: { status: string }) {
  const config = STATUS_MAP[status] ?? { label: status, cls: "badge--draft" };
  return <span className={`tpd-badge ${config.cls}`}>{config.label}</span>;
}

function LevelChip({ label }: { label: string }) {
  return <span className="tpd-level-chip">{label}</span>;
}

export function TournamentPublicDetailPage({ tournamentId }: TournamentPublicDetailPageProps) {
  const [tournament, setTournament] = useState<TournamentExtended | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [regOpen, setRegOpen] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await apiGet<TournamentExtended>(`/api/tournaments/${tournamentId}`);
      setTournament(data);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [tournamentId]);

  useEffect(() => {
    load();
  }, [load]);

  if (loading) {
    return (
      <div className="tpd-loading">
        <motion.div className="tpd-loading-dot" animate={{ opacity: [0.3, 1, 0.3] }} transition={{ duration: 1.4, repeat: Infinity }} />
        <span>Đang tải thông tin giải...</span>
      </div>
    );
  }

  if (error || !tournament) {
    return (
      <div className="tpd-error-shell">
        <a className="tpd-back-btn" href="/tournaments">
          <ArrowLeft size={16} /> Danh sách giải
        </a>
        <div className="tpd-error-box">
          <CalendarX size={36} />
          <p>{error ?? "Không tìm thấy thông tin giải đấu."}</p>
          <button onClick={load}>Thử lại</button>
        </div>
      </div>
    );
  }

  const { status } = tournament;
  const isUpcoming = status === "DRAFT" || status === "REGISTRATION_OPEN" || status === "REGISTRATION_CLOSED";
  const isLive = status === "RUNNING" || status === "COMPLETED" || status === "ARCHIVED";
  const countdown = getCountdown(tournament.registrationDeadline);

  const levels: string[] = [];
  if (tournament.phongTraoEnabled) levels.push("Phong trào");
  if (tournament.nangCaoEnabled) levels.push("Nâng cao");

  return (
    <>
      <motion.main
        className="tpd-page"
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ type: "spring", stiffness: 120, damping: 22 }}
      >
        {/* Back button */}
        <a className="tpd-back-btn" href="/tournaments">
          <ArrowLeft size={16} />
          <span>Danh sách giải</span>
        </a>

        {/* Header */}
        <header className="tpd-header">
          <div className="tpd-header-top">
            <StatusBadge status={status} />
            {levels.length > 0 && (
              <div className="tpd-levels">
                {levels.map((level) => <LevelChip key={level} label={level} />)}
              </div>
            )}
          </div>

          <h1 className="tpd-title">{tournament.name}</h1>

          <div className="tpd-header-meta">
            {tournament.organizerName && (
              <span className="tpd-meta-item">
                <Users size={14} />
                {tournament.organizerName}
              </span>
            )}
            {tournament.location && (
              <span className="tpd-meta-item">
                <MapPin size={14} />
                {tournament.location}
              </span>
            )}
            <span className="tpd-meta-item">
              <Calendar size={14} />
              {formatDateRange(tournament.startsOn, tournament.endsOn)}
            </span>
          </div>

          {tournament.description && (
            <p className="tpd-description">{tournament.description}</p>
          )}
        </header>

        {/* Info grid */}
        <section className="tpd-info-grid">
          <div className="tpd-info-card">
            <span className="tpd-info-label">
              <MapPin size={13} /> Địa điểm
            </span>
            <strong className="tpd-info-value">{tournament.location || "Chưa xác định"}</strong>
          </div>
          <div className="tpd-info-card">
            <span className="tpd-info-label">
              <Calendar size={13} /> Ngày thi đấu
            </span>
            <strong className="tpd-info-value">{formatDateRange(tournament.startsOn, tournament.endsOn)}</strong>
          </div>
          <div className="tpd-info-card">
            <span className="tpd-info-label">
              <Clock size={13} /> Hạn đăng ký
            </span>
            <strong className="tpd-info-value">{formatDate(tournament.registrationDeadline)}</strong>
          </div>
          <div className="tpd-info-card">
            <span className="tpd-info-label">
              <Trophy size={13} /> Phí đăng ký
            </span>
            <strong className="tpd-info-value">{formatMoney(tournament.registrationFee)}</strong>
          </div>
        </section>

        {/* Upcoming / Registration phase */}
        {isUpcoming && (
          <section className="tpd-action-section">
            {status === "REGISTRATION_OPEN" && countdown && (
              <div className="tpd-countdown">
                <Clock size={16} />
                <span>Còn <strong>{countdown}</strong> để đăng ký</span>
              </div>
            )}

            {status === "REGISTRATION_OPEN" && (
              <button
                className="tpd-register-btn"
                onClick={() => setRegOpen(true)}
              >
                <Award size={16} />
                Đăng ký tham dự
              </button>
            )}

            {status === "REGISTRATION_CLOSED" && (
              <div className="tpd-closed-notice">
                <CalendarX size={16} />
                <span>Đã đóng đăng ký</span>
              </div>
            )}

            {status === "DRAFT" && (
              <div className="tpd-draft-notice">
                <Clock size={16} />
                <span>Giải đấu chưa mở đăng ký</span>
              </div>
            )}
          </section>
        )}

        {/* Live / Results phase */}
        {isLive && (
          <section className="tpd-action-section">
            <div className="tpd-live-actions">
              {status === "RUNNING" && (
                <div className="tpd-live-indicator">
                  <Radio size={14} />
                  <span>Đang diễn ra</span>
                </div>
              )}
              <a
                className="tpd-action-btn tpd-action-btn--primary"
                href={`/standings/tournaments/${tournamentId}`}
              >
                <Trophy size={15} />
                Xem bảng xếp hạng
                <ExternalLink size={12} />
              </a>
              <a
                className="tpd-action-btn tpd-action-btn--secondary"
                href={`/dashboard/tournaments/${tournamentId}`}
              >
                <Radio size={15} />
                Xem live
                <ExternalLink size={12} />
              </a>
            </div>
          </section>
        )}
      </motion.main>

      <AnimatePresence>
        {regOpen && (
          <TournamentRegistrationModal
            tournamentId={tournamentId}
            open={regOpen}
            onClose={() => setRegOpen(false)}
            onSuccess={() => {
              setRegOpen(false);
              load();
            }}
          />
        )}
      </AnimatePresence>
    </>
  );
}
