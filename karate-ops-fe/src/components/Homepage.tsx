import React, { useEffect, useMemo, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import {
  Award,
  Bell,
  CalendarCheck,
  CheckCircle2,
  ChevronRight,
  CircleDollarSign,
  Megaphone,
  Trophy,
  Wallet,
  X
} from 'lucide-react';
import { apiGet, apiPost } from '../apiClient';
import { useNotifications } from '../useNotifications';
import { cx } from '../utils';
import type {
  AuthUserResponse,
  MemberAttendanceSessionResponse,
  MemberAttendanceSummaryResponse,
  MemberFeeSummaryResponse,
  NotificationResponse,
  PublicTournamentSummary
} from '../types';

const pageMotion = {
  initial: { opacity: 0, y: 14 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -8 }
};

const WEEKDAY_LABELS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];
const MONTHS = ['Tháng 1', 'Tháng 2', 'Tháng 3', 'Tháng 4', 'Tháng 5', 'Tháng 6', 'Tháng 7', 'Tháng 8', 'Tháng 9', 'Tháng 10', 'Tháng 11', 'Tháng 12'];

function todayMonth() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}

function monthLabel(ym: string) {
  const [y, m] = ym.split('-').map(Number);
  return `${MONTHS[m - 1]}, ${y}`;
}

function shiftMonth(ym: string, delta: number) {
  const [y, m] = ym.split('-').map(Number);
  const date = new Date(y, m - 1 + delta, 1);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
}

/** Returns calendar cells (null for leading blanks) for the given YYYY-MM. */
function buildCalendar(ym: string) {
  const [year, month] = ym.split('-').map(Number);
  const firstDay = new Date(year, month - 1, 1);
  const leadingBlanks = (firstDay.getDay() + 6) % 7; // Mon=0..Sun=6
  const daysInMonth = new Date(year, month, 0).getDate();
  const cells: (string | null)[] = [];
  for (let i = 0; i < leadingBlanks; i += 1) cells.push(null);
  for (let d = 1; d <= daysInMonth; d += 1) {
    cells.push(`${ym}-${String(d).padStart(2, '0')}`);
  }
  return cells;
}

function sessionDate(session: MemberAttendanceSessionResponse) {
  return session.scheduledDate || session.scheduledAt?.slice(0, 10) || '';
}

function formatMoney(value?: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(Number(value || 0));
}

function formatRelativeTime(iso: string) {
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) return '';
  const diffMin = Math.round((Date.now() - then) / 60000);
  if (diffMin < 1) return 'Vừa xong';
  if (diffMin < 60) return `${diffMin} phút trước`;
  const diffHour = Math.round(diffMin / 60);
  if (diffHour < 24) return `${diffHour} giờ trước`;
  const diffDay = Math.round(diffHour / 24);
  if (diffDay < 7) return `${diffDay} ngày trước`;
  return new Date(iso).toLocaleDateString('vi-VN');
}

function notificationIcon(type: string) {
  const t = type.toUpperCase();
  if (t.includes('TOURNAMENT') || t.includes('MATCH')) return <Trophy size={18} />;
  if (t.includes('FEE') || t.includes('PAYMENT')) return <CircleDollarSign size={18} />;
  if (t.includes('ATTENDANCE') || t.includes('LEAVE') || t.includes('SCHEDULE')) return <CalendarCheck size={18} />;
  if (t.includes('ANNOUNCE') || t.includes('CLUB')) return <Megaphone size={18} />;
  return <Bell size={18} />;
}

function tournamentStatusLabel(t: PublicTournamentSummary) {
  if (t.registrationOpen) return 'Đang mở đăng ký';
  const s = t.status?.toUpperCase() || '';
  if (s.includes('ONGOING') || s.includes('LIVE') || s.includes('RUNNING')) return 'Đang diễn ra';
  if (s.includes('FINISH') || s.includes('COMPLETE') || s.includes('DONE')) return 'Đã kết thúc';
  return 'Sắp diễn ra';
}

function tournamentDateRange(t: PublicTournamentSummary) {
  const fmt = (d?: string | null) => (d ? new Date(d).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '');
  const start = fmt(t.startsOn);
  const end = fmt(t.endsOn);
  if (start && end && start !== end) return `${start} – ${end}`;
  return start || end || 'Chưa công bố';
}

