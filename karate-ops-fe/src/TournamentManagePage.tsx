import { type ReactNode, useEffect, useMemo, useState } from "react";
import {
  Check,
  CheckCircle2,
  CircleDot,
  ExternalLink,
  Loader2,
  Play,
  RefreshCcw,
  Save,
  Settings2,
  Shuffle,
  Shield,
  Swords,
  Trophy,
  Unlock,
  Users,
} from "lucide-react";
import { apiDelete, apiGet, apiPatch, apiPost, errorMessage } from "./apiClient";
import { AuthenticatedShell } from "./components/AuthenticatedShell";
import { ConfirmModal } from "./components/ConfirmModal";
import { StatusBadge } from "./components/StatusBadge";
import { TournamentStepIndicator } from "./components/TournamentStepIndicator";
import "./tournament-styles.css";
import type {
  AthleteApprovalItem,
  AthleteApprovalSummary,
  AuthUserResponse,
  ParticipantApprovalItem,
  TournamentDraw,
  TournamentExtended,
} from "./types";

interface TournamentManagePageProps {
  tournamentId: string;
  user: AuthUserResponse;
  actualUser: AuthUserResponse;
  viewAsRole: string;
  setViewAsRole: (role: string) => void;
  onLogout: () => void;
}

interface SetupForm {
  name: string;
  description: string;
  location: string;
  startsOn: string;
  endsOn: string;
  registrationDeadline: string;
  registrationFee: number;
  phongTraoEnabled: boolean;
  nangCaoEnabled: boolean;
}

type AthleteView = "by-club" | "by-category";

