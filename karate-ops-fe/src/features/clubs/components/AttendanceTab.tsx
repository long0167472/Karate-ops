import { CalendarCheck, Plus } from "lucide-react";
import type { Dispatch, SetStateAction } from "react";
import type { AttendanceRecordResponse, AttendanceSessionResponse, ClubRosterResponse, ClubTrainingScheduleResponse } from "../../../types";
import { apiDelete, apiGet, apiPost } from "../../../apiClient";
import { cx } from "../../../utils";
import { saveAttendanceRecord, saveAttendanceRecords, upsertAttendanceRecord } from "../clubApi";
import { ATTENDANCE_STATUSES, type ClubDrawer, WEEKDAYS } from "../clubConstants";
import {
  attendanceLabel,
  attendancePercent,
  calendarDays,
  dateLabel,
  errorMessage,
  formatDateInput,
  initials,
  isoWeekday,
  monthLabel,
  scheduleDaysText,
  sessionDate,
  sessionSourceLabel,
  shiftMonth,
  today
} from "../clubUtils";

interface AttendanceTabProps {
  clubId: string;
  roster: ClubRosterResponse[];
  sessions: AttendanceSessionResponse[];
  schedule: ClubTrainingScheduleResponse | null;
  selectedDate: string;
  setSelectedDate: Dispatch<SetStateAction<string>>;
  selectedSessionId: string;
  setSelectedSessionId: Dispatch<SetStateAction<string>>;
  calendarMonth: string;
  setCalendarMonth: Dispatch<SetStateAction<string>>;
  busy: boolean;
  setBusy: (value: boolean) => void;
  setError: (value: string | null) => void;
  setDrawer: (drawer: ClubDrawer) => void;
  setSessions: Dispatch<SetStateAction<AttendanceSessionResponse[]>>;
}