export const Homepage: React.FC<{ user: AuthUserResponse }> = ({ user }) => {
  const [activeTab, setActiveTab] = useState<'club' | 'tournaments'>('club');
  const [calendarMonth, setCalendarMonth] = useState(todayMonth());

  const [fees, setFees] = useState<MemberFeeSummaryResponse | null>(null);
  const [attendance, setAttendance] = useState<MemberAttendanceSummaryResponse | null>(null);
  const [tournaments, setTournaments] = useState<PublicTournamentSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const { notifications } = useNotifications(user.id);

  // Leave-request modal (per training session)
  const [selectedSession, setSelectedSession] = useState<MemberAttendanceSessionResponse | null>(null);
  const [leaveModalOpen, setLeaveModalOpen] = useState(false);
  const [leaveReason, setLeaveReason] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const reloadAttendance = async () => {
    const next = await apiGet<MemberAttendanceSummaryResponse>('/api/me/attendance');
    setAttendance(next);
  };

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    Promise.all([
      apiGet<MemberFeeSummaryResponse>('/api/me/fees'),
      apiGet<MemberAttendanceSummaryResponse>('/api/me/attendance'),
      apiGet<PublicTournamentSummary[] | { items: PublicTournamentSummary[]; total: number }>(
        '/api/public/tournaments?phase=UPCOMING&limit=6&offset=0'
      )
    ])
      .then(([nextFees, nextAttendance, tournamentRes]) => {
        if (cancelled) return;
        setFees(nextFees);
        setAttendance(nextAttendance);
        setTournaments(Array.isArray(tournamentRes) ? tournamentRes : tournamentRes.items);
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(err instanceof Error ? err.message : 'Không thể tải dữ liệu CLB.');
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const sessionsByDate = useMemo(() => {
    const map: Record<string, MemberAttendanceSessionResponse> = {};
    (attendance?.sessionRows ?? []).forEach((s) => {
      const d = sessionDate(s);
      if (d) map[d] = s;
    });
    return map;
  }, [attendance]);

  const calendarCells = useMemo(() => buildCalendar(calendarMonth), [calendarMonth]);

  // Derived financial figures
  const unpaidAssignments = (fees?.assignments ?? []).filter((a) => Number(a.amountDue) - Number(a.paidAmount) > 0);
  const nextDueDate = unpaidAssignments
    .map((a) => a.dueDate)
    .filter((d): d is string => !!d)
    .sort()[0];
  const totalRemaining = fees?.totalRemaining ?? 0;
  const isSettled = totalRemaining <= 0;

  const recordedSessions = attendance?.sessions ?? 0;
  const attendanceRate = recordedSessions
    ? Math.round(((attendance!.present + attendance!.late) / recordedSessions) * 100)
    : 0;

  const openLeaveModal = (session: MemberAttendanceSessionResponse) => {
    setSelectedSession(session);
    setLeaveReason('');
    setLeaveModalOpen(true);
  };

  const submitLeave = async () => {
    if (!selectedSession || !leaveReason.trim()) return;
    setSubmitting(true);
    setError(null);
    try {
      await apiPost('/api/me/attendance/leave-requests', { sessionId: selectedSession.id, reason: leaveReason.trim() });
      setLeaveModalOpen(false);
      setLeaveReason('');
      await reloadAttendance();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không gửi được đơn xin nghỉ.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="club-ops-page">
      {/* Tabs */}
      <nav className="club-tab-list" style={{ gridTemplateColumns: 'repeat(2, minmax(0, 1fr))' }}>
        <button className={cx(activeTab === 'club' && 'active')} onClick={() => setActiveTab('club')}>
          <span>Clb của tôi</span>
          <small>Tài chính, lịch tập và thông báo</small>
        </button>
        <button className={cx(activeTab === 'tournaments' && 'active')} onClick={() => setActiveTab('tournaments')}>
          <span>Xem các giải đấu</span>
          <small>Các giải đang mở đăng ký</small>
        </button>
      </nav>

      <section className="club-content-panel">
        {error ? (
          <div className="club-inline-notice danger" style={{ marginBottom: 16 }}>
            <X size={18} />
            <div>
              <strong>Có lỗi xảy ra</strong>
              <span>{error}</span>
            </div>
          </div>
        ) : null}

        <motion.div key={activeTab} className="club-tab-content" {...pageMotion}>
          {activeTab === 'club' ? (
            <>
              {/* Hero */}
              <section className="club-dashboard-hero">
                <div>
                  <span className="club-ops-kicker">Bảng điều khiển cá nhân</span>
                  <h2>Chào {user.displayName}, đây là dữ liệu CLB của bạn.</h2>
                  <p>
                    Học phí còn lại, trạng thái thanh toán, lịch tập và thông báo từ câu lạc bộ được gom lại một chỗ để
                    bạn nắm nhanh và xử lý ngay.
                  </p>
                </div>
                <div className="club-dashboard-score">
                  <span>Còn phải đóng</span>
                  <strong>{loading ? '—' : Math.round(totalRemaining / 1000)}K</strong>
                  <small>{isSettled ? 'Đã thanh toán đủ' : `${unpaidAssignments.length} khoản chưa đóng`}</small>
                </div>
              </section>

              {/* Health tiles */}
              <div className="club-bento-grid" style={{ gridTemplateColumns: 'repeat(3, minmax(0, 1fr))' }}>
                <article className="club-health-tile">
                  <div><Wallet /></div>
                  <span>Còn phải đóng</span>
                  <strong>{formatMoney(totalRemaining)}</strong>
                  <p>{unpaidAssignments.length > 0 ? `${unpaidAssignments.length} khoản chưa thanh toán` : 'Không còn khoản nào'}</p>
                </article>
                <article className="club-health-tile">
                  <div><CheckCircle2 /></div>
                  <span>Trạng thái thanh toán</span>
                  <strong>{isSettled ? 'Đã đóng đủ' : 'Còn nợ'}</strong>
                  <p>{isSettled ? `Đã đóng ${formatMoney(fees?.totalPaid)}` : nextDueDate ? `Hạn gần nhất: ${new Date(nextDueDate).toLocaleDateString('vi-VN')}` : 'Chưa đặt hạn'}</p>
                </article>
                <article className="club-health-tile">
                  <div><Award /></div>
                  <span>Chuyên cần</span>
                  <strong>{attendanceRate}%</strong>
                  <p>{recordedSessions} buổi đã ghi nhận{attendance?.pendingLeaveRequests ? ` · ${attendance.pendingLeaveRequests} đơn chờ duyệt` : ''}</p>
                </article>
              </div>

              {/* Calendar + day panel */}
              <div className="club-attendance-layout">
                <aside className="club-calendar-panel">
                  <div className="club-calendar-head">
                    <button type="button" onClick={() => setCalendarMonth(shiftMonth(calendarMonth, -1))}>Tháng trước</button>
                    <strong>{monthLabel(calendarMonth)}</strong>
                    <button type="button" onClick={() => setCalendarMonth(shiftMonth(calendarMonth, 1))}>Tháng sau</button>
                  </div>
                  <div className="club-calendar-grid">
                    {WEEKDAY_LABELS.map((day) => (
                      <span className="weekday" key={day}>{day}</span>
                    ))}
                    {calendarCells.map((date, index) => {
                      if (!date) return <i key={`blank-${index}`} />;
                      const session = sessionsByDate[date];
                      const day = Number(date.slice(-2));
                      const hasLeave = !!session?.leaveRequest;
                      return (
                        <button
                          key={date}
                          type="button"
                          className={cx(date === selectedSession?.scheduledDate && 'active', session && 'fixed', hasLeave && 'has-session')}
                          disabled={!session}
                          onClick={() => session && openLeaveModal(session)}
                          title={session ? session.name : ''}
                        >
                          <span>{day}</span>
                        </button>
                      );
                    })}
                  </div>
                </aside>

                <section className="club-day-panel">
                  <div className="club-attendance-head">
                    <div>
                      <span className="club-ops-kicker">Lịch tập</span>
                      <h3>Buổi tập trong {monthLabel(calendarMonth)}</h3>
                      <p>
                        Ngày có chấm là buổi tập của bạn. Bấm vào một buổi để xin nghỉ — đơn sẽ gửi tới huấn luyện viên
                        để duyệt.
                      </p>
                    </div>
                  </div>
                  <div className="club-low-attendance-list">
                    {(attendance?.sessionRows ?? [])
                      .filter((s) => sessionDate(s).startsWith(calendarMonth))
                      .slice(0, 5)
                      .map((s) => (
                        <article className="club-low-attendance-row" key={s.id}>
                          <div>
                            <strong>{s.name}</strong>
                            <span>
                              {sessionDate(s) ? new Date(sessionDate(s)).toLocaleDateString('vi-VN') : 'Chưa xếp lịch'} ·{' '}
                              {s.leaveRequest ? `Xin nghỉ: ${s.leaveRequest.status}` : s.record?.status || 'Chưa điểm danh'}
                            </span>
                          </div>
                          {!s.leaveRequest ? (
                            <button type="button" className="club-secondary-button" onClick={() => openLeaveModal(s)}>
                              Xin nghỉ
                            </button>
                          ) : (
                            <b style={{ color: '#9b6720', fontSize: '0.8rem' }}>{s.leaveRequest.status}</b>
                          )}
                        </article>
                      ))}
                    {!loading && (attendance?.sessionRows ?? []).filter((s) => sessionDate(s).startsWith(calendarMonth)).length === 0 ? (
                      <p style={{ color: '#766f65', margin: 0 }}>Không có buổi tập nào trong tháng này.</p>
                    ) : null}
                  </div>
                </section>
              </div>

              {/* Notifications */}
              <section className="club-low-attendance-panel" style={{ gridColumn: 'auto' }}>
                <div className="club-dashboard-panel-head">
                  <div>
                    <span className="club-ops-kicker">Thông báo</span>
                    <h3>Thông báo từ câu lạc bộ</h3>
                  </div>
                </div>
                <div className="club-low-attendance-list">
                  {notifications.length === 0 ? (
                    <p style={{ color: '#766f65', margin: 0 }}>Chưa có thông báo nào.</p>
                  ) : (
                    notifications.slice(0, 5).map((notif: NotificationResponse, index) => (
                      <motion.article
                        key={notif.id}
                        initial={{ opacity: 0, y: 8 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: index * 0.05 }}
                        className="club-low-attendance-row"
                        style={notif.read ? undefined : { borderColor: 'rgba(155, 103, 32, 0.35)' }}
                      >
                        <div>
                          <strong>{notif.title}</strong>
                          <span>{notif.body ? `${notif.body} · ` : ''}{formatRelativeTime(notif.createdAt)}</span>
                        </div>
                        <b style={{ display: 'inline-flex', color: '#9b6720' }}>{notificationIcon(notif.type)}</b>
                      </motion.article>
                    ))
                  )}
                </div>
              </section>
            </>
          ) : (
            <>
              <section className="club-section-head">
                <div>
                  <span className="club-ops-kicker">Giải đấu</span>
                  <h2>Các giải đấu sắp diễn ra</h2>
                </div>
              </section>

              {!loading && tournaments.length === 0 ? (
                <p style={{ color: '#766f65' }}>Hiện chưa có giải đấu nào sắp diễn ra.</p>
              ) : (
                <div className="club-roster-grid">
                  {tournaments.map((tournament, index) => (
                    <motion.article
                      key={tournament.id}
                      initial={{ opacity: 0, y: 12 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: index * 0.06 }}
                      className="club-low-attendance-panel"
                      style={{ gridColumn: 'auto' }}
                    >
                      <div className="club-dashboard-panel-head">
                        <div>
                          <span className="club-ops-kicker">
                            <Trophy size={13} style={{ verticalAlign: '-2px', marginRight: 4 }} />
                            {tournamentStatusLabel(tournament)}
                          </span>
                          <h3>{tournament.name}</h3>
                        </div>
                      </div>
                      <p style={{ color: '#766f65', margin: 0, lineHeight: 1.5 }}>
                        {tournament.organizerName || 'Ban tổ chức chưa cập nhật'}
                        {tournament.location ? ` · ${tournament.location}` : ''}
                        {tournament.participantCount ? ` · ${tournament.participantCount} VĐV` : ''}
                      </p>
                      <div className="club-low-attendance-row">
                        <div>
                          <strong>Thời gian thi đấu</strong>
                          <span>{tournamentDateRange(tournament)}</span>
                        </div>
                        <a className="club-secondary-button" href={`/tournaments/${tournament.id}`}>
                          Chi tiết <ChevronRight size={16} />
                        </a>
                      </div>
                    </motion.article>
                  ))}
                </div>
              )}
            </>
          )}
        </motion.div>
      </section>

      {/* Leave Request Drawer */}
      <AnimatePresence>
        {leaveModalOpen && selectedSession && (
          <motion.div
            className="club-drawer-layer"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={() => setLeaveModalOpen(false)}
          >
            <motion.aside
              className="club-drawer"
              initial={{ x: 420 }}
              animate={{ x: 0 }}
              exit={{ x: 420 }}
              transition={{ type: 'spring', stiffness: 140, damping: 24 }}
              onClick={(e) => e.stopPropagation()}
            >
              <div className="club-drawer-head">
                <h2>Xin nghỉ buổi tập</h2>
                <button onClick={() => setLeaveModalOpen(false)} aria-label="Đóng">
                  <X size={20} />
                </button>
              </div>

              <form className="club-drawer-form" onSubmit={(e) => { e.preventDefault(); submitLeave(); }}>
                <div className="club-low-attendance-row">
                  <div>
                    <strong>{selectedSession.name}</strong>
                    <span>
                      {selectedSession.organizationName} ·{' '}
                      {sessionDate(selectedSession) ? new Date(sessionDate(selectedSession)).toLocaleDateString('vi-VN') : 'Chưa xếp lịch'}
                    </span>
                  </div>
                </div>

                <label className="club-field">
                  <span>Lý do</span>
                  <textarea
                    value={leaveReason}
                    onChange={(e) => setLeaveReason(e.target.value)}
                    placeholder="Nhập lý do xin nghỉ"
                    rows={3}
                    autoFocus
                  />
                </label>

                <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 4 }}>
                  <button type="button" className="club-secondary-button" onClick={() => setLeaveModalOpen(false)}>
                    Hủy
                  </button>
                  <button type="submit" className="club-primary-button" disabled={submitting || !leaveReason.trim()}>
                    <CalendarCheck size={18} /> {submitting ? 'Đang gửi…' : 'Gửi đơn'}
                  </button>
                </div>
              </form>
            </motion.aside>
          </motion.div>
        )}
      </AnimatePresence>
    </main>
  );
};

export default Homepage;
