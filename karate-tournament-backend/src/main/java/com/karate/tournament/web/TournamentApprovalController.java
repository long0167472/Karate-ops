package com.karate.tournament.web;

import com.karate.tournament.dto.request.AthleteApprovalRequest;
import com.karate.tournament.dto.request.BulkAthleteApprovalRequest;
import com.karate.tournament.dto.request.ParticipantApprovalRequest;
import com.karate.tournament.dto.response.AthleteApprovalItemResponse;
import com.karate.tournament.dto.response.AthleteApprovalSummaryResponse;
import com.karate.tournament.dto.response.EntryResponse;
import com.karate.tournament.dto.response.ParticipantApprovalItemResponse;
import com.karate.tournament.dto.response.TournamentParticipantResponse;
import com.karate.tournament.service.TournamentApprovalService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/approval")
@RequiredArgsConstructor
public class TournamentApprovalController {

  private final TournamentApprovalService approvalService;

  @GetMapping("/clubs")
  public List<ParticipantApprovalItemResponse> listParticipantsForApproval(
      @PathVariable UUID tournamentId,
      @RequestParam(required = false) String status
  ) {
    return approvalService.listParticipantsForApproval(tournamentId, status);
  }

  @PatchMapping("/clubs/{participantId}")
  public TournamentParticipantResponse approveParticipant(
      @PathVariable UUID tournamentId,
      @PathVariable UUID participantId,
      @RequestBody ParticipantApprovalRequest request
  ) {
    return approvalService.approveParticipant(tournamentId, participantId, request);
  }

  @GetMapping("/athletes")
  public List<AthleteApprovalItemResponse> listEntriesForApproval(
      @PathVariable UUID tournamentId,
      @RequestParam(required = false) String btcStatus,
      @RequestParam(required = false) UUID participantId,
      @RequestParam(required = false) UUID categoryId
  ) {
    return approvalService.listEntriesForApproval(tournamentId, btcStatus, participantId, categoryId);
  }

  @PatchMapping("/athletes/{entryId}")
  public EntryResponse approveEntry(
      @PathVariable UUID tournamentId,
      @PathVariable UUID entryId,
      @RequestBody AthleteApprovalRequest request
  ) {
    return approvalService.approveEntry(tournamentId, entryId, request);
  }

  @PostMapping("/athletes/bulk")
  public AthleteApprovalSummaryResponse bulkApproveEntries(
      @PathVariable UUID tournamentId,
      @RequestBody BulkAthleteApprovalRequest request
  ) {
    return approvalService.bulkApproveEntries(tournamentId, request);
  }

  @GetMapping("/athletes/summary")
  public AthleteApprovalSummaryResponse getEntrySummary(
      @PathVariable UUID tournamentId
  ) {
    return approvalService.getEntrySummary(tournamentId);
  }
}
