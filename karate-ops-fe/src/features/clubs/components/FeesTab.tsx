import { motion } from "framer-motion";
import {
  Banknote,
  CircleDollarSign,
  Edit3,
  Plus,
  ReceiptText,
  Search,
  Trash2,
  WalletCards,
  X
} from "lucide-react";
import { type FormEvent, type ReactNode, useEffect, useMemo, useState } from "react";
import type {
  ClubFeeItemResponse,
  ClubFeeOverviewResponse,
  ClubFinanceExpenseResponse,
  ExpenseDisbursementStatus,
  FeeItemKind,
  FeeItemStatus,
  FeeItemType,
  MemberFeeAssignmentResponse,
  ClubMemberResponse
} from "../../../types";
import { apiDelete, apiGet, apiPatch, apiPost, apiPut } from "../../../apiClient";
import { cx } from "../../../utils";
import { PAYMENT_STATUSES } from "../clubConstants";
import { errorMessage, formatDate, initials, normalizeText, paymentLabel } from "../clubUtils";

type FinanceView = "overview" | "feeItems" | "assign" | "expenses" | "debt";
type AssignMode = "tuition" | "income";

const ONE_TIME_FEE_TYPES: Array<{ value: FeeItemType; label: string }> = [
  { value: "UNIFORM", label: "Đồng phục" },
  { value: "EXAM", label: "Thi lên đai" },
  { value: "TOURNAMENT", label: "Giải đấu" },
  { value: "OTHER", label: "Khoản khác" }
];

const FEE_ITEM_STATUSES: Array<{ value: FeeItemStatus; label: string }> = [
  { value: "ACTIVE", label: "Đang áp dụng" },
  { value: "DRAFT", label: "Bản nháp" },
  { value: "ARCHIVED", label: "Đã lưu trữ" }
];

const EXPENSE_STATUSES: Array<{ value: ExpenseDisbursementStatus; label: string }> = [
  { value: "PENDING_DISBURSEMENT", label: "Chưa giải ngân" },
  { value: "DISBURSED", label: "Đã giải ngân" }
];

interface FeesTabProps {
  clubId: string;
  members: ClubMemberResponse[];
}

