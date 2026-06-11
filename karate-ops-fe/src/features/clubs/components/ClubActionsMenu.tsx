import { ChevronDown, Trash2, Edit3, Pause } from "lucide-react";
import { useState, useRef, useEffect } from "react";
import type { OrganizationResponse } from "../../../types";
import { cx } from "../../../utils";

interface ClubActionsMenuProps {
  club: OrganizationResponse;
  userIsAdmin: boolean;
  busy: boolean;
  onEdit: () => void;
  onToggleInactive: (inactive: boolean) => void;
  onDelete: () => void;
}

export function ClubActionsMenu({
  club,
  userIsAdmin,
  busy,
  onEdit,
  onToggleInactive,
  onDelete
}: ClubActionsMenuProps) {
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

  const isInactive = club.status === "INACTIVE";

  return (
    <div className="club-actions-menu-container" ref={menuRef}>
      <button
        className="club-actions-menu-button"
        onClick={() => setOpen(!open)}
        aria-label="Menu tùy chọn CLB"
        disabled={busy}
      >
        <ChevronDown size={18} />
      </button>

      {open && (
        <div className="club-actions-menu-dropdown">
          <button
            className="club-actions-menu-item"
            onClick={() => {
              onEdit();
              setOpen(false);
            }}
            disabled={busy}
          >
            <Edit3 size={16} />
            <span>Chỉnh sửa thông tin CLB</span>
          </button>

          <button
            className="club-actions-menu-item"
            onClick={() => {
              onToggleInactive(!isInactive);
              setOpen(false);
            }}
            disabled={busy}
          >
            <Pause size={16} />
            <span>{isInactive ? "Kích hoạt lại CLB" : "Tạm đóng CLB"}</span>
          </button>

          <button
            className="club-actions-menu-item danger"
            onClick={() => {
              if (confirm("⚠️ Bạn chắc chắn muốn xóa CLB này? Hành động này không thể hoàn tác.")) {
                onDelete();
              }
              setOpen(false);
            }}
            disabled={busy}
          >
            <Trash2 size={16} />
            <span>Xóa CLB</span>
          </button>
        </div>
      )}
    </div>
  );
}
