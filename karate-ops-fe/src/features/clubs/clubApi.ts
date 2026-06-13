import type {
  AccountRequestResponse,
  AthleteResponse,
  AttendanceRecordResponse,
  AttendanceSessionResponse,
  BeltExamCandidateResponse,
  BeltExamResponse,
  ClubFeeOverviewResponse,
  ClubMemberResponse,
  ClubRosterResponse,
  ClubTrainingScheduleResponse,
  OrganizationAttendanceDashboardResponse,
  OrganizationDashboardOverviewResponse,
  OrganizationResponse
} from "../../types";
import { apiDelete, apiGet, apiPatch, apiPost } from "../../apiClient";

export interface ClubDirectoryData {
  clubs: OrganizationResponse[];
  dashboards: Record<string, OrganizationDashboardOverviewResponse>;
}

export interface ClubWorkspaceData {
  overview: OrganizationDashboardOverviewResponse;
  attendance: OrganizationAttendanceDashboardResponse;
  accountRequests: AccountRequestResponse[];
  members: ClubMemberResponse[];
  roster: ClubRosterResponse[];
  sessions: AttendanceSessionResponse[];
  schedule: ClubTrainingScheduleResponse;
  athletes: AthleteResponse[];
  finance: ClubFeeOverviewResponse;
  beltExams: BeltExamResponse[];
}

export async function fetchClubDirectory(isAdmin: boolean, primaryOrganizationId?: string): Promise<ClubDirectoryData> {
  const organizations = await apiGet<OrganizationResponse[]>("/api/organizations");
  const clubs = organizations.filter((org) => org.type === "CLUB" && (isAdmin || org.id === primaryOrganizationId));
  const results = await Promise.allSettled(
    clubs.map((org) => apiGet<OrganizationDashboardOverviewResponse>(`/api/dashboard/organizations/${org.id}/overview`))
  );
  const dashboards = Object.fromEntries(results.flatMap((result, index) => result.status === "fulfilled" ? [[clubs[index].id, result.value]] : []));
  return { clubs, dashboards };
}

export async function fetchClubWorkspace(id: string): Promise<ClubWorkspaceData> {
  const [overview, attendance, accountRequests, members, roster, sessions, schedule, athletes, finance, beltExams] = await Promise.all([
    apiGet<OrganizationDashboardOverviewResponse>(`/api/dashboard/organizations/${id}/overview`),
    apiGet<OrganizationAttendanceDashboardResponse>(`/api/dashboard/organizations/${id}/attendance`),
    apiGet<AccountRequestResponse[]>(`/api/organizations/${id}/account-requests`),
    apiGet<ClubMemberResponse[]>(`/api/organizations/${id}/members`),
    apiGet<ClubRosterResponse[]>(`/api/organizations/${id}/roster`),
    apiGet<AttendanceSessionResponse[]>(`/api/organizations/${id}/attendance-sessions`),
    apiGet<ClubTrainingScheduleResponse>(`/api/organizations/${id}/training-schedule`),
    apiGet<AthleteResponse[]>("/api/athletes"),
    apiGet<ClubFeeOverviewResponse>(`/api/organizations/${id}/finance/overview`),
    apiGet<BeltExamResponse[]>(`/api/organizations/${id}/belt-exams`)
  ]);
  return {
    overview,
    attendance,
    accountRequests,
    members,
    roster,
    sessions,
    schedule,
    athletes: athletes.filter((athlete) => athlete.primaryOrganizationId === id),
    finance,
    beltExams
  };
}

export async function createBeltExam(orgId: string, body: { name: string; status?: string; examDate?: string; location?: string; examinerName?: string; notes?: string }) {
  return apiPost<BeltExamResponse>(`/api/organizations/${orgId}/belt-exams`, body);
}

export async function updateBeltExam(examId: string, body: Partial<{ name: string; status: string; examDate: string; location: string; examinerName: string; notes: string }>) {
  return apiPatch<BeltExamResponse>(`/api/belt-exams/${examId}`, body);
}

export async function deleteBeltExam(examId: string) {
  return apiDelete(`/api/belt-exams/${examId}`);
}

export async function addBeltExamCandidate(examId: string, body: { athleteId?: string; organizationMemberId?: string; currentBelt?: string; targetBelt: string }) {
  return apiPost<BeltExamCandidateResponse>(`/api/belt-exams/${examId}/candidates`, body);
}

export async function updateBeltExamCandidate(examId: string, candidateId: string, body: { result?: string; examinerNote?: string }) {
  return apiPatch<BeltExamCandidateResponse>(`/api/belt-exams/${examId}/candidates/${candidateId}`, body);
}

export async function removeBeltExamCandidate(examId: string, candidateId: string) {
  return apiDelete(`/api/belt-exams/${examId}/candidates/${candidateId}`);
}

export async function applyBeltExamResults(examId: string) {
  return apiPost<BeltExamResponse>(`/api/belt-exams/${examId}/apply-results`, {});
}

export async function saveAttendanceRecord(sessionId: string, athleteId: string, record: AttendanceRecordResponse | undefined, status: string) {
  const body = { athleteId, status, checkInAt: status === "PRESENT" || status === "LATE" ? new Date().toISOString() : undefined };
  if (record) return apiPatch<AttendanceRecordResponse>(`/api/attendance-sessions/${sessionId}/records/${record.id}`, body);
  return apiPost<AttendanceRecordResponse>(`/api/attendance-sessions/${sessionId}/records`, body);
}

export async function saveAttendanceRecords(
  sessionId: string,
  rows: Array<{ athleteId: string; record?: AttendanceRecordResponse; status: string }>
) {
  return Promise.all(rows.map((row) => saveAttendanceRecord(sessionId, row.athleteId, row.record, row.status)));
}

export function upsertAttendanceRecord(records: AttendanceRecordResponse[], nextRecord: AttendanceRecordResponse) {
  const index = records.findIndex((record) => record.id === nextRecord.id || (nextRecord.athleteId && record.athleteId === nextRecord.athleteId));
  if (index < 0) return [...records, nextRecord];
  return records.map((record, currentIndex) => currentIndex === index ? nextRecord : record);
}
