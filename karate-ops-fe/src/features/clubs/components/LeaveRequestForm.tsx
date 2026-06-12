import { useState, type FormEvent } from "react";
import { cx } from "../../../utils";
import { LEAVE_REQUEST_TYPES, LEAVE_REQUEST_TYPE_LABELS } from "../clubConstants";

export interface LeaveRequestFormSession {
  id: string;
  name: string;
  status: string;
  scheduledAt?: string;
  scheduledDate?: string;
}

export interface LeaveRequestFormData {
  requestType: "LEAVE_LONG_TERM" | "LEAVE_SESSION" | "LATE";
  sessionId?: string;
  fromDate?: string;
  toDate?: string;
  reason: string;
}

interface LeaveRequestFormProps {
  busy: boolean;
  error: string | null;
  sessions: LeaveRequestFormSession[];
  initialSessionId?: string;
  onSubmit: (data: LeaveRequestFormData) => void;
}

export function LeaveRequestForm({ busy, error, sessions, initialSessionId, onSubmit }: LeaveRequestFormProps) {
  const [requestType, setRequestType] = useState<LeaveRequestFormData["requestType"]>("LEAVE_SESSION");
  const [sessionId, setSessionId] = useState(initialSessionId || "");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [reason, setReason] = useState("");

  const openSessions = sessions.filter((session) => session.status === "OPEN");
  const needsSession = requestType === "LEAVE_SESSION" || requestType === "LATE";
  const invalid = !reason.trim()
    || (needsSession && !sessionId)
    || (requestType === "LEAVE_LONG_TERM" && (!fromDate || !toDate || toDate < fromDate));

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    if (invalid) return;
    onSubmit({
      requestType,
      sessionId: needsSession ? sessionId : undefined,
      fromDate: requestType === "LEAVE_LONG_TERM" ? fromDate : undefined,
      toDate: requestType === "LEAVE_LONG_TERM" ? toDate : undefined,
      reason: reason.trim()
    });
  };

  return (
    <form className="club-drawer-form" onSubmit={handleSubmit}>
      <div className="club-form-section-title">Loại yêu cầu</div>
      <div className="club-leave-request-type-selector">
        {LEAVE_REQUEST_TYPES.map((type) => (
          <label key={type} className={cx("leave-request-type-option", requestType === type && "active")}>
            <input
              type="radio"
              name="requestType"
              value={type}
              checked={requestType === type}
              onChange={() => setRequestType(type)}
            />
            <span>{LEAVE_REQUEST_TYPE_LABELS[type]}</span>
          </label>
        ))}
      </div>

      {needsSession ? (
        <>
          <div className="club-form-section-title">Buổi tập</div>
          {openSessions.length === 0 ? (
            <p className="club-helper-text">Không có buổi tập nào đang mở để xin nghỉ.</p>
          ) : (
            <select value={sessionId} onChange={(event) => setSessionId(event.target.value)} required className="club-form-select">
              <option value="">-- Chọn buổi tập --</option>
              {openSessions.map((session) => (
                <option key={session.id} value={session.id}>
                  {session.name} ({session.scheduledDate || session.scheduledAt?.split("T")[0] || "Chưa có ngày"})
                </option>
              ))}
            </select>
          )}
        </>
      ) : (
        <div className="club-form-grid">
          <div>
            <label className="club-form-label">Từ ngày *</label>
            <input type="date" value={fromDate} onChange={(event) => setFromDate(event.target.value)} required className="club-form-input" />
          </div>
          <div>
            <label className="club-form-label">Đến ngày *</label>
            <input type="date" value={toDate} min={fromDate || undefined} onChange={(event) => setToDate(event.target.value)} required className="club-form-input" />
          </div>
        </div>
      )}

      <div className="club-form-section-title">Lý do</div>
      <textarea
        value={reason}
        onChange={(event) => setReason(event.target.value)}
        placeholder="Nêu lý do chi tiết (bệnh, công việc, sự kiện...)"
        rows={4}
        className="club-form-textarea"
        required
      />

      <p className="club-helper-text">
        Admin CLB sẽ duyệt yêu cầu trong vòng 24h. Nếu quá hạn mà chưa được duyệt, hệ thống sẽ tự động đánh vắng mặt.
      </p>

      {error ? <p className="club-form-error">{error}</p> : null}
      <button className="club-primary-button" disabled={busy || invalid}>
        {busy ? "Đang gửi..." : "Gửi yêu cầu"}
      </button>
    </form>
  );
}
