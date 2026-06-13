import { Award, ChevronDown, ChevronRight, Plus, Trash2 } from "lucide-react";
import { type Dispatch, type FormEvent, type SetStateAction, useState } from "react";
import type { AthleteResponse, BeltExamCandidateResponse, BeltExamResponse, ClubRosterResponse } from "../../../types";
import { cx } from "../../../utils";
import {
  addBeltExamCandidate,
  applyBeltExamResults,
  createBeltExam,
  deleteBeltExam,
  removeBeltExamCandidate,
  updateBeltExam,
  updateBeltExamCandidate
} from "../clubApi";
import { BELT_RANK_LABELS, BELT_RANKS, EXAM_RESULT_LABELS, EXAM_STATUS_LABELS } from "../clubConstants";
import { errorMessage, formatDate } from "../clubUtils";

interface BeltExamTabProps {
  clubId: string;
  roster: ClubRosterResponse[];
  athletes: AthleteResponse[];
  beltExams: BeltExamResponse[];
  setBeltExams: Dispatch<SetStateAction<BeltExamResponse[]>>;
  busy: boolean;
  setBusy: (v: boolean) => void;
  setError: (v: string | null) => void;
}

export function BeltExamTab({ clubId, roster, athletes, beltExams, setBeltExams, busy, setBusy, setError }: BeltExamTabProps) {
  const [selectedExamId, setSelectedExamId] = useState<string | null>(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [createForm, setCreateForm] = useState({ name: "", examDate: "", location: "", examinerName: "", notes: "" });
  const [targetBelts, setTargetBelts] = useState<Record<string, string>>({});

  const selectedExam = beltExams.find((e) => e.id === selectedExamId) ?? null;

  async function run(action: () => Promise<void>) {
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

  function patchExam(updated: BeltExamResponse) {
    setBeltExams((prev) => prev.map((e) => e.id === updated.id ? updated : e));
  }

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    if (!createForm.name.trim()) return;
    await run(async () => {
      const created = await createBeltExam(clubId, {
        name: createForm.name.trim(),
        status: "DRAFT",
        examDate: createForm.examDate || undefined,
        location: createForm.location || undefined,
        examinerName: createForm.examinerName || undefined,
        notes: createForm.notes || undefined
      });
      setBeltExams((prev) => [created, ...prev]);
      setCreateForm({ name: "", examDate: "", location: "", examinerName: "", notes: "" });
      setShowCreateForm(false);
      setSelectedExamId(created.id);
    });
  }

  async function handleStatusChange(examId: string, status: string) {
    await run(async () => {
      const updated = await updateBeltExam(examId, { status });
      patchExam(updated);
    });
  }

  async function handleDeleteExam(examId: string) {
    await run(async () => {
      await deleteBeltExam(examId);
      setBeltExams((prev) => prev.filter((e) => e.id !== examId));
      if (selectedExamId === examId) setSelectedExamId(null);
    });
  }

  async function handleAddCandidate(athleteId: string) {
    if (!selectedExamId) return;
    const targetBelt = targetBelts[athleteId];
    if (!targetBelt) return;
    const athlete = athletes.find((a) => a.id === athleteId);
    await run(async () => {
      await addBeltExamCandidate(selectedExamId, {
        athleteId,
        currentBelt: athlete?.belt,
        targetBelt
      });
      const refreshed = await import("../../../apiClient").then(({ apiGet }) =>
        apiGet<BeltExamResponse>(`/api/belt-exams/${selectedExamId}`)
      );
      patchExam(refreshed);
      setTargetBelts((prev) => { const next = { ...prev }; delete next[athleteId]; return next; });
    });
  }

  async function handleUpdateResult(examId: string, candidateId: string, result: string) {
    await run(async () => {
      await updateBeltExamCandidate(examId, candidateId, { result });
      const refreshed = await import("../../../apiClient").then(({ apiGet }) =>
        apiGet<BeltExamResponse>(`/api/belt-exams/${examId}`)
      );
      patchExam(refreshed);
    });
  }

  async function handleRemoveCandidate(examId: string, candidateId: string) {
    await run(async () => {
      await removeBeltExamCandidate(examId, candidateId);
      setBeltExams((prev) => prev.map((e) => e.id !== examId ? e : { ...e, candidates: e.candidates.filter((c) => c.id !== candidateId) }));
    });
  }

  async function handleApplyResults(examId: string) {
    await run(async () => {
      const updated = await applyBeltExamResults(examId);
      patchExam(updated);
    });
  }

  const candidateAthleteIds = new Set(selectedExam?.candidates.map((c) => c.athleteId).filter(Boolean) as string[]);
  const eligibleRoster = roster.filter((r) => !candidateAthleteIds.has(r.athleteId));

  return (
    <div className="club-tab-content">
      <div className="club-section-head">
        <div>
          <span className="club-ops-kicker">Thăng cấp đai</span>
          <h2>Kỳ thi lên đai</h2>
        </div>
        <button className="club-primary-button" onClick={() => setShowCreateForm(!showCreateForm)}>
          <Plus size={18} /> Tạo kỳ thi mới
        </button>
      </div>

      {showCreateForm ? (
        <form className="club-form-card" onSubmit={handleCreate}>
          <h3>Tạo kỳ thi mới</h3>
          <div className="club-form-grid">
            <label>
              <span>Tên kỳ thi *</span>
              <input type="text" value={createForm.name} onChange={(e) => setCreateForm({ ...createForm, name: e.target.value })} placeholder="VD: Kỳ thi lên đai tháng 6/2026" required />
            </label>
            <label>
              <span>Ngày thi</span>
              <input type="date" value={createForm.examDate} onChange={(e) => setCreateForm({ ...createForm, examDate: e.target.value })} />
            </label>
            <label>
              <span>Địa điểm</span>
              <input type="text" value={createForm.location} onChange={(e) => setCreateForm({ ...createForm, location: e.target.value })} placeholder="Tên võ đường hoặc địa chỉ" />
            </label>
            <label>
              <span>Giám khảo</span>
              <input type="text" value={createForm.examinerName} onChange={(e) => setCreateForm({ ...createForm, examinerName: e.target.value })} placeholder="Tên HLV / giám khảo" />
            </label>
          </div>
          <label>
            <span>Ghi chú</span>
            <textarea value={createForm.notes} onChange={(e) => setCreateForm({ ...createForm, notes: e.target.value })} rows={2} placeholder="Ghi chú thêm..." />
          </label>
          <div className="club-form-actions">
            <button type="button" className="club-secondary-button" onClick={() => setShowCreateForm(false)}>Hủy</button>
            <button type="submit" className="club-primary-button" disabled={busy || !createForm.name.trim()}>Tạo kỳ thi</button>
          </div>
        </form>
      ) : null}

      <div className="club-exam-layout">
        <aside className="club-exam-list-panel">
          {beltExams.length === 0 ? (
            <div className="club-empty-state">
              <Award size={32} />
              <strong>Chưa có kỳ thi nào</strong>
              <p>Tạo kỳ thi đầu tiên để bắt đầu quản lý thăng cấp đai.</p>
            </div>
          ) : (
            beltExams.map((exam) => (
              <button
                key={exam.id}
                className={cx("club-exam-list-item", selectedExamId === exam.id && "active")}
                onClick={() => setSelectedExamId(exam.id === selectedExamId ? null : exam.id)}
              >
                <div className="club-exam-list-header">
                  <strong>{exam.name}</strong>
                  {selectedExamId === exam.id ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                </div>
                <div className="club-exam-list-meta">
                  <span className={cx("club-exam-status-chip", exam.status.toLowerCase())}>{EXAM_STATUS_LABELS[exam.status]}</span>
                  {exam.examDate ? <span>{formatDate(exam.examDate)}</span> : null}
                  <span>{exam.candidates.length} thí sinh</span>
                </div>
              </button>
            ))
          )}
        </aside>

        {selectedExam ? (
          <section className="club-exam-detail-panel">
            <div className="club-exam-detail-head">
              <div>
                <h3>{selectedExam.name}</h3>
                {selectedExam.examDate ? <p>Ngày thi: <strong>{formatDate(selectedExam.examDate)}</strong></p> : null}
                {selectedExam.location ? <p>Địa điểm: <strong>{selectedExam.location}</strong></p> : null}
                {selectedExam.examinerName ? <p>Giám khảo: <strong>{selectedExam.examinerName}</strong></p> : null}
              </div>
              <div className="club-exam-actions">
                <select
                  value={selectedExam.status}
                  disabled={busy || selectedExam.status === "COMPLETED" || selectedExam.status === "CANCELLED"}
                  onChange={(e) => handleStatusChange(selectedExam.id, e.target.value)}
                >
                  {Object.entries(EXAM_STATUS_LABELS).map(([value, label]) => (
                    <option key={value} value={value}>{label}</option>
                  ))}
                </select>
                {selectedExam.status === "COMPLETED" ? (
                  <button
                    className="club-primary-button"
                    disabled={busy}
                    onClick={() => handleApplyResults(selectedExam.id)}
                  >
                    <Award size={16} /> Áp dụng kết quả
                  </button>
                ) : null}
                {selectedExam.status !== "COMPLETED" && selectedExam.status !== "CANCELLED" ? (
                  <button
                    className="club-danger-button"
                    disabled={busy}
                    onClick={() => handleDeleteExam(selectedExam.id)}
                  >
                    <Trash2 size={16} />
                  </button>
                ) : null}
              </div>
            </div>

            <div className="club-exam-stats">
              <Metric label="Tổng thí sinh" value={selectedExam.candidates.length} />
              <Metric label="Đạt" value={selectedExam.candidates.filter((c) => c.result === "PASS").length} />
              <Metric label="Không đạt" value={selectedExam.candidates.filter((c) => c.result === "FAIL").length} />
              <Metric label="Đã áp dụng" value={selectedExam.candidates.filter((c) => c.beltApplied).length} />
            </div>

            {selectedExam.candidates.length > 0 ? (
              <div className="club-exam-candidates">
                <h4>Danh sách thí sinh</h4>
                {selectedExam.candidates.map((c) => (
                  <CandidateRow
                    key={c.id}
                    candidate={c}
                    exam={selectedExam}
                    busy={busy}
                    onUpdateResult={(result) => handleUpdateResult(selectedExam.id, c.id, result)}
                    onRemove={() => handleRemoveCandidate(selectedExam.id, c.id)}
                  />
                ))}
              </div>
            ) : null}

            {selectedExam.status !== "COMPLETED" && selectedExam.status !== "CANCELLED" && eligibleRoster.length > 0 ? (
              <div className="club-exam-add-candidates">
                <h4>Đăng ký thí sinh</h4>
                {eligibleRoster.map((r) => {
                  const athlete = athletes.find((a) => a.id === r.athleteId);
                  return (
                    <div key={r.id} className="club-exam-add-row">
                      <div className="club-person-cell">
                        <div className="club-avatar">{r.athleteName.charAt(0).toUpperCase()}</div>
                        <div>
                          <strong>{r.athleteName}</strong>
                          <span>Đai hiện tại: {athlete?.belt ? (BELT_RANK_LABELS[athlete.belt] ?? athlete.belt) : "Chưa có"}</span>
                        </div>
                      </div>
                      <div className="club-exam-add-actions">
                        <select
                          value={targetBelts[r.athleteId] ?? ""}
                          onChange={(e) => setTargetBelts((prev) => ({ ...prev, [r.athleteId]: e.target.value }))}
                        >
                          <option value="">-- Chọn đai mục tiêu --</option>
                          {BELT_RANKS.map((rank) => (
                            <option key={rank.value} value={rank.value}>{rank.label}</option>
                          ))}
                        </select>
                        <button
                          className="club-secondary-button"
                          disabled={busy || !targetBelts[r.athleteId]}
                          onClick={() => handleAddCandidate(r.athleteId)}
                        >
                          Đăng ký
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : null}
          </section>
        ) : (
          <div className="club-exam-placeholder">
            <Award size={40} />
            <p>Chọn một kỳ thi để xem chi tiết</p>
          </div>
        )}
      </div>
    </div>
  );
}

function CandidateRow({ candidate, exam, busy, onUpdateResult, onRemove }: {
  candidate: BeltExamCandidateResponse;
  exam: BeltExamResponse;
  busy: boolean;
  onUpdateResult: (result: string) => Promise<void>;
  onRemove: () => Promise<void>;
}) {
  const canEdit = exam.status !== "COMPLETED" && exam.status !== "CANCELLED";
  return (
    <div className="club-exam-candidate-row">
      <div className="club-person-cell">
        <div className="club-avatar">{(candidate.displayName ?? "?").charAt(0).toUpperCase()}</div>
        <div>
          <strong>{candidate.displayName ?? "—"}</strong>
          <span>
            {candidate.currentBelt ? (BELT_RANK_LABELS[candidate.currentBelt] ?? candidate.currentBelt) : "Chưa có đai"}
            {" → "}
            <b>{BELT_RANK_LABELS[candidate.targetBelt] ?? candidate.targetBelt}</b>
          </span>
        </div>
      </div>
      <div className="club-exam-candidate-actions">
        {canEdit ? (
          <div className="club-attendance-actions">
            {(["PASS", "FAIL", "ABSENT", "PENDING"] as const).map((result) => (
              <button
                key={result}
                className={cx("attendance-choice", candidate.result === result && "active", result.toLowerCase())}
                disabled={busy}
                onClick={() => onUpdateResult(result)}
              >
                {EXAM_RESULT_LABELS[result]}
              </button>
            ))}
          </div>
        ) : (
          <span className={cx("club-exam-result-chip", candidate.result.toLowerCase())}>
            {EXAM_RESULT_LABELS[candidate.result]}
            {candidate.beltApplied ? " ✓" : ""}
          </span>
        )}
        {canEdit ? (
          <button className="club-icon-button" disabled={busy} onClick={onRemove} title="Xóa thí sinh">
            <Trash2 size={16} />
          </button>
        ) : null}
      </div>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: number }) {
  return <div className="club-metric"><strong>{value}</strong><span>{label}</span></div>;
}