export function AttendanceTab({
  clubId,
  roster,
  sessions,
  schedule,
  selectedDate,
  setSelectedDate,
  selectedSessionId,
  setSelectedSessionId,
  calendarMonth,
  setCalendarMonth,
  busy,
  setBusy,
  setError,
  setDrawer,
  setSessions
}: AttendanceTabProps) {
  const selectedDateSessions = sessions
    .filter((session) => sessionDate(session) === selectedDate)
    .sort((a, b) => (a.scheduledAt || "").localeCompare(b.scheduledAt || ""));
  const selectedSession = selectedDateSessions.find((session) => session.id === selectedSessionId) || selectedDateSessions[0];
  const selectedDateHasFixedSchedule = schedule?.active && schedule.daysOfWeek.includes(isoWeekday(selectedDate));
  const records = selectedSession?.records || [];
  const pending = busy || !selectedSession || selectedSession.status === "CANCELLED";

  async function refreshAttendanceSessions() {
    const nextSessions = await apiGet<AttendanceSessionResponse[]>(`/api/organizations/${clubId}/attendance-sessions`);
    setSessions(nextSessions);
    setSelectedSessionId((current) => current || nextSessions.find((session) => sessionDate(session) === selectedDate)?.id || "");
  }

  async function runAttendanceAction(action: () => Promise<void>) {
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

  function mergeRecords(nextRecords: AttendanceRecordResponse[]) {
    if (!selectedSession) return;
    setSessions((current) => current.map((session) => {
      if (session.id !== selectedSession.id) return session;
      return {
        ...session,
        records: nextRecords.reduce((records, nextRecord) => upsertAttendanceRecord(records, nextRecord), session.records)
      };
    }));
  }

  async function markOne(item: ClubRosterResponse, record: AttendanceRecordResponse | undefined, status: string) {
    if (!selectedSession) return;
    await runAttendanceAction(async () => {
      const nextRecord = await saveAttendanceRecord(selectedSession.id, item.athleteId, record, status);
      mergeRecords([nextRecord]);
    });
  }

  async function markMany(status: string, onlyEmpty: boolean) {
    if (!selectedSession) return;
    const rows = roster
      .map((item) => ({ item, record: selectedSession.records.find((row) => row.athleteId === item.athleteId) }))
      .filter(({ record }) => !onlyEmpty || !record)
      .map(({ item, record }) => ({ athleteId: item.athleteId, record, status }));
    if (rows.length === 0) return;
    await runAttendanceAction(async () => {
      const nextRecords = await saveAttendanceRecords(selectedSession.id, rows);
      mergeRecords(nextRecords);
    });
  }

  return (
    <div className="club-tab-content">
      <div className="club-section-head">
        <div>
          <span className="club-ops-kicker">Lịch tập và chuyên cần</span>
          <h2>Điểm danh theo ngày</h2>
        </div>
        <div className="club-command-actions inline">
          <button className="club-secondary-button" onClick={() => setDrawer("schedule")}><CalendarCheck size={18} /> Sửa lịch cố định</button>
          <button className="club-primary-button" onClick={() => setDrawer("session")}><Plus size={18} /> Thêm buổi ngoài lịch</button>
        </div>
      </div>

      <div className="club-schedule-strip">
        <div>
          <span className="club-ops-kicker">Lịch cố định</span>
          <strong>{schedule?.name || "Chưa có lịch tập"}</strong>
          <p>{scheduleDaysText(schedule)} - {schedule?.startTime || "18:30"} - {schedule?.durationMinutes || 90} phút</p>
        </div>
        <div className="club-weekday-row">
          {WEEKDAYS.map((day) => (
            <span key={day.value} className={cx(schedule?.daysOfWeek.includes(day.value) && "active")}>{day.short}</span>
          ))}
        </div>
      </div>

      <div className="club-attendance-layout">
        <aside className="club-calendar-panel">
          <div className="club-calendar-head">
            <button onClick={() => setCalendarMonth(shiftMonth(calendarMonth, -1))}>Tháng trước</button>
            <strong>{monthLabel(calendarMonth)}</strong>
            <button onClick={() => setCalendarMonth(shiftMonth(calendarMonth, 1))}>Tháng sau</button>
          </div>
          <div className="club-calendar-grid">
            {WEEKDAYS.map((day) => <span className="weekday" key={day.value}>{day.short}</span>)}
            {calendarDays(calendarMonth).map((day, index) => {
              const date = day ? formatDateInput(day) : "";
              const hasSession = date ? sessions.some((session) => sessionDate(session) === date) : false;
              const isFixed = date ? schedule?.active && schedule.daysOfWeek.includes(isoWeekday(date)) : false;
              return day ? (
                <button
                  key={date}
                  className={cx(date === selectedDate && "active", hasSession && "has-session", isFixed && "fixed")}
                  onClick={() => {
                    setSelectedDate(date);
                    setSelectedSessionId(sessions.find((session) => sessionDate(session) === date)?.id || "");
                  }}
                >
                  <span>{day.getDate()}</span>
                </button>
              ) : <i key={`blank-${index}`} />;
            })}
          </div>
        </aside>

        <section className="club-day-panel">
          <div className="club-attendance-head">
            <div>
              <span className="club-ops-kicker">{selectedDate === today() ? "Hôm nay" : "Ngày đang xem"}</span>
              <h3>{dateLabel(selectedDate)}</h3>
              <p>
                {selectedDateHasFixedSchedule
                  ? "Ngày này thuộc lịch tập cố định. Buổi điểm danh thật sẽ được job tạo khi đến ngày."
                  : "Ngày này không nằm trong lịch tập cố định."}
              </p>
            </div>
            <Metric label="Có mặt hoặc muộn" value={`${attendancePercent(records)}%`} />
            <Metric label="Tổng VĐV" value={roster.length} />
          </div>

          {selectedDateSessions.length > 1 ? (
            <label className="club-session-picker">
              <span>Buổi tập trong ngày</span>
              <select value={selectedSession?.id || ""} onChange={(event) => setSelectedSessionId(event.target.value)}>
                {selectedDateSessions.map((session) => <option key={session.id} value={session.id}>{session.name} - {sessionSourceLabel(session)}</option>)}
              </select>
            </label>
          ) : null}

          {selectedSession && selectedSession.status !== "CANCELLED" ? (
            <div className="club-bulk-attendance">
              <button disabled={pending} onClick={() => markMany("PRESENT", true)}>Chưa điểm danh: Có mặt</button>
              <button disabled={pending} onClick={() => markMany("PRESENT", false)}>Tất cả có mặt</button>
              <button disabled={pending} onClick={() => markMany("ABSENT", false)}>Tất cả vắng</button>
            </div>
          ) : null}

          {selectedSession?.status === "CANCELLED" ? (
            <EmptyState
              title="CLB đã nghỉ tập ngày này"
              text="Ngày này có bản ghi nghỉ tập, job hằng ngày sẽ không tạo thêm buổi điểm danh trùng."
            />
          ) : selectedDateSessions.length === 0 ? (
            <EmptyState
              title={selectedDateHasFixedSchedule ? "Buổi tập sẽ tự xuất hiện khi đến ngày" : "Không có buổi tập trong ngày này"}
              text={selectedDateHasFixedSchedule ? "Job hằng ngày sẽ tạo buổi điểm danh vào đầu ngày. Bạn vẫn có thể thêm buổi ngoài lịch nếu CLB có lịch phát sinh." : "Chọn ngày khác trên lịch hoặc thêm một buổi ngoài lịch cho ngày này."}
              action="Thêm buổi ngoài lịch"
              onAction={() => setDrawer("session")}
            />
          ) : (
            <div className="club-attendance-board">
              {roster.map((item) => {
                const record = selectedSession?.records.find((row) => row.athleteId === item.athleteId);
                return (
                  <div key={item.id} className="club-attendance-row">
                    <div className="club-person-cell">
                      <div className="club-avatar">{initials(item.athleteName)}</div>
                      <div>
                        <strong>{item.athleteName}</strong>
                        <span>{record ? attendanceLabel(record.status) : "Chưa điểm danh"}</span>
                      </div>
                    </div>
                    <div className="club-attendance-actions">
                      {ATTENDANCE_STATUSES.map((status) => (
                        <button
                          key={status}
                          className={cx("attendance-choice", record?.status === status && "active", status.toLowerCase())}
                          disabled={pending}
                          onClick={() => markOne(item, record, status)}
                        >
                          {attendanceLabel(status)}
                        </button>
                      ))}
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {selectedDateHasFixedSchedule && selectedSession?.status !== "CANCELLED" ? (
            <button className="club-day-off-button" disabled={busy} onClick={() => runAttendanceAction(async () => {
              await apiPost(`/api/organizations/${clubId}/training-schedule/day-off?date=${selectedDate}`, {});
              await refreshAttendanceSessions();
            })}>
              Đánh dấu nghỉ tập ngày này
            </button>
          ) : null}
          {selectedSession?.source === "MANUAL" ? (
            <button className="club-delete-session-button" disabled={busy} onClick={() => runAttendanceAction(async () => {
              await apiDelete(`/api/attendance-sessions/${selectedSession.id}`);
              setSelectedSessionId("");
              await refreshAttendanceSessions();
            })}>
              Xóa buổi ngoài lịch này
            </button>
          ) : null}
        </section>
      </div>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return <div className="club-metric"><strong>{value}</strong><span>{label}</span></div>;
}

function EmptyState({ title, text, action, onAction }: { title: string; text: string; action?: string; onAction?: () => void }) {
  return (
    <div className="club-empty-state">
      <div className="club-empty-line" />
      <strong>{title}</strong>
      <p>{text}</p>
      {action && onAction ? <button className="club-primary-button" onClick={onAction}>{action}</button> : null}
    </div>
  );
}
