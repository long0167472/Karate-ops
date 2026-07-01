import { motion } from "framer-motion";
import { AlertTriangle, CheckCircle2, Clock, XCircle } from "lucide-react";
import { useMemo, useState } from "react";
import type { AuthUserResponse, LeaveRequestResponse } from "../../../types";
import { cx } from "../../../utils";
import { LEAVE_REQUEST_STATUS_LABELS, LEAVE_REQUEST_TYPE_LABELS } from "../clubConstants";
import { formatDate } from "../clubUtils";

interface LeaveRequestsTabProps {
  user: AuthUserResponse;
  isClubAdmin: boolean;
  leaveRequests: LeaveRequestResponse[];
  busy: boolean;
  error: string | null;
  setBusy: (value: boolean) => void;
  setError: (value: string | null) => void;
  onApprove: (requestId: string, decisionNote?: string) => Promise<void>;
  onReject: (requestId: string, decisionNote?: string) => Promise<void>;
}

type LeaveFilter = "ALL" | "PENDING" | "APPROVED" | "REJECTED" | "EXPIRED_AUTO_ABSENT";

export function LeaveRequestsTab({
  user,
  isClubAdmin,
  leaveRequests,
  busy,
  error,
  setBusy,
  setError,
  onApprove,
  onReject
}: LeaveRequestsTabProps) {
  const [filter, setFilter] = useState<LeaveFilter>("PENDING");
  const [decisionModal, setDecisionModal] = useState<{ requestId: string; action: "approve" | "reject"; note: string } | null>(null);

  const filteredRequests = useMemo(() => {
    return leaveRequests.filter((request) => filter === "ALL" || request.status === filter);
  }, [filter, leaveRequests]);

  const pendingCount = leaveRequests.filter((request) => request.status === "PENDING").length;

  async function handleDecision() {
    if (!decisionModal) return;
    setBusy(true);
    setError(null);
    try {
      if (decisionModal.action === "approve") {
        await onApprove(decisionModal.requestId, decisionModal.note || undefined);
      } else {
        await onReject(decisionModal.requestId, decisionModal.note || undefined);
      }
      setDecisionModal(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Có lỗi xảy ra");
    } finally {
      setBusy(false);
    }
  }

  return (
    <motion.div
      className="club-tab-content"
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
    >
      <div className="club-section-head">
        <div>
          <span className="club-ops-kicker">Yêu cầu xin nghỉ</span>
          <h2>Quản lý đơn xin nghỉ phép từ thành viên</h2>
        </div>
        {isClubAdmin && pendingCount > 0 ? (
          <div className="club-pending-badge">
            {pendingCount} yêu cầu chờ duyệt
          </div>
        ) : null}
      </div>

      {error ? <p className="club-form-error">{error}</p> : null}

      <div className="club-filter-bar">
        <button className={cx("club-filter-button", filter === "ALL" && "active")} onClick={() => setFilter("ALL")}>
          Tất cả ({leaveRequests.length})
        </button>
        <button className={cx("club-filter-button", filter === "PENDING" && "active")} onClick={() => setFilter("PENDING")}>
          Chờ duyệt ({leaveRequests.filter((request) => request.status === "PENDING").length})
        </button>
        <button className={cx("club-filter-button", filter === "APPROVED" && "active")} onClick={() => setFilter("APPROVED")}>
          Đã duyệt ({leaveRequests.filter((request) => request.status === "APPROVED").length})
        </button>
        <button className={cx("club-filter-button", filter === "REJECTED" && "active")} onClick={() => setFilter("REJECTED")}>
          Từ chối ({leaveRequests.filter((request) => request.status === "REJECTED").length})
        </button>
        <button
          className={cx("club-filter-button", filter === "EXPIRED_AUTO_ABSENT" && "active")}
          onClick={() => setFilter("EXPIRED_AUTO_ABSENT")}
        >
          Quá hạn ({leaveRequests.filter((request) => request.status === "EXPIRED_AUTO_ABSENT").length})
        </button>
      </div>

      <div className="club-leave-requests-list">
        {filteredRequests.length === 0 ? (
          <div className="club-empty-state">
            <p>Không có yêu cầu nào</p>
          </div>
        ) : filteredRequests.map((request) => (
          <div
            key={request.id}
            className={cx(
              "club-leave-request-card",
              request.status === "PENDING" && "pending",
              request.status === "APPROVED" && "approved",
              request.status === "REJECTED" && "rejected",
              request.status === "EXPIRED_AUTO_ABSENT" && "expired"
            )}
          >
            <div className="leave-request-header">
              <div className="leave-request-title">
                <div className="leave-request-icon">
                  {request.status === "PENDING" ? <Clock size={20} /> : null}
                  {request.status === "APPROVED" ? <CheckCircle2 size={20} /> : null}
                  {request.status === "REJECTED" ? <XCircle size={20} /> : null}
                  {request.status === "EXPIRED_AUTO_ABSENT" ? <AlertTriangle size={20} /> : null}
                </div>
                <div>
                  <h3>{request.memberName}</h3>
                  <p>{LEAVE_REQUEST_TYPE_LABELS[request.requestType]}</p>
                </div>
              </div>
              <div className={cx("leave-request-status-badge", request.status.toLowerCase())}>
                {LEAVE_REQUEST_STATUS_LABELS[request.status]}
              </div>
            </div>

            <div className="leave-request-body">
              <div className="leave-request-detail-row">
                <span className="leave-request-label">Lý do:</span>
                <span>{request.reason}</span>
              </div>

              {request.requestType !== "LEAVE_LONG_TERM" && request.sessionName ? (
                <div className="leave-request-detail-row">
                  <span className="leave-request-label">Buổi tập:</span>
                  <span>{request.sessionName}</span>
                </div>
              ) : null}

              {request.requestType === "LEAVE_LONG_TERM" && request.fromDate && request.toDate ? (
                <div className="leave-request-detail-row">
                  <span className="leave-request-label">Khoảng thời gian:</span>
                  <span>{formatDate(request.fromDate)} - {formatDate(request.toDate)}</span>
                </div>
              ) : null}

              <div className="leave-request-detail-row">
                <span className="leave-request-label">Gửi lúc:</span>
                <span>{formatDate(request.createdAt)}</span>
              </div>

              {request.status !== "PENDING" && request.decisionNote ? (
                <div className="leave-request-detail-row">
                  <span className="leave-request-label">Ghi chú từ admin:</span>
                  <span>{request.decisionNote}</span>
                </div>
              ) : null}

              {request.status === "PENDING" && request.expiresAt ? (
                <div className="leave-request-expires-bar">
                  <AlertTriangle size={16} />
                  <span>Hết hạn duyệt: {formatDate(request.expiresAt)}</span>
                </div>
              ) : null}
            </div>

            {isClubAdmin && request.status === "PENDING" ? (
              <div className="leave-request-actions">
                <button
                  className="club-secondary-button"
                  onClick={() => setDecisionModal({ requestId: request.id, action: "reject", note: "" })}
                  disabled={busy}
                >
                  Từ chối
                </button>
                <button
                  className="club-primary-button"
                  onClick={() => setDecisionModal({ requestId: request.id, action: "approve", note: "" })}
                  disabled={busy}
                >
                  Duyệt
                </button>
              </div>
            ) : null}
          </div>
        ))}
      </div>

      {decisionModal ? (
        <div className="club-modal-overlay" onClick={() => setDecisionModal(null)}>
          <div className="club-modal" onClick={(event) => event.stopPropagation()}>
            <h3>{decisionModal.action === "approve" ? "Duyệt yêu cầu" : "Từ chối yêu cầu"}</h3>
            <textarea
              value={decisionModal.note}
              onChange={(event) => setDecisionModal({ ...decisionModal, note: event.target.value })}
              placeholder="Thêm ghi chú (tuỳ chọn)"
              rows={3}
              className="club-form-textarea"
            />
            <div className="club-modal-actions">
              <button className="club-secondary-button" onClick={() => setDecisionModal(null)} disabled={busy}>
                Huỷ
              </button>
              <button
                className={cx(decisionModal.action === "approve" ? "club-primary-button" : "club-primary-button danger")}
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
