import { Award, ChevronDown, ChevronRight, ListChecks, Pencil, Plus, Trash2 } from "lucide-react";
import { type Dispatch, type FormEvent, type SetStateAction, useState } from "react";
import { apiGet } from "../../../apiClient";
import type { AthleteResponse, BeltExamCandidateResponse, BeltExamCriterionResponse, BeltExamResponse, ClubRosterResponse } from "../../../types";
import { cx } from "../../../utils";
import {
  addBeltExamCandidate,
  addBeltExamCriterion,
  applyBeltExamResults,
  createBeltExam,
  deleteBeltExam,
  removeBeltExamCandidate,
  removeBeltExamCriterion,
  scoreBeltExamCandidate,
  updateBeltExam,
  updateBeltExamCandidate,
  updateBeltExamCriterion
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
  const [createForm, setCreateForm] = useState({ name: "", examDate: "", location: "", examinerName: "", passThreshold: "", notes: "" });
  const [targetBelts, setTargetBelts] = useState<Record<string, string>>({});
  const [criterionForm, setCriterionForm] = useState({ name: "", maxScore: "10", weight: "1" });
  const [editingCriterionId, setEditingCriterionId] = useState<string | null>(null);
  const [showCriterionForm, setShowCriterionForm] = useState(false);

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

  async function refreshExam(examId: string) {
    const refreshed = await apiGet<BeltExamResponse>(`/api/belt-exams/${examId}`);
    patchExam(refreshed);
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
        passThreshold: createForm.passThreshold ? Number(createForm.passThreshold) : undefined,
        notes: createForm.notes || undefined
      });
      setBeltExams((prev) => [created, ...prev]);
      setCreateForm({ name: "", examDate: "", location: "", examinerName: "", passThreshold: "", notes: "" });
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
      await addBeltExamCandidate(selectedExamId, { athleteId, currentBelt: athlete?.belt, targetBelt });
      await refreshExam(selectedExamId);
      setTargetBelts((prev) => { const next = { ...prev }; delete next[athleteId]; return next; });
    });
  }

  async function handleUpdateResult(examId: string, candidateId: string, result: string) {
    await run(async () => {
      await updateBeltExamCandidate(examId, candidateId, { result });
      await refreshExam(examId);
    });
  }

  async function handleRemoveCandidate(examId: string, candidateId: string) {
    await run(async () => {
      await removeBeltExamCandidate(examId, candidateId);
      await refreshExam(examId);
    });
  }

  async function handleApplyResults(examId: string) {
    await run(async () => {
      const updated = await applyBeltExamResults(examId);
      patchExam(updated);
    });
  }

  async function handleSaveCriterion(e: FormEvent) {
    e.preventDefault();
    if (!selectedExamId || !criterionForm.name.trim()) return;
    const body = {
      name: criterionForm.name.trim(),
      maxScore: criterionForm.maxScore ? Number(criterionForm.maxScore) : undefined,
      weight: criterionForm.weight ? Number(criterionForm.weight) : undefined
    };
    await run(async () => {
      if (editingCriterionId) {
        await updateBeltExamCriterion(selectedExamId, editingCriterionId, body);
      } else {
        await addBeltExamCriterion(selectedExamId, body);
      }
      await refreshExam(selectedExamId);
      setCriterionForm({ name: "", maxScore: "10", weight: "1" });
      setEditingCriterionId(null);
      setShowCriterionForm(false);
    });
  }

  function startEditCriterion(c: BeltExamCriterionResponse) {
    setCriterionForm({ name: c.name, maxScore: String(c.maxScore), weight: String(c.weight) });
    setEditingCriterionId(c.id);
    setShowCriterionForm(true);
  }

  async function handleRemoveCriterion(examId: string, criterionId: string) {
    await run(async () => {
      await removeBeltExamCriterion(examId, criterionId);
      await refreshExam(examId);
    });
  }

  async function handleScore(examId: string, candidateId: string, criterionId: string, value: string) {
    const num = Number(value);
    if (value === "" || Number.isNaN(num)) return;
    await run(async () => {
      await scoreBeltExamCandidate(examId, candidateId, criterionId, { score: num });
      await refreshExam(examId);
    });
  }

  const candidateAthleteIds = new Set(selectedExam?.candidates.map((c) => c.athleteId).filter(Boolean) as string[]);
  const eligibleRoster = roster.filter((r) => !candidateAthleteIds.has(r.athleteId));
  const editable = selectedExam ? selectedExam.status !== "COMPLETED" && selectedExam.status !== "CANCELLED" : false;

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
            <label>
              <span>Điểm sàn đạt (tổng điểm tối thiểu)</span>
              <input type="number" min={0} step="0.5" value={createForm.passThreshold} onChange={(e) => setCreateForm({ ...createForm, passThreshold: e.target.value })} placeholder="VD: 60" />
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
                {selectedExam.passThreshold != null ? <p>Điểm sàn đạt: <strong>{selectedExam.passThreshold}</strong></p> : null}
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
                  <button className="club-primary-button" disabled={busy} onClick={() => handleApplyResults(selectedExam.id)}>
                    <Award size={16} /> Áp dụng kết quả
                  </button>
                ) : null}
                {editable ? (
                  <button className="club-danger-button" disabled={busy} onClick={() => handleDeleteExam(selectedExam.id)}>
                    <Trash2 size={16} />
                  </button>
                ) : null}
              </div>
            </div>

            <div className="club-exam-stats">
              <Metric label="Tổng thí sinh" value={selectedExam.candidates.length} />
              <Metric label="Đạt" value={selectedExam.candidates.filter((c) => c.result === "PASS").length} />
              <Metric label="Không đạt" value={selectedExam.candidates.filter((c) => c.result === "FAIL").length} />
              <Metric label="Tiêu chí" value={selectedExam.criteria.length} />
            </div>

            {/* Criteria management */}
            <div className="club-exam-criteria">
              <div className="club-section-head">
                <div>
                  <span className="club-ops-kicker"><ListChecks size={14} /> Tiêu chí chấm điểm</span>
                </div>
                {editable ? (
                  <button className="club-secondary-button" onClick={() => { setShowCriterionForm(!showCriterionForm); setEditingCriterionId(null); setCriterionForm({ name: "", maxScore: "10", weight: "1" }); }}>
                    <Plus size={16} /> Thêm tiêu chí
                  </button>
                ) : null}
              </div>

              {showCriterionForm && editable ? (
                <form className="club-criterion-form" onSubmit={handleSaveCriterion}>
                  <input type="text" value={criterionForm.name} onChange={(e) => setCriterionForm({ ...criterionForm, name: e.target.value })} placeholder="Tên tiêu chí (VD: Kihon, Kata, Kumite)" required />
                  <input type="number" min={0.5} step="0.5" value={criterionForm.maxScore} onChange={(e) => setCriterionForm({ ...criterionForm, maxScore: e.target.value })} placeholder="Điểm tối đa" title="Điểm tối đa" />
                  <input type="number" min={0.5} step="0.5" value={criterionForm.weight} onChange={(e) => setCriterionForm({ ...criterionForm, weight: e.target.value })} placeholder="Hệ số" title="Hệ số" />
                  <button type="submit" className="club-primary-button" disabled={busy || !criterionForm.name.trim()}>{editingCriterionId ? "Lưu" : "Thêm"}</button>
                  <button type="button" className="club-secondary-button" onClick={() => { setShowCriterionForm(false); setEditingCriterionId(null); }}>Hủy</button>
                </form>
              ) : null}

              {selectedExam.criteria.length > 0 ? (
                <div className="club-criterion-list">
                  {selectedExam.criteria.map((c) => (
                    <div key={c.id} className="club-criterion-chip">
                      <strong>{c.name}</strong>
                      <span>tối đa {c.maxScore}{c.weight !== 1 ? ` ×${c.weight}` : ""}</span>
                      {editable ? (
                        <span className="club-criterion-chip-actions">
                          <button className="club-icon-button" disabled={busy} onClick={() => startEditCriterion(c)} title="Sửa"><Pencil size={14} /></button>
                          <button className="club-icon-button" disabled={busy} onClick={() => handleRemoveCriterion(selectedExam.id, c.id)} title="Xóa"><Trash2 size={14} /></button>
                        </span>
                      ) : null}
                    </div>
                  ))}
                </div>
              ) : (
                <p className="club-muted-text">Chưa có tiêu chí. Thêm tiêu chí để bắt đầu chấm điểm chi tiết.</p>
              )}
            </div>

            {/* Scoring grid */}
            {selectedExam.candidates.length > 0 ? (
              selectedExam.criteria.length > 0 ? (
                <div className="club-exam-scoring">
                  <h4>Bảng chấm điểm</h4>
                  <div className="club-scoring-table-wrap">
                    <table className="club-scoring-table">
                      <thead>
                        <tr>
                          <th>Thí sinh</th>
                          {selectedExam.criteria.map((c) => <th key={c.id} title={`Tối đa ${c.maxScore}`}>{c.name}</th>)}
                          <th>Tổng</th>
                          <th>Kết quả</th>
                        </tr>
                      </thead>
                      <tbody>
                        {selectedExam.candidates.map((cand) => {
                          const passed = selectedExam.passThreshold != null && cand.totalScore >= selectedExam.passThreshold;
                          return (
                            <tr key={cand.id}>
                              <td className="club-scoring-name">
                                <strong>{cand.displayName ?? "—"}</strong>
                                <span>{cand.currentBelt ? (BELT_RANK_LABELS[cand.currentBelt] ?? cand.currentBelt) : "—"} → {BELT_RANK_LABELS[cand.targetBelt] ?? cand.targetBelt}</span>
                              </td>
                              {selectedExam.criteria.map((c) => {
                                const score = cand.scores.find((s) => s.criterionId === c.id);
                                return (
                                  <td key={c.id}>
                                    <input
                                      className="club-score-input"
                                      type="number"
                                      min={0}
                                      max={c.maxScore}
                                      step="0.5"
                                      disabled={busy || selectedExam.status === "CANCELLED"}
                                      defaultValue={score ? score.score : ""}
                                      onBlur={(e) => { if (e.target.value !== String(score?.score ?? "")) handleScore(selectedExam.id, cand.id, c.id, e.target.value); }}
                                    />
                                  </td>
                                );
                              })}
                              <td className={cx("club-scoring-total", selectedExam.passThreshold != null && (passed ? "pass" : "fail"))}>
                                {cand.totalScore}/{cand.maxTotalScore}
                              </td>
                              <td>
                                <ResultControl
                                  candidate={cand}
                                  editable={editable}
                                  busy={busy}
                                  onUpdateResult={(result) => handleUpdateResult(selectedExam.id, cand.id, result)}
                                  onRemove={() => handleRemoveCandidate(selectedExam.id, cand.id)}
                                />
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                </div>
              ) : (
                <div className="club-exam-candidates">
                  <h4>Danh sách thí sinh</h4>
                  {selectedExam.candidates.map((cand) => (
                    <div key={cand.id} className="club-exam-candidate-row">
                      <div className="club-person-cell">
                        <div className="club-avatar">{(cand.displayName ?? "?").charAt(0).toUpperCase()}</div>
                        <div>
                          <strong>{cand.displayName ?? "—"}</strong>
                          <span>{cand.currentBelt ? (BELT_RANK_LABELS[cand.currentBelt] ?? cand.currentBelt) : "—"} → <b>{BELT_RANK_LABELS[cand.targetBelt] ?? cand.targetBelt}</b></span>
                        </div>
                      </div>
                      <ResultControl
                        candidate={cand}
                        editable={editable}
                        busy={busy}
                        onUpdateResult={(result) => handleUpdateResult(selectedExam.id, cand.id, result)}
                        onRemove={() => handleRemoveCandidate(selectedExam.id, cand.id)}
                      />
                    </div>
                  ))}
                </div>
              )
            ) : null}

            {editable && eligibleRoster.length > 0 ? (
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
                        <select value={targetBelts[r.athleteId] ?? ""} onChange={(e) => setTargetBelts((prev) => ({ ...prev, [r.athleteId]: e.target.value }))}>
                          <option value="">-- Chọn đai mục tiêu --</option>
                          {BELT_RANKS.map((rank) => <option key={rank.value} value={rank.value}>{rank.label}</option>)}
                        </select>
                        <button className="club-secondary-button" disabled={busy || !targetBelts[r.athleteId]} onClick={() => handleAddCandidate(r.athleteId)}>Đăng ký</button>
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

function ResultControl({ candidate, editable, busy, onUpdateResult, onRemove }: {
  candidate: BeltExamCandidateResponse;
  editable: boolean;
  busy: boolean;
  onUpdateResult: (result: string) => Promise<void>;
  onRemove: () => Promise<void>;
}) {
  if (!editable) {
    return (
      <span className={cx("club-exam-result-chip", candidate.result.toLowerCase())}>
        {EXAM_RESULT_LABELS[candidate.result]}{candidate.beltApplied ? " ✓" : ""}
      </span>
    );
  }
  return (
    <div className="club-exam-candidate-actions">
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
      <button className="club-icon-button" disabled={busy} onClick={onRemove} title="Xóa thí sinh"><Trash2 size={16} /></button>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: number }) {
  return <div className="club-metric"><strong>{value}</strong><span>{label}</span></div>;
}
