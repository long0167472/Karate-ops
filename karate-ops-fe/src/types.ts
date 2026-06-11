export type Side = "aka" | "ao";
export type BackendSide = "AKA" | "AO";
export type Mode = "kumite" | "kata";

export interface TimerState {
  durationMs: number;
  remainingMs: number;
  running: boolean;
  startedAt: number | null;
}

export interface Penalties {
  chui: number;
  hansokuChui: boolean;
  hansoku: boolean;
  shikkaku: boolean;
  kiken: boolean;
}

export interface Competitor {
  name: string;
  club: string;
  bib: string;
  kataName: string;
  kataNo: string;
  score: number;
  senshu: boolean;
  penalties: Penalties;
}

export interface KumiteState {
  hantei: boolean;
  lastPoint: null | {
    id: string;
    side: Side;
    points: number;
    label: string;
    at: number;
  };
  suggestion?: null | {
    side: Side | null;
    reason: string;
  };
  vr: Record<Side, { card: boolean; active: boolean; result: "ready" | "active" | "accepted" | "denied" }>;
}

export interface KataState {
  phase: string;
  judgeCount: 5 | 7;
  reveal: boolean;
  votes: Record<string, Side>;
  countdown: TimerState;
  result?: {
    aka: number;
    ao: number;
    needed: number;
    side: Side | null;
    complete: boolean;
  };
}

export interface MatchState {
  schemaVersion: number;
  id: string | null;
  tournamentId: string | null;
  categoryId: string | null;
  tatamiId: string | null;
  mode: Mode;
  tatami: string;
  category: string;
  round: string;
  matchNo: string;
  status: string;
  winner: Side | null;
  winType: string | null;
  nextMatch: string;
  timer: TimerState;
  competitors: Record<Side, Competitor>;
  kumite: KumiteState;
  kata: KataState;
}

export interface EventRecord {
  id: string;
  at: string;
  clientId: string;
  type: string;
  label: string;
  payload: Record<string, unknown>;
}

export interface DeviceRecord {
  id: string;
  role: string;
  name: string;
  connectedAt: number;
  lastSeen: number;
  address: string;
}

export interface StatePayload {
  type: "state";
  serverTime: number;
  receivedAt: number;
  match: MatchState;
  events: EventRecord[];
  devices: DeviceRecord[];
}

export interface ScoreboardAction {
  type: string;
  payload?: Record<string, unknown>;
}

export interface TournamentResponse {
  id: string;
  name: string;
  code?: string;
  description?: string;
  location?: string;
  startsOn?: string;
  endsOn?: string;
  visibility?: string;
  status: string;
  rulesetVersion?: string;
  organizerName?: string;
  tatamiCount?: number;
  competitionLevels?: string[];
  rulesetPreset?: string;
  ruleSnapshotJson?: string;
  ownerOrganizationId?: string;
  ownerOrganizationName?: string;
  createdByUserId?: string;
}

export interface TatamiResponse {
  id: string;
  tournamentId: string;
  tatamiNo: number;
  name: string;
  status: string;
  currentMatchId?: string;
}

export interface MatchParticipantResponse {
  entryId?: string;
  athleteId?: string;
  athleteName?: string;
  teamId?: string;
  delegationName?: string;
  side: BackendSide;
}

export interface KumiteStateResponse {
  akaScore: number;
  aoScore: number;
  akaSenshu: boolean;
  aoSenshu: boolean;
  akaChui: number;
  aoChui: number;
  akaHansokuChui?: boolean;
  aoHansokuChui?: boolean;
  akaHansoku: boolean;
  aoHansoku: boolean;
  akaShikkaku: boolean;
  aoShikkaku: boolean;
  akaKiken: boolean;
  aoKiken: boolean;
  durationMs: number;
  remainingMs: number;
  timerRunning: boolean;
  timerStartedAt?: string;
}

export interface KataVoteResponse {
  judgeNumber: number;
  side: BackendSide;
  voteValue?: number;
}

export interface MatchEventResponse {
  id: string;
  type: string;
  side?: BackendSide;
  points?: number;
  penaltyCode?: string;
  judgeNumber?: number;
  voteSide?: BackendSide;
  occurredAt: string;
}

export interface MatchResponse {
  id: string;
  tournamentId: string;
  categoryId: string;
  categoryName: string;
  tatamiId?: string;
  tatamiNo?: number;
  matchNumber?: number;
  roundName?: string;
  roundNumber?: number;
  bracketPosition?: number;
  status: string;
  scheduledAt?: string;
  mode: "KUMITE" | "KATA" | "TEAM_KUMITE" | "TEAM_KATA";
  winnerEntryId?: string;
  winnerAthleteId?: string;
  winType?: string;
  participants: MatchParticipantResponse[];
  kumite?: KumiteStateResponse;
  kataVotes: KataVoteResponse[];
  recentEvents: MatchEventResponse[];
}

export interface OrganizationResponse {
  id: string;
  name: string;
  shortName?: string;
  code?: string;
  type: string;
  status: string;
  country?: string;
  province?: string;
  address?: string;
  contactEmail?: string;
  contactPhone?: string;
}

