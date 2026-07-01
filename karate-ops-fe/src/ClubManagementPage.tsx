import { AnimatePresence, motion } from "framer-motion";
import {
  AlertTriangle,
  Award,
  CalendarCheck,
  ChevronRight,
  CircleDollarSign,
  Home,
  PanelLeftClose,
  PanelLeftOpen,
  Plus,
  Search,
  Shield,
  UserPlus,
  Users,
  X
} from "lucide-react";
import { type CSSProperties, type FormEvent, type ReactNode, useCallback, useEffect, useMemo, useState } from "react";
import { apiDelete, apiGet, apiPatch, apiPost } from "./apiClient";
import { fetchClubDirectory, fetchClubWorkspace } from "./features/clubs/clubApi";
import { AttendanceTab } from "./features/clubs/components/AttendanceTab";
import { ClubActionsMenu } from "./features/clubs/components/ClubActionsMenu";
import { FeesTab } from "./features/clubs/components/FeesTab";
import { LeaveRequestsTab } from "./features/clubs/components/LeaveRequestsTab";
import { MembersTab } from "./features/clubs/components/MembersTab";
import { CLUB_TABS, MEMBER_ROLES, MEMBER_STATUSES, PAYMENT_STATUSES, WEEKDAYS, type ClubDrawer, type ClubTab } from "./features/clubs/clubConstants";
import {
  attendancePercent,
  errorMessage,
  formatDate,
  initials,
  localDateTimeIso,
  normalizeText,
  paymentLabel,
  roleLabel,
  slugCode,
  statusLabel,
  today
} from "./features/clubs/clubUtils";
import { AuthenticatedShell } from "./components/AuthenticatedShell";
import type {
  AccountRequestResponse,
  AthleteResponse,
  AttendanceSessionResponse,
  AuthUserResponse,
  ClubFeeOverviewResponse,
  ClubMemberResponse,
  ClubRosterResponse,
  ClubTrainingScheduleResponse,
  LeaveRequestResponse,
  MemberAccountCreateResponse,
  OrganizationAttendanceDashboardResponse,
  OrganizationDashboardOverviewResponse,
  OrganizationResponse,
  PersonResponse
} from "./types";
import { cx } from "./utils";

const pageMotion = {
  initial: { opacity: 0, y: 14 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -8 }
};

