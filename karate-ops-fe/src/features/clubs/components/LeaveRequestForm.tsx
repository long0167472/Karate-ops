import { useState, type FormEvent } from "react";
import { cx } from "../../../utils";
import { LEAVE_REQUEST_TYPES, LEAVE_REQUEST_TYPE_LABELS } from "../clubConstants";

export interface LeaveRequestSessionOption {
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
  sessions: LeaveRequestSessionOption[];
  onSubmit: (data: LeaveRequestFormData) => void | Promise<void>;
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

  const needsSession = requestType === "LEAVE_SESSION" || requestType === "LATE";

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    if (!reason.trim()) {
      window.alert("Vui long nhap ly do");
      return;
    }
    if (needsSession && !sessionId) {
      window.alert("Vui long chon buoi tap");
      return;
    }
    if (requestType === "LEAVE_LONG_TERM" && (!fromDate || !toDate)) {
      window.alert("Vui long nhap ngay bat dau va ket thuc");
      return;
    }
    if (requestType === "LEAVE_LONG_TERM" && toDate < fromDate) {
      window.alert("Ngay ket thuc phai sau hoac bang ngay bat dau");
      return;
    }
    void onSubmit({
      requestType,
      sessionId: needsSession ? sessionId : undefined,
      fromDate: requestType === "LEAVE_LONG_TERM" ? fromDate : undefined,
      toDate: requestType === "LEAVE_LONG_TERM" ? toDate : undefined,
      reason: reason.trim()
    });
  };

  return (
    <form className="club-drawer-form" onSubmit={handleSubmit}>
      <div className="club-form-section-title">Loai yeu cau</div>
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
          <div className="club-form-section-title">Chon buoi tap</div>
          <select
            value={sessionId}
            onChange={(event) => setSessionId(event.target.value)}
            required
            className="club-form-select"
          >
            <option value="">-- Chon buoi tap --</option>
            {sessions.filter((session) => session.status === "OPEN").map((session) => (
              <option key={session.id} value={session.id}>
                {session.name} ({session.scheduledDate || session.scheduledAt?.split("T")[0] || "Chua co ngay"})
              </option>
            ))}
          </select>
        </>
      ) : null}

      {requestType === "LEAVE_LONG_TERM" ? (
        <div className="club-form-grid">
          <div>
            <label className="club-form-label">Tu ngay *</label>
            <input
              type="date"
              value={fromDate}
              onChange={(event) => setFromDate(event.target.value)}
              required
              className="club-form-input"
            />
          </div>
          <div>
            <label className="club-form-label">Den ngay *</label>
            <input
              type="date"
              value={toDate}
              onChange={(event) => setToDate(event.target.value)}
              required
              className="club-form-input"
            />
          </div>
        </div>
      ) : null}

      <div className="club-form-section-title">Ly do</div>
      <textarea
        value={reason}
        onChange={(event) => setReason(event.target.value)}
        placeholder="Neu ly do chi tiet"
        rows={4}
        className="club-form-textarea"
        required
      />

      <p className="club-helper-text">
        Admin CLB se duyet yeu cau. Neu qua han ma chua duoc duyet, he thong se tu dong danh vang mat.
      </p>

      {error ? <p className="club-form-error">{error}</p> : null}
      <button
        className="club-primary-button"
        disabled={
          busy
          || !reason.trim()
          || (needsSession && !sessionId)
          || (requestType === "LEAVE_LONG_TERM" && (!fromDate || !toDate || toDate < fromDate))
        }
      >
        {busy ? "Dang gui..." : "Gui yeu cau"}
      </button>
    </form>
  );
}
