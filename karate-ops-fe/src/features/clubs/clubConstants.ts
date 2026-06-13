export type ClubTab = "overview" | "members" | "fees" | "roster" | "attendance" | "leaves" | "exams";
export type ClubDrawer = "club" | "member" | "roster" | "session" | "schedule" | "exam" | null;

export const CLUB_TABS: Array<{ id: ClubTab; label: string; hint: string }> = [
  { id: "overview", label: "Tổng quan", hint: "Sức khỏe CLB" },
  { id: "members", label: "Thành viên", hint: "Người tập và nhân sự" },
  { id: "fees", label: "Tài chính", hint: "Học phí và thu chi" },
  { id: "roster", label: "VĐV", hint: "Hồ sơ thi đấu" },
  { id: "attendance", label: "Điểm danh", hint: "Buổi tập hôm nay" },
  { id: "leaves", label: "Yêu cầu nghỉ", hint: "Duyệt đơn xin nghỉ" },
  { id: "exams", label: "Thi lên đai", hint: "Kỳ thi thăng cấp" }
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

export const BELT_RANKS = [
  { value: "WHITE", label: "Trắng" },
  { value: "ORANGE", label: "Cam" },
  { value: "BLUE", label: "Xanh dương" },
  { value: "YELLOW", label: "Vàng" },
  { value: "GREEN", label: "Xanh lá" },
  { value: "PURPLE", label: "Tím" },
  { value: "BROWN_3", label: "Nâu 3" },
  { value: "BROWN_2", label: "Nâu 2" },
  { value: "BROWN_1", label: "Nâu 1" },
  { value: "BLACK_1", label: "Đen 1 Đẳng" },
  { value: "BLACK_2", label: "Đen 2 Đẳng" },
  { value: "BLACK_3", label: "Đen 3 Đẳng" },
  { value: "BLACK_4", label: "Đen 4 Đẳng" },
  { value: "BLACK_5", label: "Đen 5 Đẳng" }
] as const;

export const BELT_RANK_LABELS: Record<string, string> = Object.fromEntries(
  BELT_RANKS.map((r) => [r.value, r.label])
);

export const EXAM_STATUS_LABELS: Record<string, string> = {
  DRAFT: "Bản nháp",
  OPEN: "Đang mở",
  IN_PROGRESS: "Đang diễn ra",
  COMPLETED: "Đã hoàn thành",
  CANCELLED: "Đã hủy"
};

export const EXAM_RESULT_LABELS: Record<string, string> = {
  PENDING: "Chờ kết quả",
  PASS: "Đạt",
  FAIL: "Không đạt",
  ABSENT: "Vắng mặt"
};
