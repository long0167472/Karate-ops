import { motion } from "framer-motion";
import {
  Activity,
  Award,
  Gauge,
  History,
  ListChecks,
  Monitor,
  Shield,
  Swords,
  Trophy,
  Users,
  Vote,
  Eye,
  Radio,
  Download,
  Plus,
  Wifi,
  WifiOff
} from "lucide-react";
import { useState, useEffect } from "react";
import type { TournamentResponse, TatamiResponse, AuthUserResponse } from "./types";
import { apiGet } from "./apiClient";
import { NotificationBell } from "./NotificationBell";
import { cx } from "./utils";

interface OperationsHubProps {
  connected: boolean;
  user: AuthUserResponse;
  actualUser: AuthUserResponse;
  viewAsRole: string;
  setViewAsRole: (role: string) => void;
  onLogout: () => void;
}

interface HubAction {
  id: string;
  icon: React.ReactNode;
  title: string;
  desc: string;
  href: string;
  disabled?: boolean;
  size?: "primary" | "secondary" | "utility";
  badge?: string;
}

function canManageClub(user: AuthUserResponse): boolean {
  return user.roles.includes("CLUB_MANAGER") || user.roles.includes("GLOBAL_ADMIN");
}

function canManageTournament(user: AuthUserResponse): boolean {
  return user.roles.includes("GLOBAL_ADMIN");
}

