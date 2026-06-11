import { motion } from "framer-motion";
import { Award, CheckCircle2, CircleDollarSign, Mail, Search, UserPlus, X, XCircle } from "lucide-react";
import { type Dispatch, type ReactNode, type SetStateAction, useCallback, useEffect, useMemo, useState } from "react";
import type {
  AccountRequestResponse,
  AthleteResponse,
  AttendanceRecordResponse,
  AttendanceSessionResponse,
  AuthUserResponse,
  ClubFeeItemResponse,
  ClubFeeOverviewResponse,
  ClubMemberResponse,
  ClubRosterResponse,
  MemberFeeAssignmentResponse,
  MemberTuitionOverrideResponse,
  PersonResponse
} from "../../../types";
import { apiDelete, apiGet, apiPatch, apiPost } from "../../../apiClient";
import { cx } from "../../../utils";
import { MEMBER_ROLES, MEMBER_STATUSES, PAYMENT_STATUSES } from "../clubConstants";
import { attendanceLabel, attendancePercent, errorMessage, formatDate, genderLabel, initials, paymentLabel, roleLabel, statusLabel, today } from "../clubUtils";
import { MemberActionsMenu } from "./MemberActionsMenu";

type MemberDetailTab = "profile" | "finance" | "attendance" | "competition";

interface MembersTabProps {
  clubId: string;
  user: AuthUserResponse;
  isClubAdmin: boolean;
  accountRequests: AccountRequestResponse[];
  members: ClubMemberResponse[];
  filteredMembers: ClubMemberResponse[];
  sessions: AttendanceSessionResponse[];
  roster: ClubRosterResponse[];
  athletes: AthleteResponse[];
  busy: boolean;
  error: string | null;
  memberSearch: string;
  memberRoleFilter: string;
  memberStatusFilter: string;
  memberTuitionFilter: string;
  memberStudentFilter: string;
  memberInsights: {
    activeStudents: number;
    tuitionPending: number;
    paidTuition: number;
    hiddenAttendance: number;
  };
  setBusy: (value: boolean) => void;
  setError: (value: string | null) => void;
  setDrawer: (drawer: "member") => void;
  setMemberSearch: (value: string) => void;
  setMemberRoleFilter: (value: string) => void;
  setMemberStatusFilter: (value: string) => void;
  setMemberTuitionFilter: (value: string) => void;
  setMemberStudentFilter: (value: string) => void;
  mergeMember: (member: ClubMemberResponse) => void;
  removeMember: (memberId: string) => void;
  setRoster: Dispatch<SetStateAction<ClubRosterResponse[]>>;
  setAthletes: Dispatch<SetStateAction<AthleteResponse[]>>;
  onDecideAccountRequest: (requestId: string, status: "APPROVED" | "REJECTED", decisionNote?: string) => Promise<void>;
  onOpenFinance: () => void;
}