export interface PublicClubLookupResponse {
  id: string;
  name: string;
  shortName?: string;
  code?: string;
  province?: string;
  address?: string;
  contactEmail?: string;
  contactPhone?: string;
}

export interface AccountRequestResponse {
  id: string;
  organizationId: string;
  organizationName: string;
  organizationCode?: string;
  displayName: string;
  email: string;
  phone: string;
  gender?: string;
  birthDate?: string;
  currentAddress?: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  decisionNote?: string;
  decidedByUserId?: string;
  approvedUserId?: string;
  decidedAt?: string;
  createdAt: string;
}

export interface ClubMemberResponse {
  id: string;
  organizationId: string;
  organizationName: string;
  personId?: string;
  personName?: string;
  userId?: string;
  userName?: string;
  role: string;
  status: string;
  joinedAt?: string;
  gender?: string;
  phone?: string;
  email?: string;
  currentAddress?: string;
  student?: boolean;
  attendanceViewEnabled?: boolean;
  tuitionStatus?: string;
  tuitionPaidAmount?: number;
  otherFeeStatus?: string;
  otherFeePaidAmount?: number;
  paymentNote?: string;
  memberNote?: string;
}

export interface ClubRosterResponse {
  id: string;
  organizationId: string;
  organizationName: string;
  athleteId: string;
  athleteName: string;
  personId: string;
  status: string;
  joinedAt?: string;
}

export interface AttendanceRecordResponse {
  id: string;
  sessionId: string;
  organizationMemberId?: string;
  athleteId?: string;
  personId?: string;
  displayName?: string;
  status: string;
  checkInAt?: string;
  note?: string;
}

export interface AttendanceSessionResponse {
  id: string;
  organizationId: string;
  organizationName: string;
  tournamentParticipantId?: string;
  name: string;
  type: string;
  status: string;
  scheduledAt?: string;
  source?: string;
  scheduledDate?: string;
  trainingScheduleId?: string;
  notes?: string;
  records: AttendanceRecordResponse[];
}

export interface ClubTrainingScheduleResponse {
  id?: string;
  organizationId: string;
  organizationName: string;
  name: string;
  daysOfWeek: number[];
  startTime: string;
  durationMinutes: number;
  timezone: string;
  active: boolean;
}

export interface PersonResponse {
  id: string;
  displayName: string;
  firstName?: string;
  lastName?: string;
  birthDate?: string;
  gender?: string;
  nationalId?: string;
  email?: string;
  phone?: string;
  currentAddress?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
}

export interface AthleteResponse {
  id: string;
  personId: string;
  displayName: string;
  primaryOrganizationId?: string;
  primaryOrganizationName?: string;
  externalCode?: string;
  belt?: string;
  weightKg?: number;
  heightCm?: number;
  status: string;
}

export interface TournamentParticipantResponse {
  id: string;
  tournamentId: string;
  organizationId: string;
  organizationName: string;
  displayName?: string;
  status: string;
  approvedAt?: string;
}

export interface CategoryResponse {
  id: string;
  tournamentId: string;
  name: string;
  discipline: string;
  gender?: string;
  ageMin?: number;
  ageMax?: number;
  weightMinKg?: number;
  weightMaxKg?: number;
  competitionLevel?: string;
  weightLabel?: string;
  openWeight?: boolean;
  entryType?: string;
  status: string;
  rulesetVersion?: string;
  repechageEnabled?: boolean;
  matchDurationSeconds?: number;
  kataJudgeCount?: number;
  kataRepeatAllowed?: boolean;
  entryLimitPerOrganization?: number;
}

export interface AuthUserResponse {
  id: string;
  displayName: string;
  email: string;
  username?: string;
  phone?: string;
  primaryOrganizationId?: string;
  primaryOrganizationName?: string;
  status: string;
  roles: string[];
}

export interface MemberAccountCreateResponse {
  member?: ClubMemberResponse;
  username?: string;
  temporaryPassword?: string;
}

export interface LeaveRequestResponse {
  id: string;
  sessionId: string;
  sessionName: string;
  organizationId: string;
  organizationName: string;
  memberId: string;
  memberName?: string;
  requesterUserId?: string;
  decidedByUserId?: string;
  requestType: "LEAVE_LONG_TERM" | "LEAVE_SESSION" | "LATE";
  status: "PENDING" | "APPROVED" | "REJECTED" | "EXPIRED_AUTO_ABSENT";
  reason: string;
  fromDate?: string;
  toDate?: string;
  decisionNote?: string;
  decidedAt?: string;
  createdAt: string;
  expiresAt?: string;
}

export interface MemberClubProfileResponse {
  memberships: ClubMemberResponse[];
}

export interface MemberFeeSummaryResponse {
  totalDue: number;
  totalPaid: number;
  totalRemaining: number;
  assignments: MemberFeeAssignmentResponse[];
}

export interface MemberAttendanceSessionResponse {
  id: string;
  organizationId: string;
  organizationName: string;
  name: string;
  type: string;
  status: string;
  scheduledAt?: string;
  scheduledDate?: string;
  record?: AttendanceRecordResponse;
  leaveRequest?: LeaveRequestResponse;
}