export function OperationsHub({
  connected,
  user,
  actualUser,
  viewAsRole,
  setViewAsRole,
  onLogout
}: OperationsHubProps) {
  const [tournaments, setTournaments] = useState<TournamentResponse[]>([]);
  const [tatamis, setTatamis] = useState<TatamiResponse[]>([]);
  const [tournamentId, setTournamentId] = useState(() => window.localStorage.getItem("karate-ops.lastTournamentId") || "");
  const [tatamiId, setTatamiId] = useState(() => window.localStorage.getItem("karate-ops.lastTatamiId") || "");

  useEffect(() => {
    apiGet<TournamentResponse[]>("/api/tournaments")
      .then((data) => {
        setTournaments(data);
        if (!tournamentId && data[0]) setTournamentId(data[0].id);
      })
      .catch((err) => console.error("Failed to load tournaments:", err));
  }, [tournamentId]);

  useEffect(() => {
    if (!tournamentId) return;
    window.localStorage.setItem("karate-ops.lastTournamentId", tournamentId);
    apiGet<TatamiResponse[]>(`/api/tournaments/${tournamentId}/tatamis`)
      .then((data) => {
        setTatamis(data);
        if (!data.some((tatami) => tatami.id === tatamiId)) setTatamiId(data[0]?.id || "");
      })
      .catch((err) => console.error("Failed to load tatamis:", err));
  }, [tatamiId, tournamentId]);

  useEffect(() => {
    if (tatamiId) window.localStorage.setItem("karate-ops.lastTatamiId", tatamiId);
  }, [tatamiId]);

  const query = tournamentId && tatamiId ? `?tournamentId=${tournamentId}&tatamiId=${tatamiId}` : "";

  // Primary actions (Tournament & Tatami)
  const primaryActions: HubAction[] = [
    {
      id: "tournament-admin",
      icon: <Trophy />,
      title: "Tournament Admin",
      desc: "Tạo giải, duyệt đoàn, chia tatami, hạng cân và hồ sơ thi đấu.",
      href: "/tournaments",
      disabled: !canManageTournament(user),
      size: "primary",
      badge: tournaments.length > 0 ? `${tournaments.length}` : undefined
    },
    {
      id: "tatami-control",
      icon: <Gauge />,
      title: "Tatami Control",
      desc: "Bàn thư ký điều khiển điểm, đồng hồ, cảnh báo và preview.",
      href: `/control${query}`,
      disabled: !query,
      size: "primary",
      badge: tatamis.length > 0 ? `${tatamis.length}` : undefined
    }
  ];

  // Secondary features
  const secondaryActions: HubAction[] = [
    {
      id: "member-portal",
      icon: <Users />,
      title: "Member Portal",
      desc: "Xem hồ sơ của mình, học phí, chuyên cần và gửi request xin nghỉ.",
      href: "/member",
      size: "secondary"
    },
    {
      id: "club-space",
      icon: <Activity />,
      title: "Không gian CLB",
      desc: "Quản lý thành viên, roster, học phí, chuyên cần và dashboard.",
      href: "/clubs",
      disabled: !canManageClub(user),
      size: "secondary"
    },
    {
      id: "club-dashboard",
      icon: <Shield />,
      title: "Dashboard CLB",
      desc: "Mở tổng quan CLB đã gộp với cảnh báo, chuyên cần và readiness.",
      href: user.primaryOrganizationId ? `/clubs/${user.primaryOrganizationId}?tab=overview` : "/clubs",
      disabled: !canManageClub(user) && !user.primaryOrganizationId,
      size: "secondary"
    },
    {
      id: "public-tournaments",
      icon: <Trophy />,
      title: "Giải công khai",
      desc: "Xem danh sách các giải đấu công khai, đăng ký tham dự.",
      href: "/tournaments/public",
      size: "secondary"
    },
    {
      id: "tournament-dashboard",
      icon: <History />,
      title: "Tournament Dashboard",
      desc: "Theo dõi tổng quan giải, huy chương, tatami và trạng thái trận.",
      href: tournamentId ? `/dashboard/tournaments/${tournamentId}` : "#",
      disabled: !tournamentId,
      size: "secondary"
    },
    {
      id: "public-standings",
      icon: <Award />,
      title: "Standings",
      desc: "Xem bảng xếp hạng và kết quả giải đấu công khai.",
      href: "/tournaments/public",
      size: "secondary"
    }
  ];

  // Utility actions (tatami tools)
  const utilityActions: HubAction[] = [
    {
      id: "display",
      icon: <Monitor />,
      title: "Display",
      desc: "Bảng điểm fullscreen cho TV, màn chiếu hoặc LED.",
      href: `/display${query}`,
      disabled: !query,
      size: "utility"
    },
    {
      id: "judge-vote",
      icon: <Vote />,
      title: "Judge Vote",
      desc: "Màn bỏ phiếu Kata cho trọng tài.",
      href: `/judge${query}`,
      disabled: !query,
      size: "utility"
    },
    {
      id: "obs-overlay",
      icon: <Eye />,
      title: "OBS Overlay",
      desc: "Score strip nền trong suốt cho livestream.",
      href: `/overlay${query}`,
      disabled: !query,
      size: "utility"
    }
  ];

  const actions = {
    primary: primaryActions,
    secondary: secondaryActions,
    utility: utilityActions
  };

  return (
    <main className="operations-hub">
      {/* Header */}
      <motion.header
        className="hub-header"
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ type: "spring", stiffness: 120, damping: 20 }}
      >
        <div className="hub-header-content">
          <div className="hub-brand">
            <div className="hub-mark">K</div>
            <div>
              <h1>Karate Ops</h1>
              <p>Operations Hub</p>
            </div>
          </div>

          <div className="hub-user-context">
            <span className="hub-greeting">Chào {user.displayName}</span>
            <span className="hub-role">{user.roles.join(" · ")}</span>
            {user.primaryOrganizationName && (
              <span className="hub-org">{user.primaryOrganizationName}</span>
            )}
          </div>
        </div>

        <div className="hub-header-controls">
          <div className={cx("hub-connection", connected ? "connected" : "disconnected")}>
            {connected ? <Wifi size={16} /> : <WifiOff size={16} />}
            <span>{connected ? "Connected" : "Offline"}</span>
          </div>

          {actualUser.roles.includes("GLOBAL_ADMIN") && (
            <label className="hub-view-as">
              <span>View as</span>
              <select value={viewAsRole} onChange={(event) => setViewAsRole(event.target.value)}>
                <option value="ACTUAL">Actual</option>
                <option value="GLOBAL_ADMIN">Global admin</option>
                <option value="CLUB_MANAGER">Admin CLB</option>
                <option value="MEMBER">Member</option>
              </select>
            </label>
          )}

          <NotificationBell userId={actualUser.id} />
          <button className="hub-logout" onClick={onLogout}>
            Đăng xuất
          </button>
        </div>
      </motion.header>

      {/* Tatami Selector */}
      <motion.section
        className="hub-selector"
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ type: "spring", stiffness: 120, damping: 20, delay: 0.1 }}
      >
        <div className="selector-head">
          <div>
            <h2>Khóa giải và tatami trước khi mở control</h2>
            <p>Chọn tournament và tatami để truy cập bàn điều khiển, display, judge vote hoặc OBS overlay.</p>
          </div>
        </div>

        <div className="selector-grid">
          <label className="selector-field">
            <span>Tournament</span>
            <select value={tournamentId} onChange={(event) => setTournamentId(event.target.value)}>
              {tournaments.length === 0 ? <option value="">Chưa có giải đấu</option> : null}
              {tournaments.map((tournament) => (
                <option key={tournament.id} value={tournament.id}>
                  {tournament.name}
                </option>
              ))}
            </select>
          </label>

          <label className="selector-field">
            <span>Tatami</span>
            <select value={tatamiId} onChange={(event) => setTatamiId(event.target.value)}>
              {tatamis.length === 0 ? <option value="">Chưa có tatami</option> : null}
              {tatamis.map((tatami) => (
                <option key={tatami.id} value={tatami.id}>
                  {tatami.name || `Tatami ${tatami.tatamiNo}`}
                </option>
              ))}
            </select>
          </label>
        </div>
      </motion.section>

      {/* Primary Actions */}
      <motion.section
        className="hub-primary"
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ type: "spring", stiffness: 120, damping: 20, delay: 0.2 }}
      >
        {actions.primary.map((action) => (
          <HubActionCard key={action.id} action={action} />
        ))}
      </motion.section>

      {/* Secondary Actions - Bento Grid */}
      <motion.section
        className="hub-secondary"
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ type: "spring", stiffness: 120, damping: 20, delay: 0.3 }}
      >
        <div className="secondary-grid">
          {actions.secondary.map((action, idx) => (
            <HubActionCard key={action.id} action={action} delay={idx * 0.05} />
          ))}
        </div>
      </motion.section>

      {/* Utility Actions - Bottom Row */}
      <motion.section
        className="hub-utility"
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ type: "spring", stiffness: 120, damping: 20, delay: 0.4 }}
      >
        <div className="utility-grid">
          {actions.utility.map((action, idx) => (
            <HubActionCard key={action.id} action={action} delay={idx * 0.05} />
          ))}
        </div>
      </motion.section>
    </main>
  );
}

interface HubActionCardProps {
  action: HubAction;
  delay?: number;
}

function HubActionCard({ action, delay = 0 }: HubActionCardProps) {
  return (
    <motion.a
      href={action.disabled ? "#" : action.href}
      className={cx(
        "hub-action-card",
        `hub-action-${action.size || "secondary"}`,
        action.disabled && "disabled"
      )}
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ type: "spring", stiffness: 120, damping: 20, delay }}
      whileHover={!action.disabled ? { y: -4 } : undefined}
    >
      <div className="action-icon">
        {action.icon}
      </div>
      <div className="action-content">
        <strong className="action-title">{action.title}</strong>
        <p className="action-desc">{action.desc}</p>
      </div>
      {action.badge && (
        <span className="action-badge">{action.badge}</span>
      )}
    </motion.a>
  );
}