export function MembersTab({
  clubId,
  user,
  isClubAdmin,
  accountRequests,
  members,
  filteredMembers,
  sessions,
  roster,
  athletes,
  busy,
  error,
  memberSearch,
  memberRoleFilter,
  memberStatusFilter,
  memberTuitionFilter,
  memberStudentFilter,
  memberInsights,
  setBusy,
  setError,
  setDrawer,
  setMemberSearch,
  setMemberRoleFilter,
  setMemberStatusFilter,
  setMemberTuitionFilter,
  setMemberStudentFilter,
  mergeMember,
  removeMember,
  setRoster,
  setAthletes,
  onDecideAccountRequest,
  onOpenFinance
}: MembersTabProps) {
  const [selectedMemberId, setSelectedMemberId] = useState<string | null>(null);
  const [selectedMemberIds, setSelectedMemberIds] = useState<string[]>([]);
  const [showAccountRequests, setShowAccountRequests] = useState(false);
  const [requestFilter, setRequestFilter] = useState<"ALL" | "PENDING" | "APPROVED" | "REJECTED">("PENDING");
  const [decisionDraft, setDecisionDraft] = useState<{ request: AccountRequestResponse; status: "APPROVED" | "REJECTED"; note: string } | null>(null);
  const [feeOverview, setFeeOverview] = useState<ClubFeeOverviewResponse | null>(null);
  const [feeLoading, setFeeLoading] = useState(false);

  const selectedMember = members.find((member) => member.id === selectedMemberId) || null;
  const pendingRequests = accountRequests.filter((request) => request.status === "PENDING").length;

  const loadFeeOverview = useCallback(async () => {
    setFeeLoading(true);
    try {
      const overview = await apiGet<ClubFeeOverviewResponse>(`/api/organizations/${clubId}/finance/overview`);
      setFeeOverview(overview);
    } catch (err) {
      console.warn("Finance API not available:", err);
      setFeeOverview(null);
    } finally {
      setFeeLoading(false);
    }
  }, [clubId]);

  useEffect(() => {
    loadFeeOverview();
  }, [loadFeeOverview]);

  useEffect(() => {
    setSelectedMemberIds((current) => current.filter((memberId) => members.some((member) => member.id === memberId)));
  }, [members]);

  const defaultTuition = feeOverview?.feeItems.find((item) => item.feeKind === "MONTHLY_TUITION_DEFAULT");
  const tuitionOverrideByMember = useMemo(() => {
    return Object.fromEntries((feeOverview?.tuitionOverrides ?? []).map((row) => [row.memberId, row]));
  }, [feeOverview?.tuitionOverrides]);
  const assignmentsByMember = useMemo(() => {
    return (feeOverview?.assignments ?? []).reduce<Record<string, NonNullable<ClubFeeOverviewResponse["assignments"]>>>((grouped, assignment) => {
      grouped[assignment.memberId] = [...(grouped[assignment.memberId] ?? []), assignment];
      return grouped;
    }, {});
  }, [feeOverview?.assignments]);
  const selectedVisibleIds = filteredMembers.map((member) => member.id);
  const allVisibleSelected = selectedVisibleIds.length > 0 && selectedVisibleIds.every((id) => selectedMemberIds.includes(id));

  async function runMemberAction(action: () => Promise<void>) {
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

  async function patchMember(member: ClubMemberResponse, body: Partial<ClubMemberResponse>) {
    const updated = await apiPatch<ClubMemberResponse>(`/api/organizations/${clubId}/members/${member.id}`, body);
    mergeMember(updated);
  }

  async function patchPerson(member: ClubMemberResponse, body: Partial<PersonResponse>) {
    if (!member.personId) throw new Error("Thành viên này chưa có hồ sơ cá nhân.");
    const person = await apiPatch<PersonResponse>(`/api/persons/${member.personId}`, body);
    mergeMember({
      ...member,
      personName: person.displayName,
      gender: person.gender,
      phone: person.phone,
      email: person.email,
      currentAddress: person.currentAddress
    });
  }

  if (selectedMember) {
    const financialSummary = memberFinancialSummary(selectedMember, defaultTuition, tuitionOverrideByMember[selectedMember.id], assignmentsByMember[selectedMember.id] ?? []);
    return (
      <MemberDetailScreen
        member={selectedMember}
        user={user}
        isClubAdmin={isClubAdmin}
        canGoPrevious={filteredMembers.findIndex((member) => member.id === selectedMember.id) > 0}
        canGoNext={filteredMembers.findIndex((member) => member.id === selectedMember.id) < filteredMembers.length - 1}
        sessions={sessions}
        roster={roster}
        athletes={athletes}
        financialSummary={financialSummary}
        busy={busy}
        error={error}
        onBack={() => setSelectedMemberId(null)}
        onPrevious={() => {
          const index = filteredMembers.findIndex((member) => member.id === selectedMember.id);
          if (index > 0) setSelectedMemberId(filteredMembers[index - 1].id);
        }}
        onNext={() => {
          const index = filteredMembers.findIndex((member) => member.id === selectedMember.id);
          if (index >= 0 && index < filteredMembers.length - 1) setSelectedMemberId(filteredMembers[index + 1].id);
        }}
        onPatchMember={(member, body) => runMemberAction(() => patchMember(member, body))}
        onPatchPerson={(member, body) => runMemberAction(() => patchPerson(member, body))}
        onOpenFinance={onOpenFinance}
        onDeleteMember={(member) => runMemberAction(async () => {
          await apiDelete(`/api/organizations/${clubId}/members/${member.id}`);
          removeMember(member.id);
          setSelectedMemberId(null);
        })}
        onCreateRoster={(member) => runMemberAction(async () => {
          if (!member.personId) throw new Error("Thành viên này chưa có hồ sơ cá nhân để tạo VĐV.");
          const athlete = athletes.find((item) => item.personId === member.personId)
            || await apiPost<AthleteResponse>("/api/athletes", { personId: member.personId, primaryOrganizationId: clubId, status: "ACTIVE" });
          const rosterItem = await apiPost<ClubRosterResponse>(`/api/organizations/${clubId}/roster`, { athleteId: athlete.id, status: "ACTIVE", joinedAt: today() });
          setRoster((current) => [rosterItem, ...current]);
          setAthletes((current) => current.some((item) => item.id === athlete.id) ? current : [...current, athlete]);
        })}
      />
    );
  }

  return (
    <div className="club-tab-content">
      <div className="club-section-head">
        <div>
          <span className="club-ops-kicker">Quản lý con người</span>
          <h2>Thành viên CLB</h2>
        </div>
        <div className="club-command-actions">
          <button className="club-secondary-button" onClick={() => setShowAccountRequests(true)}>
            <Mail size={18} /> Yêu cầu tài khoản {pendingRequests ? `(${pendingRequests})` : ""}
          </button>
          <button className="club-primary-button" onClick={() => setDrawer("member")}><UserPlus size={18} /> Thêm thành viên</button>
        </div>
      </div>

      <div className="club-member-insights compact">
        <Metric label="Học viên active" value={memberInsights.activeStudents} />
        <Metric label="Cần thu học phí" value={memberInsights.tuitionPending} />
        <Metric label="Đã nộp/miễn" value={memberInsights.paidTuition} />
        <Metric label="Ẩn chuyên cần" value={memberInsights.hiddenAttendance} />
      </div>

      <div className="club-toolbar compact">
        <label className="club-search">
          <Search size={18} />
          <input value={memberSearch} onChange={(event) => setMemberSearch(event.target.value)} placeholder="Tìm thành viên" />
        </label>
        <select value={memberRoleFilter} onChange={(event) => setMemberRoleFilter(event.target.value)}>
          <option value="ALL">Tất cả vai trò CLB</option>
          {MEMBER_ROLES.map((role) => <option key={role} value={role}>{roleLabel(role)}</option>)}
        </select>
        <select value={memberStatusFilter} onChange={(event) => setMemberStatusFilter(event.target.value)}>
          <option value="ALL">Tất cả trạng thái</option>
          {MEMBER_STATUSES.map((status) => <option key={status} value={status}>{statusLabel(status)}</option>)}
        </select>
        <select value={memberTuitionFilter} onChange={(event) => setMemberTuitionFilter(event.target.value)}>
          <option value="ALL">Tất cả học phí</option>
          {PAYMENT_STATUSES.map((status) => <option key={status} value={status}>{paymentLabel(status)}</option>)}
        </select>
        <select value={memberStudentFilter} onChange={(event) => setMemberStudentFilter(event.target.value)}>
          <option value="ALL">Tất cả hồ sơ</option>
          <option value="STUDENT">Chỉ học viên</option>
          <option value="NON_STUDENT">Không phải học viên</option>
        </select>
      </div>

      {selectedMemberIds.length > 0 ? (
        <div className="club-bulk-bar">
          <strong>{selectedMemberIds.length} thành viên đã chọn</strong>
          <span>Việc gán học phí ghi đè hoặc khoản thu được xử lý ở tab Tài chính.</span>
          <button className="club-secondary-button" disabled={busy} onClick={onOpenFinance}>
            <CircleDollarSign size={17} /> Mở Tài chính
          </button>
          <button className="club-text-button" onClick={() => setSelectedMemberIds([])}>Bỏ chọn</button>
        </div>
      ) : null}

      {error ? <p className="club-form-error">{error}</p> : null}

      {filteredMembers.length === 0 ? <EmptyState title="Chưa có thành viên phù hợp" text="Thêm thành viên mới hoặc thay đổi bộ lọc để xem dữ liệu." /> : (
        <div className="club-data-table member-list">
          <label className="club-table-select-all">
            <input
              type="checkbox"
              checked={allVisibleSelected}
              onChange={(event) => {
                if (event.target.checked) setSelectedMemberIds((current) => Array.from(new Set([...current, ...selectedVisibleIds])));
                else setSelectedMemberIds((current) => current.filter((id) => !selectedVisibleIds.includes(id)));
              }}
            />
            <span>Chọn tất cả trong danh sách hiện tại</span>
          </label>
          {filteredMembers.map((member) => {
            const attendance = memberAttendance(member, sessions);
            const hasRoster = roster.some((item) => item.personId === member.personId);
            const financialSummary = memberFinancialSummary(member, defaultTuition, tuitionOverrideByMember[member.id], assignmentsByMember[member.id] ?? []);
            const selected = selectedMemberIds.includes(member.id);
            return (
              <article className={cx("club-table-row club-member-row-record", selected && "selected")} key={member.id}>
                <label className="club-row-check" aria-label={`Chọn ${member.personName || member.userName || "thành viên"}`}>
                  <input
                    type="checkbox"
                    checked={selected}
                    onChange={(event) => {
                      setSelectedMemberIds((current) => event.target.checked
                        ? [...current, member.id]
                        : current.filter((id) => id !== member.id));
                    }}
                  />
                </label>
                <button className="club-member-row-main" onClick={() => setSelectedMemberId(member.id)}>
                  <div className="club-person-cell">
                    <div className="club-avatar">{initials(member.personName || member.userName || "TV")}</div>
                    <div>
                      <strong>{member.personName || member.userName || "Chưa có tên"}</strong>
                      <span>{genderLabel(member.gender)} - {member.phone || "Chưa có SĐT"} - Gia nhập {formatDate(member.joinedAt)}</span>
                      <small>{member.currentAddress || "Chưa cập nhật địa chỉ"}</small>
                    </div>
                  </div>
                </button>
                <div className="club-member-row-meta">
                  <Badge>{roleLabel(member.role)}</Badge>
                  <Badge>{statusLabel(member.status)}</Badge>
                  {member.student ? <Badge tone="warm">Sinh viên</Badge> : null}
                  {hasRoster ? <Badge tone="green">Roster</Badge> : null}
                  <Badge tone={financialSummary.override ? "green" : undefined}>{financialSummary.tuitionName}</Badge>
                </div>
                <div className="club-member-row-score">
                  <strong>{attendance.rate}%</strong>
                  <span>Chuyên cần</span>
                </div>
                <div className="club-member-row-score">
                  <strong>{formatMoney(financialSummary.currentTuition)}</strong>
                  <span>Học phí</span>
                </div>
                <div className="club-member-row-actions">
                  <button disabled={feeLoading} onClick={onOpenFinance}>
                    <CircleDollarSign size={16} /> Tài chính
                  </button>
                </div>
              </article>
            );
          })}
        </div>
      )}

      <AccountRequestsDrawer
        open={showAccountRequests}
        busy={busy}
        requestFilter={requestFilter}
        requests={accountRequests}
        onClose={() => setShowAccountRequests(false)}
        onFilter={setRequestFilter}
        onDecision={setDecisionDraft}
      />

      {decisionDraft ? (
        <DecisionModal
          draft={decisionDraft}
          busy={busy}
          onClose={() => setDecisionDraft(null)}
          onSubmit={async () => {
            await onDecideAccountRequest(decisionDraft.request.id, decisionDraft.status, decisionDraft.note);
            setDecisionDraft(null);
          }}
          onNote={(note) => setDecisionDraft({ ...decisionDraft, note })}
        />
      ) : null}

    </div>
  );
}

function MemberDetailScreen({
  member,
  user,
  isClubAdmin,
  canGoPrevious,
  canGoNext,
  sessions,
  roster,
  athletes,
  financialSummary,
  busy,
  error,
  onBack,
  onPrevious,
  onNext,
  onPatchMember,
  onPatchPerson,
  onOpenFinance,
  onDeleteMember,
  onCreateRoster
}: {
  member: ClubMemberResponse;
  user: AuthUserResponse;
  isClubAdmin: boolean;
  canGoPrevious: boolean;
  canGoNext: boolean;
  sessions: AttendanceSessionResponse[];
  roster: ClubRosterResponse[];
  athletes: AthleteResponse[];
  financialSummary: ReturnType<typeof memberFinancialSummary>;
  busy: boolean;
  error: string | null;
  onBack: () => void;
  onPrevious: () => void;
  onNext: () => void;
  onPatchMember: (member: ClubMemberResponse, body: Partial<ClubMemberResponse>) => Promise<void>;
  onPatchPerson: (member: ClubMemberResponse, body: Partial<PersonResponse>) => Promise<void>;
  onOpenFinance: () => void;
  onDeleteMember: (member: ClubMemberResponse) => Promise<void>;
  onCreateRoster: (member: ClubMemberResponse) => Promise<void>;
}) {
  const [attendanceMonth, setAttendanceMonth] = useState(today().slice(0, 7));
  const [activeTab, setActiveTab] = useState<MemberDetailTab>("profile");
  const attendance = useMemo(() => member ? memberAttendance(member, sessions) : emptyAttendance(), [member, sessions]);
  const attendanceByDate = useMemo(() => {
    return Object.fromEntries(attendance.records.map((record) => [String(record.scheduledAt || "").slice(0, 10), record]));
  }, [attendance.records]);
  const rosterItem = member.personId ? roster.find((item) => item.personId === member.personId) : undefined;
  const athlete = member.personId ? athletes.find((item) => item.personId === member.personId) : undefined;
  const tabs: Array<{ id: MemberDetailTab; label: string }> = [
    { id: "profile", label: "Hồ sơ" },
    { id: "finance", label: "Tài chính" },
    { id: "attendance", label: "Chuyên cần" },
    { id: "competition", label: "Thi đấu" }
  ];

  return (
    <motion.div className="club-member-detail-page focused" initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }}>
      <div className="club-member-detail-page-head">
        <button className="club-back-link" onClick={onBack}>Quay lại danh sách</button>
        <div className="club-member-title-block">
          <div className="club-avatar large">{initials(member.personName || member.userName || "TV")}</div>
          <div>
            <span className="club-ops-kicker">Hồ sơ thành viên</span>
            <h2>{member.personName || member.userName || "Chưa có tên"}</h2>
            <p>{roleLabel(member.role)} - {statusLabel(member.status)}</p>
          </div>
        </div>
        <div className="club-member-preview-actions">
          <button onClick={onPrevious} disabled={!canGoPrevious}>Trước</button>
          <button onClick={onNext} disabled={!canGoNext}>Sau</button>
          <MemberActionsMenu
            member={member}
            userIsAdmin={isClubAdmin}
            busy={busy}
            onEdit={() => {}}
            onToggleInactive={(makeInactive) =>
              onPatchMember(member, { status: makeInactive ? "INACTIVE" : "ACTIVE" })
            }
            onDelete={() => onDeleteMember(member)}
          />
        </div>
      </div>

      <div className="club-member-alerts">
        {attendance.rate > 0 && attendance.rate < 70 ? <Badge tone="danger">Chuyên cần thấp</Badge> : null}
        {member.tuitionStatus === "OVERDUE" || member.tuitionStatus === "PARTIAL" || member.tuitionStatus === "PENDING" ? <Badge tone="danger">Cần xử lý học phí</Badge> : null}
        {!member.phone || !member.currentAddress ? <Badge tone="warm">Thiếu thông tin hồ sơ</Badge> : null}
        {rosterItem ? <Badge tone="green">Đã có trong roster</Badge> : null}
      </div>

      {error ? <p className="club-form-error">{error}</p> : null}

      <div className="club-member-detail-tabs">
        {tabs.map((tab) => (
          <button key={tab.id} className={cx(activeTab === tab.id && "active")} onClick={() => setActiveTab(tab.id)}>
            {tab.label}
          </button>
        ))}
      </div>

      <section className="club-member-detail-section">
        {activeTab === "profile" ? (
          <>
            <InfoGrid rows={[
              ["Giới tính", genderLabel(member.gender)],
              ["SĐT", member.phone || "Chưa cập nhật"],
              ["Email", member.email || "Chưa cập nhật"],
              ["Địa chỉ", member.currentAddress || "Chưa cập nhật"],
              ["Ngày gia nhập", formatDate(member.joinedAt)],
              ["CLB", member.organizationName],
              ["Ghi chú", member.memberNote || "Chưa có ghi chú"]
            ]} />
            <div className="club-form-grid">
              <Field label="Vai trò CLB">
                <select value={member.role} disabled={busy} onChange={(event) => onPatchMember(member, { role: event.target.value })}>
                  {MEMBER_ROLES.map((role) => <option key={role} value={role}>{roleLabel(role)}</option>)}
                </select>
              </Field>
              <Field label="Trạng thái">
                <select value={member.status} disabled={busy} onChange={(event) => onPatchMember(member, { status: event.target.value })}>
                  {MEMBER_STATUSES.map((status) => <option key={status} value={status}>{statusLabel(status)}</option>)}
                </select>
              </Field>
            </div>
            <div className="club-member-switches">
              <button disabled={busy} className={cx(member.student && "active")} onClick={() => onPatchMember(member, { student: !member.student })}>Là sinh viên</button>
              <button disabled={busy} className={cx(member.attendanceViewEnabled && "active")} onClick={() => onPatchMember(member, { attendanceViewEnabled: !member.attendanceViewEnabled })}>Cho xem chuyên cần</button>
            </div>
            <div className="club-form-grid">
              <TextPatch label="Địa chỉ hiện tại" value={member.currentAddress || ""} disabled={busy} onSave={(value) => onPatchPerson(member, { currentAddress: value })} />
              <TextPatch label="Ghi chú quản lý" value={member.memberNote || ""} disabled={busy} onSave={(value) => onPatchMember(member, { memberNote: value })} />
            </div>
          </>
        ) : null}

        {activeTab === "finance" ? (
          <>
            <div className="club-billing-hero">
              <div>
                <span>Học phí hiện tại</span>
                <strong>{formatMoney(financialSummary.currentTuition)}</strong>
              </div>
              <p>{financialSummary.override ? `Đang dùng học phí ghi đè ${financialSummary.tuitionName}.` : `Đang dùng ${financialSummary.tuitionName}.`}</p>
            </div>
            <div className="club-billing-secondary">
              <div>
                <span>Còn phải thu</span>
                <strong>{formatMoney(financialSummary.outstanding)}</strong>
              </div>
              <div>
                <span>Đã nộp</span>
                <strong>{formatMoney(financialSummary.paid)}</strong>
              </div>
            </div>
            {financialSummary.assignments.length === 0 ? (
              <EmptyState title="Chưa có khoản phải thu" text="Các khoản thu một lần sau khi gán ở tab Tài chính sẽ hiển thị trong tóm tắt này." />
            ) : (
              <div className="club-fee-group-list">
                {financialSummary.assignments.slice(0, 5).map((assignment) => (
                  <article className="club-fee-role-card" key={assignment.id}>
                    <div>
                      <b>{assignment.feeItemName}</b>
                      <span>{paymentLabel(assignment.status)} - còn {formatMoney(Number(assignment.amountDue || 0) - Number(assignment.paidAmount || 0))}</span>
                      {assignment.note ? <small>{assignment.note}</small> : null}
                    </div>
                    <Badge tone={assignment.status === "PAID" ? "green" : "warm"}>{formatMoney(assignment.amountDue)}</Badge>
                  </article>
                ))}
              </div>
            )}
            <button className="club-primary-button" disabled={busy} onClick={onOpenFinance}><CircleDollarSign size={18} /> Mở tab Tài chính</button>
          </>
        ) : null}

        {activeTab === "attendance" ? (
          <>
            <div className="club-member-attendance-summary">
              <Metric label="Tỷ lệ" value={`${attendance.rate}%`} />
              <Metric label="Có mặt/muộn" value={attendance.presentOrLate} />
              <Metric label="Vắng/có phép" value={attendance.absentOrExcused} />
            </div>
            <MemberAttendanceCalendar month={attendanceMonth} recordsByDate={attendanceByDate} onMonthChange={setAttendanceMonth} />
            <div className="club-member-history">
              {attendance.records.length === 0 ? <EmptyState title="Chưa có dữ liệu chuyên cần" text="Khi CLB điểm danh buổi tập, lịch sử của thành viên sẽ hiển thị tại đây." /> : attendance.records.slice(0, 12).map((record) => (
                <div key={record.id}>
                  <strong>{record.sessionName}</strong>
                  <span>{formatDate(record.scheduledAt)} - {attendanceLabel(record.status)}</span>
                </div>
              ))}
            </div>
          </>
        ) : null}

        {activeTab === "competition" ? (
          <>
            <InfoGrid rows={[
              ["Hồ sơ VĐV", athlete ? "Đã có" : "Chưa có"],
              ["Roster CLB", rosterItem ? statusLabel(rosterItem.status) : "Chưa vào roster"],
              ["Mã VĐV", athlete?.externalCode || "Chưa cập nhật"],
              ["Đai", athlete?.belt || "Chưa cập nhật"],
              ["Cân nặng", athlete?.weightKg ? `${athlete.weightKg} kg` : "Chưa cập nhật"]
            ]} />
            {!rosterItem ? (
              <button className="club-primary-button" disabled={busy || !member.personId} onClick={() => onCreateRoster(member)}><Award size={18} /> Tạo hồ sơ VĐV và thêm roster</button>
            ) : null}
          </>
        ) : null}
      </section>

      <div className="club-member-detail-footer">
        <button className="club-text-danger" disabled={busy} onClick={() => onDeleteMember(member)}>Xóa thành viên khỏi CLB</button>
      </div>
    </motion.div>
  );
}

