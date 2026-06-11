import type {
  AccountRequestResponse,
  AthleteResponse,
  AttendanceRecordResponse,
  AttendanceSessionResponse,
  ClubFeeOverviewResponse,
  ClubMemberResponse,
  ClubRosterResponse,
  ClubTrainingScheduleResponse,
  OrganizationAttendanceDashboardResponse,
  OrganizationDashboardOverviewResponse,
  OrganizationResponse
} from "../../types";
import { apiGet, apiPatch, apiPost } from "../../apiClient";

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
  const [overview, attendance, accountRequests, members, roster, sessions, schedule, athletes, finance] = await Promise.all([
    apiGet<OrganizationDashboardOverviewResponse>(`/api/dashboard/organizations/${id}/overview`),
    apiGet<OrganizationAttendanceDashboardResponse>(`/api/dashboard/organizations/${id}/attendance`),
    apiGet<AccountRequestResponse[]>(`/api/organizations/${id}/account-requests`),
    apiGet<ClubMemberResponse[]>(`/api/organizations/${id}/members`),
    apiGet<ClubRosterResponse[]>(`/api/organizations/${id}/roster`),
    apiGet<AttendanceSessionResponse[]>(`/api/organizations/${id}/attendance-sessions`),
    apiGet<ClubTrainingScheduleResponse>(`/api/organizations/${id}/training-schedule`),
    apiGet<AthleteResponse[]>("/api/athletes"),
    apiGet<ClubFeeOverviewResponse>(`/api/organizations/${id}/finance/overview`)
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
    finance
  };
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
