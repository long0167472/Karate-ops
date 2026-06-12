import { useEffect, useState } from "react";
import {
  apiGet,
  apiPost,
  apiPatch,
  apiDelete,
  errorMessage,
} from "./apiClient";
import type {
  TournamentExtended,
  ParticipantApprovalItem,
  AthleteApprovalItem,
  AthleteApprovalSummary,
  TournamentDraw,
} from "./types";
import { StatusBadge } from "./components/StatusBadge";
import { TournamentStepIndicator } from "./components/TournamentStepIndicator";
import { ConfirmModal } from "./components/ConfirmModal";
import "./tournament-styles.css";

// ─── Props ─────────────────────────────────────────────────────────────────

interface TournamentManagePageProps {
  tournamentId: string;
}

// ─── Root page ─────────────────────────────────────────────────────────────

export function TournamentManagePage({ tournamentId }: TournamentManagePageProps) {
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tournamentId]);

  if (loading) {
    return (
      <div className="page-spinner">
        <SpinnerIcon />
        <span>Đang tải thông tin giải...</span>
      </div>
    );
  }

  if (error || !tournament) {
    return (
      <div className="page-error">
        <p>Không tải được thông tin giải.</p>
        {error && <p style={{ fontSize: "0.8rem", color: "var(--muted-2)" }}>{error}</p>}
        <button className="btn btn-ghost" onClick={loadTournament}>
          Thử lại
        </button>
      </div>
    );
  }

  return (
    <div className="tournament-manage-page">
      {/* Header */}
      <div className="tournament-manage-header">
        <h1 className="tournament-manage-title">
          <TrophyIcon />
          {tournament.name}
          <StatusBadge status={tournament.status} />
        </h1>
        {tournament.location && (
          <p className="tournament-manage-subtitle">
            {tournament.location}
            {tournament.startsOn && ` · ${formatDate(tournament.startsOn)}`}
            {tournament.endsOn && ` – ${formatDate(tournament.endsOn)}`}
          </p>
        )}
      </div>

      {/* Step indicator */}
      <div className="manage-panel" style={{ padding: "20px 24px" }}>
        <TournamentStepIndicator currentStep={tournament.step} />
      </div>

      {/* Step panels */}
      {tournament.step === 0 && (
        <SetupPanel
          tournament={tournament}
          onSaved={loadTournament}
          onOpened={loadTournament}
        />
      )}
      {tournament.step === 1 && (
        <ParticipantApprovalPanel
          tournamentId={tournamentId}
          onAdvanced={loadTournament}
        />
      )}
      {tournament.step === 2 && (
        <AthleteApprovalPanel
          tournamentId={tournamentId}
          onAdvanced={loadTournament}
        />
      )}
      {tournament.step === 3 && (
        <DrawPanel
          tournamentId={tournamentId}
          onStarted={loadTournament}
        />
      )}
      {tournament.step === 4 && (
        <RunningPanel tournamentId={tournamentId} />
      )}
    </div>
  );
}