function AccountRequestsDrawer({
  open,
  busy,
  requestFilter,
  requests,
  onClose,
  onFilter,
  onDecision
}: {
  open: boolean;
  busy: boolean;
  requestFilter: "ALL" | "PENDING" | "APPROVED" | "REJECTED";
  requests: AccountRequestResponse[];
  onClose: () => void;
  onFilter: (status: "ALL" | "PENDING" | "APPROVED" | "REJECTED") => void;
  onDecision: (draft: { request: AccountRequestResponse; status: "APPROVED" | "REJECTED"; note: string }) => void;
}) {
  if (!open) return null;
  const filtered = requests.filter((request) => requestFilter === "ALL" || request.status === requestFilter);
  return (
    <div className="club-drawer-layer">
      <button className="club-drawer-scrim" aria-label="Đóng" onClick={onClose} />
      <motion.aside className="club-drawer club-wide-drawer" initial={{ opacity: 0, x: 24 }} animate={{ opacity: 1, x: 0 }}>
        <div className="club-drawer-head">
          <div>
            <span className="club-ops-kicker">Cấp tài khoản</span>
            <h2>Yêu cầu tài khoản CLB</h2>
          </div>
          <button onClick={onClose}><X size={18} /></button>
        </div>
        <div className="club-request-filter">
          {(["PENDING", "APPROVED", "REJECTED", "ALL"] as const).map((status) => (
            <button key={status} className={cx(requestFilter === status && "active")} onClick={() => onFilter(status)}>
              {requestStatusLabel(status)}
            </button>
          ))}
        </div>
        {filtered.length === 0 ? (
          <EmptyState title="Không có yêu cầu phù hợp" text="Yêu cầu sẽ xuất hiện khi user nhập mã CLB ở màn đăng ký và gửi thông tin." />
        ) : (
          <div className="club-request-list">
            {filtered.map((request) => (
              <article className={cx("club-request-row", request.status.toLowerCase())} key={request.id}>
                <div className="club-avatar">{initials(request.displayName)}</div>
                <div>
                  <strong>{request.displayName}</strong>
                  <span><Mail size={14} /> {request.email} - {request.phone}</span>
                  <small>{request.currentAddress || "Chưa có địa chỉ"}{request.decisionNote ? ` - ${request.decisionNote}` : ""}</small>
                </div>
                <Badge tone={request.status === "APPROVED" ? "green" : request.status === "REJECTED" ? "danger" : "warm"}>{requestStatusLabel(request.status)}</Badge>
                {request.status === "PENDING" ? (
                  <div className="club-request-actions">
                    <button disabled={busy} onClick={() => onDecision({ request, status: "APPROVED", note: "" })}><CheckCircle2 size={16} /> Duyệt</button>
                    <button disabled={busy} className="danger" onClick={() => onDecision({ request, status: "REJECTED", note: "" })}><XCircle size={16} /> Từ chối</button>
                  </div>
                ) : null}
              </article>
            ))}
          </div>
        )}
      </motion.aside>
    </div>
  );
}

