export type ClubTab = "overview" | "members" | "fees" | "roster" | "attendance" | "leaves";
export type ClubDrawer = "club" | "member" | "roster" | "session" | "schedule" | null;

export const CLUB_TABS: Array<{ id: ClubTab; label: string; hint: string }> = [
  { id: "overview", label: "Tổng quan", hint: "Sức khỏe CLB" },
  { id: "members", label: "Thành viên", hint: "Người tập và nhân sự" },
  { id: "fees", label: "Tài chính", hint: "Học phí và thu chi" },
  { id: "roster", label: "VĐV", hint: "Hồ sơ thi đấu" },
  { id: "attendance", label: "Điểm danh", hint: "Buổi tập hôm nay" },
  { id: "leaves", label: "Yêu cầu nghỉ", hint: "Duyệt đơn xin nghỉ" }
];

export const MEMBER_ROLES = ["OWNER", "MANAGER", "COACH", "ATHLETE", "PARENT", "STAFF"];
export const MEMBER_STATUSES = ["ACTIVE", "PENDING", "INACTIVE", "LEFT", "SUSPENDED"];
export const ATTENDANCE_STATUSES = ["PRESENT", "LATE", "ABSENT", "EXCUSED"] as const;
export const PAYMENT_STATUSES = ["PAID", "PENDING", "OVERDUE", "WAIVED", "PARTIAL"];

export const WEEKDAYS = [
  { value: 1, short: "T2", label: "Thứ hai" },
  { value: 2, short: "T3", label: "Thứ ba" },
  { value: 3, short: "T4", label: "Thứ tư" },
  { value: 4, short: "T5", label: "Thứ năm" },
  { value: 5, short: "T6", label: "Thứ sáu" },
  { value: 6, short: "T7", label: "Thứ bảy" },
  { value: 7, short: "CN", label: "Chủ nhật" }
];

export const ROLE_LABELS: Record<string, string> = {
  OWNER: "Chủ CLB",
  MANAGER: "Quản lý",
  COACH: "HLV",
  ATHLETE: "VĐV",
  PARENT: "Phụ huynh",
  STAFF: "Nhân sự"
};

export const STATUS_LABELS: Record<string, string> = {
  ACTIVE: "Đang hoạt động",
  PENDING: "Chờ duyệt",
  INACTIVE: "Tạm nghỉ",
  LEFT: "Đã rời CLB",
  SUSPENDED: "Tạm khóa",
  OPEN: "Đang mở",
  CLOSED: "Đã đóng",
  DRAFT: "Bản nháp"
};

export const ATTENDANCE_LABELS: Record<string, string> = {
  PRESENT: "Có mặt",
  LATE: "Muộn",
  ABSENT: "Vắng",
  EXCUSED: "Có phép"
};

export const PAYMENT_LABELS: Record<string, string> = {
  PAID: "Đã nộp",
  PENDING: "Chờ nộp",
  OVERDUE: "Quá hạn",
  WAIVED: "Miễn phí",
  PARTIAL: "Nộp một phần"
};

export const LEAVE_REQUEST_TYPES = ["LEAVE_LONG_TERM", "LEAVE_SESSION", "LATE"] as const;
export const LEAVE_REQUEST_STATUSES = ["PENDING", "APPROVED", "REJECTED", "EXPIRED_AUTO_ABSENT"] as const;

export const LEAVE_REQUEST_TYPE_LABELS: Record<string, string> = {
  LEAVE_LONG_TERM: "Nghỉ dài hạn",
  LEAVE_SESSION: "Nghỉ 1 buổi",
  LATE: "Đến muộn"
};

export const LEAVE_REQUEST_STATUS_LABELS: Record<string, string> = {
  PENDING: "Chờ duyệt",
  APPROVED: "Đã duyệt",
  REJECTED: "Từ chối",
  EXPIRED_AUTO_ABSENT: "Quá hạn - Đánh vắng"
};