// ─── Step 0: Setup ─────────────────────────────────────────────────────────

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
    setForm((f) => ({ ...f, [key]: value }));
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
          <GearIcon />
          Cấu hình giải đấu
        </h2>
        {isOpen && (
          <div className="status-banner status-banner--success">
            <CheckCircleIcon />
            Đang mở đăng ký
          </div>
        )}
      </div>

      <div className="manage-panel-body">
        {saveError && (
          <div className="status-banner status-banner--warn">{saveError}</div>
        )}

        <div className="form-grid">
          <div className="form-field form-field--full">
            <label>Tên giải</label>
            <input
              type="text"
              value={form.name}
              onChange={(e) => setField("name", e.target.value)}
              placeholder="VD: Giải vô địch Karate tỉnh 2026"
            />
          </div>

          <div className="form-field form-field--full">
            <label>Mô tả</label>
            <textarea
              value={form.description}
              onChange={(e) => setField("description", e.target.value)}
              placeholder="Mô tả ngắn về giải..."
            />
          </div>

          <div className="form-field">
            <label>Địa điểm</label>
            <input
              type="text"
              value={form.location}
              onChange={(e) => setField("location", e.target.value)}
              placeholder="Tên sân / tỉnh thành"
            />
          </div>

          <div className="form-field">
            <label>Phí đăng ký (VND)</label>
            <input
              type="number"
              value={form.registrationFee}
              min={0}
              onChange={(e) => setField("registrationFee", Number(e.target.value))}
            />
          </div>

          <div className="form-field">
            <label>Ngày bắt đầu</label>
            <input
              type="date"
              value={form.startsOn}
              onChange={(e) => setField("startsOn", e.target.value)}
            />
          </div>

          <div className="form-field">
            <label>Ngày kết thúc</label>
            <input
              type="date"
              value={form.endsOn}
              onChange={(e) => setField("endsOn", e.target.value)}
            />
          </div>

          <div className="form-field">
            <label>Hạn đăng ký</label>
            <input
              type="date"
              value={form.registrationDeadline}
              onChange={(e) => setField("registrationDeadline", e.target.value)}
            />
          </div>
        </div>

        <hr className="section-divider" />

        {/* Toggles */}
        <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
          <p style={{ margin: 0, fontSize: "0.8rem", fontWeight: 700, color: "var(--muted)", textTransform: "uppercase", letterSpacing: "0.05em" }}>
            Hạng thi đấu
          </p>
          <label className="toggle-row">
            <div
              className={`toggle-track${form.phongTraoEnabled ? " toggle-track--on" : ""}`}
              onClick={() => setField("phongTraoEnabled", !form.phongTraoEnabled)}
            >
              <div className="toggle-thumb" />
            </div>
            <span className="toggle-label-text">Hạng phong trào</span>
          </label>
          <label className="toggle-row">
            <div
              className={`toggle-track${form.nangCaoEnabled ? " toggle-track--on" : ""}`}
              onClick={() => setField("nangCaoEnabled", !form.nangCaoEnabled)}
            >
              <div className="toggle-thumb" />
            </div>
            <span className="toggle-label-text">Hạng nâng cao</span>
          </label>
        </div>

        <hr className="section-divider" />

        <div className="action-row">
          <button className="btn btn-ghost" onClick={handleSave} disabled={saving}>
            {saving ? <SpinnerIcon /> : <SaveIcon />}
            {saving ? "Đang lưu..." : "Lưu thông tin"}
          </button>

          {!isOpen && (
            <button
              className="btn btn-primary"
              onClick={() => setConfirmOpen(true)}
              disabled={opening}
            >
              {opening ? <SpinnerIcon /> : <UnlockIcon />}
              {opening ? "Đang xử lý..." : "Mở đăng ký"}
            </button>
          )}
        </div>

        {openError && (
          <div className="status-banner status-banner--warn">{openError}</div>
        )}
      </div>

      <ConfirmModal
        open={confirmOpen}
        title="Mở đăng ký?"
        message="Sau khi mở đăng ký, các CLB có thể đăng ký tham dự. Bạn vẫn có thể chỉnh sửa thông tin giải sau."
        confirmLabel="Mở đăng ký"
        onConfirm={handleOpenRegistration}
        onCancel={() => setConfirmOpen(false)}
      />
    </div>
  );
}

// ─── Step 1: Participant (Club) Approval ───────────────────────────────────

