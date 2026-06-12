interface StatusBadgeProps {
  status: string;
  size?: "sm" | "md";
}

type ColorConfig = { bg: string; text: string };

const STATUS_COLOR_MAP: Record<string, ColorConfig> = {
  // Tournament statuses
  DRAFT: { bg: "rgba(164, 173, 186, 0.15)", text: "var(--muted)" },
  REGISTRATION_OPEN: { bg: "rgba(38, 124, 232, 0.15)", text: "var(--ao)" },
  REGISTRATION_CLOSED: { bg: "rgba(245, 158, 11, 0.15)", text: "#f59e0b" },
  RUNNING: { bg: "rgba(34, 197, 94, 0.15)", text: "#22c55e" },
  COMPLETED: { bg: "rgba(164, 173, 186, 0.15)", text: "var(--muted)" },
  ARCHIVED: { bg: "rgba(164, 173, 186, 0.15)", text: "var(--muted)" },
  // Entry / member statuses
  REQUESTED: { bg: "rgba(245, 158, 11, 0.15)", text: "#f59e0b" },
  APPROVED: { bg: "rgba(38, 124, 232, 0.15)", text: "var(--ao)" },
  REJECTED: { bg: "rgba(225, 61, 76, 0.15)", text: "var(--aka)" },
  INACTIVE: { bg: "rgba(225, 61, 76, 0.15)", text: "var(--aka)" },
  WITHDRAWN: { bg: "rgba(164, 173, 186, 0.15)", text: "var(--muted)" },
  // BTC approval
  PENDING: { bg: "rgba(245, 158, 11, 0.15)", text: "#f59e0b" },
};

const LABEL_MAP: Record<string, string> = {
  DRAFT: "Nháp",
  REGISTRATION_OPEN: "Mở đăng ký",
  REGISTRATION_CLOSED: "Đóng đăng ký",
  RUNNING: "Đang thi đấu",
  COMPLETED: "Hoàn thành",
  ARCHIVED: "Lưu trữ",
  REQUESTED: "Chờ duyệt",
  APPROVED: "Đã duyệt",
  REJECTED: "Từ chối",
  INACTIVE: "Không hoạt động",
  WITHDRAWN: "Đã rút",
  PENDING: "Chờ BTC",
};

const DEFAULT_COLOR: ColorConfig = {
  bg: "rgba(164, 173, 186, 0.12)",
  text: "var(--muted)",
};

export function StatusBadge({ status, size = "md" }: StatusBadgeProps) {
  const colors = STATUS_COLOR_MAP[status] ?? DEFAULT_COLOR;
  const label = LABEL_MAP[status] ?? status;

  const sizeStyle: React.CSSProperties =
    size === "sm"
      ? { fontSize: "0.7rem", padding: "0.15rem 0.45rem" }
      : { fontSize: "0.75rem", padding: "0.2rem 0.6rem" };

  return (
    <span
      className="status-badge"
      style={{
        background: colors.bg,
        color: colors.text,
        ...sizeStyle,
      }}
    >
      {label}
    </span>
  );
}
