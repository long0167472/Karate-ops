package com.karate.tournament.service.impl;

import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.dto.request.AthleteApprovalRequest;
import com.karate.tournament.dto.request.BulkAthleteApprovalRequest;
import com.karate.tournament.dto.request.ParticipantApprovalRequest;
import com.karate.tournament.dto.response.AthleteApprovalItemResponse;
import com.karate.tournament.dto.response.AthleteApprovalSummaryResponse;
import com.karate.tournament.dto.response.EntryResponse;
import com.karate.tournament.dto.response.ParticipantApprovalItemResponse;
import com.karate.tournament.dto.response.TournamentParticipantResponse;
import com.karate.tournament.entity.AppUser;
import com.karate.tournament.entity.Entry;
import com.karate.tournament.entity.Tournament;
import com.karate.tournament.entity.TournamentParticipant;
import com.karate.tournament.entity.enums.BtcApprovalStatus;
import com.karate.tournament.entity.enums.ParticipantStatus;
import com.karate.tournament.exception.BadRequestException;
import com.karate.tournament.exception.BusinessConflictException;
import com.karate.tournament.exception.ResourceNotFoundException;
import com.karate.tournament.repository.AppUserRepository;
import com.karate.tournament.repository.EntryRepository;
import com.karate.tournament.repository.TournamentParticipantRepository;
import com.karate.tournament.repository.TournamentRepository;
import com.karate.tournament.service.TournamentApprovalService;
import com.karate.tournament.web.ApiMapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TournamentApprovalServiceImpl implements TournamentApprovalService {

  private final TournamentRepository tournaments;
  private final TournamentParticipantRepository participants;
  private final EntryRepository entries;
  private final AppUserRepository users;
  private final PermissionService permissions;
  private final ApiMapper mapper;

  @Transactional(readOnly = true)
  @Override
  public List<ParticipantApprovalItemResponse> listParticipantsForApproval(UUID tournamentId, String status) {
    Tournament tournament = requireTournament(tournamentId);
    permissions.requireTournamentManage(tournament);

    List<TournamentParticipant> all =
        participants.findByTournament_IdAndDeletedAtIsNullOrderByDisplayNameAsc(tournamentId);

    if (status != null && !status.isBlank()) {
      ParticipantStatus filterStatus = ParticipantStatus.valueOf(status.toUpperCase());
      all = all.stream()
          .filter(p -> p.status == filterStatus)
          .toList();
    }

    List<Entry> tournamentEntries = entries.findByTournament(tournamentId);

    return all.stream()
        .map(p -> {
          int total = (int) tournamentEntries.stream()
              .filter(e -> e.tournamentParticipant != null && e.tournamentParticipant.id.equals(p.id))
              .count();
          int approved = (int) tournamentEntries.stream()
              .filter(e -> e.tournamentParticipant != null && e.tournamentParticipant.id.equals(p.id))
              .filter(e -> e.btcApprovalStatus == BtcApprovalStatus.APPROVED)
              .count();
          return new ParticipantApprovalItemResponse(
              p.id,
              p.organization == null ? null : p.organization.id,
              p.organization == null ? null : p.organization.name,
              p.displayName,
              p.status == null ? null : p.status.name(),
              approved,
              total
          );
        })
        .toList();
  }

  @Override
  public TournamentParticipantResponse approveParticipant(UUID tournamentId, UUID participantId, ParticipantApprovalRequest req) {
    Tournament tournament = requireTournament(tournamentId);
    permissions.requireTournamentManage(tournament);

    TournamentParticipant participant = participants.findByIdAndDeletedAtIsNull(participantId)
        .orElseThrow(() -> new ResourceNotFoundException("Tournament participant not found: " + participantId));
    if (!participant.tournament.id.equals(tournamentId)) {
      throw new ResourceNotFoundException("Participant does not belong to tournament");
    }

    String action = req.action();
    if (action == null || action.isBlank()) {
      throw new BadRequestException("action is required");
    }

    switch (action.toUpperCase()) {
      case "APPROVE" -> {
        participant.status = ParticipantStatus.APPROVED;
        participant.approvedAt = Instant.now();
        AppUser currentUser = users.findByIdAndDeletedAtIsNull(permissions.currentActor().userId()).orElse(null);
        participant.approvedBy = currentUser;
      }
      case "INACTIVE" -> {
        participant.status = ParticipantStatus.INACTIVE;
        participant.inactivatedAt = Instant.now();
      }
      default -> throw new BadRequestException("Unknown action: " + action + ". Expected APPROVE or INACTIVE");
    }

    return mapper.participant(participant);
  }

  @Transactional(readOnly = true)
  @Override
  public List<AthleteApprovalItemResponse> listEntriesForApproval(UUID tournamentId, String btcStatus, UUID participantId, UUID categoryId) {
    Tournament tournament = requireTournament(tournamentId);
    permissions.requireTournamentManage(tournament);

    List<Entry> all = entries.findByTournament(tournamentId);

    if (btcStatus != null && !btcStatus.isBlank()) {
      BtcApprovalStatus filterStatus = BtcApprovalStatus.valueOf(btcStatus.toUpperCase());
      all = all.stream()
          .filter(e -> e.btcApprovalStatus == filterStatus)
          .toList();
    }
    if (participantId != null) {
      all = all.stream()
          .filter(e -> e.tournamentParticipant.id.equals(participantId))
          .toList();
    }
    if (categoryId != null) {
      all = all.stream()
          .filter(e -> e.category.id.equals(categoryId))
          .toList();
    }

    return all.stream()
        .map(this::toApprovalItem)
        .toList();
  }

  private AthleteApprovalItemResponse toApprovalItem(Entry e) {
    TournamentParticipant tp = e.tournamentParticipant;
    return new AthleteApprovalItemResponse(
        e.id,
        e.athlete == null ? null : e.athlete.id,
        e.athlete == null ? null : e.athlete.person.displayName,
        tp == null || tp.organization == null ? null : tp.organization.id,
        tp == null || tp.organization == null ? null : tp.organization.name,
        e.category == null ? null : e.category.id,
        e.category == null ? null : e.category.name,
        e.registrationWeightKg,
        e.btcApprovalStatus == null ? null : e.btcApprovalStatus.name()
    );
  }

  @Override
  public EntryResponse approveEntry(UUID tournamentId, UUID entryId, AthleteApprovalRequest req) {
    Tournament tournament = requireTournament(tournamentId);
    permissions.requireTournamentManage(tournament);

    if (tournament.step >= 3) {
      throw new BusinessConflictException("Cannot modify entry approval after tournament step 3");
    }

    Entry entry = entries.findByIdAndDeletedAtIsNull(entryId)
        .orElseThrow(() -> new ResourceNotFoundException("Entry not found: " + entryId));
    if (!entry.category.tournament.id.equals(tournamentId)) {
      throw new ResourceNotFoundException("Entry does not belong to tournament");
    }

    String action = req.action();
    if (action == null || action.isBlank()) {
      throw new BadRequestException("action is required");
    }

    switch (action.toUpperCase()) {
      case "APPROVE" -> entry.btcApprovalStatus = BtcApprovalStatus.APPROVED;
      case "REJECT" -> entry.btcApprovalStatus = BtcApprovalStatus.REJECTED;
      default -> throw new BadRequestException("Unknown action: " + action + ". Expected APPROVE or REJECT");
    }

    return mapper.entry(entry);
  }

  @Override
  public AthleteApprovalSummaryResponse bulkApproveEntries(UUID tournamentId, BulkAthleteApprovalRequest req) {
    Tournament tournament = requireTournament(tournamentId);
    permissions.requireTournamentManage(tournament);

    if (tournament.step >= 3) {
      throw new BusinessConflictException("Cannot modify entry approval after tournament step 3");
    }

    List<Entry> targets;

    if (Boolean.TRUE.equals(req.approveAll())) {
      targets = entries.findByTournament(tournamentId).stream()
          .filter(e -> e.btcApprovalStatus == BtcApprovalStatus.PENDING)
          .toList();
    } else if (req.participantId() != null) {
      targets = entries.findByTournament(tournamentId).stream()
          .filter(e -> e.tournamentParticipant.id.equals(req.participantId()))
          .filter(e -> e.btcApprovalStatus == BtcApprovalStatus.PENDING)
          .toList();
    } else if (req.entryIds() != null && !req.entryIds().isEmpty()) {
      targets = entries.findByTournament(tournamentId).stream()
          .filter(e -> req.entryIds().contains(e.id))
          .toList();
    } else {
      targets = List.of();
    }

    for (Entry e : targets) {
      e.btcApprovalStatus = BtcApprovalStatus.APPROVED;
    }

    return buildSummary(tournamentId);
  }

  @Transactional(readOnly = true)
  @Override
  public AthleteApprovalSummaryResponse getEntrySummary(UUID tournamentId) {
    Tournament tournament = requireTournament(tournamentId);
    permissions.requireTournamentManage(tournament);
    return buildSummary(tournamentId);
  }

  private AthleteApprovalSummaryResponse buildSummary(UUID tournamentId) {
    List<Entry> all = entries.findByTournament(tournamentId);
    int approved = (int) all.stream().filter(e -> e.btcApprovalStatus == BtcApprovalStatus.APPROVED).count();
    int rejected = (int) all.stream().filter(e -> e.btcApprovalStatus == BtcApprovalStatus.REJECTED).count();
    int pending = (int) all.stream().filter(e -> e.btcApprovalStatus == BtcApprovalStatus.PENDING).count();
    return new AthleteApprovalSummaryResponse(all.size(), approved, rejected, pending);
  }

  private Tournament requireTournament(UUID id) {
    return tournaments.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new ResourceNotFoundException("Tournament not found: " + id));
  }
}
