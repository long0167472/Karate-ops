import { ChevronDown, Trash2, Edit3, Pause } from "lucide-react";
import { useState, useRef, useEffect } from "react";
import type { ClubMemberResponse } from "../../../types";
import { cx } from "../../../utils";

interface MemberActionsMenuProps {
  member: ClubMemberResponse;
  userIsAdmin: boolean;
  busy: boolean;
  onEdit: () => void;
  onToggleInactive: (inactive: boolean) => void;
  onDelete: () => void;
}

export function MemberActionsMenu({
  member,
  userIsAdmin,
  busy,
  onEdit,
  onToggleInactive,
  onDelete
}: MemberActionsMenuProps) {
  const [open, setOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }

    if (open) {
      document.addEventListener("mousedown", handleClickOutside);
      return () => document.removeEventListener("mousedown", handleClickOutside);
    }
  }, [open]);

  if (!userIsAdmin) return null;

  const isInactive = member.status === "INACTIVE";

  return (
    <div className="member-actions-menu-container" ref={menuRef}>
      <button
        className="member-actions-menu-button"
        onClick={() => setOpen(!open)}
        aria-label="Menu tùy chọn thành viên"
        disabled={busy}
      >
        <ChevronDown size={18} />
      </button>

      {open && (
        <div className="member-actions-menu-dropdown">
          <button
            className="member-actions-menu-item"
            onClick={() => {
              onEdit();
              setOpen(false);
            }}
            disabled={busy}
          >
            <Edit3 size={16} />
            <span>Chỉnh sửa thông tin</span>
          </button>

          <button
            className="member-actions-menu-item"
            onClick={() => {
              onToggleInactive(!isInactive);
              setOpen(false);
            }}
            disabled={busy}
          >
            <Pause size={16} />
            <span>{isInactive ? "Kích hoạt lại" : "Tạm đóng trí hoạt động"}</span>
          </button>

          <button
            className="member-actions-menu-item danger"
            onClick={() => {
              if (confirm("⚠️ Bạn chắc chắn muốn xóa thành viên này? Hành động này không thể hoàn tác.")) {
                onDelete();
              }
              setOpen(false);
            }}
            disabled={busy}
          >
            <Trash2 size={16} />
            <span>Xóa thành viên</span>
          </button>
        </div>
      )}
    </div>
  );
}
