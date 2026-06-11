package com.karate.tournament.service;

import com.karate.tournament.entity.AttendanceSession;
import com.karate.tournament.dto.request.AttendanceRecordRequest;
import com.karate.tournament.dto.response.AttendanceRecordResponse;
import com.karate.tournament.dto.request.AttendanceRecordUpdateRequest;
import com.karate.tournament.dto.request.AttendanceSessionCreateRequest;
import com.karate.tournament.dto.response.AttendanceSessionResponse;
import com.karate.tournament.dto.request.AttendanceSessionUpdateRequest;
import java.util.List;
import java.util.UUID;

public interface AttendanceService {
  List<AttendanceSessionResponse> list(UUID organizationId);
  AttendanceSessionResponse get(UUID sessionId);
  AttendanceSessionResponse create(UUID organizationId, AttendanceSessionCreateRequest request);
  AttendanceSessionResponse update(UUID sessionId, AttendanceSessionUpdateRequest request);
  void deleteManualSession(UUID sessionId);
  AttendanceRecordResponse record(UUID sessionId, AttendanceRecordRequest request);
  AttendanceRecordResponse updateRecord(UUID sessionId, UUID recordId, AttendanceRecordUpdateRequest request);
  AttendanceSession requireSession(UUID sessionId);
}