export function TournamentManagePage({
  tournamentId,
  user,
  actualUser,
  viewAsRole,
  setViewAsRole,
  onLogout,
}: TournamentManagePageProps) {
  const [tournament, setTournament] = useState<TournamentExtended | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function loadTournament() {
    try {
      setLoading(true);
      setError(null);
      const data = await apiGet<TournamentExtended>(`/api/tournaments/${tournamentId}`);
      setTournament(data);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadTournament();
  }, [tournamentId]);

  const stepMeta = [
    { label: "Cấu hình", description: "Chốt dữ liệu nền và mở đăng ký đúng lúc." },
    { label: "Duyệt CLB", description: "Xác nhận đơn vị tham gia trước khi chốt danh sách." },
    { label: "Duyệt VĐV", description: "Lọc pending và khóa entry đúng quy trình." },
    { label: "Bốc thăm", description: "Tạo nhánh đấu trước khi mở vận hành thực tế." },
    { label: "Đang chạy", description: "Giải đã vào pha điều phối và giám sát live." },
  ] as const;

  if (loading) {
    return (
      <AuthenticatedShell
        user={user}
        actualUser={actualUser}
        viewAsRole={viewAsRole}
        setViewAsRole={setViewAsRole}
        onLogout={onLogout}
        activeNav="tournaments"
        eyebrow="Tournament operations"
        title="Chi tiết giải đấu"
        description="Đang tải dữ liệu quản trị giải đấu."
        breadcrumbs={[{ label: "Ứng dụng", href: "/app" }, { label: "Giải đấu", href: "/tournaments" }, { label: "Chi tiết giải" }]}
      >
        <div className="page-spinner">
          <Loader2 size={18} className="spin" />
          <span>Đang tải thông tin giải...</span>
        </div>
      </AuthenticatedShell>
    );
  }

  if (error || !tournament) {
    return (
      <AuthenticatedShell
        user={user}
        actualUser={actualUser}
        viewAsRole={viewAsRole}
        setViewAsRole={setViewAsRole}
        onLogout={onLogout}
        activeNav="tournaments"
        eyebrow="Tournament operations"
        title="Chi tiết giải đấu"
        description="Không tải được dữ liệu giải đấu cần quản trị."
        breadcrumbs={[{ label: "Ứng dụng", href: "/app" }, { label: "Giải đấu", href: "/tournaments" }, { label: "Chi tiết giải" }]}
      >
        <div className="page-error">
          <p>Không tải được thông tin giải.</p>
          {error ? <p style={{ fontSize: "0.8rem", color: "var(--muted-2)" }}>{error}</p> : null}
          <button className="btn btn-ghost" onClick={loadTournament}>
            Thử lại
          </button>
        </div>
      </AuthenticatedShell>
    );
  }

  const currentStep = stepMeta[tournament.step] ?? stepMeta[0];
  const activeLevels = Number(Boolean(tournament.phongTraoEnabled)) + Number(Boolean(tournament.nangCaoEnabled));

  return (
    <AuthenticatedShell
      user={user}
      actualUser={actualUser}
      viewAsRole={viewAsRole}
      setViewAsRole={setViewAsRole}
      onLogout={onLogout}
      activeNav="tournaments"
      eyebrow="Tournament operations"
      title={tournament.name}
      description={currentStep.description}
      breadcrumbs={[{ label: "Ứng dụng", href: "/app" }, { label: "Giải đấu", href: "/tournaments" }, { label: tournament.name }]}
      headerActions={<a className="auth-page-primary-action" href={`/tournaments/${tournamentId}`}>Xem trang công khai</a>}
    >
      <section className="auth-metric-strip tournament-admin-metrics tournament-manage-metrics">
        <div className="auth-metric-card compact">
          <span>Trạng thái</span>
          <strong>{tournament.status}</strong>
        </div>
        <div className="auth-metric-card compact">
          <span>Bước hiện tại</span>
          <strong>{tournament.step + 1}/5</strong>
        </div>
        <div className="auth-metric-card compact">
          <span>Hạn đăng ký</span>
          <strong>{tournament.registrationDeadline ? formatDate(tournament.registrationDeadline) : "Chưa đặt"}</strong>
        </div>
        <div className="auth-metric-card compact">
          <span>Hạng thi đấu</span>
          <strong>{activeLevels}</strong>
        </div>
      </section>

      <section className="auth-overview-columns sharp tournament-manage-overview">
        <article className="auth-overview-panel primary">
          <div className="auth-panel-head">
            <div>
              <span>Quy trình vận hành</span>
              <h2>{currentStep.label}</h2>
            </div>
            <StatusBadge status={tournament.status} />
          </div>
          <div className="tournament-manage-header">
            <h1 className="tournament-manage-title">
              <Trophy size={18} />
              {tournament.name}
            </h1>
            <p className="tournament-manage-subtitle">
              {tournament.location || "Chưa cập nhật địa điểm"}
              {tournament.startsOn ? ` · ${formatDate(tournament.startsOn)}` : ""}
              {tournament.endsOn ? ` - ${formatDate(tournament.endsOn)}` : ""}
            </p>
          </div>
          <div className="manage-panel manage-panel-step">
            <TournamentStepIndicator currentStep={tournament.step} />
          </div>
        </article>

        <article className="auth-overview-panel">
          <div className="auth-panel-head">
            <div>
              <span>Đi nhanh</span>
              <h2>Liên kết chính</h2>
            </div>
          </div>
          <div className="auth-action-list">
            <a className="auth-action-row" href="/tournaments">
              <Trophy size={18} />
              <div>
                <strong>Quay về workspace giải</strong>
                <span>Mở lại command-center và danh sách giải đang quản trị.</span>
              </div>
            </a>
            <a className="auth-action-row" href={`/tournaments/${tournamentId}`}>
              <ExternalLink size={18} />
              <div>
                <strong>Mở trang công khai</strong>
                <span>Kiểm tra thông tin mà CLB và thành viên nhìn thấy.</span>
              </div>
            </a>
            <a className="auth-action-row" href={`/standings/tournaments/${tournamentId}`}>
              <Swords size={18} />
              <div>
                <strong>Xem bảng xếp hạng</strong>
                <span>Đi thẳng tới standings nếu giải đã có dữ liệu thi đấu.</span>
              </div>
            </a>
          </div>
        </article>
      </section>

      <div className="tournament-manage-page shell-embedded">
        {tournament.step === 0 ? <SetupPanel tournament={tournament} onSaved={loadTournament} onOpened={loadTournament} /> : null}
        {tournament.step === 1 ? <ParticipantApprovalPanel tournamentId={tournamentId} onAdvanced={loadTournament} /> : null}
        {tournament.step === 2 ? <AthleteApprovalPanel tournamentId={tournamentId} onAdvanced={loadTournament} /> : null}
        {tournament.step === 3 ? <DrawPanel tournamentId={tournamentId} onStarted={loadTournament} /> : null}
        {tournament.step === 4 ? <RunningPanel tournamentId={tournamentId} /> : null}
      </div>
    </AuthenticatedShell>
  );
}