export default function ClubManagementPage({
  user,
  actualUser,
  viewAsRole,
  setViewAsRole,
  onLogout
}: {
  user: AuthUserResponse;
  actualUser: AuthUserResponse;
  viewAsRole: string;
  setViewAsRole: (role: string) => void;
  onLogout: () => void;
}) {
  const pathParts = window.location.pathname.split("/").filter(Boolean);
  const clubId = pathParts[0] === "clubs" && pathParts[1] ? pathParts[1] : "";
  const params = new URLSearchParams(window.location.search);
  const initialTab = (params.get("tab") === "dashboard" ? "overview" : params.get("tab") || "overview") as ClubTab;

  const [organizations, setOrganizations] = useState<OrganizationResponse[]>([]);
  const [dashboards, setDashboards] = useState<Record<string, OrganizationDashboardOverviewResponse>>({});
  const [overview, setOverview] = useState<OrganizationDashboardOverviewResponse | null>(null);
  const [attendance, setAttendance] = useState<OrganizationAttendanceDashboardResponse | null>(null);
  const [financeOverview, setFinanceOverview] = useState<ClubFeeOverviewResponse | null>(null);
  const [accountRequests, setAccountRequests] = useState<AccountRequestResponse[]>([]);
  const [members, setMembers] = useState<ClubMemberResponse[]>([]);
  const [roster, setRoster] = useState<ClubRosterResponse[]>([]);
  const [sessions, setSessions] = useState<AttendanceSessionResponse[]>([]);
  const [schedule, setSchedule] = useState<ClubTrainingScheduleResponse | null>(null);
  const [athletes, setAthletes] = useState<AthleteResponse[]>([]);
  const [leaveRequests, setLeaveRequests] = useState<LeaveRequestResponse[]>([]);
  const [activeTab, setActiveTab] = useState<ClubTab>(CLUB_TABS.some((tab) => tab.id === initialTab) ? initialTab : "overview");
  const [selectedSessionId, setSelectedSessionId] = useState("");
  const [selectedDate, setSelectedDate] = useState(today());
  const [calendarMonth, setCalendarMonth] = useState(today().slice(0, 7));
  const [drawer, setDrawer] = useState<ClubDrawer>(null);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [busy, setBusy] = useState(false);
  const [loadingClubs, setLoadingClubs] = useState(true);
  const [loadingClub, setLoadingClub] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [clubSearch, setClubSearch] = useState("");
  const [provinceFilter, setProvinceFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [memberSearch, setMemberSearch] = useState("");
  const [memberRoleFilter, setMemberRoleFilter] = useState("ALL");
  const [memberStatusFilter, setMemberStatusFilter] = useState("ALL");
  const [memberTuitionFilter, setMemberTuitionFilter] = useState("ALL");
  const [memberStudentFilter, setMemberStudentFilter] = useState("ALL");
  const [rosterStatusFilter, setRosterStatusFilter] = useState("ALL");

  const [clubForm, setClubForm] = useState({
    name: "",
    shortName: "",
    code: "",
    province: "",
    address: "",
    contactEmail: "",
    contactPhone: "",
    country: "Việt Nam"
  });
  const [memberForm, setMemberForm] = useState(emptyMemberForm);
  const [rosterPersonId, setRosterPersonId] = useState("");
  const [sessionName, setSessionName] = useState("");
  const [scheduleForm, setScheduleForm] = useState({ name: "", daysOfWeek: [] as number[], startTime: "18:30", durationMinutes: 90, active: true });

  const isAdmin = hasRole(user, "GLOBAL_ADMIN");
  const userClubMember = useMemo(() => members.find((m) => m.userId === user.id), [members, user.id]);
  const isClubAdmin = isAdmin || (!!userClubMember && ["OWNER", "MANAGER"].includes(userClubMember.role));

  const mergeMember = useCallback((nextMember: ClubMemberResponse) => {
    setMembers((current) => current.map((member) => member.id === nextMember.id ? nextMember : member));
  }, []);

  const removeMember = useCallback((memberId: string) => {
    setMembers((current) => current.filter((member) => member.id !== memberId));
  }, []);

  const removeRosterItem = useCallback((rosterId: string) => {
    setRoster((current) => current.filter((item) => item.id !== rosterId));
  }, []);

  const appendSession = useCallback((session: AttendanceSessionResponse) => {
    setSessions((current) => [...current.filter((item) => item.id !== session.id), session]);
  }, []);

  const loadOrganizations = useCallback(async () => {
    setLoadingClubs(true);
    setError(null);
    try {
      const { clubs, dashboards: nextDashboards } = await fetchClubDirectory();
      setOrganizations(clubs);
      setDashboards(nextDashboards);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoadingClubs(false);
    }
  }, []);

  const loadClub = useCallback(async (id: string) => {
    if (!id) return;
    setLoadingClub(true);
    setError(null);
    try {
      const { overview: nextOverview, attendance: nextAttendance, accountRequests: nextAccountRequests, members: nextMembers, roster: nextRoster, sessions: nextSessions, schedule: nextSchedule, athletes: nextAthletes, finance: nextFinance } = await fetchClubWorkspace(id);
      setOverview(nextOverview);
      setAttendance(nextAttendance);
      setFinanceOverview(nextFinance || null);
      setAccountRequests(nextAccountRequests);
      setMembers(nextMembers);
      setRoster(nextRoster);
      setSessions(nextSessions);
      setSchedule(nextSchedule);
      setScheduleForm({
        name: nextSchedule.name,
        daysOfWeek: nextSchedule.daysOfWeek,
        startTime: nextSchedule.startTime,
        durationMinutes: nextSchedule.durationMinutes,
        active: nextSchedule.active
      });
      setAthletes(nextAthletes);
      setSelectedSessionId((current) => current || nextSessions[0]?.id || "");
      const nextLeaveRequests = await apiGet<LeaveRequestResponse[]>(`/api/organizations/${id}/attendance-leave-requests`);
      setLeaveRequests(nextLeaveRequests);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoadingClub(false);
    }
  }, []);

  const loadAccountRequests = useCallback(async (id: string) => {
    const rows = await apiGet<AccountRequestResponse[]>(`/api/organizations/${id}/account-requests`);
    setAccountRequests(rows);
  }, []);

  useEffect(() => {
    loadOrganizations();
  }, [loadOrganizations]);

  useEffect(() => {
    if (clubId) loadClub(clubId);
  }, [clubId, loadClub]);

  const provinces = useMemo(() => {
    return Array.from(new Set(organizations.map((org) => org.province).filter(Boolean) as string[])).sort((a, b) => a.localeCompare(b));
  }, [organizations]);

  const visibleClubs = useMemo(() => {
    const keyword = normalizeText(clubSearch);
    return organizations.filter((org) => {
      const haystack = normalizeText(`${org.name} ${org.shortName || ""} ${org.code || ""} ${org.province || ""}`);
      return (!keyword || haystack.includes(keyword))
        && (provinceFilter === "ALL" || org.province === provinceFilter)
        && (statusFilter === "ALL" || org.status === statusFilter);
    });
  }, [clubSearch, organizations, provinceFilter, statusFilter]);

  const selectedOrg = organizations.find((org) => org.id === clubId);
  const activeMembers = members.filter((member) => member.status === "ACTIVE");
  const rosterPersonIds = new Set(roster.map((item) => item.personId));
  const rosterCandidates = members.filter((member) => member.personId && member.status === "ACTIVE" && !rosterPersonIds.has(member.personId));
  const financeAssignmentsByMember = useMemo(
    () => groupFeeAssignmentsByMember(financeOverview?.assignments ?? []),
    [financeOverview?.assignments]
  );
  const financeStatusByMember = useMemo(
    () => Object.fromEntries(members.map((member) => [member.id, memberFinanceStatus(financeAssignmentsByMember[member.id] ?? [])])),
    [financeAssignmentsByMember, members]
  );

  const filteredMembers = useMemo(() => {
    const keyword = normalizeText(memberSearch);
    return members.filter((member) => {
      const name = normalizeText(`${member.personName || ""} ${member.userName || ""}`);
      return (!keyword || name.includes(keyword))
        && (memberRoleFilter === "ALL" || member.role === memberRoleFilter)
        && (memberStatusFilter === "ALL" || member.status === memberStatusFilter)
        && (memberTuitionFilter === "ALL" || financeStatusByMember[member.id] === memberTuitionFilter)
        && (memberStudentFilter === "ALL" || (memberStudentFilter === "STUDENT" ? member.student : !member.student));
    });
  }, [financeStatusByMember, memberRoleFilter, memberSearch, memberStatusFilter, memberStudentFilter, memberTuitionFilter, members]);

  const filteredRoster = useMemo(() => {
    return roster.filter((item) => rosterStatusFilter === "ALL" || item.status === rosterStatusFilter);
  }, [roster, rosterStatusFilter]);

  const attendanceRecords = sessions.flatMap((session) => session.records);
  const attendanceFallback = {
    present: attendanceRecords.filter((record) => record.status === "PRESENT").length,
    late: attendanceRecords.filter((record) => record.status === "LATE").length,
    absent: attendanceRecords.filter((record) => record.status === "ABSENT").length,
    excused: attendanceRecords.filter((record) => record.status === "EXCUSED").length
  };
  const attendanceRate = Math.round(attendance?.attendanceRate ?? overview?.attendanceRate ?? attendancePercent(attendanceRecords));
  const attendanceBreakdown = [
    { label: "Có mặt", value: attendance?.present ?? attendanceFallback.present, tone: "present" },
    { label: "Muộn", value: attendance?.late ?? attendanceFallback.late, tone: "late" },
    { label: "Vắng", value: attendance?.absent ?? attendanceFallback.absent, tone: "absent" },
    { label: "Có phép", value: attendance?.excused ?? attendanceFallback.excused, tone: "excused" }
  ];
  const lowAttendanceRows = attendance?.lowAttendance ?? [];
  const memberInsights = {
    activeStudents: members.filter((member) => member.student && member.status === "ACTIVE").length,
    tuitionPending: members.filter((member) => {
      const status = financeStatusByMember[member.id];
      return status === "PENDING" || status === "OVERDUE" || status === "PARTIAL";
    }).length,
    paidTuition: members.filter((member) => {
      const status = financeStatusByMember[member.id];
      return status === "PAID" || status === "WAIVED";
    }).length,
    hiddenAttendance: members.filter((member) => !member.attendanceViewEnabled).length
  };
  const pendingAccountRequests = accountRequests.filter((request) => request.status === "PENDING").length;
  const healthWarnings = [
    pendingAccountRequests > 0 ? `${pendingAccountRequests} yêu cầu tài khoản đang chờ duyệt.` : "",
    members.length === 0 ? "CLB chưa có thành viên." : "",
    roster.length === 0 ? "Chưa có VĐV trong roster." : "",
    !schedule || schedule.daysOfWeek.length === 0 ? "Chưa thiết lập lịch tập cố định trong tuần." : "",
    sessions.length === 0 ? "Chưa có buổi điểm danh nào." : "",
    attendanceRate > 0 && attendanceRate < 70 ? "Tỷ lệ chuyên cần đang thấp hơn 70%." : "",
    (financeOverview?.summary.totalOutstanding ?? 0) > 0 ? `${formatMoneyShort(financeOverview?.summary.totalOutstanding)} công nợ đang mở.` : ""
  ].filter(Boolean);

  const submit = async (event: FormEvent, action: () => Promise<void>) => {
    event.preventDefault();
    await submitNoEvent(action, setBusy, setError);
  };

  const updateClubForm = (field: keyof typeof clubForm, value: string) => {
    const next = { ...clubForm, [field]: field === "code" ? slugCode(value) : value };
    if (field === "name" && !clubForm.code) next.code = slugCode(value);
    if (field === "name" && !clubForm.shortName) next.shortName = value.slice(0, 28);
    setClubForm(next);
  };

  const resetClubForm = () => setClubForm({ name: "", shortName: "", code: "", province: "", address: "", contactEmail: "", contactPhone: "", country: "Việt Nam" });

  const handleEditClub = () => {
    if (selectedOrg) {
      setClubForm({
        name: selectedOrg.name,
        shortName: selectedOrg.shortName || "",
        code: selectedOrg.code || "",
        province: selectedOrg.province || "",
        address: selectedOrg.address || "",
        contactEmail: selectedOrg.contactEmail || "",
        contactPhone: selectedOrg.contactPhone || "",
        country: "Việt Nam"
      });
      setDrawer("club");
    }
  };

  const handleToggleClubInactive = async (makeInactive: boolean) => {
    if (!selectedOrg) return;
    setBusy(true);
    setError(null);
    try {
      const updated = await apiPatch<OrganizationResponse>(`/api/organizations/${selectedOrg.id}`, {
        status: makeInactive ? "INACTIVE" : "ACTIVE"
      });
      setOrganizations((current) =>
        current.map((org) => org.id === updated.id ? updated : org)
      );
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const handleDeleteClub = async () => {
    if (!selectedOrg) return;
    setBusy(true);
    setError(null);
    try {
      await apiDelete(`/api/organizations/${selectedOrg.id}`);
      setOrganizations((current) =>
        current.filter((org) => org.id !== selectedOrg.id)
      );
      window.location.href = "/clubs";
    } catch (err) {
      setError(errorMessage(err));
      setBusy(false);
    }
  };

  const setTab = (tab: ClubTab) => {
    setActiveTab(tab);
    window.history.replaceState(null, "", `/clubs/${clubId}?tab=${tab}`);
  };

  if (!clubId) {
    const directoryStats = {
      totalClubs: visibleClubs.length,
      activeMembers: visibleClubs.reduce((sum, org) => sum + (dashboards[org.id]?.activeMembers ?? 0), 0),
      totalAthletes: visibleClubs.reduce((sum, org) => sum + (dashboards[org.id]?.activeAthletes ?? 0), 0),
      needsAttention: visibleClubs.filter((org) => {
        const dashboard = dashboards[org.id];
        return !dashboard || dashboard.attendanceRate < 70 || dashboard.activeMembers === 0;
      }).length
    };
    const featuredClub = visibleClubs[0];
    const featuredDashboard = featuredClub ? dashboards[featuredClub.id] : undefined;

    return (
      <AuthenticatedShell
        user={user}
        actualUser={actualUser}
        viewAsRole={viewAsRole}
        setViewAsRole={setViewAsRole}
        onLogout={onLogout}
        activeNav="clubs"
        eyebrow="Club operations"
        title="Danh sách các câu lạc bộ"
        description="Quét nhanh CLB cần xử lý, lọc theo khu vực và mở thẳng workspace quản lý."
        breadcrumbs={[{ label: "Ứng dụng", href: "/app" }, { label: "Câu lạc bộ" }]}
        headerActions={
          isAdmin
            ? <button className="auth-page-primary-action button-reset" onClick={() => setDrawer("club")}><Plus size={18} /> Tạo CLB mới</button>
            : <a className="auth-page-primary-action" href={user.primaryOrganizationId ? `/clubs/${user.primaryOrganizationId}?tab=overview` : "/clubs"}>Mở CLB của tôi</a>
        }
      >
        <section className="auth-metric-strip">
          <article className="auth-metric-card compact">
            <span>Tổng CLB</span>
            <strong>{loadingClubs ? "..." : directoryStats.totalClubs}</strong>
          </article>
          <article className="auth-metric-card compact">
            <span>Thành viên active</span>
            <strong>{loadingClubs ? "..." : directoryStats.activeMembers}</strong>
          </article>
          <article className="auth-metric-card compact">
            <span>VĐV active</span>
            <strong>{loadingClubs ? "..." : directoryStats.totalAthletes}</strong>
          </article>
          <article className="auth-metric-card compact warn">
            <span>Cần chú ý</span>
            <strong>{loadingClubs ? "..." : directoryStats.needsAttention}</strong>
          </article>
        </section>

        <section className="auth-overview-panel">
          <div className="auth-panel-head">
            <div>
              <span>Club directory</span>
              <h2>Danh sách CLB theo tín hiệu vận hành</h2>
            </div>
            {featuredClub ? <a className="auth-inline-link" href={`/clubs/${featuredClub.id}`}>Mở CLB nổi bật <ChevronRight size={15} /></a> : null}
          </div>

          <div className="club-directory-toolbar">
            <div className="club-toolbar">
              <label className="club-search">
                <Search size={18} />
                <input value={clubSearch} onChange={(event) => setClubSearch(event.target.value)} placeholder="Tìm theo tên, mã CLB, tỉnh thành" />
              </label>
              <select value={provinceFilter} onChange={(event) => setProvinceFilter(event.target.value)}>
                <option value="ALL">Tất cả tỉnh thành</option>
                {provinces.map((province) => <option key={province} value={province}>{province}</option>)}
              </select>
              <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
                <option value="ALL">Tất cả trạng thái</option>
                {MEMBER_STATUSES.slice(0, 3).map((status) => <option key={status} value={status}>{statusLabel(status)}</option>)}
              </select>
            </div>
            {featuredClub && featuredDashboard ? (
              <div className="club-directory-spotlight">
                <strong>{featuredClub.name}</strong>
                <span>{featuredDashboard.activeMembers} thành viên · {Math.round(featuredDashboard.attendanceRate)}% chuyên cần</span>
              </div>
            ) : null}
          </div>

          {error ? <InlineNotice tone="danger" title="Không tải được dữ liệu" text={error} /> : null}
          {loadingClubs ? <ClubListSkeleton /> : visibleClubs.length === 0 ? (
            <EmptyState title="Không tìm thấy CLB phù hợp" text="Thử đổi bộ lọc hoặc tạo CLB mới nếu bạn là quản trị hệ thống." />
          ) : (
            <motion.div className="club-card-stack compact" initial="hidden" animate="show" variants={{ show: { transition: { staggerChildren: 0.04 } } }}>
              {visibleClubs.map((org) => {
                const dashboard = dashboards[org.id];
                const needsAttention = !dashboard || dashboard.attendanceRate < 70 || dashboard.activeMembers === 0;
                return (
                  <motion.a className={cx("club-list-card compact", needsAttention && "attention")} href={`/clubs/${org.id}`} key={org.id} variants={{ hidden: { opacity: 0, y: 8 }, show: { opacity: 1, y: 0 } }}>
                    <div className="club-list-identity">
                      <div className="club-mini-mark">{initials(org.name)}</div>
                      <div>
                        <strong>{org.name}</strong>
                        <span>{[org.code || "Chưa có mã", org.province || "Chưa có tỉnh thành"].join(" · ")}</span>
                      </div>
                    </div>
                    <div className="club-list-summary">
                      <span>{dashboard?.activeMembers ?? 0} thành viên</span>
                      <span>{dashboard?.activeAthletes ?? 0} VĐV</span>
                      <span>{Math.round(dashboard?.attendanceRate ?? 0)}% chuyên cần</span>
                    </div>
                    <div className="club-list-cta">
                      <strong>Mở quản lý</strong>
                      <ChevronRight size={18} />
                    </div>
                  </motion.a>
                );
              })}
            </motion.div>
          )}
        </section>

        <ClubDrawer drawer={drawer} title="Tạo CLB mới" onClose={() => setDrawer(null)}>
          <ClubForm
            busy={busy}
            clubForm={clubForm}
            error={error}
            onChange={updateClubForm}
            onSubmit={(event) => submit(event, async () => {
              const created = await apiPost<OrganizationResponse>("/api/organizations", { ...clubForm, type: "CLUB" });
              resetClubForm();
              setDrawer(null);
              await loadOrganizations();
              window.location.href = `/clubs/${created.id}`;
            })}
          />
        </ClubDrawer>
      </AuthenticatedShell>
    );
  }

  if (!selectedOrg && !loadingClubs && organizations.length > 0) {
    return (
      <AuthenticatedShell
        user={user}
        actualUser={actualUser}
        viewAsRole={viewAsRole}
        setViewAsRole={setViewAsRole}
        onLogout={onLogout}
        activeNav="clubs"
        eyebrow="Club operations"
        title="Không có quyền truy cập CLB này"
        description="Tài khoản hiện tại chỉ được xem CLB được phân quyền."
        breadcrumbs={[{ label: "Ứng dụng", href: "/app" }, { label: "Câu lạc bộ", href: "/clubs" }, { label: "Không khả dụng" }]}
      >
        <section className="club-access-denied">
          <Shield size={30} />
          <h1>Không có quyền truy cập CLB này</h1>
          <p>Tài khoản hiện tại chỉ được xem CLB được phân quyền. Hãy quay lại danh sách CLB để chọn đúng đơn vị.</p>
          <a className="club-primary-button" href="/clubs">Quay lại trung tâm CLB</a>
        </section>
      </AuthenticatedShell>
    );
  }

  return (
    <AuthenticatedShell
      user={user}
      actualUser={actualUser}
      viewAsRole={viewAsRole}
      setViewAsRole={setViewAsRole}
      onLogout={onLogout}
      activeNav="clubs"
      eyebrow={selectedOrg?.code || "Club workspace"}
      title={selectedOrg?.name || overview?.organizationName || "Đang tải CLB"}
      description="Giữ tab nghiệp vụ CLB hiện có trong một shell thống nhất để điều hướng xuyên module rõ ràng hơn."
      breadcrumbs={[{ label: "Ứng dụng", href: "/app" }, { label: "Câu lạc bộ", href: "/clubs" }, { label: selectedOrg?.shortName || selectedOrg?.name || "Workspace" }]}
      headerActions={
        <>
          <button className="auth-page-secondary-action button-reset" onClick={() => setDrawer("member")}><UserPlus size={18} /> Thêm thành viên</button>
          <button className="auth-page-primary-action button-reset" onClick={() => setDrawer("schedule")}><CalendarCheck size={18} /> Sửa lịch tập</button>
        </>
      }
    >
    <main className={cx("club-ops-page club-workspace-page auth-embedded-page", sidebarCollapsed && "sidebar-collapsed")}>
      <section className="club-workspace-shell">
        <aside className="club-context-panel">
          <div className="club-sidebar-head">
            <div className="club-feature-mark">{initials(selectedOrg?.name || overview?.organizationName || "CLB")}</div>
            <button
              className="club-sidebar-toggle"
              onClick={() => setSidebarCollapsed((current) => !current)}
              aria-label={sidebarCollapsed ? "Mở rộng thanh điều hướng CLB" : "Thu gọn thanh điều hướng CLB"}
            >
              {sidebarCollapsed ? <PanelLeftOpen size={19} /> : <PanelLeftClose size={19} />}
            </button>
          </div>
          <div className="club-context-identity">
            <div className="club-sidebar-copy">
              <h2>{selectedOrg?.shortName || selectedOrg?.name || "CLB"}</h2>
              <p>{selectedOrg?.address || "Chưa có địa chỉ võ đường."}</p>
            </div>
            <div className="club-context-meta">
              <span>{selectedOrg?.province || "Chưa cập nhật tỉnh thành"}</span>
              <span>{selectedOrg?.contactPhone || selectedOrg?.contactEmail || "Chưa có liên hệ"}</span>
            </div>
          </div>
          <a className="club-sidebar-home" href="/clubs" title="Trung tâm CLB">
            <Home size={18} />
            <span>Trung tâm CLB</span>
          </a>
          <nav className="club-tab-list workspace">
            {CLUB_TABS.map((tab) => (
              <button key={tab.id} className={cx(activeTab === tab.id && "active")} onClick={() => setTab(tab.id)} title={tab.label}>
                <b className="club-tab-code">{tabShort(tab.id)}</b>
                <span>{tab.label}</span>
                <small>{tab.hint}</small>
              </button>
            ))}
          </nav>
        </aside>

        <section className="club-workspace-main">
          <motion.header className="club-command-bar" {...pageMotion}>
            <div>
              <span className="club-ops-kicker">{selectedOrg?.code || "CLB"}</span>
              <h1>{selectedOrg?.name || overview?.organizationName || "Đang tải CLB"}</h1>
            </div>
            <div className="club-command-actions">
              <button className="club-secondary-button" onClick={() => setDrawer("member")}><UserPlus size={18} /> Thêm thành viên</button>
              <button className="club-secondary-button" onClick={() => setDrawer("roster")}><Award size={18} /> Thêm VĐV</button>
              <button className="club-primary-button" onClick={() => setDrawer("schedule")}><CalendarCheck size={18} /> Sửa lịch tập</button>
              {selectedOrg && <ClubActionsMenu
                club={selectedOrg}
                userIsAdmin={isClubAdmin}
                busy={busy}
                onEdit={handleEditClub}
                onToggleInactive={handleToggleClubInactive}
                onDelete={handleDeleteClub}
              />}
            </div>
          </motion.header>

          <section className="club-content-panel">
          {error ? <InlineNotice tone="danger" title="Có lỗi xảy ra" text={error} /> : null}
          {loadingClub ? <WorkspaceSkeleton /> : null}
          <AnimatePresence mode="wait">
            {!loadingClub && activeTab === "overview" ? (
              <motion.div key="overview" className="club-tab-content" {...pageMotion}>
                <section className="club-dashboard-hero">
                  <div>
                    <span className="club-ops-kicker">Tổng quan vận hành</span>
                    <h2>Dashboard CLB nằm ngay trong workspace.</h2>
                    <p>Không cần mở một màn riêng: sức khỏe CLB, chuyên cần, roster và việc cần xử lý được gom ở đây để bạn chuyển thẳng sang hành động.</p>
                  </div>
                  <div className="club-dashboard-score">
                    <span>Chuyên cần</span>
                    <strong>{attendanceRate}%</strong>
                    <small>{attendance?.sessions ?? overview?.attendanceSessions ?? sessions.length} buổi tập đã ghi nhận</small>
                  </div>
                </section>

                <div className="club-bento-grid club-dashboard-grid">
                  <HealthTile className="wide" icon={<Users />} label="Thành viên active" value={overview?.activeMembers ?? activeMembers.length} detail={`${members.length} hồ sơ trong CLB`} />
                  <HealthTile icon={<Award />} label="VĐV active" value={overview?.activeAthletes ?? roster.length} detail="Sẵn sàng đăng ký giải" />
                  <HealthTile icon={<CalendarCheck />} label="Buổi tập" value={overview?.attendanceSessions ?? sessions.length} detail={schedule?.name || "Chưa có lịch cố định"} />
                  <HealthTile icon={<CircleDollarSign />} label="Dòng tiền ròng" value={formatMoneyShort(financeOverview?.summary.netCash)} detail={`${formatMoneyShort(financeOverview?.summary.totalOutstanding)} công nợ mở`} />

                  <section className="club-attendance-dashboard-card">
                    <div className="club-dashboard-panel-head">
                      <div>
                        <span className="club-ops-kicker">Chuyên cần</span>
                        <h3>Phân bổ điểm danh</h3>
                      </div>
                      <button className="club-secondary-button" onClick={() => setTab("attendance")}>Mở điểm danh</button>
                    </div>
                    <div className="club-attendance-donut" style={{ "--attendance-rate": `${attendanceRate * 3.6}deg` } as CSSProperties}>
                      <strong>{attendanceRate}%</strong>
                      <span>{attendance?.records ?? attendanceRecords.length} lượt ghi nhận</span>
                    </div>
                    <div className="club-attendance-breakdown">
                      {attendanceBreakdown.map((item) => (
                        <div className={cx("club-attendance-stat", item.tone)} key={item.label}>
                          <b>{item.value}</b>
                          <span>{item.label}</span>
                        </div>
                      ))}
                    </div>
                  </section>

                  <section className="club-low-attendance-panel">
                    <div className="club-dashboard-panel-head">
                      <div>
                        <span className="club-ops-kicker">Cần chú ý</span>
                        <h3>VĐV chuyên cần thấp</h3>
                      </div>
                      <button className="club-secondary-button" onClick={() => setTab("members")}>Xem thành viên</button>
                    </div>
                    {lowAttendanceRows.length > 0 ? (
                      <div className="club-low-attendance-list">
                        {lowAttendanceRows.slice(0, 5).map((row) => (
                          <article className="club-low-attendance-row" key={row.athleteId}>
                            <div>
                              <strong>{row.athleteName}</strong>
                              <span>{row.presentOrLate}/{row.sessions} buổi có mặt hoặc đi muộn</span>
                            </div>
                            <b>{Math.round(row.attendanceRate)}%</b>
                          </article>
                        ))}
                      </div>
                    ) : (
                      <EmptyState title="Chưa có VĐV cần cảnh báo" text="Khi có VĐV chuyên cần thấp, danh sách ưu tiên sẽ xuất hiện tại đây." />
                    )}
                  </section>

                  <section className="club-readiness-panel">
                    <div className="club-dashboard-panel-head">
                      <div>
                        <span className="club-ops-kicker">Readiness</span>
                        <h3>Việc cần xử lý</h3>
                      </div>
                      <AlertTriangle size={22} />
                    </div>
                    {healthWarnings.length > 0 ? (
                      <ul>{healthWarnings.map((warning) => <li key={warning}>{warning}</li>)}</ul>
                    ) : (
                      <p>Dữ liệu CLB đang đủ tốt để dùng cho điểm danh và đăng ký giải.</p>
                    )}
                    <div className="club-dashboard-actions">
                      <button className="club-secondary-button" onClick={() => setDrawer("member")}><UserPlus size={18} /> Thêm thành viên</button>
                      <button className="club-secondary-button" onClick={() => setDrawer("roster")}><Award size={18} /> Thêm VĐV</button>
                      <button className="club-primary-button" onClick={() => setDrawer("schedule")}><CalendarCheck size={18} /> Sửa lịch tập</button>
                    </div>
                  </section>
                </div>
              </motion.div>
            ) : null}

            {!loadingClub && activeTab === "members" ? (
              <motion.div key="members" className="club-tab-content" {...pageMotion}>
                <MembersTab
                  clubId={clubId}
                  user={user}
                  isClubAdmin={isClubAdmin}
                  accountRequests={accountRequests}
                  members={members}
                  filteredMembers={filteredMembers}
                  sessions={sessions}
                  roster={roster}
                  athletes={athletes}
                  financeOverview={financeOverview}
                  busy={busy}
                  error={error}
                  memberSearch={memberSearch}
                  memberRoleFilter={memberRoleFilter}
                  memberStatusFilter={memberStatusFilter}
                  memberTuitionFilter={memberTuitionFilter}
                  memberStudentFilter={memberStudentFilter}
                  memberInsights={memberInsights}
                  setBusy={setBusy}
                  setError={setError}
                  setDrawer={setDrawer}
                  setMemberSearch={setMemberSearch}
                  setMemberRoleFilter={setMemberRoleFilter}
                  setMemberStatusFilter={setMemberStatusFilter}
                  setMemberTuitionFilter={setMemberTuitionFilter}
                  setMemberStudentFilter={setMemberStudentFilter}
                  mergeMember={mergeMember}
                  removeMember={removeMember}
                  setRoster={setRoster}
                  setAthletes={setAthletes}
                  onOpenFinance={() => setTab("fees")}
                  onDecideAccountRequest={async (requestId, status, decisionNote) => {
                    await submitNoEvent(async () => {
                      const result = await apiPatch<MemberAccountCreateResponse>(`/api/organizations/${clubId}/account-requests/${requestId}/decision`, { status, decisionNote: decisionNote || undefined });
                      await loadAccountRequests(clubId);
                      if (status === "APPROVED" && result.username && result.temporaryPassword) {
                        if (result.member) setMembers((current) => [result.member!, ...current]);
                        window.alert(`Tài khoản đã tạo:\nUsername: ${result.username}\nPassword: ${result.temporaryPassword}`);
                      }
                    }, setBusy, setError);
                  }}
                />
              </motion.div>
            ) : null}

            {!loadingClub && activeTab === "fees" ? (
              <motion.div key="fees" className="club-tab-content" {...pageMotion}>
                <FeesTab
                  clubId={clubId}
                  members={members}
                  overviewData={financeOverview}
                  onOverviewChange={setFinanceOverview}
                />
              </motion.div>
            ) : null}

            {!loadingClub && activeTab === "roster" ? (
              <motion.div key="roster" className="club-tab-content" {...pageMotion}>
                <div className="club-section-head">
                  <div>
                    <span className="club-ops-kicker">Hồ sơ thi đấu</span>
                    <h2>VĐV trong roster</h2>
                  </div>
                  <button className="club-primary-button" onClick={() => setDrawer("roster")}><Award size={18} /> Thêm VĐV</button>
                </div>
                {rosterCandidates.length > 0 ? (
                  <InlineNotice tone="warm" title="Có thành viên chưa có hồ sơ VĐV" text={`${rosterCandidates.length} thành viên active có thể đưa vào roster để dùng khi đăng ký giải.`} />
                ) : null}
                <div className="club-toolbar compact">
                  <select value={rosterStatusFilter} onChange={(event) => setRosterStatusFilter(event.target.value)}>
                    <option value="ALL">Tất cả trạng thái</option>
                    {MEMBER_STATUSES.map((status) => <option key={status} value={status}>{statusLabel(status)}</option>)}
                  </select>
                </div>
                {filteredRoster.length === 0 ? <EmptyState title="Roster chưa có VĐV" text="Chọn một thành viên active để tạo hồ sơ VĐV và thêm vào roster." action="Thêm VĐV" onAction={() => setDrawer("roster")} /> : (
                  <div className="club-roster-grid">
                    {filteredRoster.map((item, index) => (
                      <motion.article className="club-athlete-card" key={item.id} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: index * 0.04 }}>
                        <div className="club-avatar large">{initials(item.athleteName)}</div>
                        <div>
                          <strong>{item.athleteName}</strong>
                          <span>{statusLabel(item.status)} - Gia nhập {formatDate(item.joinedAt)}</span>
                        </div>
                        <button className="club-text-danger" disabled={busy} onClick={() => submitNoEvent(async () => {
                          await apiDelete(`/api/organizations/${clubId}/roster/${item.id}`);
                          removeRosterItem(item.id);
                        }, setBusy, setError)}>Gỡ khỏi roster</button>
                      </motion.article>
                    ))}
                  </div>
                )}
              </motion.div>
            ) : null}

            {!loadingClub && activeTab === "attendance" ? (
              <motion.div key="attendance" {...pageMotion}>
                <AttendanceTab
                  clubId={clubId}
                  roster={roster}
                  sessions={sessions}
                  schedule={schedule}
                  selectedDate={selectedDate}
                  setSelectedDate={setSelectedDate}
                  selectedSessionId={selectedSessionId}
                  setSelectedSessionId={setSelectedSessionId}
                  calendarMonth={calendarMonth}
                  setCalendarMonth={setCalendarMonth}
                  busy={busy}
                  setBusy={setBusy}
                  setError={setError}
                  setDrawer={setDrawer}
                  setSessions={setSessions}
                />
              </motion.div>
            ) : null}
            {!loadingClub && activeTab === "leaves" ? (
              <LeaveRequestsTab
                user={user}
                isClubAdmin={isClubAdmin}
                leaveRequests={leaveRequests}
                busy={busy}
                error={error}
                setBusy={setBusy}
                setError={setError}
                onApprove={(requestId, decisionNote) =>
                  submitNoEvent(async () => {
                    await apiPatch(`/api/attendance-leave-requests/${requestId}/decision`, {
                      status: "APPROVED",
                      decisionNote
                    });
                    const updated = await apiGet<LeaveRequestResponse[]>(`/api/organizations/${clubId}/attendance-leave-requests`);
                    setLeaveRequests(updated);
                  }, setBusy, setError)
                }
                onReject={(requestId, decisionNote) =>
                  submitNoEvent(async () => {
                    await apiPatch(`/api/attendance-leave-requests/${requestId}/decision`, {
                      status: "REJECTED",
                      decisionNote
                    });
                    const updated = await apiGet<LeaveRequestResponse[]>(`/api/organizations/${clubId}/attendance-leave-requests`);
                    setLeaveRequests(updated);
                  }, setBusy, setError)
                }
              />
            ) : null}
          </AnimatePresence>
        </section>
        </section>
      </section>

      <ClubDrawer drawer={drawer === "club" && selectedOrg ? "club" : null} title="Chỉnh sửa thông tin CLB" onClose={() => setDrawer(null)}>
        <ClubForm
          busy={busy}
          clubForm={clubForm}
          error={error}
          onChange={updateClubForm}
          submitLabel="Lưu thay đổi"
          onSubmit={(event) => submit(event, async () => {
            if (!selectedOrg) return;
            const updated = await apiPatch<OrganizationResponse>(`/api/organizations/${selectedOrg.id}`, {
              name: clubForm.name,
              shortName: clubForm.shortName,
              code: clubForm.code,
              province: clubForm.province,
              address: clubForm.address,
              contactEmail: clubForm.contactEmail,
              contactPhone: clubForm.contactPhone
            });
            setOrganizations((current) =>
              current.map((org) => org.id === updated.id ? updated : org)
            );
            setDrawer(null);
          })}
        />
      </ClubDrawer>

      <ClubDrawer drawer={drawer === "member" ? "member" : null} title="Thêm thành viên" onClose={() => setDrawer(null)}>
        <MemberForm
          busy={busy}
          error={error}
          memberForm={memberForm}
          onChange={setMemberForm}
          onSubmit={(event) => submit(event, async () => {
            if (memberForm.createAccount) {
              const result = await apiPost<MemberAccountCreateResponse>(`/api/organizations/${clubId}/member-accounts`, {
                displayName: memberForm.displayName,
                phone: memberForm.phone,
                email: memberForm.email,
                gender: memberForm.gender || undefined,
                birthDate: memberForm.birthDate || undefined,
                currentAddress: memberForm.currentAddress || undefined,
                emergencyContactName: memberForm.emergencyContactName || undefined,
                emergencyContactPhone: memberForm.emergencyContactPhone || undefined,
                role: memberForm.role,
                status: memberForm.status,
                student: memberForm.student,
                attendanceViewEnabled: memberForm.attendanceViewEnabled,
                memberNote: memberForm.memberNote || undefined
              });
              if (result.member) setMembers((current) => [result.member!, ...current]);
              setMemberForm(emptyMemberForm());
              setDrawer(null);
              if (result.username && result.temporaryPassword) {
                window.alert(`Tài khoản đã tạo:\nUsername: ${result.username}\nPassword: ${result.temporaryPassword}`);
              }
              return;
            }
            const person = await apiPost<PersonResponse>("/api/persons", {
              displayName: memberForm.displayName,
              phone: memberForm.phone || undefined,
              email: memberForm.email || undefined,
              gender: memberForm.gender || undefined,
              birthDate: memberForm.birthDate || undefined,
              currentAddress: memberForm.currentAddress || undefined,
              emergencyContactName: memberForm.emergencyContactName || undefined,
              emergencyContactPhone: memberForm.emergencyContactPhone || undefined
            });
            const member = await apiPost<ClubMemberResponse>(`/api/organizations/${clubId}/members`, {
              personId: person.id,
              role: memberForm.role,
              status: memberForm.status,
              joinedAt: today(),
              student: memberForm.student,
              attendanceViewEnabled: memberForm.attendanceViewEnabled,
              memberNote: memberForm.memberNote || undefined
            });
            setMembers((current) => [member, ...current]);
            setMemberForm(emptyMemberForm());
            setDrawer(null);
          })}
        />
      </ClubDrawer>

      <ClubDrawer drawer={drawer === "roster" ? "roster" : null} title="Thêm VĐV vào roster" onClose={() => setDrawer(null)}>
        <RosterForm
          busy={busy}
          candidates={rosterCandidates}
          error={error}
          rosterPersonId={rosterPersonId}
          onChange={setRosterPersonId}
          onSubmit={(event) => submit(event, async () => {
            const personId = rosterPersonId || rosterCandidates[0]?.personId;
            if (!personId) throw new Error("Không có thành viên phù hợp để thêm vào roster.");
            const athlete = athletes.find((item) => item.personId === personId)
              || await apiPost<AthleteResponse>(`/api/organizations/${clubId}/athletes`, { personId, status: "ACTIVE" });
            const rosterItem = await apiPost<ClubRosterResponse>(`/api/organizations/${clubId}/roster`, { athleteId: athlete.id, status: "ACTIVE", joinedAt: today() });
            setRoster((current) => [rosterItem, ...current]);
            setAthletes((current) => current.some((item) => item.id === athlete.id) ? current : [...current, athlete]);
            setRosterPersonId("");
            setDrawer(null);
          })}
        />
      </ClubDrawer>

      <ClubDrawer drawer={drawer === "session" ? "session" : null} title="Tạo buổi điểm danh" onClose={() => setDrawer(null)}>
        <SessionForm
          busy={busy}
          error={error}
          sessionName={sessionName}
          onChange={setSessionName}
          onSubmit={(event) => submit(event, async () => {
            const session = await apiPost<AttendanceSessionResponse>(`/api/organizations/${clubId}/attendance-sessions`, {
              name: sessionName,
              type: "TRAINING",
              status: "OPEN",
              scheduledAt: localDateTimeIso(selectedDate, schedule?.startTime || "18:30")
            });
            setSessionName("");
            setDrawer(null);
            appendSession(session);
            setSelectedSessionId(session.id);
            setTab("attendance");
          })}
        />
      </ClubDrawer>

      <ClubDrawer drawer={drawer === "schedule" ? "schedule" : null} title="Lịch tập cố định" onClose={() => setDrawer(null)}>
        <ScheduleForm
          busy={busy}
          error={error}
          scheduleForm={scheduleForm}
          onChange={setScheduleForm}
          onSubmit={(event) => submit(event, async () => {
            const nextSchedule = await apiPatch<ClubTrainingScheduleResponse>(`/api/organizations/${clubId}/training-schedule`, {
              name: scheduleForm.name,
              daysOfWeek: scheduleForm.daysOfWeek,
              startTime: scheduleForm.startTime,
              durationMinutes: scheduleForm.durationMinutes,
              timezone: "Asia/Ho_Chi_Minh",
              active: scheduleForm.active
            });
            setSchedule(nextSchedule);
            setDrawer(null);
          })}
        />
      </ClubDrawer>
    </main>
    </AuthenticatedShell>
  );
}

function ClubDrawer({ drawer, title, children, onClose }: { drawer: ClubDrawer; title: string; children: ReactNode; onClose: () => void }) {
  return (
    <AnimatePresence>
      {drawer ? (
        <motion.div className="club-drawer-layer" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
          <button className="club-drawer-scrim" aria-label="Đóng" onClick={onClose} />
          <motion.aside className="club-drawer" initial={{ x: 420 }} animate={{ x: 0 }} exit={{ x: 420 }} transition={{ type: "spring", stiffness: 140, damping: 24 }}>
            <div className="club-drawer-head">
              <h2>{title}</h2>
              <button onClick={onClose} aria-label="Đóng"><X size={20} /></button>
            </div>
            {children}
          </motion.aside>
        </motion.div>
      ) : null}
    </AnimatePresence>
  );
}

function ClubForm({ busy, clubForm, error, onChange, onSubmit, submitLabel = "Tạo CLB" }: {
  busy: boolean;
  clubForm: { name: string; shortName: string; code: string; province: string; address: string; contactEmail: string; contactPhone: string; country: string };
  error: string | null;
  onChange: (field: keyof typeof clubForm, value: string) => void;
  onSubmit: (event: FormEvent) => void;
  submitLabel?: string;
}) {
  return (
    <form className="club-drawer-form" onSubmit={onSubmit}>
      <Field label="Tên CLB *"><input value={clubForm.name} onChange={(event) => onChange("name", event.target.value)} placeholder="Sakura Karate Do" required /></Field>
      <div className="club-form-grid">
        <Field label="Tên ngắn *"><input value={clubForm.shortName} onChange={(event) => onChange("shortName", event.target.value)} required /></Field>
        <Field label="Mã CLB *"><input value={clubForm.code} onChange={(event) => onChange("code", event.target.value)} required /></Field>
      </div>
      <div className="club-form-grid">
        <Field label="Tỉnh thành *"><input value={clubForm.province} onChange={(event) => onChange("province", event.target.value)} required /></Field>
        <Field label="Điện thoại *"><input value={clubForm.contactPhone} onChange={(event) => onChange("contactPhone", event.target.value)} required /></Field>
      </div>
      <Field label="Địa chỉ"><input value={clubForm.address} onChange={(event) => onChange("address", event.target.value)} /></Field>
      <Field label="Email"><input type="email" value={clubForm.contactEmail} onChange={(event) => onChange("contactEmail", event.target.value)} /></Field>
      {error ? <p className="club-form-error">{error}</p> : null}
      <button className="club-primary-button" disabled={busy || !clubForm.name.trim() || !clubForm.shortName.trim() || !clubForm.code.trim() || !clubForm.province.trim() || !clubForm.contactPhone.trim()}>{submitLabel}</button>
    </form>
  );
}

function MemberForm({ busy, error, memberForm, onChange, onSubmit }: {
  busy: boolean;
  error: string | null;
  memberForm: ReturnType<typeof emptyMemberForm>;
  onChange: (value: ReturnType<typeof emptyMemberForm>) => void;
  onSubmit: (event: FormEvent) => void;
}) {
  return (
    <form className="club-drawer-form" onSubmit={onSubmit}>
      <div className="club-form-section-title">Hồ sơ cá nhân</div>
      <Field label="Họ tên *"><input value={memberForm.displayName} onChange={(event) => onChange({ ...memberForm, displayName: event.target.value })} required /></Field>
      <div className="club-form-grid">
        <Field label="Giới tính">
          <select value={memberForm.gender} onChange={(event) => onChange({ ...memberForm, gender: event.target.value })}>
            <option value="MALE">Nam</option>
            <option value="FEMALE">Nữ</option>
            <option value="OTHER">Khác</option>
          </select>
        </Field>
        <Field label="Ngày sinh"><input type="date" value={memberForm.birthDate} onChange={(event) => onChange({ ...memberForm, birthDate: event.target.value })} /></Field>
      </div>
      <Field label="Địa chỉ hiện tại"><input value={memberForm.currentAddress} onChange={(event) => onChange({ ...memberForm, currentAddress: event.target.value })} /></Field>
      <div className="club-form-grid">
        <Field label="Điện thoại"><input value={memberForm.phone} onChange={(event) => onChange({ ...memberForm, phone: event.target.value })} /></Field>
        <Field label="Email"><input type="email" value={memberForm.email} onChange={(event) => onChange({ ...memberForm, email: event.target.value })} /></Field>
      </div>
      <label className="club-toggle-row account">
        <input type="checkbox" checked={memberForm.createAccount} onChange={(event) => onChange({ ...memberForm, createAccount: event.target.checked })} />
        <span>Tạo tài khoản đăng nhập cho thành viên này</span>
      </label>
      {memberForm.createAccount ? <p className="club-helper-text">Hệ thống sẽ tự sinh tên tài khoản và mật khẩu tạm thời, sau đó chỉ hiển thị một lần.</p> : null}
      <div className="club-form-grid">
        <Field label="Người liên hệ khẩn cấp"><input value={memberForm.emergencyContactName} onChange={(event) => onChange({ ...memberForm, emergencyContactName: event.target.value })} /></Field>
        <Field label="SĐT khẩn cấp"><input value={memberForm.emergencyContactPhone} onChange={(event) => onChange({ ...memberForm, emergencyContactPhone: event.target.value })} /></Field>
      </div>
      <div className="club-form-section-title">Vai trò và trạng thái</div>
      <Field label="Vai trò">
        <select value={memberForm.role} onChange={(event) => onChange({ ...memberForm, role: event.target.value })}>
          {MEMBER_ROLES.map((role) => <option key={role} value={role}>{roleLabel(role)}</option>)}
        </select>
      </Field>
      <div className="club-toggle-stack">
        <label className="club-toggle-row"><input type="checkbox" checked={memberForm.student} onChange={(event) => onChange({ ...memberForm, student: event.target.checked })} /><span>Là sinh viên/học viên đang theo học</span></label>
        <label className="club-toggle-row"><input type="checkbox" checked={memberForm.attendanceViewEnabled} onChange={(event) => onChange({ ...memberForm, attendanceViewEnabled: event.target.checked })} /><span>Cho phép xem chuyên cần</span></label>
      </div>
      <Field label="Ghi chú thành viên"><input value={memberForm.memberNote} onChange={(event) => onChange({ ...memberForm, memberNote: event.target.value })} /></Field>
      {error ? <p className="club-form-error">{error}</p> : null}
      <button className="club-primary-button" disabled={busy || !memberForm.displayName.trim() || (memberForm.createAccount && (!memberForm.email.trim() || !memberForm.phone.trim()))}>
        {memberForm.createAccount ? "Tạo thành viên và tài khoản" : "Lưu thành viên"}
      </button>
    </form>
  );
}

function RosterForm({ busy, candidates, error, rosterPersonId, onChange, onSubmit }: {
  busy: boolean;
  candidates: ClubMemberResponse[];
  error: string | null;
  rosterPersonId: string;
  onChange: (value: string) => void;
  onSubmit: (event: FormEvent) => void;
}) {
  return (
    <form className="club-drawer-form" onSubmit={onSubmit}>
      {candidates.length === 0 ? <EmptyState title="Không có thành viên phù hợp" text="Roster chỉ nhận thành viên đang hoạt động và chưa có hồ sơ VĐV." /> : (
        <Field label="Chọn thành viên">
          <select value={rosterPersonId} onChange={(event) => onChange(event.target.value)}>
            {candidates.map((member) => <option key={member.id} value={member.personId}>{member.personName || member.userName || member.id}</option>)}
          </select>
        </Field>
      )}
      {error ? <p className="club-form-error">{error}</p> : null}
      <button className="club-primary-button" disabled={busy || candidates.length === 0}>Thêm vào roster</button>
    </form>
  );
}

function SessionForm({ busy, error, sessionName, onChange, onSubmit }: {
  busy: boolean;
  error: string | null;
  sessionName: string;
  onChange: (value: string) => void;
  onSubmit: (event: FormEvent) => void;
}) {
  return (
    <form className="club-drawer-form" onSubmit={onSubmit}>
      <Field label="Tên buổi tập *"><input value={sessionName} onChange={(event) => onChange(event.target.value)} placeholder="Buổi tập tối thứ ba" required /></Field>
      {error ? <p className="club-form-error">{error}</p> : null}
      <button className="club-primary-button" disabled={busy || !sessionName.trim()}>Tạo buổi điểm danh</button>
    </form>
  );
}

function ScheduleForm({ busy, error, scheduleForm, onChange, onSubmit }: {
  busy: boolean;
  error: string | null;
  scheduleForm: { name: string; daysOfWeek: number[]; startTime: string; durationMinutes: number; active: boolean };
  onChange: (value: { name: string; daysOfWeek: number[]; startTime: string; durationMinutes: number; active: boolean }) => void;
  onSubmit: (event: FormEvent) => void;
}) {
  const toggleDay = (value: number) => {
    const nextDays = scheduleForm.daysOfWeek.includes(value)
      ? scheduleForm.daysOfWeek.filter((day) => day !== value)
      : [...scheduleForm.daysOfWeek, value].sort((a, b) => a - b);
    onChange({ ...scheduleForm, daysOfWeek: nextDays });
  };

  return (
    <form className="club-drawer-form" onSubmit={onSubmit}>
      <Field label="Tên lịch tập *"><input value={scheduleForm.name} onChange={(event) => onChange({ ...scheduleForm, name: event.target.value })} required /></Field>
      <div className="club-week-picker">
        {WEEKDAYS.map((day) => (
          <button type="button" key={day.value} className={cx(scheduleForm.daysOfWeek.includes(day.value) && "active")} onClick={() => toggleDay(day.value)}>
            <strong>{day.short}</strong>
            <span>{day.label}</span>
          </button>
        ))}
      </div>
      <div className="club-form-grid">
        <Field label="Giờ bắt đầu"><input type="time" value={scheduleForm.startTime} onChange={(event) => onChange({ ...scheduleForm, startTime: event.target.value })} /></Field>
        <Field label="Thời lượng phút"><input type="number" min={30} max={240} value={scheduleForm.durationMinutes} onChange={(event) => onChange({ ...scheduleForm, durationMinutes: Number(event.target.value) })} /></Field>
      </div>
      <label className="club-toggle-row">
        <input type="checkbox" checked={scheduleForm.active} onChange={(event) => onChange({ ...scheduleForm, active: event.target.checked })} />
        <span>Lịch tập đang hoạt động</span>
      </label>
      <p className="club-helper-text">Lịch cố định chỉ là quy tắc tuần. Job hằng ngày sẽ tạo buổi điểm danh khi đến đúng ngày, không tạo sẵn cả năm.</p>
      {error ? <p className="club-form-error">{error}</p> : null}
      <button className="club-primary-button" disabled={busy || !scheduleForm.name.trim() || scheduleForm.daysOfWeek.length === 0}>Lưu lịch tập</button>
    </form>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return <label className="club-field"><span>{label}</span>{children}</label>;
}

function HealthTile({ className, icon, label, value, detail }: { className?: string; icon: ReactNode; label: string; value: string | number; detail: string }) {
  return (
    <article className={cx("club-health-tile", className)}>
      <div>{icon}</div>
      <span>{label}</span>
      <strong>{value}</strong>
      <p>{detail}</p>
    </article>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return <div className="club-metric"><strong>{value}</strong><span>{label}</span></div>;
}

function formatMoneyShort(value?: number) {
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(Number(value || 0));
}

function EmptyState({ title, text, action, onAction }: { title: string; text: string; action?: string; onAction?: () => void }) {
  return (
    <div className="club-empty-state">
      <div className="club-empty-line" />
      <strong>{title}</strong>
      <p>{text}</p>
      {action && onAction ? <button className="club-primary-button" onClick={onAction}>{action}</button> : null}
    </div>
  );
}

function InlineNotice({ tone, title, text }: { tone: "danger" | "warm"; title: string; text: string }) {
  return (
    <div className={cx("club-inline-notice", tone)}>
      <AlertTriangle size={18} />
      <div><strong>{title}</strong><span>{text}</span></div>
    </div>
  );
}

function ClubSkeleton() {
  return <div className="club-skeleton-block"><span /><span /><span /><span /></div>;
}

function ClubListSkeleton() {
  return <div className="club-card-stack">{[0, 1, 2].map((item) => <div className="club-list-card skeleton" key={item}><span /><span /><span /></div>)}</div>;
}

function WorkspaceSkeleton() {
  return <div className="club-workspace-skeleton"><span /><span /><span /><span /></div>;
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

function hasRole(user: AuthUserResponse, role: string) {
  return user.roles.includes(role);
}

function tabShort(tab: ClubTab) {
  if (tab === "overview") return "TQ";
  if (tab === "members") return "TV";
  if (tab === "fees") return "TC";
  if (tab === "roster") return "VĐV";
  return "ĐD";
}

function emptyMemberForm() {
  return {
    displayName: "",
    phone: "",
    email: "",
    gender: "MALE",
    birthDate: "",
    currentAddress: "",
    emergencyContactName: "",
    emergencyContactPhone: "",
    role: "ATHLETE",
    status: "ACTIVE",
    student: true,
    createAccount: false,
    attendanceViewEnabled: true,
    memberNote: ""
  };
}

function groupFeeAssignmentsByMember(assignments: ClubFeeOverviewResponse["assignments"]) {
  return assignments.reduce<Record<string, ClubFeeOverviewResponse["assignments"]>>((grouped, assignment) => {
    grouped[assignment.memberId] = [...(grouped[assignment.memberId] ?? []), assignment];
    return grouped;
  }, {});
}

function memberFinanceStatus(assignments: ClubFeeOverviewResponse["assignments"]) {
  if (assignments.length === 0) {
    return null;
  }
  if (assignments.some((assignment) => assignment.status === "OVERDUE")) {
    return "OVERDUE";
  }
  if (assignments.some((assignment) => assignment.status === "PARTIAL")) {
    return "PARTIAL";
  }
  const outstanding = assignments.reduce((sum, assignment) => {
    return sum + Math.max(0, Number(assignment.amountDue || 0) - Number(assignment.paidAmount || 0));
  }, 0);
  if (outstanding <= 0) {
    return assignments.every((assignment) => assignment.status === "WAIVED") ? "WAIVED" : "PAID";
  }
  return "PENDING";
}