function DecisionModal({
  draft,
  busy,
  onClose,
  onSubmit,
  onNote
}: {
  draft: { request: AccountRequestResponse; status: "APPROVED" | "REJECTED"; note: string };
  busy: boolean;
  onClose: () => void;
  onSubmit: () => Promise<void>;
  onNote: (note: string) => void;
}) {
  return (
    <div className="club-request-modal-layer">
      <button className="club-request-modal-scrim" aria-label="Đóng" onClick={onClose} />
      <motion.form
        className="club-request-modal"
        initial={{ opacity: 0, y: 18, scale: 0.98 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        onSubmit={async (event) => {
          event.preventDefault();
          await onSubmit();
        }}
      >
        <button type="button" className="club-request-modal-close" onClick={onClose}><X size={18} /></button>
        <span className="club-ops-kicker">{draft.status === "APPROVED" ? "Duyệt tài khoản" : "Từ chối yêu cầu"}</span>
        <h3>{draft.request.displayName}</h3>
        <p>{draft.status === "APPROVED" ? "Hệ thống sẽ tạo tài khoản, mật khẩu tạm thời và gửi email cho user." : "Nhập lý do từ chối để user nhận được phản hồi rõ ràng qua email."}</p>
        {draft.status === "REJECTED" ? (
          <label className="club-field">
            <span>Lý do từ chối *</span>
            <textarea value={draft.note} onChange={(event) => onNote(event.target.value)} rows={4} required />
          </label>
        ) : null}
        <button className={cx("club-primary-button", draft.status === "REJECTED" && "danger")} disabled={busy || (draft.status === "REJECTED" && !draft.note.trim())}>
          {draft.status === "APPROVED" ? "Duyệt và tạo tài khoản" : "Gửi từ chối"}
        </button>
      </motion.form>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return <div className="club-metric"><strong>{value}</strong><span>{label}</span></div>;
}

function Badge({ children, tone }: { children: ReactNode; tone?: "warm" | "green" | "danger" }) {
  return <span className={cx("club-member-badge", tone)}>{children}</span>;
}

function requestStatusLabel(status: "ALL" | "PENDING" | "APPROVED" | "REJECTED") {
  if (status === "PENDING") return "Pending";
  if (status === "APPROVED") return "Approved";
  if (status === "REJECTED") return "Rejected";
  return "Tất cả";
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return <label className="club-field"><span>{label}</span>{children}</label>;
}

function TextPatch({ label, value, disabled, onSave }: { label: string; value: string; disabled: boolean; onSave: (value: string) => Promise<void> }) {
  const [nextValue, setNextValue] = useState(value);
  useEffect(() => {
    setNextValue(value);
  }, [value]);
  return (
    <label className="club-field">
      <span>{label}</span>
      <textarea value={nextValue} onChange={(event) => setNextValue(event.target.value)} rows={3} />
      <button type="button" className="club-secondary-button" disabled={disabled || nextValue === value} onClick={() => onSave(nextValue)}>Lưu</button>
    </label>
  );
}

function InfoGrid({ rows }: { rows: Array<[string, string]> }) {
  return (
    <div className="club-member-info-grid">
      {rows.map(([label, value]) => (
        <div key={label}>
          <span>{label}</span>
          <strong>{value}</strong>
        </div>
      ))}
    </div>
  );
}

function MemberAttendanceCalendar({
  month,
  recordsByDate,
  onMonthChange
}: {
  month: string;
  recordsByDate: Record<string, MemberAttendanceRecord>;
  onMonthChange: (month: string) => void;
}) {
  const days = calendarDays(month);
  const [selectedDate, setSelectedDate] = useState("");
  const selectedRecord = selectedDate ? recordsByDate[selectedDate] : undefined;
  useEffect(() => {
    setSelectedDate("");
  }, [month, recordsByDate]);
  return (
    <div className="club-member-calendar-card">
      <div className="club-member-calendar-head">
        <button onClick={() => onMonthChange(shiftMonth(month, -1))}>Tháng trước</button>
        <strong>{monthLabel(month)}</strong>
        <div>
          <button onClick={() => onMonthChange(today().slice(0, 7))}>Tháng hiện tại</button>
          <button onClick={() => onMonthChange(shiftMonth(month, 1))}>Tháng sau</button>
        </div>
      </div>
      <div className="club-member-calendar-legend">
        <span><i className="present" /> Có mặt</span>
        <span><i className="late" /> Muộn</span>
        <span><i className="absent" /> Vắng</span>
        <span><i className="excused" /> Có phép</span>
      </div>
      <div className="club-member-calendar-grid">
        {["T2", "T3", "T4", "T5", "T6", "T7", "CN"].map((day) => <b key={day}>{day}</b>)}
        {days.map((day, index) => {
          const date = day ? formatDateInput(day) : "";
          const record = date ? recordsByDate[date] : undefined;
          return day ? (
            <button
              key={date}
              className={cx("club-member-calendar-day", record?.status.toLowerCase(), selectedDate === date && "selected")}
              disabled={!record}
              title={record ? `${record.sessionName} - ${attendanceLabel(record.status)}` : undefined}
              onClick={() => setSelectedDate(date)}
            >
              <span>{day.getDate()}</span>
              {record ? <em>{attendanceMark(record.status)}</em> : null}
            </button>
          ) : <i key={`blank-${index}`} />;
        })}
      </div>
      {selectedRecord ? (
        <div className="club-member-calendar-detail">
          <span>Ngày đã chọn</span>
          <strong>{formatDate(selectedRecord.scheduledAt)} - {selectedRecord.sessionName}</strong>
          <p>{attendanceLabel(selectedRecord.status)}{selectedRecord.note ? ` - ${selectedRecord.note}` : ""}</p>
        </div>
      ) : (
        <div className="club-member-calendar-detail muted">
          <span>Gợi ý</span>
          <strong>Bấm vào một ngày có dấu để xem buổi tập.</strong>
          <p>Ô trống nghĩa là ngày đó chưa có bản ghi điểm danh cho thành viên này.</p>
        </div>
      )}
    </div>
  );
}

function EmptyState({ title, text }: { title: string; text: string }) {
  return (
    <div className="club-empty-state">
      <div className="club-empty-line" />
      <strong>{title}</strong>
      <p>{text}</p>
    </div>
  );
}

type MemberAttendanceRecord = AttendanceRecordResponse & {
  sessionName: string;
  scheduledAt?: string;
};

function memberAttendance(member: ClubMemberResponse, sessions: AttendanceSessionResponse[]) {
  const records = sessions
    .flatMap((session) => session.records.map((record) => ({ ...record, sessionName: session.name, scheduledAt: session.scheduledAt || session.scheduledDate })))
    .filter((record) => (member.personId && record.personId === member.personId) || (member.personName && record.displayName === member.personName))
    .sort((a, b) => String(b.scheduledAt || "").localeCompare(String(a.scheduledAt || "")));
  const presentOrLate = records.filter((record) => record.status === "PRESENT" || record.status === "LATE").length;
  return {
    records,
    presentOrLate,
    absentOrExcused: records.filter((record) => record.status === "ABSENT" || record.status === "EXCUSED").length,
    rate: attendancePercent(records as AttendanceRecordResponse[])
  };
}

function memberFinancialSummary(
  member: ClubMemberResponse,
  defaultTuition: ClubFeeItemResponse | undefined,
  override: MemberTuitionOverrideResponse | undefined,
  assignments: MemberFeeAssignmentResponse[]
) {
  const currentTuition = Number(override?.amount ?? defaultTuition?.defaultAmount ?? 0);
  const tuitionName = override?.feeItemName || defaultTuition?.name || "Học phí mặc định";
  const paid = assignments.reduce((sum, row) => sum + Number(row.paidAmount || 0), 0);
  const due = assignments.reduce((sum, row) => sum + Number(row.amountDue || 0), 0);
  return {
    memberId: member.id,
    tuitionName,
    currentTuition,
    override: Boolean(override?.feeItemId),
    assignments,
    paid,
    outstanding: Math.max(0, due - paid)
  };
}

function emptyAttendance() {
  return { records: [], presentOrLate: 0, absentOrExcused: 0, rate: 0 };
}

function formatMoney(value?: number) {
  if (!value) return "0đ";
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(value);
}

function calendarDays(month: string) {
  const first = new Date(`${month}-01T00:00:00`);
  const firstIso = first.getDay() === 0 ? 7 : first.getDay();
  const daysInMonth = new Date(first.getFullYear(), first.getMonth() + 1, 0).getDate();
  const blanks = Array.from({ length: firstIso - 1 }, () => null);
  const days = Array.from({ length: daysInMonth }, (_, index) => new Date(first.getFullYear(), first.getMonth(), index + 1));
  return [...blanks, ...days];
}

function formatDateInput(value: Date) {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, "0");
  const day = String(value.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function shiftMonth(value: string, delta: number) {
  const date = new Date(`${value}-01T00:00:00`);
  date.setMonth(date.getMonth() + delta);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
}

function monthLabel(value: string) {
  return new Intl.DateTimeFormat("vi-VN", { month: "long", year: "numeric" }).format(new Date(`${value}-01T00:00:00`));
}

function attendanceMark(status: string) {
  if (status === "PRESENT" || status === "LATE") return "✓";
  if (status === "ABSENT") return "×";
  if (status === "EXCUSED") return "P";
  return "";
}