export interface MemberAttendanceSummaryResponse {
  sessions: number;
  present: number;
  late: number;
  absent: number;
  excused: number;
  pendingLeaveRequests: number;
  sessionRows: MemberAttendanceSessionResponse[];
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: AuthUserResponse;
}

export interface EntryResponse {
  id: string;
  categoryId: string;
  tournamentParticipantId: string;
  participantName: string;
  athleteId?: string;
  athleteName?: string;
  teamId?: string;
  seedNo?: number;
  status: string;
  registrationWeightKg?: number;
  weighInStatus?: string;
  teamName?: string;
  teamMemberAthleteIds?: string[];
  validationNotes?: string;
}

export interface DashboardOverviewResponse {
  tournamentId: string;
  organizations: number;
  athletes: number;
  categories: number;
  matches: number;
  completedMatches: number;
  matchesByStatus: Record<string, number>;
}

export interface MedalTableRow {
  organizationId: string;
  organizationName: string;
  gold: number;
  silver: number;
  bronze: number;
  total: number;
}

export interface TatamiDashboardRow {
  tatamiId: string;
  tatamiNo: number;
  name: string;
  scheduled: number;
  running: number;
  completed: number;
  currentMatchId?: string;
}

export interface OrganizationDashboardOverviewResponse {
  organizationId: string;
  organizationName: string;
  activeMembers: number;
  activeAthletes: number;
  attendanceSessions: number;
  tournamentEntries: number;
  attendanceRate: number;
}

export interface LowAttendanceAthleteRow {
  athleteId: string;
  athleteName: string;
  sessions: number;
  presentOrLate: number;
  attendanceRate: number;
}

export interface OrganizationAttendanceDashboardResponse {
  organizationId: string;
  from: string;
  to: string;
  sessions: number;
  records: number;
  present: number;
  absent: number;
  late: number;
  excused: number;
  attendanceRate: number;
  lowAttendance: LowAttendanceAthleteRow[];
}

export type FeeItemType = "TUITION" | "UNIFORM" | "EXAM" | "TOURNAMENT" | "OTHER";
export type FeeItemKind = "MONTHLY_TUITION_DEFAULT" | "MONTHLY_TUITION_OVERRIDE" | "ONE_TIME_INCOME";
export type BillingCycle = "ONE_TIME" | "MONTHLY" | "QUARTERLY" | "YEARLY";
export type FeeItemStatus = "DRAFT" | "ACTIVE" | "ARCHIVED";
export type FeeAssignmentSource = "RULE" | "MANUAL";
export type ExpenseDisbursementStatus = "PENDING_DISBURSEMENT" | "DISBURSED";

export interface ClubFeeRoleResponse {
  id: string;
  organizationId: string;
  code: string;
  name: string;
  description?: string;
  priority: number;
  active: boolean;
}

export interface MemberFeeRoleResponse {
  memberId: string;
  roles: ClubFeeRoleResponse[];
}

export interface FeeRoleAmountResponse {
  id: string;
  feeRoleId: string;
  feeRoleName: string;
  amount: number;
  exempt: boolean;
}

export interface ClubFeeItemResponse {
  id: string;
  organizationId: string;
  name: string;
  feeType: FeeItemType;
  feeKind: FeeItemKind;
  billingCycle: BillingCycle;
  status: FeeItemStatus;
  defaultAmount: number;
  dueDay?: number;
  description?: string;
  roleAmounts: FeeRoleAmountResponse[];
}

export interface MemberFeeAssignmentResponse {
  id: string;
  organizationId: string;
  memberId: string;
  memberName?: string;
  feeItemId: string;
  feeItemName: string;
  assignedRoleId?: string;
  assignedRoleName?: string;
  amountDue: number;
  paidAmount: number;
  status: string;
  dueDate?: string;
  source: FeeAssignmentSource;
  note?: string;
}

export interface MemberTuitionOverrideResponse {
  memberId: string;
  memberName?: string;
  feeItemId?: string;
  feeItemName: string;
  amount: number;
}

export interface ClubFinanceExpenseResponse {
  id: string;
  organizationId: string;
  name: string;
  amount: number;
  expenseDate?: string;
  status: ExpenseDisbursementStatus;
  note?: string;
}

export interface ClubFinanceSummaryResponse {
  activeMembers: number;
  monthlyTuitionExpected: number;
  oneTimeIncomeDue: number;
  totalReceivable: number;
  totalPaid: number;
  totalOutstanding: number;
  expensesTotal: number;
  expensesDisbursed: number;
  expensesPending: number;
  netCash: number;
}

export interface ClubFeeOverviewResponse {
  roles: ClubFeeRoleResponse[];
  memberRoles: MemberFeeRoleResponse[];
  feeItems: ClubFeeItemResponse[];
  assignments: MemberFeeAssignmentResponse[];
  tuitionOverrides: MemberTuitionOverrideResponse[];
  expenses: ClubFinanceExpenseResponse[];
  summary: ClubFinanceSummaryResponse;
}