function ParticipantApprovalPanel({
  tournamentId,
  onAdvanced,
}: {
  tournamentId: string;
  onAdvanced: () => void;
}) {
  const [clubs, setClubs] = useState<ParticipantApprovalItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [confirm, setConfirm] = useState<{
    participantId: string;
    action: "approve" | "inactive";
    name: string;
  } | null>(null);
  const [actioning, setActioning] = useState(false);
  const [advancing, setAdvancing] = useState(false);
  const [advanceError, setAdvanceError] = useState<string | null>(null);
  const [confirmAdvance, setConfirmAdvance] = useState(false);

  async function load() {
    try {
      setLoading(true);
      setError(null);
      const data = await apiGet<ParticipantApprovalItem[]>(
        `/api/tournaments/${tournamentId}/approval/clubs`
      );
      setClubs(data);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tournamentId]);

  async function handleAction() {
    if (!confirm) return;
    try {
      setActioning(true);
      await apiPatch(
        `/api/tournaments/${tournamentId}/approval/clubs/${confirm.participantId}`,
        { action: confirm.action === "approve" ? "APPROVE" : "INACTIVE" }
      );
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
          <ClubIcon />
          Duyệt câu lạc bộ
        </h2>
        <button
          className="btn btn-primary"
          onClick={() => setConfirmAdvance(true)}
          disabled={advancing}
        >
          {advancing ? <SpinnerIcon /> : null}
          Tiếp theo: Duyệt VDV
        </button>
      </div>

      <div className="manage-panel-body">
        {error && <div className="status-banner status-banner--warn">{error}</div>}
        {advanceError && <div className="status-banner status-banner--warn">{advanceError}</div>}

        {loading ? (
          <div className="page-spinner"><SpinnerIcon /> Đang tải...</div>
        ) : clubs.length === 0 ? (
          <div className="empty-state">
            <ClubIcon />
            <p>Chưa có câu lạc bộ nào đăng ký.</p>
          </div>
        ) : (
          <div style={{ overflowX: "auto" }}>
            <table className="manage-table">
              <thead>
                <tr>
                  <th>CLB</th>
                  <th>Trạng thái</th>
                  <th>VDV</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {clubs.map((club) => (
                  <tr key={club.participantId}>
                    <td>
                      <div style={{ fontWeight: 600 }}>{club.organizationName}</div>
                      {club.displayName && club.displayName !== club.organizationName && (
                        <div style={{ fontSize: "0.78rem", color: "var(--muted)" }}>{club.displayName}</div>
                      )}
                    </td>
                    <td>
                      <StatusBadge status={club.status} size="sm" />
                    </td>
                    <td style={{ color: "var(--muted)" }}>
                      {club.approvedEntries}/{club.totalEntries}
                    </td>
                    <td>
                      <div className="action-row">
                        {club.status !== "APPROVED" && (
                          <button
                            className="btn btn-outline btn-sm"
                            onClick={() =>
                              setConfirm({
                                participantId: club.participantId,
                                action: "approve",
                                name: club.organizationName,
                              })
                            }
                          >
                            Duyệt
                          </button>
                        )}
                        {club.status !== "INACTIVE" && (
                          <button
                            className="btn btn-danger btn-sm"
                            onClick={() =>
                              setConfirm({
                                participantId: club.participantId,
                                action: "inactive",
                                name: club.organizationName,
                              })
                            }
                          >
                            Không tham dự
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Action confirm */}
      <ConfirmModal
        open={confirm !== null}
        title={
          confirm?.action === "approve"
            ? `Duyệt CLB ${confirm?.name}?`
            : `Đánh dấu không tham dự?`
        }
        message={
          confirm?.action === "approve"
            ? `CLB ${confirm?.name} sẽ được duyệt tham dự giải.`
            : `CLB ${confirm?.name} sẽ bị đánh dấu không tham dự giải này.`
        }
        confirmLabel={confirm?.action === "approve" ? "Duyệt" : "Xác nhận"}
        dangerous={confirm?.action === "inactive"}
        onConfirm={handleAction}
        onCancel={() => setConfirm(null)}
      />

      {/* Advance confirm */}
      <ConfirmModal
        open={confirmAdvance}
        title="Chuyển sang bước Duyệt VDV?"
        message="Sau bước này, bạn sẽ không thể thêm CLB mới. Đảm bảo đã duyệt đủ các CLB cần thiết."
        confirmLabel="Tiếp tục"
        onConfirm={handleAdvance}
        onCancel={() => setConfirmAdvance(false)}
      />
    </div>
  );
}

// ─── Step 2: Athlete Approval ──────────────────────────────────────────────

type AthleteView = "by-club" | "by-category";

function AthleteApprovalPanel({
  tournamentId,
  onAdvanced,
}: {
  tournamentId: string;
  onAdvanced: () => void;
}) {
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
      const [list, sum] = await Promise.all([
        apiGet<AthleteApprovalItem[]>(`/api/tournaments/${tournamentId}/approval/athletes`),
        apiGet<AthleteApprovalSummary>(`/api/tournaments/${tournamentId}/approval/athletes/summary`),
      ]);
      setAthletes(list);
      setSummary(sum);
      setSelected(new Set());
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tournamentId]);

  function toggleSelect(entryId: string) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(entryId)) next.delete(entryId);
      else next.add(entryId);
      return next;
    });
  }

  function toggleAll() {
    const pending = athletes.filter((a) => a.btcApprovalStatus === "PENDING");
    if (selected.size === pending.length && pending.length > 0) {
      setSelected(new Set());
    } else {
      setSelected(new Set(pending.map((a) => a.entryId)));
    }
  }

  async function handleBulkApprove() {
    if (selected.size === 0) return;
    try {
      setBulkApproving(true);
      await apiPost(`/api/tournaments/${tournamentId}/approval/athletes/bulk`, {
        entryIds: Array.from(selected),
      });
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

  // Group athletes for display
  const grouped = groupAthletes(athletes, view);
  const pendingAthletes = athletes.filter((a) => a.btcApprovalStatus === "PENDING");
  const allPendingSelected =
    pendingAthletes.length > 0 && selected.size === pendingAthletes.length;

  return (
    <div className="manage-panel">
      <div className="manage-panel-header">
        <h2 className="manage-panel-title">
          <AthleteIcon />
          Duyệt vận động viên
        </h2>
        <button
          className="btn btn-primary"
          onClick={() => setConfirmAdvance(true)}
          disabled={advancing}
        >
          {advancing ? <SpinnerIcon /> : null}
          Tạo Sigma
        </button>
      </div>

      <div className="manage-panel-body">
        {/* Summary */}
        {summary && (
          <div className="summary-counters">
            <div className="summary-counter">
              <span className="summary-counter__value">{summary.totalEntries}</span>
              <span className="summary-counter__label">Tổng đăng ký</span>
            </div>
            <div className="summary-counter">
              <span className="summary-counter__value" style={{ color: "var(--ao)" }}>
                {summary.approved}
              </span>
              <span className="summary-counter__label">Đã duyệt</span>
            </div>
            <div className="summary-counter">
              <span className="summary-counter__value" style={{ color: "#f59e0b" }}>
                {summary.pending}
              </span>
              <span className="summary-counter__label">Chờ duyệt</span>
            </div>
            <div className="summary-counter">
              <span className="summary-counter__value" style={{ color: "var(--aka)" }}>
                {summary.rejected}
              </span>
              <span className="summary-counter__label">Từ chối</span>
            </div>
          </div>
        )}

        {error && <div className="status-banner status-banner--warn">{error}</div>}
        {advanceError && <div className="status-banner status-banner--warn">{advanceError}</div>}

        {/* Toolbar */}
        <div className="action-row" style={{ justifyContent: "space-between", flexWrap: "wrap" }}>
          <div className="view-switcher">
            <button
              className={`view-switcher__btn${view === "by-club" ? " view-switcher__btn--active" : ""}`}
              onClick={() => setView("by-club")}
            >
              Theo CLB
            </button>
            <button
              className={`view-switcher__btn${view === "by-category" ? " view-switcher__btn--active" : ""}`}
              onClick={() => setView("by-category")}
            >
              Theo hạng cân
            </button>
          </div>

          {selected.size > 0 && (
            <button
              className="btn btn-outline"
              onClick={handleBulkApprove}
              disabled={bulkApproving}
            >
              {bulkApproving ? <SpinnerIcon /> : <CheckIcon />}
              Duyệt {selected.size} VDV
            </button>
          )}
        </div>

        {loading ? (
          <div className="page-spinner"><SpinnerIcon /> Đang tải...</div>
        ) : athletes.length === 0 ? (
          <div className="empty-state">
            <AthleteIcon />
            <p>Chưa có vận động viên nào đăng ký.</p>
          </div>
        ) : (
          <div style={{ overflowX: "auto" }}>
            <table className="manage-table">
              <thead>
                <tr>
                  <th style={{ width: 32 }}>
                    <input
                      type="checkbox"
                      checked={allPendingSelected}
                      onChange={toggleAll}
                    />
                  </th>
                  <th>{view === "by-club" ? "CLB" : "Hạng cân"}</th>
                  <th>Vận động viên</th>
                  <th>{view === "by-club" ? "Hạng cân" : "CLB"}</th>
                  <th>Cân nặng (kg)</th>
                  <th>Trạng thái</th>
                </tr>
              </thead>
              <tbody>
                {grouped.map((row) => (
                  <tr key={row.entryId}>
                    <td>
                      {row.btcApprovalStatus === "PENDING" && (
                        <input
                          type="checkbox"
                          checked={selected.has(row.entryId)}
                          onChange={() => toggleSelect(row.entryId)}
                        />
                      )}
                    </td>
                    <td style={{ color: "var(--muted)", fontSize: "0.8rem" }}>
                      {view === "by-club" ? row.organizationName : row.categoryName}
                    </td>
                    <td style={{ fontWeight: 600 }}>{row.athleteName}</td>
                    <td style={{ color: "var(--muted)", fontSize: "0.8rem" }}>
                      {view === "by-club" ? row.categoryName : row.organizationName}
                    </td>
                    <td style={{ color: "var(--muted)" }}>
                      {row.registrationWeightKg ?? "—"}
                    </td>
                    <td>
                      <StatusBadge status={row.btcApprovalStatus} size="sm" />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <ConfirmModal
        open={confirmAdvance}
        title="Tạo Sigma (bốc thăm)?"
        message="Sau bước này, danh sách VDV sẽ được chốt và hệ thống sẽ tạo nhánh đấu. Không thể thêm VDV sau khi bắt đầu sigma."
        confirmLabel="Tạo Sigma"
        onConfirm={handleAdvance}
        onCancel={() => setConfirmAdvance(false)}
      />
    </div>
  );
}

function groupAthletes(athletes: AthleteApprovalItem[], view: AthleteView): AthleteApprovalItem[] {
  if (view === "by-club") {
    return [...athletes].sort((a, b) =>
      a.organizationName.localeCompare(b.organizationName) ||
      a.athleteName.localeCompare(b.athleteName)
    );
  }
  return [...athletes].sort((a, b) =>
    a.categoryName.localeCompare(b.categoryName) ||
    a.athleteName.localeCompare(b.athleteName)
  );
}

// ─── Step 3: Draw / Sigma ──────────────────────────────────────────────────

function DrawPanel({
  tournamentId,
  onStarted,
}: {
  tournamentId: string;
  onStarted: () => void;
}) {
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
      const data = await apiGet<TournamentDraw>(`/api/tournaments/${tournamentId}/draw`);
      setDraw(data);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
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

  const allDrawn = draw?.categories.every((c) => c.hasActiveDraw) ?? false;

  return (
    <div className="manage-panel">
      <div className="manage-panel-header">
        <h2 className="manage-panel-title">
          <DrawIcon />
          Bốc thăm (Sigma)
        </h2>
        <div className="action-row">
          <button
            className="btn btn-ghost"
            onClick={() => setConfirmReset(true)}
            disabled={resetting || drawing}
          >
            {resetting ? <SpinnerIcon /> : <ResetIcon />}
            Reset
          </button>
          <button
            className="btn btn-outline"
            onClick={handleDraw}
            disabled={drawing || resetting}
          >
            {drawing ? <SpinnerIcon /> : <DiceIcon />}
            Tạo ngẫu nhiên
          </button>
          <button
            className="btn btn-primary"
            onClick={() => setConfirmStart(true)}
            disabled={starting || !allDrawn}
            title={!allDrawn ? "Cần tạo nhánh đấu cho tất cả hạng cân trước" : undefined}
          >
            {starting ? <SpinnerIcon /> : <PlayIcon />}
            Bắt đầu giải
          </button>
        </div>
      </div>

      <div className="manage-panel-body">
        {error && <div className="status-banner status-banner--warn">{error}</div>}

        {!allDrawn && draw && draw.categories.length > 0 && (
          <div className="status-banner status-banner--info">
            Cần hoàn thành bốc thăm tất cả hạng cân trước khi bắt đầu giải.
          </div>
        )}

        {loading ? (
          <div className="page-spinner"><SpinnerIcon /> Đang tải...</div>
        ) : !draw || draw.categories.length === 0 ? (
          <div className="empty-state">
            <DrawIcon />
            <p>Chưa có hạng cân nào. Kiểm tra lại cấu hình giải.</p>
          </div>
        ) : (
          <div className="draw-grid">
            {draw.categories.map((cat) => (
              <div
                key={cat.categoryId}
                className={`draw-card${cat.hasActiveDraw ? " draw-card--has-draw" : ""}`}
              >
                <div className="draw-card__name">{cat.categoryName}</div>
                <div className="draw-card__meta">
                  <span>{cat.athleteCount} VDV</span>
                  <span>Bảng {cat.bracketSize}</span>
                </div>
                <div
                  className="draw-card__status"
                  style={{ color: cat.hasActiveDraw ? "var(--green)" : "var(--muted)" }}
                >
                  {cat.hasActiveDraw ? "Đã bốc thăm" : "Chưa bốc thăm"}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <ConfirmModal
        open={confirmReset}
        title="Reset bốc thăm?"
        message="Toàn bộ kết quả bốc thăm sẽ bị xoá. Bạn có thể bốc thăm lại sau."
        confirmLabel="Reset"
        dangerous
        onConfirm={handleReset}
        onCancel={() => setConfirmReset(false)}
      />

      <ConfirmModal
        open={confirmStart}
        title="Bắt đầu giải đấu?"
        message="Sau khi bắt đầu, sơ đồ đấu sẽ được chốt và không thể thay đổi. Trọng tài có thể vào điều khiển trận đấu."
        confirmLabel="Bắt đầu"
        onConfirm={handleStart}
        onCancel={() => setConfirmStart(false)}
      />
    </div>
  );
}

// ─── Step 4: Running ───────────────────────────────────────────────────────

function RunningPanel({ tournamentId }: { tournamentId: string }) {
  return (
    <div className="manage-panel">
      <div className="manage-panel-header">
        <h2 className="manage-panel-title">
          <PlayIcon />
          Giải đang diễn ra
        </h2>
      </div>
      <div className="manage-panel-body">
        <div className="status-banner status-banner--success">
          <CheckCircleIcon />
          Giải đấu đã bắt đầu. Trọng tài và tatami đang hoạt động.
        </div>
        <div className="action-row">
          <a
            className="btn btn-primary"
            href={`/tournament/${tournamentId}/dashboard`}
          >
            Xem Dashboard giải
          </a>
          <a
            className="btn btn-ghost"
            href={`/tournament/${tournamentId}/tatami`}
          >
            Quản lý Tatami
          </a>
        </div>
      </div>
    </div>
  );
}

// ─── Helpers ───────────────────────────────────────────────────────────────

function formatDate(iso: string): string {
  try {
    return new Intl.DateTimeFormat("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    }).format(new Date(iso));
  } catch {
    return iso;
  }
}

// ─── Inline icon components ────────────────────────────────────────────────

function SpinnerIcon() {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      style={{ animation: "spin 0.8s linear infinite" }}
    >
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
      <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeDasharray="50 14" />
    </svg>
  );
}

function TrophyIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M8 21h8M12 17v4M7 4H5a2 2 0 0 0-2 2v2a4 4 0 0 0 4 4h.1M17 4h2a2 2 0 0 1 2 2v2a4 4 0 0 1-4 4h-.1M7 4h10v6a5 5 0 0 1-10 0V4Z" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  );
}

function GearIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z" stroke="currentColor" strokeWidth="1.7"/>
      <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1Z" stroke="currentColor" strokeWidth="1.7"/>
    </svg>
  );
}

function ClubIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9Z" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round"/>
      <path d="M9 22V12h6v10" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  );
}

function AthleteIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <circle cx="12" cy="7" r="4" stroke="currentColor" strokeWidth="1.7"/>
      <path d="M4 21v-1a8 8 0 0 1 16 0v1" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round"/>
    </svg>
  );
}

function DrawIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect x="3" y="3" width="6" height="6" rx="1" stroke="currentColor" strokeWidth="1.7"/>
      <rect x="15" y="3" width="6" height="6" rx="1" stroke="currentColor" strokeWidth="1.7"/>
      <rect x="15" y="15" width="6" height="6" rx="1" stroke="currentColor" strokeWidth="1.7"/>
      <path d="M9 6h3v12h3" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round"/>
      <path d="M12 12h3" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round"/>
    </svg>
  );
}

function SaveIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2Z" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round"/>
      <path d="M17 21v-8H7v8" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round"/>
      <path d="M7 3v5h8" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  );
}

function UnlockIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect x="3" y="11" width="18" height="11" rx="2" stroke="currentColor" strokeWidth="1.7"/>
      <path d="M7 11V7a5 5 0 0 1 9.9-1" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round"/>
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M20 6L9 17l-5-5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  );
}

function CheckCircleIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="1.7"/>
      <path d="M8.5 12.5l2.5 2.5 4.5-5" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  );
}

function ResetIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round"/>
      <path d="M3 3v5h5" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  );
}

function DiceIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect x="3" y="3" width="18" height="18" rx="3" stroke="currentColor" strokeWidth="1.7"/>
      <circle cx="8" cy="8" r="1.2" fill="currentColor"/>
      <circle cx="16" cy="8" r="1.2" fill="currentColor"/>
      <circle cx="8" cy="16" r="1.2" fill="currentColor"/>
      <circle cx="16" cy="16" r="1.2" fill="currentColor"/>
      <circle cx="12" cy="12" r="1.2" fill="currentColor"/>
    </svg>
  );
}

function PlayIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <polygon points="5,3 19,12 5,21" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" fill="currentColor" fillOpacity="0.15"/>
    </svg>
  );
}