export function FeesTab({ clubId, members }: FeesTabProps) {
  const [overview, setOverview] = useState<ClubFeeOverviewResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeView, setActiveView] = useState<FinanceView>("overview");
  const [assignMode, setAssignMode] = useState<AssignMode>("tuition");
  const [memberSearch, setMemberSearch] = useState("");
  const [selectedMemberIds, setSelectedMemberIds] = useState<string[]>([]);
  const [selectedOverrideItemId, setSelectedOverrideItemId] = useState("");
  const [selectedIncomeItemId, setSelectedIncomeItemId] = useState("");
  const [debtItemId, setDebtItemId] = useState("ALL");
  const [applyNote, setApplyNote] = useState("");
  const [itemDrawerOpen, setItemDrawerOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<ClubFeeItemResponse | null>(null);
  const [itemForm, setItemForm] = useState(emptyItemForm());
  const [expenseDrawerOpen, setExpenseDrawerOpen] = useState(false);
  const [editingExpense, setEditingExpense] = useState<ClubFinanceExpenseResponse | null>(null);
  const [expenseForm, setExpenseForm] = useState(emptyExpenseForm());

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const next = await apiGet<ClubFeeOverviewResponse>(`/api/organizations/${clubId}/finance/overview`);
      setOverview(next);
      const overrideItems = next.feeItems.filter((item) => item.feeKind === "MONTHLY_TUITION_OVERRIDE" && item.status === "ACTIVE");
      const incomeItems = next.feeItems.filter((item) => item.feeKind === "ONE_TIME_INCOME" && item.status === "ACTIVE");
      setSelectedOverrideItemId((current) => overrideItems.some((item) => item.id === current) ? current : overrideItems[0]?.id || "");
      setSelectedIncomeItemId((current) => incomeItems.some((item) => item.id === current) ? current : incomeItems[0]?.id || "");
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [clubId]);

  useEffect(() => {
    setSelectedMemberIds((current) => current.filter((id) => members.some((member) => member.id === id)));
  }, [members]);

  const feeItems = overview?.feeItems ?? [];
  const assignments = overview?.assignments ?? [];
  const expenses = overview?.expenses ?? [];
  const summary = overview?.summary;
  const defaultTuition = feeItems.find((item) => item.feeKind === "MONTHLY_TUITION_DEFAULT");
  const tuitionOverrideItems = feeItems.filter((item) => item.feeKind === "MONTHLY_TUITION_OVERRIDE");
  const oneTimeItems = feeItems.filter((item) => item.feeKind === "ONE_TIME_INCOME");
  const unpaidAssignments = assignments.filter((row) => row.status === "PENDING" || row.status === "OVERDUE" || row.status === "PARTIAL");
  const debtAssignments = debtItemId === "ALL" ? assignments : assignments.filter((row) => row.feeItemId === debtItemId);
  const activeMembers = members.filter((member) => member.status === "ACTIVE");
  const visibleMembers = activeMembers.filter((member) => {
    const keyword = normalizeText(memberSearch);
    return !keyword || normalizeText(`${member.personName || ""} ${member.userName || ""} ${member.phone || ""}`).includes(keyword);
  });
  const selectedOverride = tuitionOverrideItems.find((item) => item.id === selectedOverrideItemId);
  const selectedIncome = oneTimeItems.find((item) => item.id === selectedIncomeItemId);
  const tuitionOverrideByMember = useMemo(() => {
    return Object.fromEntries((overview?.tuitionOverrides ?? []).map((row) => [row.memberId, row]));
  }, [overview?.tuitionOverrides]);
  const allVisibleSelected = visibleMembers.length > 0 && visibleMembers.every((member) => selectedMemberIds.includes(member.id));

  async function run(action: () => Promise<void>) {
    setBusy(true);
    setError(null);
    try {
      await action();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  function startCreateItem(kind: FeeItemKind) {
    setEditingItem(null);
    setItemForm(emptyItemForm(kind));
    setItemDrawerOpen(true);
  }

  function startEditItem(item: ClubFeeItemResponse) {
    setEditingItem(item);
    setItemForm({
      name: item.name,
      feeKind: item.feeKind,
      feeType: item.feeKind === "ONE_TIME_INCOME" ? item.feeType : "TUITION",
      status: item.status,
      defaultAmount: item.defaultAmount || 0,
      dueDay: item.dueDay || 10,
      description: item.description || ""
    });
    setItemDrawerOpen(true);
  }

  function startCreateExpense() {
    setEditingExpense(null);
    setExpenseForm(emptyExpenseForm());
    setExpenseDrawerOpen(true);
  }

  function startEditExpense(expense: ClubFinanceExpenseResponse) {
    setEditingExpense(expense);
    setExpenseForm({
      name: expense.name,
      amount: expense.amount || 0,
      expenseDate: expense.expenseDate || todayDate(),
      status: expense.status,
      note: expense.note || ""
    });
    setExpenseDrawerOpen(true);
  }

  const submitItem = (event: FormEvent) => {
    event.preventDefault();
    run(async () => {
      const body = {
        name: itemForm.name,
        feeKind: itemForm.feeKind,
        feeType: itemForm.feeKind === "ONE_TIME_INCOME" ? itemForm.feeType : "TUITION",
        status: itemForm.status,
        defaultAmount: Number(itemForm.defaultAmount || 0),
        dueDay: itemForm.feeKind === "ONE_TIME_INCOME" ? undefined : Number(itemForm.dueDay || 10),
        description: itemForm.description || undefined,
        roleAmounts: []
      };
      const saved = editingItem
        ? await apiPatch<ClubFeeItemResponse>(`/api/organizations/${clubId}/fee-items/${editingItem.id}`, body)
        : await apiPost<ClubFeeItemResponse>(`/api/organizations/${clubId}/fee-items`, body);
      if (saved.feeKind === "MONTHLY_TUITION_OVERRIDE") setSelectedOverrideItemId(saved.id);
      if (saved.feeKind === "ONE_TIME_INCOME") setSelectedIncomeItemId(saved.id);
      setItemDrawerOpen(false);
      await load();
    });
  };

  const submitExpense = (event: FormEvent) => {
    event.preventDefault();
    run(async () => {
      const body = {
        name: expenseForm.name,
        amount: Number(expenseForm.amount || 0),
        expenseDate: expenseForm.expenseDate || todayDate(),
        status: expenseForm.status,
        note: expenseForm.note || undefined
      };
      if (editingExpense) await apiPatch(`/api/organizations/${clubId}/finance/expenses/${editingExpense.id}`, body);
      else await apiPost(`/api/organizations/${clubId}/finance/expenses`, body);
      setExpenseDrawerOpen(false);
      await load();
    });
  };

  const applyTuitionOverride = (clear = false) => {
    if (!clear && !selectedOverride) return;
    if (selectedMemberIds.length === 0) return;
    run(async () => {
      await apiPut(`/api/organizations/${clubId}/finance/tuition-overrides/bulk`, {
        memberIds: selectedMemberIds,
        feeItemId: clear ? null : selectedOverride?.id
      });
      setSelectedMemberIds([]);
      await load();
    });
  };

  const applyIncomeItem = () => {
    if (!selectedIncome || selectedMemberIds.length === 0) return;
    run(async () => {
      await apiPost(`/api/organizations/${clubId}/fee-items/${selectedIncome.id}/apply`, {
        memberIds: selectedMemberIds,
        note: applyNote || `Áp dụng ${selectedIncome.name}`
      });
      setSelectedMemberIds([]);
      setApplyNote("");
      await load();
      setActiveView("debt");
      setDebtItemId(selectedIncome.id);
    });
  };

  const deleteItem = (item: ClubFeeItemResponse) => {
    run(async () => {
      await apiDelete(`/api/organizations/${clubId}/fee-items/${item.id}`);
      await load();
    });
  };

  const deleteExpense = (expense: ClubFinanceExpenseResponse) => {
    run(async () => {
      await apiDelete(`/api/organizations/${clubId}/finance/expenses/${expense.id}`);
      await load();
    });
  };

  return (
    <div className="club-tab-content club-fees-tab">
      <div className="club-section-head">
        <div>
          <span className="club-ops-kicker">Tài chính CLB</span>
          <h2>Học phí, khoản thu và khoản chi</h2>
          <p className="club-helper-text">Học phí tháng dùng mức mặc định hoặc ghi đè theo từng học viên. Khoản thu một lần và khoản chi được quản lý riêng để không lẫn với học phí tương lai.</p>
        </div>
        <div className="club-fee-summary-inline">
          <Metric label="Còn phải thu" value={formatMoney(summary?.totalOutstanding)} />
          <Metric label="Dòng tiền ròng" value={formatMoney(summary?.netCash)} />
        </div>
      </div>

      {error ? <div className="club-inline-notice danger"><CircleDollarSign size={18} /><div><strong>Không xử lý được tài chính</strong><span>{error}</span></div></div> : null}
      {loading ? <div className="club-workspace-skeleton"><span /><span /><span /><span /></div> : null}

      {!loading ? (
        <>
          <div className="club-segmented-control wide">
            {([
              ["overview", "Tổng quan"],
              ["feeItems", "Các khoản phí"],
              ["assign", "Gán phí"],
              ["expenses", "Khoản chi"],
              ["debt", "Công nợ"]
            ] as Array<[FinanceView, string]>).map(([view, label]) => (
              <button key={view} className={cx(activeView === view && "active")} onClick={() => setActiveView(view)}>
                {label}
              </button>
            ))}
          </div>

          {activeView === "overview" ? (
            <section className="club-fee-workspace">
              <div className="club-finance-metric-strip">
                <FinanceMetric icon={<WalletCards />} label="Học phí tháng dự kiến" value={formatMoney(summary?.monthlyTuitionExpected)} detail={`${summary?.activeMembers ?? activeMembers.length} học viên active`} />
                <FinanceMetric icon={<ReceiptText />} label="Khoản thu một lần" value={formatMoney(summary?.oneTimeIncomeDue)} detail={`${assignments.length} khoản phải thu`} />
                <FinanceMetric icon={<Banknote />} label="Đã thu" value={formatMoney(summary?.totalPaid)} detail={`${unpaidAssignments.length} công nợ mở`} />
                <FinanceMetric icon={<CircleDollarSign />} label="Đã giải ngân" value={formatMoney(summary?.expensesDisbursed)} detail={`${formatMoney(summary?.expensesPending)} chờ giải ngân`} />
              </div>
              <div className="club-finance-ledger">
                <div className="club-fee-panel-head compact">
                  <div>
                    <h3>Nhịp tài chính gần đây</h3>
                    <p>Chỉ giữ các dòng cần xử lý, không dồn nhiều bảng trên cùng một màn.</p>
                  </div>
                  <button className="club-secondary-button" onClick={() => setActiveView("debt")}>Mở công nợ</button>
                </div>
                {unpaidAssignments.slice(0, 5).length === 0 ? (
                  <EmptyState title="Không có công nợ mở" text="Các khoản phải thu mới sẽ xuất hiện sau khi gán khoản thu một lần." />
                ) : unpaidAssignments.slice(0, 5).map((assignment) => (
                  <article className="club-fee-item-row" key={assignment.id}>
                    <button type="button" onClick={() => { setDebtItemId(assignment.feeItemId); setActiveView("debt"); }}>
                      <strong>{assignment.memberName || "Thành viên"}</strong>
                      <span>{assignment.feeItemName} - còn {formatMoney(Number(assignment.amountDue || 0) - Number(assignment.paidAmount || 0))}</span>
                    </button>
                    <Badge>{paymentLabel(assignment.status)}</Badge>
                  </article>
                ))}
              </div>
            </section>
          ) : null}

          {activeView === "feeItems" ? (
            <section className="club-fee-workspace">
              <div className="club-fee-panel-head">
                <div>
                  <h3>Các khoản phí</h3>
                  <p>Mức mặc định dùng cho học phí tháng. Học phí ghi đè chỉ tạo khi có nhóm học viên cần mức khác, còn khoản thu một lần dùng cho đồng phục, thi lên đai hoặc giải đấu.</p>
                </div>
                <div className="club-row-actions">
                  <button className="club-secondary-button" onClick={() => startCreateItem("MONTHLY_TUITION_OVERRIDE")}><Plus size={17} /> Học phí ghi đè</button>
                  <button className="club-primary-button" onClick={() => startCreateItem("ONE_TIME_INCOME")}><Plus size={17} /> Khoản thu</button>
                </div>
              </div>
              <div className="club-fee-item-list">
                {defaultTuition ? (
                  <FeeItemRow item={defaultTuition} membersUsing={activeMembers.length - (overview?.tuitionOverrides?.length ?? 0)} onEdit={startEditItem} />
                ) : <EmptyState title="Chưa có học phí mặc định" text="Backend sẽ tạo khoản Học phí mặc định cho CLB. Thử tải lại nếu dữ liệu vừa được khởi tạo." />}
                {tuitionOverrideItems.map((item) => (
                  <FeeItemRow
                    key={item.id}
                    item={item}
                    membersUsing={(overview?.tuitionOverrides ?? []).filter((row) => row.feeItemId === item.id).length}
                    onEdit={startEditItem}
                    onDelete={deleteItem}
                  />
                ))}
                {oneTimeItems.map((item) => (
                  <FeeItemRow
                    key={item.id}
                    item={item}
                    membersUsing={assignments.filter((assignment) => assignment.feeItemId === item.id).length}
                    onEdit={startEditItem}
                    onDelete={deleteItem}
                  />
                ))}
              </div>
            </section>
          ) : null}

          {activeView === "assign" ? (
            <section className="club-fee-workspace">
              <div className="club-fee-panel-head">
                <div>
                  <h3>Gán phí</h3>
                  <p>Chọn học viên một lần, sau đó gán học phí ghi đè hoặc khoản thu một lần. Gỡ ghi đè sẽ đưa học viên về mức Học phí mặc định.</p>
                </div>
                <div className="club-fee-selected-count">
                  <strong>{selectedMemberIds.length}</strong>
                  <span>đã chọn</span>
                </div>
              </div>
              <div className="club-segmented-control">
                <button className={cx(assignMode === "tuition" && "active")} onClick={() => setAssignMode("tuition")}>Học phí ghi đè</button>
                <button className={cx(assignMode === "income" && "active")} onClick={() => setAssignMode("income")}>Khoản thu một lần</button>
              </div>
              <div className="club-fee-assign-head">
                {assignMode === "tuition" ? (
                  <label className="club-field">
                    <span>Học phí ghi đè</span>
                    <select value={selectedOverrideItemId} onChange={(event) => setSelectedOverrideItemId(event.target.value)}>
                      {tuitionOverrideItems.map((item) => <option key={item.id} value={item.id}>{item.name} - {formatMoney(item.defaultAmount)}</option>)}
                    </select>
                  </label>
                ) : (
                  <label className="club-field">
                    <span>Khoản thu</span>
                    <select value={selectedIncomeItemId} onChange={(event) => setSelectedIncomeItemId(event.target.value)}>
                      {oneTimeItems.map((item) => <option key={item.id} value={item.id}>{item.name} - {formatMoney(item.defaultAmount)}</option>)}
                    </select>
                  </label>
                )}
                <div className="club-row-actions">
                  {assignMode === "tuition" ? (
                    <>
                      <button className="club-secondary-button" disabled={busy || selectedMemberIds.length === 0} onClick={() => applyTuitionOverride(true)}>Gỡ ghi đè</button>
                      <button className="club-primary-button" disabled={busy || selectedMemberIds.length === 0 || !selectedOverride} onClick={() => applyTuitionOverride(false)}>Gán học phí</button>
                    </>
                  ) : (
                    <button className="club-primary-button" disabled={busy || selectedMemberIds.length === 0 || !selectedIncome} onClick={applyIncomeItem}>Gán khoản thu</button>
                  )}
                </div>
              </div>
              {assignMode === "income" ? (
                <label className="club-field">
                  <span>Ghi chú khi gán</span>
                  <input value={applyNote} onChange={(event) => setApplyNote(event.target.value)} placeholder="Ví dụ: Thu đồng phục đợt tháng 6" />
                </label>
              ) : null}
              {assignMode === "tuition" && tuitionOverrideItems.length === 0 ? <EmptyState title="Chưa có học phí ghi đè" text="Tạo Sinh viên, Trẻ em hoặc mức đặc biệt trong Các khoản phí trước khi gán." /> : null}
              {assignMode === "income" && oneTimeItems.length === 0 ? <EmptyState title="Chưa có khoản thu một lần" text="Tạo khoản thu như Đồng phục hoặc Thi lên đai trong Các khoản phí trước khi gán." /> : null}
              <MemberPicker
                members={visibleMembers}
                selectedMemberIds={selectedMemberIds}
                search={memberSearch}
                allVisibleSelected={allVisibleSelected}
                tuitionOverrideByMember={tuitionOverrideByMember}
                defaultTuition={defaultTuition}
                onSearch={setMemberSearch}
                onToggleAll={() => setSelectedMemberIds(allVisibleSelected ? selectedMemberIds.filter((id) => !visibleMembers.some((member) => member.id === id)) : Array.from(new Set([...selectedMemberIds, ...visibleMembers.map((member) => member.id)])))}
                onToggle={(memberId) => setSelectedMemberIds((current) => current.includes(memberId) ? current.filter((id) => id !== memberId) : [...current, memberId])}
              />
            </section>
          ) : null}

          {activeView === "expenses" ? (
            <section className="club-fee-workspace">
              <div className="club-fee-panel-head">
                <div>
                  <h3>Khoản chi</h3>
                  <p>Theo dõi các khoản chi của CLB, trạng thái giải ngân và ghi chú xử lý.</p>
                </div>
                <button className="club-primary-button" onClick={startCreateExpense}><Plus size={17} /> Tạo khoản chi</button>
              </div>
              <div className="club-fee-assignment-table">
                {expenses.length === 0 ? <EmptyState title="Chưa có khoản chi" text="Tạo khoản chi đầu tiên để dashboard thu chi phản ánh dòng tiền ròng của CLB." /> : expenses.map((expense) => (
                  <ExpenseRow key={expense.id} expense={expense} busy={busy} onEdit={startEditExpense} onDelete={deleteExpense} />
                ))}
              </div>
            </section>
          ) : null}

          {activeView === "debt" ? (
            <section className="club-fee-workspace">
              <div className="club-fee-panel-head compact">
                <div>
                  <h3>Công nợ</h3>
                  <p>Cập nhật trạng thái và số tiền đã nộp cho từng khoản phải thu.</p>
                </div>
                <label className="club-field compact-select">
                  <span>Lọc khoản phí</span>
                  <select value={debtItemId} onChange={(event) => setDebtItemId(event.target.value)}>
                    <option value="ALL">Tất cả khoản phải thu</option>
                    {feeItems.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
                  </select>
                </label>
              </div>
              <div className="club-fee-assignment-table">
                {debtAssignments.length === 0 ? (
                  <EmptyState title="Chưa có khoản phải thu" text="Gán khoản thu một lần cho học viên để bắt đầu theo dõi công nợ." />
                ) : debtAssignments.map((assignment) => (
                  <AssignmentRow
                    key={assignment.id}
                    assignment={assignment}
                    busy={busy}
                    onUpdate={(body) => run(async () => { await apiPatch(`/api/organizations/${clubId}/fee-assignments/${assignment.id}`, body); await load(); })}
                    onDelete={() => run(async () => { await apiDelete(`/api/organizations/${clubId}/fee-assignments/${assignment.id}`); await load(); })}
                  />
                ))}
              </div>
            </section>
          ) : null}
        </>
      ) : null}

      {itemDrawerOpen ? (
        <ItemDrawer
          busy={busy}
          editingItem={editingItem}
          itemForm={itemForm}
          onClose={() => setItemDrawerOpen(false)}
          onSubmit={submitItem}
          onChange={setItemForm}
        />
      ) : null}

      {expenseDrawerOpen ? (
        <ExpenseDrawer
          busy={busy}
          editingExpense={editingExpense}
          expenseForm={expenseForm}
          onClose={() => setExpenseDrawerOpen(false)}
          onSubmit={submitExpense}
          onChange={setExpenseForm}
        />
      ) : null}
    </div>
  );
}

function FeeItemRow({ item, membersUsing, onEdit, onDelete }: {
  item: ClubFeeItemResponse;
  membersUsing: number;
  onEdit: (item: ClubFeeItemResponse) => void;
  onDelete?: (item: ClubFeeItemResponse) => void;
}) {
  const protectedDefault = item.feeKind === "MONTHLY_TUITION_DEFAULT";
  return (
    <article className={cx("club-fee-item-row", protectedDefault && "active")}>
      <button type="button" onClick={() => onEdit(item)}>
        <strong>{item.name}</strong>
        <span>{kindLabel(item.feeKind)} - {formatMoney(item.defaultAmount)}{item.dueDay ? ` - hạn ngày ${item.dueDay}` : ""}</span>
        {item.description ? <small>{item.description}</small> : <small>{membersUsing} học viên/khoản đang dùng</small>}
      </button>
      <div className="club-row-actions">
        <button onClick={() => onEdit(item)} aria-label="Sửa khoản phí"><Edit3 size={15} /></button>
        {onDelete && !protectedDefault ? <button className="danger" onClick={() => onDelete(item)} aria-label="Xóa khoản phí"><Trash2 size={15} /></button> : null}
      </div>
    </article>
  );
}

function MemberPicker({
  members,
  selectedMemberIds,
  search,
  allVisibleSelected,
  tuitionOverrideByMember,
  defaultTuition,
  onSearch,
  onToggleAll,
  onToggle
}: {
  members: ClubMemberResponse[];
  selectedMemberIds: string[];
  search: string;
  allVisibleSelected: boolean;
  tuitionOverrideByMember: Record<string, { feeItemName?: string; amount?: number }>;
  defaultTuition?: ClubFeeItemResponse;
  onSearch: (value: string) => void;
  onToggleAll: () => void;
  onToggle: (memberId: string) => void;
}) {
  return (
    <div className="club-fee-picker-list">
      <div className="club-fee-picker-toolbar">
        <label className="club-search">
          <Search size={18} />
          <input value={search} onChange={(event) => onSearch(event.target.value)} placeholder="Tìm học viên theo tên, tài khoản, số điện thoại" />
        </label>
        <button className="club-secondary-button" onClick={onToggleAll}>{allVisibleSelected ? "Bỏ chọn danh sách" : "Chọn danh sách"}</button>
      </div>
      {members.length === 0 ? <EmptyState title="Không có học viên phù hợp" text="Thử đổi từ khóa tìm kiếm hoặc kiểm tra trạng thái thành viên." /> : members.map((member) => {
        const selected = selectedMemberIds.includes(member.id);
        const override = tuitionOverrideByMember[member.id];
        return (
          <label className={cx("club-fee-picker-row", selected && "selected")} key={member.id}>
            <input type="checkbox" checked={selected} onChange={() => onToggle(member.id)} />
            <div className="club-avatar">{initials(member.personName || member.userName || "TV")}</div>
            <span>
              <strong>{member.personName || member.userName || "Thành viên"}</strong>
              <small>{override?.feeItemName || defaultTuition?.name || "Học phí mặc định"} - {formatMoney(override?.amount ?? defaultTuition?.defaultAmount)}</small>
            </span>
          </label>
        );
      })}
    </div>
  );
}

function AssignmentRow({
  assignment,
  busy,
  onUpdate,
  onDelete
}: {
  assignment: MemberFeeAssignmentResponse;
  busy: boolean;
  onUpdate: (body: Partial<MemberFeeAssignmentResponse>) => void;
  onDelete: () => void;
}) {
  const [paidAmount, setPaidAmount] = useState(String(assignment.paidAmount || 0));
  useEffect(() => {
    setPaidAmount(String(assignment.paidAmount || 0));
  }, [assignment.paidAmount]);
  return (
    <article className="club-fee-assignment-row">
      <div>
        <strong>{assignment.memberName || "Thành viên"}</strong>
        <span>{assignment.feeItemName}</span>
        <small>Hạn nộp {formatDate(assignment.dueDate)}{assignment.note ? ` - ${assignment.note}` : ""}</small>
      </div>
      <div className="club-fee-money">
        <b>{formatMoney(assignment.amountDue)}</b>
        <span>Đã nộp {formatMoney(assignment.paidAmount)}</span>
      </div>
      <select disabled={busy} value={assignment.status} onChange={(event) => onUpdate({ status: event.target.value })}>
        {PAYMENT_STATUSES.map((status) => <option key={status} value={status}>{paymentLabel(status)}</option>)}
      </select>
      <input
        type="number"
        min={0}
        value={paidAmount}
        disabled={busy}
        onChange={(event) => setPaidAmount(event.target.value)}
        onBlur={() => onUpdate({ paidAmount: Number(paidAmount || 0) })}
      />
      <button className="danger" disabled={busy} onClick={onDelete} aria-label="Xóa khoản phải thu"><Trash2 size={15} /></button>
    </article>
  );
}

function ExpenseRow({ expense, busy, onEdit, onDelete }: {
  expense: ClubFinanceExpenseResponse;
  busy: boolean;
  onEdit: (expense: ClubFinanceExpenseResponse) => void;
  onDelete: (expense: ClubFinanceExpenseResponse) => void;
}) {
  return (
    <article className="club-fee-assignment-row expense">
      <div>
        <strong>{expense.name}</strong>
        <span>{expenseStatusLabel(expense.status)}</span>
        <small>{formatDate(expense.expenseDate)}{expense.note ? ` - ${expense.note}` : ""}</small>
      </div>
      <div className="club-fee-money">
        <b>{formatMoney(expense.amount)}</b>
        <span>Khoản chi CLB</span>
      </div>
      <Badge tone={expense.status === "DISBURSED" ? "green" : "warm"}>{expenseStatusLabel(expense.status)}</Badge>
      <button disabled={busy} onClick={() => onEdit(expense)} aria-label="Sửa khoản chi"><Edit3 size={15} /></button>
      <button className="danger" disabled={busy} onClick={() => onDelete(expense)} aria-label="Xóa khoản chi"><Trash2 size={15} /></button>
    </article>
  );
}

function ItemDrawer({
  busy,
  editingItem,
  itemForm,
  onClose,
  onSubmit,
  onChange
}: {
  busy: boolean;
  editingItem: ClubFeeItemResponse | null;
  itemForm: ReturnType<typeof emptyItemForm>;
  onClose: () => void;
  onSubmit: (event: FormEvent) => void;
  onChange: (value: ReturnType<typeof emptyItemForm>) => void;
}) {
  const isDefault = itemForm.feeKind === "MONTHLY_TUITION_DEFAULT";
  const isTuition = itemForm.feeKind !== "ONE_TIME_INCOME";
  return (
    <div className="club-drawer-layer">
      <button className="club-drawer-scrim" aria-label="Đóng" onClick={onClose} />
      <motion.aside className="club-drawer club-wide-drawer" initial={{ x: 460 }} animate={{ x: 0 }} exit={{ x: 460 }} transition={{ type: "spring", stiffness: 150, damping: 24 }}>
        <div className="club-drawer-head">
          <div>
            <span className="club-ops-kicker">Các khoản phí</span>
            <h2>{editingItem ? "Sửa khoản phí" : isTuition ? "Tạo học phí ghi đè" : "Tạo khoản thu"}</h2>
          </div>
          <button onClick={onClose} aria-label="Đóng"><X size={20} /></button>
        </div>
        <form className="club-drawer-form" onSubmit={onSubmit}>
          <div className="club-form-grid">
            <label className="club-field"><span>Tên khoản *</span><input value={itemForm.name} onChange={(event) => onChange({ ...itemForm, name: event.target.value })} required /></label>
            <label className="club-field">
              <span>Loại khoản</span>
              <select value={itemForm.feeKind} disabled={isDefault} onChange={(event) => onChange({ ...emptyItemForm(event.target.value as FeeItemKind), name: itemForm.name, defaultAmount: itemForm.defaultAmount })}>
                <option value="MONTHLY_TUITION_DEFAULT">Học phí mặc định</option>
                <option value="MONTHLY_TUITION_OVERRIDE">Học phí ghi đè</option>
                <option value="ONE_TIME_INCOME">Khoản thu một lần</option>
              </select>
            </label>
          </div>
          <div className="club-form-grid">
            {itemForm.feeKind === "ONE_TIME_INCOME" ? (
              <label className="club-field">
                <span>Nhóm khoản thu</span>
                <select value={itemForm.feeType} onChange={(event) => onChange({ ...itemForm, feeType: event.target.value as FeeItemType })}>
                  {ONE_TIME_FEE_TYPES.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
                </select>
              </label>
            ) : (
              <label className="club-field"><span>Chu kỳ</span><input value="Hằng tháng" disabled /></label>
            )}
            <label className="club-field">
              <span>Trạng thái</span>
              <select value={itemForm.status} onChange={(event) => onChange({ ...itemForm, status: event.target.value as FeeItemStatus })}>
                {FEE_ITEM_STATUSES.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
              </select>
            </label>
          </div>
          <div className="club-form-grid">
            <label className="club-field"><span>Số tiền *</span><input type="number" min={0} value={itemForm.defaultAmount} onChange={(event) => onChange({ ...itemForm, defaultAmount: Number(event.target.value) })} required /></label>
            {isTuition ? <label className="club-field"><span>Hạn nộp ngày</span><input type="number" min={1} max={28} value={itemForm.dueDay} onChange={(event) => onChange({ ...itemForm, dueDay: Number(event.target.value) })} /></label> : null}
          </div>
          <label className="club-field"><span>Ghi chú</span><textarea value={itemForm.description} onChange={(event) => onChange({ ...itemForm, description: event.target.value })} rows={3} /></label>
          <button className="club-primary-button" disabled={busy || !itemForm.name.trim()}>{editingItem ? "Lưu khoản phí" : "Tạo khoản phí"}</button>
        </form>
      </motion.aside>
    </div>
  );
}

function ExpenseDrawer({
  busy,
  editingExpense,
  expenseForm,
  onClose,
  onSubmit,
  onChange
}: {
  busy: boolean;
  editingExpense: ClubFinanceExpenseResponse | null;
  expenseForm: ReturnType<typeof emptyExpenseForm>;
  onClose: () => void;
  onSubmit: (event: FormEvent) => void;
  onChange: (value: ReturnType<typeof emptyExpenseForm>) => void;
}) {
  return (
    <div className="club-drawer-layer">
      <button className="club-drawer-scrim" aria-label="Đóng" onClick={onClose} />
      <motion.aside className="club-drawer club-wide-drawer" initial={{ x: 460 }} animate={{ x: 0 }} exit={{ x: 460 }} transition={{ type: "spring", stiffness: 150, damping: 24 }}>
        <div className="club-drawer-head">
          <div>
            <span className="club-ops-kicker">Khoản chi</span>
            <h2>{editingExpense ? "Sửa khoản chi" : "Tạo khoản chi"}</h2>
          </div>
          <button onClick={onClose} aria-label="Đóng"><X size={20} /></button>
        </div>
        <form className="club-drawer-form" onSubmit={onSubmit}>
          <label className="club-field"><span>Tên khoản chi *</span><input value={expenseForm.name} onChange={(event) => onChange({ ...expenseForm, name: event.target.value })} required /></label>
          <div className="club-form-grid">
            <label className="club-field"><span>Số tiền *</span><input type="number" min={0} value={expenseForm.amount} onChange={(event) => onChange({ ...expenseForm, amount: Number(event.target.value) })} required /></label>
            <label className="club-field"><span>Ngày chi</span><input type="date" value={expenseForm.expenseDate} onChange={(event) => onChange({ ...expenseForm, expenseDate: event.target.value })} /></label>
          </div>
          <label className="club-field">
            <span>Trạng thái</span>
            <select value={expenseForm.status} onChange={(event) => onChange({ ...expenseForm, status: event.target.value as ExpenseDisbursementStatus })}>
              {EXPENSE_STATUSES.map((status) => <option key={status.value} value={status.value}>{status.label}</option>)}
            </select>
          </label>
          <label className="club-field"><span>Note</span><textarea value={expenseForm.note} onChange={(event) => onChange({ ...expenseForm, note: event.target.value })} rows={3} /></label>
          <button className="club-primary-button" disabled={busy || !expenseForm.name.trim()}>{editingExpense ? "Lưu khoản chi" : "Tạo khoản chi"}</button>
        </form>
      </motion.aside>
    </div>
  );
}

function FinanceMetric({ icon, label, value, detail }: { icon: ReactNode; label: string; value: string; detail: string }) {
  return (
    <article className="club-finance-metric">
      <div>{icon}</div>
      <span>{label}</span>
      <strong>{value}</strong>
      <small>{detail}</small>
    </article>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return <div className="club-metric"><strong>{value}</strong><span>{label}</span></div>;
}

function Badge({ children, tone }: { children: ReactNode; tone?: "green" | "warm" }) {
  return <span className={cx("club-row-badge", tone)}>{children}</span>;
}

function EmptyState({ title, text }: { title: string; text: string }) {
  return (
    <div className="club-empty-state">
      <div className="club-empty-line" />
      <strong>{title}</strong>
      <p>{text}</p>
    </div>
  );
}

function emptyItemForm(kind: FeeItemKind = "MONTHLY_TUITION_OVERRIDE") {
  return {
    name: kind === "ONE_TIME_INCOME" ? "" : kind === "MONTHLY_TUITION_DEFAULT" ? "Học phí" : "",
    feeKind: kind,
    feeType: "OTHER" as FeeItemType,
    status: "ACTIVE" as FeeItemStatus,
    defaultAmount: kind === "ONE_TIME_INCOME" ? 0 : 250000,
    dueDay: 10,
    description: ""
  };
}

function emptyExpenseForm() {
  return {
    name: "",
    amount: 0,
    expenseDate: todayDate(),
    status: "PENDING_DISBURSEMENT" as ExpenseDisbursementStatus,
    note: ""
  };
}

function formatMoney(value: number | undefined) {
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(Number(value || 0));
}

function kindLabel(value: FeeItemKind) {
  if (value === "MONTHLY_TUITION_DEFAULT") return "Học phí mặc định";
  if (value === "MONTHLY_TUITION_OVERRIDE") return "Học phí ghi đè";
  return "Khoản thu một lần";
}

function expenseStatusLabel(value: ExpenseDisbursementStatus) {
  return EXPENSE_STATUSES.find((item) => item.value === value)?.label || value;
}

function todayDate() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
}
