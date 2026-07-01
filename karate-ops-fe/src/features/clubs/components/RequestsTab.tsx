import { motion } from "framer-motion";
import { AlertTriangle, CalendarRange, CheckCircle2, Clock, Trophy, XCircle } from "lucide-react";
import { useMemo, useState } from "react";
import type { LeaveRequestResponse, TournamentJoinRequestResponse } from "../../../types";
import { cx } from "../../../utils";
import {
  JOIN_REQUEST_STATUS_LABELS,
  LEAVE_REQUEST_STATUS_LABELS,
  LEAVE_REQUEST_TYPE_LABELS
} from "../clubConstants";
import { formatDate } from "../clubUtils";

type RequestKind = "leave" | "tournament";
type StatusFilter = "ALL" | "PENDING" | "APPROVED" | "REJECTED";

interface RequestsTabProps {
  isClubAdmin: boolean;
  leaveRequests: LeaveRequestResponse[];
  joinRequests: TournamentJoinRequestResponse[];
  busy: boolean;
  error: string | null;
  onDecideLeave: (requestId: string, action: "approve" | "reject", decisionNote?: string) => Promise<void>;
  onDecideJoin: (requestId: string, action: "approve" | "reject", decisionNote?: string) => Promise<void>;
}

export function RequestsTab({
  isClubAdmin,
  leaveRequests,
  joinRequests,
  busy,
  error,
  onDecideLeave,
  onDecideJoin
}: RequestsTabProps) {
  const [kind, setKind] = useState<RequestKind>("leave");
  const [filter, setFilter] = useState<StatusFilter>("PENDING");
  const [decisionModal, setDecisionModal] = useState<{ kind: RequestKind; requestId: string; action: "approve" | "reject"; note: string } | null>(null);

  const pendingLeave = leaveRequests.filter((request) => request.status === "PENDING").length;
  const pendingJoin = joinRequests.filter((request) => request.status === "PENDING").length;

  const visibleLeave = useMemo(
    () => leaveRequests.filter((request) => filter === "ALL" || request.status === filter),
    [leaveRequests, filter]
  );
  const visibleJoin = useMemo(
    () => joinRequests.filter((request) => filter === "ALL" || request.status === filter),
    [joinRequests, filter]
  );

  const counts = (statuses: Array<{ status: string }>) => ({
    ALL: statuses.length,
    PENDING: statuses.filter((row) => row.status === "PENDING").length,
    APPROVED: statuses.filter((row) => row.status === "APPROVED").length,
    REJECTED: statuses.filter((row) => row.status === "REJECTED").length
  });
  const activeCounts = kind === "leave" ? counts(leaveRequests) : counts(joinRequests);

  const handleDecision = async () => {
    if (!decisionModal) return;
    const { kind: modalKind, requestId, action, note } = decisionModal;
    if (modalKind === "leave") {
      await onDecideLeave(requestId, action, note || undefined);
    } else {
      await onDecideJoin(requestId, action, note || undefined);
    }
    setDecisionModal(null);
  };

  return (
    <motion.div className="club-tab-content" initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }}>
      <div className="club-section-head">
        <div>
          <span className="club-ops-kicker">Trung tâm yêu cầu</span>
          <h2>Duyệt yêu cầu từ thành viên</h2>
        </div>
        {isClubAdmin && pendingLeave + pendingJoin > 0 ? (
          <div className="club-pending-badge">{pendingLeave + pendingJoin} yêu cầu chờ duyệt</div>
        ) : null}
      </div>

      {error ? <p className="club-form-error">{error}</p> : null}

      <div className="club-request-kind-switch">
        <button className={cx(kind === "leave" && "active")} onClick={() => setKind("leave")}>
          <CalendarRange size={17} />
          <span>Xin nghỉ / báo muộn</span>
          {pendingLeave > 0 ? <b>{pendingLeave}</b> : null}
        </button>
        <button className={cx(kind === "tournament" && "active")} onClick={() => setKind("tournament")}>
          <Trophy size={17} />
          <span>Xin tham gia giải</span>
          {pendingJoin > 0 ? <b>{pendingJoin}</b> : null}
        </button>
      </div>

      <div className="club-filter-bar">
        {(["PENDING", "APPROVED", "REJECTED", "ALL"] as StatusFilter[]).map((value) => (
          <button key={value} className={cx("club-filter-button", filter === value && "active")} onClick={() => setFilter(value)}>
            {value === "ALL" ? "Tất cả" : LEAVE_REQUEST_STATUS_LABELS[value]} ({activeCounts[value]})
          </button>
        ))}
      </div>

      <div className="club-leave-requests-list">
        {kind === "leave" ? (
          visibleLeave.length === 0 ? (
            <div className="club-empty-state"><p>Không có yêu cầu xin nghỉ nào ở trạng thái này.</p></div>
          ) : (
            visibleLeave.map((request) => (
              <div key={request.id} className={cx("club-leave-request-card", statusTone(request.status))}>
                <div className="leave-request-header">
                  <div className="leave-request-title">
                    <div className="leave-request-icon"><StatusIcon status={request.status} /></div>
                    <div>
                      <h3>{request.memberName || "Thành viên"}</h3>
                      <p>{LEAVE_REQUEST_TYPE_LABELS[request.requestType] || request.requestType}</p>
                    </div>
                  </div>
                  <div className={cx("leave-request-status-badge", request.status.toLowerCase())}>
                    {LEAVE_REQUEST_STATUS_LABELS[request.status] || request.status}
                  </div>
                </div>
                <div className="leave-request-body">
                  <div className="leave-request-detail-row">
                    <span className="leave-request-label">Lý do:</span>
                    <span>{request.reason}</span>
                  </div>
                  {request.sessionName ? (
                    <div className="leave-request-detail-row">
                      <span className="leave-request-label">Buổi tập:</span>
                      <span>{request.sessionName}</span>
                    </div>
                  ) : null}
                  {request.requestType === "LEAVE_LONG_TERM" && request.fromDate && request.toDate ? (
                    <div className="leave-request-detail-row">
                      <span className="leave-request-label">Thời gian nghỉ:</span>
                      <span>{formatDate(request.fromDate)} - {formatDate(request.toDate)}</span>
                    </div>
                  ) : null}
                  <div className="leave-request-detail-row">
                    <span className="leave-request-label">Gửi lúc:</span>
                    <span>{formatDate(request.createdAt)}</span>
                  </div>
                  {request.status !== "PENDING" && request.decisionNote ? (
                    <div className="leave-request-detail-row">
                      <span className="leave-request-label">Ghi chú quyết định:</span>
                      <span>{request.decisionNote}</span>
                    </div>
                  ) : null}
                  {request.status === "PENDING" && request.expiresAt ? (
                    <div className="leave-request-expires-bar">
                      <AlertTriangle size={16} />
                      <span>Hết hạn duyệt: {formatDate(request.expiresAt)} - quá hạn hệ thống tự đánh vắng.</span>
                    </div>
                  ) : null}
                </div>
                {isClubAdmin && request.status === "PENDING" ? (
                  <DecisionButtons
                    busy={busy}
                    onApprove={() => setDecisionModal({ kind: "leave", requestId: request.id, action: "approve", note: "" })}
                    onReject={() => setDecisionModal({ kind: "leave", requestId: request.id, action: "reject", note: "" })}
                  />
                ) : null}
              </div>
            ))
          )
        ) : visibleJoin.length === 0 ? (
          <div className="club-empty-state"><p>Không có yêu cầu tham gia giải nào ở trạng thái này.</p></div>
        ) : (
          visibleJoin.map((request) => (
            <div key={request.id} className={cx("club-leave-request-card", statusTone(request.status))}>
              <div className="leave-request-header">
                <div className="leave-request-title">
                  <div className="leave-request-icon"><StatusIcon status={request.status} /></div>
                  <div>
                    <h3>{request.memberName || "Thành viên"}</h3>
                    <p>{request.tournamentName}</p>
                  </div>
                </div>
                <div className={cx("leave-request-status-badge", request.status.toLowerCase())}>
                  {JOIN_REQUEST_STATUS_LABELS[request.status] || request.status}
                </div>
              </div>
              <div className="leave-request-body">
                {request.tournamentStartsOn ? (
                  <div className="leave-request-detail-row">
                    <span className="leave-request-label">Ngày thi đấu:</span>
                    <span>{formatDate(request.tournamentStartsOn)}</span>
                  </div>
                ) : null}
                {request.note ? (
                  <div className="leave-request-detail-row">
                    <span className="leave-request-label">Lời nhắn:</span>
                    <span>{request.note}</span>
                  </div>
                ) : null}
                <div className="leave-request-detail-row">
                  <span className="leave-request-label">Gửi lúc:</span>
                  <span>{formatDate(request.createdAt)}</span>
                </div>
                {request.status !== "PENDING" && request.decisionNote ? (
                  <div className="leave-request-detail-row">
                    <span className="leave-request-label">Ghi chú quyết định:</span>
                    <span>{request.decisionNote}</span>
                  </div>
                ) : null}
                {request.status === "APPROVED" ? (
                  <div className="leave-request-detail-row">
                    <span className="leave-request-label">Sau duyệt:</span>
                    <span>VĐV được thêm vào roster và CLB được ghi danh vào giải.</span>
                  </div>
                ) : null}
              </div>
              {isClubAdmin && request.status === "PENDING" ? (
                <DecisionButtons
                  busy={busy}
                  onApprove={() => setDecisionModal({ kind: "tournament", requestId: request.id, action: "approve", note: "" })}
                  onReject={() => setDecisionModal({ kind: "tournament", requestId: request.id, action: "reject", note: "" })}
                />
              ) : null}
            </div>
          ))
        )}
      </div>

      {decisionModal ? (
        <div className="club-modal-overlay" onClick={() => setDecisionModal(null)}>
          <div className="club-modal" onClick={(event) => event.stopPropagation()}>
            <h3>{decisionModal.action === "approve" ? "Duyệt yêu cầu" : "Từ chối yêu cầu"}</h3>
            <textarea
              value={decisionModal.note}
              onChange={(event) => setDecisionModal({ ...decisionModal, note: event.target.value })}
              placeholder="Thêm ghi chú gửi thành viên (tuỳ chọn)"
              rows={3}
              className="club-form-textarea"
            />
            <div className="club-modal-actions">
              <button className="club-secondary-button" onClick={() => setDecisionModal(null)} disabled={busy}>Hủy</button>
              <button
                className={cx("club-primary-button", decisionModal.action === "reject" && "danger")}
                onClick={handleDecision}
                disabled={busy}
              >
                {decisionModal.action === "approve" ? "Duyệt" : "Từ chối"}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </motion.div>
  );
}

function DecisionButtons({ busy, onApprove, onReject }: { busy: boolean; onApprove: () => void; onReject: () => void }) {
  return (
    <div className="leave-request-actions">
      <button className="club-secondary-button" onClick={onReject} disabled={busy}>Từ chối</button>
      <button className="club-primary-button" onClick={onApprove} disabled={busy}>Duyệt</button>
    </div>
  );
}

function StatusIcon({ status }: { status: string }) {
  if (status === "APPROVED") return <CheckCircle2 size={20} />;
  if (status === "REJECTED" || status === "EXPIRED_AUTO_ABSENT") return <XCircle size={20} />;
  return <Clock size={20} />;
}

function statusTone(status: string) {
  if (status === "PENDING") return "pending";
  if (status === "APPROVED") return "approved";
  return "rejected";
}
