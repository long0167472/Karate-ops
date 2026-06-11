import type { AttendanceRecordResponse, AttendanceSessionResponse, ClubTrainingScheduleResponse } from "../../types";
import { ATTENDANCE_LABELS, PAYMENT_LABELS, ROLE_LABELS, STATUS_LABELS, WEEKDAYS } from "./clubConstants";

export function roleLabel(role: string) {
  return ROLE_LABELS[role] || role;
}

export function statusLabel(status: string) {
  return STATUS_LABELS[status] || status;
}

export function attendanceLabel(status: string) {
  return ATTENDANCE_LABELS[status] || status;
}

export function paymentLabel(status: string) {
  return PAYMENT_LABELS[status] || status;
}

export function genderLabel(gender?: string) {
  if (gender === "MALE") return "Nam";
  if (gender === "FEMALE") return "Nữ";
  if (gender === "OTHER") return "Khác";
  return "Chưa rõ giới tính";
}

export function sessionSourceLabel(session: AttendanceSessionResponse) {
  if (session.status === "CANCELLED") return "Nghỉ tập";
  return session.source === "SCHEDULED" ? "Lịch cố định" : "Buổi ngoài lịch";
}

export function scheduleDaysText(schedule: ClubTrainingScheduleResponse | null) {
  if (!schedule || schedule.daysOfWeek.length === 0) return "Chưa chọn ngày tập";
  return schedule.daysOfWeek.map((value) => WEEKDAYS.find((day) => day.value === value)?.label || value).join(", ");
}

export function formatDate(value?: string) {
  if (!value) return "chưa cập nhật";
  return new Intl.DateTimeFormat("vi-VN").format(new Date(value));
}

export function dateLabel(value: string) {
  return new Intl.DateTimeFormat("vi-VN", { weekday: "long", day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date(`${value}T00:00:00`));
}

export function monthLabel(value: string) {
  return new Intl.DateTimeFormat("vi-VN", { month: "long", year: "numeric" }).format(new Date(`${value}-01T00:00:00`));
}

export function sessionDate(session: AttendanceSessionResponse) {
  return session.scheduledDate || (session.scheduledAt ? session.scheduledAt.slice(0, 10) : "");
}

export function isoWeekday(value: string) {
  const day = new Date(`${value}T00:00:00`).getDay();
  return day === 0 ? 7 : day;
}

export function calendarDays(month: string) {
  const first = new Date(`${month}-01T00:00:00`);
  const firstIso = first.getDay() === 0 ? 7 : first.getDay();
  const daysInMonth = new Date(first.getFullYear(), first.getMonth() + 1, 0).getDate();
  const blanks = Array.from({ length: firstIso - 1 }, () => null);
  const days = Array.from({ length: daysInMonth }, (_, index) => new Date(first.getFullYear(), first.getMonth(), index + 1));
  return [...blanks, ...days];
}

export function formatDateInput(value: Date) {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, "0");
  const day = String(value.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function shiftMonth(value: string, delta: number) {
  const date = new Date(`${value}-01T00:00:00`);
  date.setMonth(date.getMonth() + delta);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
}

export function localDateTimeIso(date: string, time: string) {
  return new Date(`${date}T${time || "18:30"}:00`).toISOString();
}

export function initials(value: string) {
  return value.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]?.toUpperCase()).join("") || "CL";
}

export function normalizeText(value: string) {
  return value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase();
}

export function slugCode(value: string) {
  return normalizeText(value).replace(/[^a-z0-9]+/g, "_").replace(/^_+|_+$/g, "").toUpperCase();
}

export function today() {
  return new Date().toISOString().slice(0, 10);
}

export function attendancePercent(records: AttendanceRecordResponse[]) {
  if (records.length === 0) return 0;
  const positive = records.filter((record) => record.status === "PRESENT" || record.status === "LATE").length;
  return Math.round((positive / records.length) * 100);
}

export function errorMessage(err: unknown) {
  return err instanceof Error ? err.message : "Không thể hoàn tất thao tác.";
}
