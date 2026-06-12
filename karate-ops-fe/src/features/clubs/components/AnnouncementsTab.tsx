import { motion } from "framer-motion";
import { Megaphone, Pencil, Pin, PinOff, Plus, Trash2 } from "lucide-react";
import type { ClubAnnouncementResponse } from "../../../types";
import { cx } from "../../../utils";
import { formatDate } from "../clubUtils";

interface AnnouncementsTabProps {
  isClubAdmin: boolean;
  announcements: ClubAnnouncementResponse[];
  busy: boolean;
  error: string | null;
  onCreate: () => void;
  onEdit: (announcement: ClubAnnouncementResponse) => void;
  onTogglePin: (announcement: ClubAnnouncementResponse) => Promise<void>;
  onDelete: (announcement: ClubAnnouncementResponse) => Promise<void>;
}

export function AnnouncementsTab({
  isClubAdmin,
  announcements,
  busy,
  error,
  onCreate,
  onEdit,
  onTogglePin,
  onDelete
}: AnnouncementsTabProps) {
  return (
    <motion.div className="club-tab-content" initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }}>
      <div className="club-section-head">
        <div>
          <span className="club-ops-kicker">Bảng tin CLB</span>
          <h2>Thông báo gửi tới thành viên</h2>
        </div>
        {isClubAdmin ? (
          <button className="club-primary-button" onClick={onCreate}>
            <Plus size={18} /> Đăng thông báo
          </button>
        ) : null}
      </div>

      <p className="club-helper-text">
        Thông báo hiển thị ở cổng thành viên của tất cả thành viên CLB. Ghim những tin quan trọng để luôn nằm đầu danh sách.
      </p>

      {error ? <p className="club-form-error">{error}</p> : null}

      {announcements.length === 0 ? (
        <div className="club-empty-state">
          <div className="club-empty-line" />
          <strong>Chưa có thông báo nào</strong>
          <p>Đăng thông báo đầu tiên về lịch tập, lệ phí hoặc giải đấu để thành viên nắm thông tin.</p>
          {isClubAdmin ? <button className="club-primary-button" onClick={onCreate}>Đăng thông báo</button> : null}
        </div>
      ) : (
        <div className="club-announcement-list">
          {announcements.map((announcement, index) => (
            <motion.article
              className={cx("club-announcement-card", announcement.pinned && "pinned")}
              key={announcement.id}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.04 }}
            >
              <div className="club-announcement-head">
                <div className="club-announcement-mark"><Megaphone size={18} /></div>
                <div className="club-announcement-title">
                  <strong>
                    {announcement.pinned ? <span className="club-announcement-pin-flag"><Pin size={13} /> Ghim</span> : null}
                    {announcement.title}
                  </strong>
                  <span>{announcement.createdByName || "Ban quản lý"} - {formatDate(announcement.createdAt)}</span>
                </div>
                {isClubAdmin ? (
                  <div className="club-announcement-actions">
                    <button title={announcement.pinned ? "Bỏ ghim" : "Ghim lên đầu"} disabled={busy} onClick={() => onTogglePin(announcement)}>
                      {announcement.pinned ? <PinOff size={16} /> : <Pin size={16} />}
                    </button>
                    <button title="Chỉnh sửa" disabled={busy} onClick={() => onEdit(announcement)}>
                      <Pencil size={16} />
                    </button>
                    <button
                      className="danger"
                      title="Xóa thông báo"
                      disabled={busy}
                      onClick={() => {
                        if (window.confirm(`Xóa thông báo "${announcement.title}"?`)) {
                          void onDelete(announcement);
                        }
                      }}
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                ) : null}
              </div>
              <p className="club-announcement-content">{announcement.content}</p>
            </motion.article>
          ))}
        </div>
      )}
    </motion.div>
  );
}
