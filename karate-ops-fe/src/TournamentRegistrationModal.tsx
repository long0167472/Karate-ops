import { AnimatePresence, motion } from "framer-motion";
import {
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  Loader2,
  Plus,
  Search,
  Trash2,
  X,
  XCircle
} from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import { apiDelete, apiGet, apiGetOptional, apiPost, errorMessage } from "./apiClient";
import type {
  AthleteResponse,
  CategoryResponse,
  RegistrationEntry,
  TournamentRegistration
} from "./types";

interface TournamentRegistrationModalProps {
  tournamentId: string;
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

// ─── Status badge for BTC approval ─────────────────────────────────────────

type BtcStatus = "PENDING" | "APPROVED" | "REJECTED";

function BtcBadge({ status }: { status: BtcStatus }) {
  if (status === "APPROVED") {
    return (
      <span className="trm-btc-badge trm-btc-badge--approved">
        <CheckCircle2 size={12} /> Đã duyệt
      </span>
    );
  }
  if (status === "REJECTED") {
    return (
      <span className="trm-btc-badge trm-btc-badge--rejected">
        <XCircle size={12} /> Bị từ chối
      </span>
    );
  }
  return (
    <span className="trm-btc-badge trm-btc-badge--pending">
      <Loader2 size={12} /> Chờ duyệt
    </span>
  );
}

// ─── Category tree helpers ──────────────────────────────────────────────────

interface CategoryGroup {
  level: string;
  genders: {
    gender: string;
    categories: CategoryResponse[];
  }[];
}

function groupCategories(categories: CategoryResponse[]): CategoryGroup[] {
  const levelMap = new Map<string, Map<string, CategoryResponse[]>>();
  for (const cat of categories) {
    const level = cat.competitionLevel ?? "OPEN";
    const gender = cat.gender ?? "OPEN";
    if (!levelMap.has(level)) levelMap.set(level, new Map());
    const genderMap = levelMap.get(level)!;
    if (!genderMap.has(gender)) genderMap.set(gender, []);
    genderMap.get(gender)!.push(cat);
  }
  const groups: CategoryGroup[] = [];
  for (const [level, genderMap] of levelMap) {
    const genders: CategoryGroup["genders"] = [];
    for (const [gender, cats] of genderMap) {
      genders.push({ gender, categories: cats });
    }
    groups.push({ level, genders });
  }
  return groups;
}

function levelLabel(level: string) {
  if (level === "PHONG_TRAO") return "Phong trào";
  if (level === "NANG_CAO") return "Nâng cao";
  return level;
}

function genderLabel(gender: string) {
  if (gender === "MALE") return "Nam";
  if (gender === "FEMALE") return "Nữ";
  if (gender === "MIXED") return "Hỗn hợp";
  return gender;
}

// ─── Add athlete popover ────────────────────────────────────────────────────

interface AddAthletePopoverProps {
  categoryId: string;
  participantId: string;
  tournamentId: string;
  alreadyRegistered: string[];
  onAdded: () => void;
  onClose: () => void;
}

function AddAthletePopover({
  categoryId,
  participantId,
  tournamentId,
  alreadyRegistered,
  onAdded,
  onClose
}: AddAthletePopoverProps) {
  const [athletes, setAthletes] = useState<AthleteResponse[]>([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setLoading(true);
    apiGet<AthleteResponse[]>("/api/athletes")
      .then(setAthletes)
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    function handler(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) onClose();
    }
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [onClose]);

  async function addAthlete(athleteId: string) {
    setBusy(true);
    setError(null);
    try {
      await apiPost(`/api/tournaments/${tournamentId}/registration/athletes`, {
        participantId,
        categoryId,
        athleteId
      });
      onAdded();
      onClose();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  const filtered = athletes.filter((a) => {
    const kw = search.toLowerCase();
    const name = (a.displayName ?? "").toLowerCase();
    return (!kw || name.includes(kw)) && !alreadyRegistered.includes(a.id);
  });

  return (
    <motion.div
      ref={ref}
      className="trm-add-popover"
      initial={{ opacity: 0, y: -6, scale: 0.97 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      exit={{ opacity: 0, y: -6, scale: 0.97 }}
      transition={{ type: "spring", stiffness: 280, damping: 26 }}
    >
      <div className="trm-add-popover-search">
        <Search size={14} />
        <input
          autoFocus
          placeholder="Tìm VĐV..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>
      {error && <p className="trm-error-text">{error}</p>}
      <div className="trm-add-popover-list">
        {loading ? (
          <div className="trm-add-popover-empty">
            <Loader2 size={16} className="trm-spin" /> Đang tải...
          </div>
        ) : filtered.length === 0 ? (
          <div className="trm-add-popover-empty">Không có VĐV phù hợp</div>
        ) : (
          filtered.slice(0, 25).map((a) => (
            <button
              key={a.id}
              className="trm-add-popover-item"
              disabled={busy}
              onClick={() => addAthlete(a.id)}
            >
              <span className="trm-add-popover-name">{a.displayName}</span>
              {a.weightKg && <span className="trm-add-popover-weight">{a.weightKg}kg</span>}
            </button>
          ))
        )}
      </div>
    </motion.div>
  );
}

// ─── Category row ────────────────────────────────────────────────────────────

interface CategoryRowProps {
  category: CategoryResponse;
  entries: RegistrationEntry[];
  participantId: string;
  tournamentId: string;
  onRefresh: () => void;
}

function CategoryRow({ category, entries, participantId, tournamentId, onRefresh }: CategoryRowProps) {
  const [expanded, setExpanded] = useState(false);
  const [addOpen, setAddOpen] = useState(false);
  const [removing, setRemoving] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const myEntries = entries.filter((e) => e.categoryId === category.id);
  const registeredAthleteIds = myEntries.map((e) => e.athleteId);

  async function removeEntry(entryId: string) {
    setRemoving(entryId);
    setError(null);
    try {
      await apiDelete(`/api/tournaments/${tournamentId}/registration/athletes/${entryId}`);
      onRefresh();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setRemoving(null);
    }
  }

  return (
    <div className="trm-cat-row">
      <button
        className="trm-cat-header"
        onClick={() => setExpanded((v) => !v)}
      >
        <span className="trm-cat-chevron">
          {expanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
        </span>
        <span className="trm-cat-name">{category.name}</span>
        <span className="trm-cat-count">{myEntries.length} VĐV</span>
      </button>

      <AnimatePresence>
        {expanded && (
          <motion.div
            className="trm-cat-body"
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.2 }}
          >
            {error && <p className="trm-error-text">{error}</p>}

            {myEntries.length === 0 ? (
              <p className="trm-cat-empty">Chưa có VĐV nào đăng ký hạng này.</p>
            ) : (
              <ul className="trm-entry-list">
                {myEntries.map((entry) => (
                  <li key={entry.entryId} className="trm-entry-item">
                    <span className="trm-entry-name">{entry.athleteName}</span>
                    {entry.registrationWeightKg && (
                      <span className="trm-entry-weight">{entry.registrationWeightKg}kg</span>
                    )}
                    <BtcBadge status={entry.btcApprovalStatus} />
                    <button
                      className="trm-entry-remove"
                      disabled={removing === entry.entryId}
                      onClick={() => removeEntry(entry.entryId)}
                      title="Xóa VĐV khỏi hạng này"
                    >
                      {removing === entry.entryId ? <Loader2 size={13} className="trm-spin" /> : <Trash2 size={13} />}
                    </button>
                  </li>
                ))}
              </ul>
            )}

            <div className="trm-cat-add-wrap">
              <button
                className="trm-add-athlete-btn"
                onClick={() => setAddOpen((v) => !v)}
              >
                <Plus size={13} /> Thêm VĐV
              </button>
              <AnimatePresence>
                {addOpen && (
                  <AddAthletePopover
                    categoryId={category.id}
                    participantId={participantId}
                    tournamentId={tournamentId}
                    alreadyRegistered={registeredAthleteIds}
                    onAdded={onRefresh}
                    onClose={() => setAddOpen(false)}
                  />
                )}
              </AnimatePresence>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

// ─── Main modal ──────────────────────────────────────────────────────────────

export function TournamentRegistrationModal({
  tournamentId,
  open,
  onClose,
  onSuccess
}: TournamentRegistrationModalProps) {
  const [registration, setRegistration] = useState<TournamentRegistration | null>(null);
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [loadingReg, setLoadingReg] = useState(true);
  const [loadingCats, setLoadingCats] = useState(true);
  const [registering, setRegistering] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadRegistration = useCallback(async () => {
    setLoadingReg(true);
    setError(null);
    try {
      const data = await apiGetOptional<TournamentRegistration>(`/api/tournaments/${tournamentId}/registration`);
      setRegistration(data);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoadingReg(false);
    }
  }, [tournamentId]);

  const loadCategories = useCallback(async () => {
    setLoadingCats(true);
    try {
      const data = await apiGet<CategoryResponse[]>(`/api/tournaments/${tournamentId}/categories`);
      setCategories(data);
    } catch {
      // Non-fatal — tree won't render but modal still usable
    } finally {
      setLoadingCats(false);
    }
  }, [tournamentId]);

  useEffect(() => {
    if (!open) return;
    loadRegistration();
    loadCategories();
  }, [open, loadRegistration, loadCategories]);

  async function registerClub() {
    setRegistering(true);
    setError(null);
    try {
      await apiPost<TournamentRegistration>(`/api/tournaments/${tournamentId}/registration`, {});
      await loadRegistration();
      onSuccess();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setRegistering(false);
    }
  }

  const groups = groupCategories(categories);
  const entries = registration?.entries ?? [];
  const loading = loadingReg || loadingCats;

  return (
    <motion.div
      className="trm-overlay"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.18 }}
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <motion.aside
        className="trm-panel"
        initial={{ x: "100%" }}
        animate={{ x: 0 }}
        exit={{ x: "100%" }}
        transition={{ type: "spring", stiffness: 280, damping: 30 }}
      >
        {/* Panel header */}
        <div className="trm-panel-header">
          <div>
            <span className="trm-panel-kicker">Giải đấu</span>
            <h2 className="trm-panel-title">Đăng ký tham dự giải</h2>
          </div>
          <button className="trm-close-btn" onClick={onClose} aria-label="Đóng">
            <X size={18} />
          </button>
        </div>

        {/* Panel body */}
        <div className="trm-panel-body">
          {error && (
            <div className="trm-error-banner">
              <XCircle size={15} />
              <span>{error}</span>
            </div>
          )}

          {loading ? (
            <div className="trm-panel-loading">
              <Loader2 size={22} className="trm-spin" />
              <span>Đang tải...</span>
            </div>
          ) : !registration ? (
            /* No registration yet */
            <div className="trm-no-reg">
              <div className="trm-no-reg-copy">
                <strong>CLB chưa đăng ký tham dự</strong>
                <span>Đăng ký để thêm VĐV vào các hạng mục của giải.</span>
              </div>
              <button
                className="trm-register-club-btn"
                disabled={registering}
                onClick={registerClub}
              >
                {registering ? <Loader2 size={15} className="trm-spin" /> : <Plus size={15} />}
                {registering ? "Đang đăng ký..." : "Đăng ký CLB"}
              </button>
            </div>
          ) : (
            /* Registration tree */
            <div className="trm-reg-tree">
              <div className="trm-reg-status-row">
                <span className="trm-reg-org">{registration.displayName}</span>
                <span className={`trm-reg-status-badge trm-reg-status--${registration.status.toLowerCase()}`}>
                  {registration.status}
                </span>
              </div>

              {groups.length === 0 ? (
                <p className="trm-tree-empty">Giải chưa có hạng mục nào.</p>
              ) : (
                groups.map((group) => (
                  <div key={group.level} className="trm-level-group">
                    <div className="trm-level-label">{levelLabel(group.level)}</div>
                    {group.genders.map((genderGroup) => (
                      <div key={genderGroup.gender} className="trm-gender-group">
                        <div className="trm-gender-label">{genderLabel(genderGroup.gender)}</div>
                        <div className="trm-cats-list">
                          {genderGroup.categories.map((cat) => (
                            <CategoryRow
                              key={cat.id}
                              category={cat}
                              entries={entries}
                              participantId={registration.participantId}
                              tournamentId={tournamentId}
                              onRefresh={loadRegistration}
                            />
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>
                ))
              )}
            </div>
          )}
        </div>
      </motion.aside>
    </motion.div>
  );
}
