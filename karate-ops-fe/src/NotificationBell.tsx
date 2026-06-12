import { AnimatePresence, motion } from "framer-motion";
import { Bell } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useNotifications } from "./useNotifications";
import type { NotificationResponse } from "./types";

interface NotificationBellProps {
  userId: string | null;
}

export function NotificationBell({ userId }: NotificationBellProps) {
  const [open, setOpen] = useState(false);
  const { notifications, unreadCount, markRead, markAllRead } = useNotifications(userId);
  const panelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    function handleClickOutside(e: MouseEvent) {
      if (panelRef.current && !panelRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [open]);

  if (!userId) return null;

  return (
    <div className="notif-bell-root" ref={panelRef}>
      <button
        className="notif-bell-btn"
        aria-label={`Thông báo${unreadCount > 0 ? ` (${unreadCount} chưa đọc)` : ""}`}
        onClick={() => setOpen((v) => !v)}
      >
        <Bell />
        {unreadCount > 0 && (
          <span className="notif-badge">{unreadCount > 9 ? "9+" : unreadCount}</span>
        )}
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            className="notif-panel"
            initial={{ opacity: 0, y: -6, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -6, scale: 0.97 }}
            transition={{ type: "spring", stiffness: 280, damping: 26 }}
          >
            <div className="notif-panel-header">
              <span className="notif-panel-title">Thông báo</span>
              {unreadCount > 0 && (
                <button className="notif-mark-all-btn" onClick={() => { markAllRead(); }}>
                  Đánh dấu tất cả đã đọc
                </button>
              )}
            </div>

            <div className="notif-list">
              {notifications.length === 0 ? (
                <div className="notif-empty">
                  <Bell />
                  <p>Chưa có thông báo nào.</p>
                </div>
              ) : (
                notifications.map((n) => (
                  <NotifItem
                    key={n.id}
                    item={n}
                    onRead={() => {
                      markRead(n.id);
                      if (n.link) {
                        window.location.href = n.link;
                        setOpen(false);
                      }
                    }}
                  />
                ))
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

function NotifItem({ item, onRead }: { item: NotificationResponse; onRead: () => void }) {
  const timeAgo = formatRelativeTime(item.createdAt);

  return (
    <button
      className={`notif-item${item.read ? " notif-item--read" : ""}`}
      onClick={onRead}
    >
      <span className="notif-dot-wrap">
        {!item.read && <span className="notif-unread-dot" />}
      </span>
      <span className="notif-item-body">
        <span className="notif-item-title">{item.title}</span>
        {item.body && <span className="notif-item-desc">{item.body}</span>}
        <span className="notif-item-time">{timeAgo}</span>
      </span>
    </button>
  );
}

function formatRelativeTime(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return "Vừa xong";
  if (mins < 60) return `${mins} phút trước`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours} giờ trước`;
  const days = Math.floor(hours / 24);
  return `${days} ngày trước`;
}
