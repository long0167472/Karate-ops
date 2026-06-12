package com.karate.tournament.service;

import com.karate.tournament.dto.request.AthleteApprovalRequest;
import com.karate.tournament.dto.request.BulkAthleteApprovalRequest;
import com.karate.tournament.dto.request.ParticipantApprovalRequest;
import com.karate.tournament.dto.response.AthleteApprovalItemResponse;
import com.karate.tournament.dto.response.AthleteApprovalSummaryResponse;
import com.karate.tournament.dto.response.EntryResponse;
import com.karate.tournament.dto.response.ParticipantApprovalItemResponse;
import com.karate.tournament.dto.response.TournamentParticipantResponse;

import java.util.List;
import java.util.UUID;

public interface TournamentApprovalService {

  List<ParticipantApprovalItemResponse> listParticipantsForApproval(UUID tournamentId, String status);

  TournamentParticipantResponse approveParticipant(UUID tournamentId, UUID participantId, ParticipantApprovalRequest req);

  List<AthleteApprovalItemResponse> listEntriesForApproval(UUID tournamentId, String btcStatus, UUID participantId, UUID categoryId);

  EntryResponse approveEntry(UUID tournamentId, UUID entryId, AthleteApprovalRequest req);

  AthleteApprovalSummaryResponse bulkApproveEntries(UUID tournamentId, BulkAthleteApprovalRequest req);

  AthleteApprovalSummaryResponse getEntrySummary(UUID tournamentId);
}
