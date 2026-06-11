package com.karate.tournament.web;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.AttendanceService;
import com.karate.tournament.dto.request.AttendanceRecordRequest;
import com.karate.tournament.dto.response.AttendanceRecordResponse;
import com.karate.tournament.dto.request.AttendanceRecordUpdateRequest;
import com.karate.tournament.dto.request.AttendanceSessionCreateRequest;
import com.karate.tournament.dto.response.AttendanceSessionResponse;
import com.karate.tournament.dto.request.AttendanceSessionUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AttendanceController {
  private final AttendanceService attendance;

  @GetMapping("/organizations/{organizationId}/attendance-sessions")
  public List<AttendanceSessionResponse> listSessions(@PathVariable UUID organizationId) {
    return attendance.list(organizationId);
  }

  @PostMapping("/organizations/{organizationId}/attendance-sessions")
  @ResponseStatus(HttpStatus.CREATED)
  public AttendanceSessionResponse createSession(
      @PathVariable UUID organizationId,
      @Valid @RequestBody AttendanceSessionCreateRequest request
  ) {
    return attendance.create(organizationId, request);
  }

  @GetMapping("/attendance-sessions/{sessionId}")
  public AttendanceSessionResponse getSession(@PathVariable UUID sessionId) {
    return attendance.get(sessionId);
  }

  @PatchMapping("/attendance-sessions/{sessionId}")
  public AttendanceSessionResponse updateSession(
      @PathVariable UUID sessionId,
      @Valid @RequestBody AttendanceSessionUpdateRequest request
  ) {
    return attendance.update(sessionId, request);
  }

  @DeleteMapping("/attendance-sessions/{sessionId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteSession(@PathVariable UUID sessionId) {
    attendance.deleteManualSession(sessionId);
  }

  @PostMapping("/attendance-sessions/{sessionId}/records")
  @ResponseStatus(HttpStatus.CREATED)
  public AttendanceRecordResponse record(
      @PathVariable UUID sessionId,
      @Valid @RequestBody AttendanceRecordRequest request
  ) {
    return attendance.record(sessionId, request);
  }

  @PatchMapping("/attendance-sessions/{sessionId}/records/{recordId}")
  public AttendanceRecordResponse updateRecord(
      @PathVariable UUID sessionId,
      @PathVariable UUID recordId,
      @Valid @RequestBody AttendanceRecordUpdateRequest request
  ) {
    return attendance.updateRecord(sessionId, recordId, request);
  }
}
