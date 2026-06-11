import { motion } from "framer-motion";
import { X, Calendar } from "lucide-react";
import { useState, type FormEvent } from "react";
import type { AttendanceSessionResponse } from "../../../types";
import { cx } from "../../../utils";
import { LEAVE_REQUEST_TYPES, LEAVE_REQUEST_TYPE_LABELS } from "../clubConstants";

interface LeaveRequestFormProps {
  busy: boolean;
  error: string | null;
  sessions: AttendanceSessionResponse[];
  onSubmit: (event: FormEvent, data: {
    requestType: "LEAVE_LONG_TERM" | "LEAVE_SESSION" | "LATE";
    sessionId?: string;
    fromDate?: string;
    toDate?: string;
    reason: string;
  }) => void;
}

export function LeaveRequestForm({
  busy,
  error,
  sessions,
  onSubmit
}: LeaveRequestFormProps) {
  const [requestType, setRequestType] = useState<"LEAVE_LONG_TERM" | "LEAVE_SESSION" | "LATE">("LEAVE_SESSION");
  const [sessionId, setSessionId] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [reason, setReason] = useState("");

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!reason.trim()) {
      alert("Vui lòng nhập lý do");
      return;
    }

    if (requestType === "LEAVE_SESSION" && !sessionId) {
      alert("Vui lòng chọn buổi tập");
      return;
    }

    if (requestType === "LEAVE_LONG_TERM" && (!fromDate || !toDate)) {
      alert("Vui lòng nhập ngày bắt đầu và kết thúc");
      return;
    }

    onSubmit(e, {
      requestType,
      sessionId: requestType === "LEAVE_SESSION" ? sessionId : undefined,
      fromDate: requestType === "LEAVE_LONG_TERM" ? fromDate : undefined,
      toDate: requestType === "LEAVE_LONG_TERM" ? toDate : undefined,
      reason
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
              onChange={() => setRequestType(type as any)}
            />
            <span>{LEAVE_REQUEST_TYPE_LABELS[type]}</span>
          </label>
        ))}
      </div>

      {requestType === "LEAVE_SESSION" && (
        <>
          <div className="club-form-section-title">Chọn buổi tập</div>
          <select
            value={sessionId}
            onChange={(e) => setSessionId(e.target.value)}
            required
            className="club-form-select"
          >
            <option value="">-- Chọn buổi tập --</option>
            {sessions.filter((s) => s.status === "OPEN").map((session) => (
              <option key={session.id} value={session.id}>
                {session.name} ({session.scheduledAt?.split("T")[0] || "Chưa có ngày"})
              </option>
            ))}
          </select>
        </>
      )}

      {requestType === "LEAVE_LONG_TERM" && (
        <div className="club-form-grid">
          <div>
            <label className="club-form-label">Từ ngày *</label>
            <input
              type="date"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
              required
              className="club-form-input"
            />
          </div>
          <div>
            <label className="club-form-label">Đến ngày *</label>
            <input
              type="date"
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
              required
              className="club-form-input"
            />
          </div>
        </div>
      )}

      <div className="club-form-section-title">Lý do</div>
      <textarea
        value={reason}
        onChange={(e) => setReason(e.target.value)}
        placeholder="Nêu lý do chi tiết (bệnh, công việc, sự kiện...)"
        rows={4}
        className="club-form-textarea"
        required
      />

      <p className="club-helper-text">
        Admin CLB sẽ duyệt yêu cầu trong vòng 24h. Nếu quá hạn mà chưa được duyệt, hệ thống sẽ tự động đánh vắng mặt.
      </p>

      {error ? <p className="club-form-error">{error}</p> : null}
      <button
        className="club-primary-button"
        disabled={busy || !reason.trim() || (requestType === "LEAVE_SESSION" && !sessionId) || (requestType === "LEAVE_LONG_TERM" && (!fromDate || !toDate))}
      >
        {busy ? "Đang gửi..." : "Gửi yêu cầu"}
      </button>
    </form>
  );
}
