package com.karate.tournament.service.impl;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.*;
import com.karate.tournament.exception.BusinessConflictException;
import com.karate.tournament.exception.ResourceNotFoundException;

import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.entity.AppUser;
import com.karate.tournament.entity.Organization;
import com.karate.tournament.entity.enums.ParticipantStatus;
import com.karate.tournament.entity.enums.RulesetVersion;
import com.karate.tournament.entity.enums.RulesetPreset;
import com.karate.tournament.entity.Tournament;
import com.karate.tournament.entity.TournamentParticipant;
import com.karate.tournament.entity.enums.TournamentStatus;
import com.karate.tournament.entity.enums.TournamentVisibility;
import com.karate.tournament.repository.AppUserRepository;
import com.karate.tournament.repository.OrganizationRepository;
import com.karate.tournament.repository.TournamentParticipantRepository;
import com.karate.tournament.repository.TournamentRepository;
import com.karate.tournament.web.ApiMapper;
import com.karate.tournament.dto.request.ParticipantCreateRequest;
import com.karate.tournament.dto.request.ParticipantStatusRequest;
import com.karate.tournament.dto.request.TournamentCreateRequest;
import com.karate.tournament.dto.response.TournamentParticipantResponse;
import com.karate.tournament.dto.response.TournamentResponse;
import com.karate.tournament.dto.request.TournamentUpdateRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TournamentServiceImpl implements TournamentService {
  private final TournamentRepository tournaments;
  private final OrganizationRepository organizations;
  private final AppUserRepository users;
  private final TournamentParticipantRepository participants;
  private final PermissionService permissions;
  private final ApiMapper mapper;

  @Transactional(readOnly = true)
  public List<TournamentResponse> list() {
    return tournaments.findByDeletedAtIsNullOrderByStartsOnDescCreatedAtDesc().stream()
        .map(mapper::tournament)
        .toList();
  }

  @Transactional(readOnly = true)
  public TournamentResponse get(UUID id) {
    Tournament tournament = requireTournament(id);
    permissions.requireViewTournament(tournament);
    return mapper.tournament(tournament);
  }

  @Transactional
  public TournamentResponse create(TournamentCreateRequest request) {
    UUID ownerOrganizationId = request.ownerOrganizationId() == null ? permissions.currentActor().primaryOrganizationId() : request.ownerOrganizationId();
    permissions.requireTournamentCreate(ownerOrganizationId);
    Tournament tournament = Tournament.create();
    applyCreateOrUpdate(tournament, request);
    AppUser createdBy = users.findByIdAndDeletedAtIsNull(permissions.currentActor().userId()).orElse(null);
    tournament.createdByUser = createdBy;
    if (tournament.ownerOrganization == null) {
      tournament.ownerOrganization = organizations
          .findByIdAndDeletedAtIsNull(ownerOrganizationId)
          .orElse(null);
    }
    return mapper.tournament(tournaments.save(tournament));
  }

  @Transactional
  public TournamentResponse update(UUID id, TournamentUpdateRequest request) {
    Tournament tournament = requireTournament(id);
    permissions.requireTournamentManage(tournament);
    if (request.name() != null) tournament.name = request.name();
    if (request.code() != null) tournament.code = request.code();
    if (request.description() != null) tournament.description = request.description();
    if (request.location() != null) tournament.location = request.location();
    if (request.startsOn() != null) tournament.startsOn = request.startsOn();
    if (request.endsOn() != null) tournament.endsOn = request.endsOn();
    if (request.visibility() != null) tournament.visibility = request.visibility();
    if (request.status() != null) tournament.status = request.status();
    if (request.rulesetVersion() != null) tournament.rulesetVersion = request.rulesetVersion();
    if (request.organizerName() != null) tournament.organizerName = request.organizerName();
    if (request.tatamiCount() != null) tournament.tatamiCount = Math.max(1, request.tatamiCount());
    if (request.competitionLevels() != null) tournament.competitionLevels = normalizeLevels(request.competitionLevels());
    if (request.rulesetPreset() != null) tournament.rulesetPreset = request.rulesetPreset();
    if (request.ruleSnapshotJson() != null) tournament.ruleSnapshotJson = request.ruleSnapshotJson();
    if (request.ownerOrganizationId() != null) {
      tournament.ownerOrganization = requireOrganization(request.ownerOrganizationId());
    }
    return mapper.tournament(tournament);
  }

  @Transactional
  public void delete(UUID id) {
    Tournament tournament = requireTournament(id);
    permissions.requireTournamentManage(tournament);
    tournament.softDelete();
  }

  @Transactional(readOnly = true)
  public List<TournamentParticipantResponse> listParticipants(UUID tournamentId) {
    Tournament tournament = requireTournament(tournamentId);
    permissions.requireViewTournament(tournament);
    return participants.findByTournament_IdAndDeletedAtIsNullOrderByDisplayNameAsc(tournamentId)
        .stream()
        .map(mapper::participant)
        .toList();
  }

  @Transactional
  public TournamentParticipantResponse addParticipant(UUID tournamentId, ParticipantCreateRequest request) {
    Tournament tournament = requireTournament(tournamentId);
    permissions.requireTournamentManage(tournament);
    Organization organization = requireOrganization(request.organizationId());
    participants.findByTournament_IdAndOrganization_IdAndDeletedAtIsNull(tournamentId, organization.id)
        .ifPresent(existing -> {
          throw new BusinessConflictException("Organization already participates in tournament");
        });
    TournamentParticipant participant = TournamentParticipant.create();
    participant.tournament = tournament;
    participant.organization = organization;
    String displayName = request.displayName() == null || request.displayName().isBlank()
        ? organization.name
        : request.displayName();
    participant.displayName = displayName == null || displayName.isBlank() ? request.organizationId().toString() : displayName;
    participant.status = request.status() == null ? ParticipantStatus.REQUESTED : request.status();
    if (participant.status == ParticipantStatus.APPROVED) {
      participant.approvedAt = Instant.now();
    }
    return mapper.participant(participants.save(participant));
  }

  @Transactional
  public TournamentParticipantResponse updateParticipantStatus(UUID tournamentId, UUID participantId, ParticipantStatusRequest request) {
    Tournament tournament = requireTournament(tournamentId);
    permissions.requireTournamentManage(tournament);
    // Pessimistic lock: concurrent status transitions on the same delegation serialize.
    TournamentParticipant participant = participants.findWithLockById(participantId)
        .orElseThrow(() -> new ResourceNotFoundException("Tournament participant not found: " + participantId));
    if (!participant.tournament.id.equals(tournamentId)) {
      throw new ResourceNotFoundException("Participant does not belong to tournament");
    }
    participant.status = request.status();
    participant.approvedAt = request.status() == ParticipantStatus.APPROVED ? Instant.now() : participant.approvedAt;
    return mapper.participant(participant);
  }

  public Tournament requireTournament(UUID id) {
    return tournaments.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new ResourceNotFoundException("Tournament not found: " + id));
  }

  private void applyCreateOrUpdate(Tournament tournament, TournamentCreateRequest request) {
    tournament.name = request.name();
    tournament.code = request.code();
    tournament.description = request.description();
    tournament.location = request.location();
    tournament.startsOn = request.startsOn();
    tournament.endsOn = request.endsOn();
    tournament.visibility = request.visibility() == null ? TournamentVisibility.PRIVATE : request.visibility();
    tournament.status = request.status() == null ? TournamentStatus.DRAFT : request.status();
    tournament.rulesetVersion = request.rulesetVersion() == null ? RulesetVersion.WKF_2026 : request.rulesetVersion();
    tournament.organizerName = request.organizerName();
    tournament.tatamiCount = request.tatamiCount() == null ? 1 : Math.max(1, request.tatamiCount());
    tournament.competitionLevels = normalizeLevels(request.competitionLevels());
    tournament.rulesetPreset = request.rulesetPreset() == null ? RulesetPreset.WKF : request.rulesetPreset();
    tournament.ruleSnapshotJson = request.ruleSnapshotJson() == null ? defaultRuleSnapshot(tournament.rulesetPreset) : request.ruleSnapshotJson();
    if (request.ownerOrganizationId() != null) {
      tournament.ownerOrganization = requireOrganization(request.ownerOrganizationId());
    }
  }

  private String normalizeLevels(List<String> levels) {
    if (levels == null || levels.isEmpty()) {
      return "PHONG_TRAO,NANG_CAO";
    }
    String normalized = levels.stream()
        .filter(level -> level != null && !level.isBlank())
        .map(level -> level.trim().toUpperCase())
        .distinct()
        .reduce((left, right) -> left + "," + right)
        .orElse("PHONG_TRAO,NANG_CAO");
    return normalized.isBlank() ? "PHONG_TRAO,NANG_CAO" : normalized;
  }

  private String defaultRuleSnapshot(RulesetPreset preset) {
    return switch (preset) {
      case PHONG_TRAO -> "{\"preset\":\"PHONG_TRAO\",\"kumiteDurationSeconds\":120,\"kataJudgeCount\":5,\"repechage\":false}";
      case NANG_CAO -> "{\"preset\":\"NANG_CAO\",\"kumiteDurationSeconds\":180,\"kataJudgeCount\":5,\"repechage\":true}";
      case CUSTOM -> "{\"preset\":\"CUSTOM\"}";
      case WKF -> "{\"preset\":\"WKF\",\"kumiteDurationSeconds\":180,\"kataJudgeCount\":5,\"repechage\":true,\"kumiteScoring\":{\"yuko\":1,\"wazaAri\":2,\"ippon\":3}}";
    };
  }

  private Organization requireOrganization(UUID id) {
    return organizations.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
  }
}