function SetupPanel({
  tournament,
  onSaved,
  onOpened,
}: {
  tournament: TournamentExtended;
  onSaved: () => void;
  onOpened: () => void;
}) {
  const [form, setForm] = useState<SetupForm>({
    name: tournament.name ?? "",
    description: tournament.description ?? "",
    location: tournament.location ?? "",
    startsOn: tournament.startsOn ?? "",
    endsOn: tournament.endsOn ?? "",
    registrationDeadline: tournament.registrationDeadline ?? "",
    registrationFee: tournament.registrationFee ?? 0,
    phongTraoEnabled: tournament.phongTraoEnabled ?? false,
    nangCaoEnabled: tournament.nangCaoEnabled ?? false,
  });
  const [saving, setSaving] = useState(false);
  const [opening, setOpening] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [openError, setOpenError] = useState<string | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);

  function setField<K extends keyof SetupForm>(key: K, value: SetupForm[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  async function handleSave() {
    try {
      setSaving(true);
      setSaveError(null);
      await apiPatch(`/api/tournaments/${tournament.id}`, form);
      onSaved();
    } catch (e) {
      setSaveError(errorMessage(e));
    } finally {
      setSaving(false);
    }
  }

  async function handleOpenRegistration() {
    try {
      setOpening(true);
      setOpenError(null);
      await apiPost(`/api/tournaments/${tournament.id}/open-registration`, {});
      onOpened();
    } catch (e) {
      setOpenError(errorMessage(e));
    } finally {
      setOpening(false);
      setConfirmOpen(false);
    }
  }

  const isOpen = tournament.status === "REGISTRATION_OPEN";

  return (
    <div className="manage-panel">
      <div className="manage-panel-header">
        <h2 className="manage-panel-title">
          <Settings2 size={18} />
          Cấu hình giải đấu
        </h2>
        {isOpen ? (
          <div className="status-banner status-banner--success">
            <CheckCircle2 size={16} />
            Đang mở đăng ký
          </div>
        ) : null}
      </div>

      <div className="manage-panel-body">
        {saveError ? <div className="status-banner status-banner--warn">{saveError}</div> : null}
        {openError ? <div className="status-banner status-banner--warn">{openError}</div> : null}

        <div className="form-grid">
          <div className="form-field form-field--full">
            <label>Tên giải</label>
            <input type="text" value={form.name} onChange={(event) => setField("name", event.target.value)} placeholder="VD: Hanoi Open 2026" />
          </div>

          <div className="form-field form-field--full">
            <label>Mô tả</label>
            <textarea value={form.description} onChange={(event) => setField("description", event.target.value)} placeholder="Tóm tắt mục tiêu và phạm vi giải đấu" />
          </div>

          <div className="form-field">
            <label>Địa điểm</label>
            <input type="text" value={form.location} onChange={(event) => setField("location", event.target.value)} placeholder="Nhà thi đấu / tỉnh thành" />
          </div>

          <div className="form-field">
            <label>Phí đăng ký (VND)</label>
            <input type="number" min="0" value={form.registrationFee} onChange={(event) => setField("registrationFee", Number(event.target.value))} />
          </div>

          <div className="form-field">
            <label>Ngày bắt đầu</label>
            <input type="date" value={form.startsOn} onChange={(event) => setField("startsOn", event.target.value)} />
          </div>

          <div className="form-field">
            <label>Ngày kết thúc</label>
            <input type="date" value={form.endsOn} onChange={(event) => setField("endsOn", event.target.value)} />
          </div>

          <div className="form-field">
            <label>Hạn đăng ký</label>
            <input type="date" value={form.registrationDeadline} onChange={(event) => setField("registrationDeadline", event.target.value)} />
          </div>
        </div>

        <hr className="section-divider" />

        <div className="toggle-group">
          <p>Hạng thi đấu</p>
          <label className="toggle-row">
            <div className={`toggle-track${form.phongTraoEnabled ? " toggle-track--on" : ""}`} onClick={() => setField("phongTraoEnabled", !form.phongTraoEnabled)}>
              <div className="toggle-thumb" />
            </div>
            <span className="toggle-label-text">Hạng phong trào</span>
          </label>
          <label className="toggle-row">
            <div className={`toggle-track${form.nangCaoEnabled ? " toggle-track--on" : ""}`} onClick={() => setField("nangCaoEnabled", !form.nangCaoEnabled)}>
              <div className="toggle-thumb" />
            </div>
            <span className="toggle-label-text">Hạng nâng cao</span>
          </label>
        </div>

        <div className="action-row">
          <button className="btn btn-ghost" onClick={handleSave} disabled={saving}>
            {saving ? <Loader2 size={16} className="spin" /> : <Save size={16} />}
            {saving ? "Đang lưu..." : "Lưu thông tin"}
          </button>
          {!isOpen ? (
            <button className="btn btn-primary" onClick={() => setConfirmOpen(true)} disabled={opening}>
              {opening ? <Loader2 size={16} className="spin" /> : <Unlock size={16} />}
              {opening ? "Đang xử lý..." : "Mở đăng ký"}
            </button>
          ) : null}
        </div>
      </div>

      <ConfirmModal
        open={confirmOpen}
        title="Mở đăng ký?"
        message="Sau khi mở đăng ký, các CLB có thể gửi danh sách tham dự. Bạn vẫn có thể cập nhật thông tin mô tả sau đó."
        confirmLabel="Mở đăng ký"
        onConfirm={handleOpenRegistration}
        onCancel={() => setConfirmOpen(false)}
      />
    </div>
  );
}

function ParticipantApprovalPanel({ tournamentId, onAdvanced }: { tournamentId: string; onAdvanced: () => void }) {
  const [clubs, setClubs] = useState<ParticipantApprovalItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [confirm, setConfirm] = useState<{ participantId: string; action: "approve" | "inactive"; name: string } | null>(null);
  const [actioning, setActioning] = useState(false);
  const [advancing, setAdvancing] = useState(false);
  const [advanceError, setAdvanceError] = useState<string | null>(null);
  const [confirmAdvance, setConfirmAdvance] = useState(false);

  async function load() {
    try {
      setLoading(true);
      setError(null);
      setClubs(await apiGet<ParticipantApprovalItem[]>(`/api/tournaments/${tournamentId}/approval/clubs`));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, [tournamentId]);

  async function handleAction() {
    if (!confirm) return;
    try {
      setActioning(true);
      await apiPatch(`/api/tournaments/${tournamentId}/approval/clubs/${confirm.participantId}`, {
        action: confirm.action === "approve" ? "APPROVE" : "INACTIVE",
      });
      await load();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setActioning(false);
      setConfirm(null);
    }
  }

  async function handleAdvance() {
    try {
      setAdvancing(true);
      setAdvanceError(null);
      await apiPost(`/api/tournaments/${tournamentId}/advance-step`, {});
      onAdvanced();
    } catch (e) {
      setAdvanceError(errorMessage(e));
    } finally {
      setAdvancing(false);
      setConfirmAdvance(false);
    }
  }

  return (
    <div className="manage-panel">
      <div className="manage-panel-header">
        <h2 className="manage-panel-title">
          <Shield size={18} />
          Duyệt câu lạc bộ
        </h2>
        <button className="btn btn-primary" onClick={() => setConfirmAdvance(true)} disabled={advancing}>
          {advancing ? <Loader2 size={16} className="spin" /> : null}
          Tiếp theo: Duyệt VĐV
        </button>
      </div>

      <div className="manage-panel-body">
        {error ? <div className="status-banner status-banner--warn">{error}</div> : null}
        {advanceError ? <div className="status-banner status-banner--warn">{advanceError}</div> : null}

        {loading ? (
          <div className="page-spinner"><Loader2 size={18} className="spin" /> Đang tải...</div>
        ) : clubs.length === 0 ? (
          <EmptyState icon={<Shield size={18} />} text="Chưa có câu lạc bộ nào đăng ký." />
        ) : (
          <div className="manage-table-wrap">
            <table className="manage-table">
              <thead>
                <tr>
                  <th>CLB</th>
                  <th>Trạng thái</th>
                  <th>VDV</th>
                  <th>Hành động</th>
                </tr>
              </thead>
              <tbody>
                {clubs.map((club) => (
                  <tr key={club.participantId}>
                    <td>
                      <div className="manage-primary-cell">
                        <strong>{club.organizationName}</strong>
                        {club.displayName && club.displayName !== club.organizationName ? <span>{club.displayName}</span> : null}
                      </div>
                    </td>
                    <td><StatusBadge status={club.status} size="sm" /></td>
                    <td>{club.approvedEntries}/{club.totalEntries}</td>
                    <td>
                      <div className="action-row compact">
                        {club.status !== "APPROVED" ? (
                          <button className="btn btn-outline btn-sm" onClick={() => setConfirm({ participantId: club.participantId, action: "approve", name: club.organizationName })}>
                            Duyệt
                          </button>
                        ) : null}
                        {club.status !== "INACTIVE" ? (
                          <button className="btn btn-danger btn-sm" onClick={() => setConfirm({ participantId: club.participantId, action: "inactive", name: club.organizationName })}>
                            Không tham dự
                          </button>
                        ) : null}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <ConfirmModal
        open={confirm !== null}
        title={confirm?.action === "approve" ? `Duyệt CLB ${confirm?.name}?` : "Đánh dấu không tham dự?"}
        message={confirm?.action === "approve" ? `CLB ${confirm?.name} sẽ được duyệt vào giải.` : `CLB ${confirm?.name} sẽ bị chuyển sang trạng thái không tham dự.`}
        confirmLabel={confirm?.action === "approve" ? "Duyệt" : "Xác nhận"}
        dangerous={confirm?.action === "inactive"}
        onConfirm={handleAction}
        onCancel={() => setConfirm(null)}
      />

      <ConfirmModal
        open={confirmAdvance}
        title="Chuyển sang bước duyệt VĐV?"
        message="Sau bước này, bạn nên xem như danh sách CLB đã chốt để tiếp tục xử lý entry của vận động viên."
        confirmLabel="Tiếp tục"
        onConfirm={handleAdvance}
        onCancel={() => setConfirmAdvance(false)}
      />
    </div>
  );
}

function AthleteApprovalPanel({ tournamentId, onAdvanced }: { tournamentId: string; onAdvanced: () => void }) {
  const [athletes, setAthletes] = useState<AthleteApprovalItem[]>([]);
  const [summary, setSummary] = useState<AthleteApprovalSummary | null>(null);
  const [view, setView] = useState<AthleteView>("by-club");
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [bulkApproving, setBulkApproving] = useState(false);
  const [advancing, setAdvancing] = useState(false);
  const [advanceError, setAdvanceError] = useState<string | null>(null);
  const [confirmAdvance, setConfirmAdvance] = useState(false);

  async function load() {
    try {
      setLoading(true);
      setError(null);
      const [list, nextSummary] = await Promise.all([
        apiGet<AthleteApprovalItem[]>(`/api/tournaments/${tournamentId}/approval/athletes`),
        apiGet<AthleteApprovalSummary>(`/api/tournaments/${tournamentId}/approval/athletes/summary`),
      ]);
      setAthletes(list);
      setSummary(nextSummary);
      setSelected(new Set());
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, [tournamentId]);

  const pendingAthletes = athletes.filter((athlete) => athlete.btcApprovalStatus === "PENDING");
  const allPendingSelected = pendingAthletes.length > 0 && selected.size === pendingAthletes.length;
  const grouped = useMemo(() => groupAthletes(athletes, view), [athletes, view]);

  function toggleSelect(entryId: string) {
    setSelected((current) => {
      const next = new Set(current);
      if (next.has(entryId)) next.delete(entryId);
      else next.add(entryId);
      return next;
    });
  }

  function toggleAll() {
    if (allPendingSelected) {
      setSelected(new Set());
      return;
    }
    setSelected(new Set(pendingAthletes.map((athlete) => athlete.entryId)));
  }

  async function handleBulkApprove() {
    if (!selected.size) return;
    try {
      setBulkApproving(true);
      await apiPost(`/api/tournaments/${tournamentId}/approval/athletes/bulk`, { entryIds: Array.from(selected) });
      await load();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBulkApproving(false);
    }
  }

  async function handleAdvance() {
    try {
      setAdvancing(true);
      setAdvanceError(null);
      await apiPost(`/api/tournaments/${tournamentId}/advance-step`, {});
      onAdvanced();
    } catch (e) {
      setAdvanceError(errorMessage(e));
    } finally {
      setAdvancing(false);
      setConfirmAdvance(false);
    }
  }

  return (
    <div className="manage-panel">
      <div className="manage-panel-header">
        <h2 className="manage-panel-title">
          <Users size={18} />
          Duyệt vận động viên
        </h2>
        <button className="btn btn-primary" onClick={() => setConfirmAdvance(true)} disabled={advancing}>
          {advancing ? <Loader2 size={16} className="spin" /> : null}
          Tiếp theo: Bốc thăm
        </button>
      </div>

      <div className="manage-panel-body">
        {summary ? (
          <div className="summary-counters">
            <div className="summary-counter">
              <span className="summary-counter__value">{summary.totalEntries}</span>
              <span className="summary-counter__label">Tổng đăng ký</span>
            </div>
            <div className="summary-counter">
              <span className="summary-counter__value">{summary.approved}</span>
              <span className="summary-counter__label">Đã duyệt</span>
            </div>
            <div className="summary-counter">
              <span className="summary-counter__value">{summary.pending}</span>
              <span className="summary-counter__label">Chờ duyệt</span>
            </div>
            <div className="summary-counter">
              <span className="summary-counter__value">{summary.rejected}</span>
              <span className="summary-counter__label">Từ chối</span>
            </div>
          </div>
        ) : null}

        {error ? <div className="status-banner status-banner--warn">{error}</div> : null}
        {advanceError ? <div className="status-banner status-banner--warn">{advanceError}</div> : null}

        <div className="action-row spread">
          <div className="view-switcher">
            <button className={`view-switcher__btn${view === "by-club" ? " view-switcher__btn--active" : ""}`} onClick={() => setView("by-club")}>
              Theo CLB
            </button>
            <button className={`view-switcher__btn${view === "by-category" ? " view-switcher__btn--active" : ""}`} onClick={() => setView("by-category")}>
              Theo hạng cân
            </button>
          </div>

          {selected.size ? (
            <button className="btn btn-outline" onClick={handleBulkApprove} disabled={bulkApproving}>
              {bulkApproving ? <Loader2 size={16} className="spin" /> : <Check size={16} />}
              Duyệt {selected.size} VĐV
            </button>
          ) : null}
        </div>

        {loading ? (
          <div className="page-spinner"><Loader2 size={18} className="spin" /> Đang tải...</div>
        ) : athletes.length === 0 ? (
          <EmptyState icon={<Users size={18} />} text="Chưa có vận động viên nào được gửi vào giải." />
        ) : (
          <div className="manage-table-wrap">
            <table className="manage-table">
              <thead>
                <tr>
                  <th style={{ width: 36 }}>
                    <input type="checkbox" checked={allPendingSelected} onChange={toggleAll} />
                  </th>
                  <th>{view === "by-club" ? "CLB" : "Hạng cân"}</th>
                  <th>Vận động viên</th>
                  <th>{view === "by-club" ? "Hạng cân" : "CLB"}</th>
                  <th>Cân nặng</th>
                  <th>Trạng thái</th>
                </tr>
              </thead>
              <tbody>
                {grouped.map((row) => (
                  <tr key={row.entryId}>
                    <td>
                      {row.btcApprovalStatus === "PENDING" ? (
                        <input type="checkbox" checked={selected.has(row.entryId)} onChange={() => toggleSelect(row.entryId)} />
                      ) : null}
                    </td>
                    <td>{view === "by-club" ? row.organizationName : row.categoryName}</td>
                    <td><strong>{row.athleteName}</strong></td>
                    <td>{view === "by-club" ? row.categoryName : row.organizationName}</td>
                    <td>{row.registrationWeightKg ?? "-"}</td>
                    <td><StatusBadge status={row.btcApprovalStatus} size="sm" /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <ConfirmModal
        open={confirmAdvance}
        title="Chuyển sang bước bốc thăm?"
        message="Sau bước này, danh sách vận động viên nên được xem như đã chốt để tạo nhánh đấu."
        confirmLabel="Tiếp tục"
        onConfirm={handleAdvance}
        onCancel={() => setConfirmAdvance(false)}
      />
    </div>
  );
}

function DrawPanel({ tournamentId, onStarted }: { tournamentId: string; onStarted: () => void }) {
  const [draw, setDraw] = useState<TournamentDraw | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [drawing, setDrawing] = useState(false);
  const [resetting, setResetting] = useState(false);
  const [starting, setStarting] = useState(false);
  const [confirmReset, setConfirmReset] = useState(false);
  const [confirmStart, setConfirmStart] = useState(false);

  async function load() {
    try {
      setLoading(true);
      setError(null);
      setDraw(await apiGet<TournamentDraw>(`/api/tournaments/${tournamentId}/draw`));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, [tournamentId]);

  async function handleDraw() {
    try {
      setDrawing(true);
      setError(null);
      await apiPost(`/api/tournaments/${tournamentId}/draw`, {});
      await load();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setDrawing(false);
    }
  }

  async function handleReset() {
    try {
      setResetting(true);
      setError(null);
      await apiDelete(`/api/tournaments/${tournamentId}/draw`);
      await load();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setResetting(false);
      setConfirmReset(false);
    }
  }

  async function handleStart() {
    try {
      setStarting(true);
      setError(null);
      await apiPost(`/api/tournaments/${tournamentId}/draw/start`, {});
      onStarted();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setStarting(false);
      setConfirmStart(false);
    }
  }

  const allDrawn = draw?.categories.every((category) => category.hasActiveDraw) ?? false;

  return (
    <div className="manage-panel">
      <div className="manage-panel-header">
        <h2 className="manage-panel-title">
          <Shuffle size={18} />
          Bốc thăm và tạo nhánh
        </h2>
        <div className="action-row compact">
          <button className="btn btn-ghost" onClick={() => setConfirmReset(true)} disabled={resetting || drawing}>
            {resetting ? <Loader2 size={16} className="spin" /> : <RefreshCcw size={16} />}
            Reset
          </button>
          <button className="btn btn-outline" onClick={handleDraw} disabled={drawing || resetting}>
            {drawing ? <Loader2 size={16} className="spin" /> : <Shuffle size={16} />}
            Tạo ngẫu nhiên
          </button>
          <button className="btn btn-primary" onClick={() => setConfirmStart(true)} disabled={starting || !allDrawn}>
            {starting ? <Loader2 size={16} className="spin" /> : <Play size={16} />}
            Bắt đầu giải
          </button>
        </div>
      </div>

      <div className="manage-panel-body">
        {error ? <div className="status-banner status-banner--warn">{error}</div> : null}
        {!allDrawn && draw && draw.categories.length ? <div className="status-banner status-banner--info">Cần hoàn thành bốc thăm cho tất cả hạng cân trước khi bắt đầu giải.</div> : null}

        {loading ? (
          <div className="page-spinner"><Loader2 size={18} className="spin" /> Đang tải...</div>
        ) : !draw || !draw.categories.length ? (
          <EmptyState icon={<CircleDot size={18} />} text="Chưa có hạng cân nào để tạo nhánh đấu." />
        ) : (
          <div className="draw-grid">
            {draw.categories.map((category) => (
              <div key={category.categoryId} className={`draw-card${category.hasActiveDraw ? " draw-card--has-draw" : ""}`}>
                <div className="draw-card__name">{category.categoryName}</div>
                <div className="draw-card__meta">
                  <span>{category.athleteCount} VĐV</span>
                  <span>Bảng {category.bracketSize}</span>
                </div>
                <div className="draw-card__status">
                  {category.hasActiveDraw ? "Đã bốc thăm" : "Chưa bốc thăm"}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <ConfirmModal
        open={confirmReset}
        title="Reset bốc thăm?"
        message="Toàn bộ nhánh đấu sẽ bị xóa để tạo lại từ đầu."
        confirmLabel="Reset"
        dangerous
        onConfirm={handleReset}
        onCancel={() => setConfirmReset(false)}
      />

      <ConfirmModal
        open={confirmStart}
        title="Bắt đầu giải đấu?"
        message="Sau khi bắt đầu, hệ thống sẽ xem nhánh đấu là dữ liệu vận hành chính cho tatami và dashboard."
        confirmLabel="Bắt đầu"
        onConfirm={handleStart}
        onCancel={() => setConfirmStart(false)}
      />
    </div>
  );
}

function RunningPanel({ tournamentId }: { tournamentId: string }) {
  return (
    <div className="manage-panel">
      <div className="manage-panel-header">
        <h2 className="manage-panel-title">
          <Play size={18} />
          Giải đang diễn ra
        </h2>
      </div>

      <div className="manage-panel-body">
        <div className="status-banner status-banner--success">
          <CheckCircle2 size={16} />
          Giải đấu đã bắt đầu. Lúc này nên điều phối qua dashboard, tatami control và các màn hình live.
        </div>

        <div className="auth-action-list">
          <a className="auth-action-row" href={`/dashboard/tournaments/${tournamentId}`}>
            <Trophy size={18} />
            <div>
              <strong>Mở dashboard giải</strong>
              <span>Theo dõi tiến độ và luồng điều phối tổng thể.</span>
            </div>
          </a>
          <a className="auth-action-row" href="/tournaments">
            <Swords size={18} />
            <div>
              <strong>Quay về command-center</strong>
              <span>Tiếp tục quản lý tatami, entries hoặc các giải khác.</span>
            </div>
          </a>
        </div>
      </div>
    </div>
  );
}

function EmptyState({ icon, text }: { icon: ReactNode; text: string }) {
  return (
    <div className="empty-state">
      {icon}
      <p>{text}</p>
    </div>
  );
}

function groupAthletes(athletes: AthleteApprovalItem[], view: AthleteView) {
  if (view === "by-club") {
    return [...athletes].sort((a, b) => a.organizationName.localeCompare(b.organizationName) || a.athleteName.localeCompare(b.athleteName));
  }

  return [...athletes].sort((a, b) => a.categoryName.localeCompare(b.categoryName) || a.athleteName.localeCompare(b.athleteName));
}

function formatDate(iso: string) {
  try {
    return new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date(iso));
  } catch {
    return iso;
  }
}
