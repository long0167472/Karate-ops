import { AlertTriangle, CalendarCheck, ChevronRight, CircleDollarSign, Shield, Trophy } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { canManageClub, hasRole } from "./authNavigation";
import { apiGet } from "./apiClient";
import { AuthenticatedShell } from "./components/AuthenticatedShell";
import { fetchClubDirectory } from "./features/clubs/clubApi";
import type { AuthUserResponse, OrganizationDashboardOverviewResponse, OrganizationResponse, PublicTournamentSummary } from "./types";

interface AuthenticatedHomePageProps {
  user: AuthUserResponse;
  actualUser: AuthUserResponse;
  viewAsRole: string;
  setViewAsRole: (role: string) => void;
  onLogout: () => void;
}

export function AuthenticatedHomePage({
  user,
  actualUser,
  viewAsRole,
  setViewAsRole,
  onLogout
}: AuthenticatedHomePageProps) {
  const [clubs, setClubs] = useState<OrganizationResponse[]>([]);
  const [dashboards, setDashboards] = useState<Record<string, OrganizationDashboardOverviewResponse>>({});
  const [tournaments, setTournaments] = useState<PublicTournamentSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const manageMode = canManageClub(user);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    const load = async () => {
      if (!manageMode) {
        window.location.replace("/member");
        return;
      }

      const [{ clubs: nextClubs, dashboards: nextDashboards }, tournamentRes] = await Promise.all([
        fetchClubDirectory(hasRole(user, "GLOBAL_ADMIN"), user.primaryOrganizationId),
        apiGet<PublicTournamentSummary[] | { items: PublicTournamentSummary[]; total: number }>(
          "/api/public/tournaments?phase=UPCOMING&limit=6&offset=0"
        )
      ]);

      if (cancelled) return;
      setClubs(nextClubs);
      setDashboards(nextDashboards);
      setTournaments(Array.isArray(tournamentRes) ? tournamentRes : tournamentRes.items);
      setLoading(false);
    };

    load().catch((err: unknown) => {
      if (cancelled) return;
      setError(err instanceof Error ? err.message : "Không thể tải dashboard.");
      setLoading(false);
    });

    return () => {
      cancelled = true;
    };
  }, [manageMode, user]);

  const metrics = useMemo(() => {
    const rows = clubs.map((club) => dashboards[club.id]).filter(Boolean);
    const totalSessions = rows.reduce((sum, item) => sum + (item?.attendanceSessions ?? 0), 0);
    const needsAttention = rows.filter((item) => (item?.attendanceRate ?? 0) < 70 || (item?.activeMembers ?? 0) === 0).length;
    return {
      clubs: clubs.length,
      needsAttention,
      totalSessions,
      upcomingTournaments: tournaments.length
    };
  }, [clubs, dashboards, tournaments]);

  const alerts = useMemo(() => {
    return clubs
      .map((club) => ({
        club,
        dashboard: dashboards[club.id]
      }))
      .filter(({ dashboard }) => !dashboard || dashboard.attendanceRate < 70 || dashboard.activeMembers === 0)
      .slice(0, 5);
  }, [clubs, dashboards]);

  const leadClubHref = user.primaryOrganizationId ? `/clubs/${user.primaryOrganizationId}?tab=overview` : "/clubs";

  return (
    <AuthenticatedShell
      user={user}
      actualUser={actualUser}
      viewAsRole={viewAsRole}
      setViewAsRole={setViewAsRole}
      onLogout={onLogout}
      activeNav="overview"
      eyebrow={hasRole(user, "GLOBAL_ADMIN") ? "Operations overview" : "Workspace overview"}
      title={hasRole(user, "GLOBAL_ADMIN") ? "Tổng quan vận hành" : "Câu lạc bộ của tôi"}
      description={hasRole(user, "GLOBAL_ADMIN")
        ? "Theo dõi CLB, tín hiệu cần xử lý và lối vào nhanh tới các module vận hành."
        : "Điểm vào chung cho CLB, học phí, lịch tập và các tác vụ cần xử lý trong ngày."}
      breadcrumbs={[{ label: "Ứng dụng", href: "/app" }, { label: "Tổng quan" }]}
      headerActions={
        <>
          <a className="auth-page-secondary-action" href="/clubs">Mở danh sách CLB</a>
          <a className="auth-page-primary-action" href={hasRole(user, "GLOBAL_ADMIN") ? "/tournaments" : leadClubHref}>
            {hasRole(user, "GLOBAL_ADMIN") ? "Đi tới giải đấu" : "Mở CLB của tôi"}
          </a>
        </>
      }
    >
      {error ? <div className="auth-inline-error"><AlertTriangle size={18} /> {error}</div> : null}

      <section className="auth-metric-strip">
        <article className="auth-metric-card compact">
          <span>Tổng CLB</span>
          <strong>{loading ? "..." : metrics.clubs}</strong>
        </article>
        <article className="auth-metric-card compact warn">
          <span>Cần chú ý</span>
          <strong>{loading ? "..." : metrics.needsAttention}</strong>
        </article>
        <article className="auth-metric-card compact">
          <span>Buổi tập đã ghi nhận</span>
          <strong>{loading ? "..." : metrics.totalSessions}</strong>
        </article>
        <article className="auth-metric-card compact">
          <span>Giải sắp mở</span>
          <strong>{loading ? "..." : metrics.upcomingTournaments}</strong>
        </article>
      </section>

      <section className="auth-overview-columns sharp">
        <article className="auth-overview-panel primary">
          <div className="auth-panel-head">
            <div>
              <span>Cảnh báo ưu tiên</span>
              <h2>CLB nên kiểm tra trước</h2>
            </div>
            <Shield size={18} />
          </div>
          {loading ? (
            <div className="auth-empty-state">Đang tải tín hiệu vận hành...</div>
          ) : alerts.length === 0 ? (
            <div className="auth-empty-state">Chưa có cảnh báo nổi bật. Các CLB đang ở trạng thái ổn định.</div>
          ) : (
            <div className="auth-list-block">
              {alerts.map(({ club, dashboard }) => (
                <a className="auth-list-row tone-warn" key={club.id} href={`/clubs/${club.id}?tab=overview`}>
                  <div>
                    <strong>{club.name}</strong>
                    <span>{club.province || "Chưa cập nhật địa điểm"}</span>
                  </div>
                  <small>
                    {!dashboard ? "Thiếu dữ liệu" : dashboard.activeMembers === 0 ? "Chưa có thành viên" : `${Math.round(dashboard.attendanceRate)}% chuyên cần`}
                  </small>
                  <ChevronRight size={16} />
                </a>
              ))}
            </div>
          )}
        </article>

        <article className="auth-overview-panel">
          <div className="auth-panel-head">
            <div>
              <span>Quick actions</span>
              <h2>Đi nhanh tới tác vụ chính</h2>
            </div>
            <ChevronRight size={18} />
          </div>
          <div className="auth-quick-stack">
            <a className="auth-quick-card lead" href="/clubs">
              <Shield size={18} />
              <div>
                <strong>Quản lý CLB</strong>
                <span>Mở danh sách CLB và bộ lọc vận hành.</span>
              </div>
              <ChevronRight size={16} />
            </a>

            <div className="auth-action-list">
              <a className="auth-action-row" href={user.primaryOrganizationId ? `/clubs/${user.primaryOrganizationId}?tab=fees` : "/clubs"}>
                <CircleDollarSign size={16} />
                <div>
                  <strong>Kiểm tra học phí</strong>
                  <span>Đi tới khu phí và công nợ của CLB.</span>
                </div>
              </a>
              <a className="auth-action-row" href={user.primaryOrganizationId ? `/clubs/${user.primaryOrganizationId}?tab=attendance` : "/clubs"}>
                <CalendarCheck size={16} />
                <div>
                  <strong>Xem lịch tập</strong>
                  <span>Kiểm tra buổi tập và tình trạng điểm danh.</span>
                </div>
              </a>
              <a className="auth-action-row" href="/tournaments">
                <Trophy size={16} />
                <div>
                  <strong>Đi tới giải đấu</strong>
                  <span>Mở module giải và các command links.</span>
                </div>
              </a>
            </div>
          </div>
        </article>
      </section>

      <section className="auth-overview-panel">
        <div className="auth-panel-head">
          <div>
            <span>Giải sắp diễn ra</span>
            <h2>Những giải đang mở đăng ký</h2>
          </div>
          <Trophy size={18} />
        </div>
        {loading ? (
          <div className="auth-empty-state">Đang tải danh sách giải...</div>
        ) : tournaments.length === 0 ? (
          <div className="auth-empty-state">Hiện chưa có giải sắp mở. Khi có đợt đăng ký mới, danh sách sẽ xuất hiện tại đây.</div>
        ) : (
          <div className="auth-list-block">
            {tournaments.map((event) => (
              <a className="auth-list-row" href={`/tournaments/${event.id}`} key={event.id}>
                <div>
                  <strong>{event.name}</strong>
                  <span>{event.location || "Chưa công bố địa điểm"}</span>
                </div>
                <ChevronRight size={16} />
              </a>
            ))}
          </div>
        )}
      </section>
    </AuthenticatedShell>
  );
}
