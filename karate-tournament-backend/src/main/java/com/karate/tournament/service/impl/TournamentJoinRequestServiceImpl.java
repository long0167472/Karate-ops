package com.karate.tournament.service.impl;

import com.karate.tournament.auth.CurrentActor;
import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.dto.request.TournamentJoinRequestCreateRequest;
import com.karate.tournament.dto.response.TournamentJoinRequestResponse;
import com.karate.tournament.entity.Athlete;
import com.karate.tournament.entity.ClubRoster;
import com.karate.tournament.entity.OrganizationMember;
import com.karate.tournament.entity.Tournament;
import com.karate.tournament.entity.TournamentJoinRequest;
import com.karate.tournament.entity.TournamentParticipant;
import com.karate.tournament.entity.enums.AthleteStatus;
import com.karate.tournament.entity.enums.ClubRosterStatus;
import com.karate.tournament.entity.enums.ParticipantStatus;
import com.karate.tournament.entity.enums.TournamentJoinRequestStatus;
import com.karate.tournament.entity.enums.TournamentStatus;
import com.karate.tournament.exception.BadRequestException;
import com.karate.tournament.exception.BusinessConflictException;
import com.karate.tournament.exception.ResourceNotFoundException;
import com.karate.tournament.repository.AppUserRepository;
import com.karate.tournament.repository.AthleteRepository;
import com.karate.tournament.repository.ClubRosterRepository;
import com.karate.tournament.repository.OrganizationMemberRepository;
import com.karate.tournament.repository.TournamentJoinRequestRepository;
import com.karate.tournament.repository.TournamentParticipantRepository;
import com.karate.tournament.repository.TournamentRepository;
import com.karate.tournament.service.TournamentJoinRequestService;
import com.karate.tournament.web.ApiMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TournamentJoinRequestServiceImpl implements TournamentJoinRequestService {
  private final TournamentJoinRequestRepository joinRequests;
  private final TournamentRepository tournaments;
  private final OrganizationMemberRepository members;
  private final AthleteRepository athletes;
  private final ClubRosterRepository roster;
  private final TournamentParticipantRepository participants;
  private final AppUserRepository users;
  private final PermissionService permissions;
  private final ApiMapper mapper;

  @Transactional
  public TournamentJoinRequestResponse createForCurrentUser(TournamentJoinRequestCreateRequest request) {
    CurrentActor actor = permissions.currentActor();
    permissions.requireMemberSelfView(actor.userId());
    Tournament tournament = tournaments.findByIdAndDeletedAtIsNull(request.tournamentId())
        .orElseThrow(() -> new ResourceNotFoundException("Tournament not found: " + request.tournamentId()));
    if (tournament.status != TournamentStatus.REGISTRATION_OPEN) {
      throw new BusinessConflictException("Tournament is not open for registration");
    }
    OrganizationMember member = resolveMembership(actor.userId(), request.organizationId());
    // Pessimistic lock on the existing row: re-submission after a rejection cannot race a
    // concurrent manager decision; brand-new duplicates are blocked by the unique index.
    TournamentJoinRequest joinRequest = joinRequests.findWithLockByTournamentAndMember(tournament.id, member.id)
        .orElseGet(TournamentJoinRequest::create);
    if (joinRequest.id != null && joinRequest.status != TournamentJoinRequestStatus.REJECTED) {
      throw new BusinessConflictException("A join request for this tournament already exists");
    }
    joinRequest.tournament = tournament;
    joinRequest.organization = member.organization;
    joinRequest.member = member;
    joinRequest.requesterUser = users.findByIdAndDeletedAtIsNull(actor.userId()).orElse(null);
    joinRequest.status = TournamentJoinRequestStatus.PENDING;
    joinRequest.note = request.note() == null ? null : request.note().trim();
    joinRequest.decisionNote = null;
    joinRequest.decidedAt = null;
    joinRequest.decidedByUser = null;
    try {
      return mapper.joinRequest(joinRequests.saveAndFlush(joinRequest));
    } catch (DataIntegrityViolationException ex) {
      throw new BusinessConflictException("A join request for this tournament already exists");
    }
  }

  @Transactional(readOnly = true)
  public List<TournamentJoinRequestResponse> listForCurrentUser() {
    CurrentActor actor = permissions.currentActor();
    permissions.requireMemberSelfView(actor.userId());
    List<UUID> memberIds = members.findByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(actor.userId())
        .stream()
        .map(member -> member.id)
        .toList();
    if (memberIds.isEmpty()) {
      return List.of();
    }
    return joinRequests.findByMember_IdInAndDeletedAtIsNullOrderByCreatedAtDesc(memberIds)
        .stream()
        .map(mapper::joinRequest)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<TournamentJoinRequestResponse> listByOrganization(UUID organizationId) {
    permissions.requireClubView(organizationId);
    return joinRequests.findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId)
        .stream()
        .map(mapper::joinRequest)
        .toList();
  }

  @Transactional
  public TournamentJoinRequestResponse decide(UUID organizationId, UUID requestId, TournamentJoinRequestStatus status, String decisionNote) {
    if (status != TournamentJoinRequestStatus.APPROVED && status != TournamentJoinRequestStatus.REJECTED) {
      throw new BadRequestException("Decision status must be APPROVED or REJECTED");
    }
    permissions.requireRosterManage(organizationId);
    // Pessimistic lock: two managers deciding at once serialize; the loser sees a decided
    // status and fails instead of duplicating roster/participant side effects.
    TournamentJoinRequest joinRequest = joinRequests.findWithLockById(requestId)
        .orElseThrow(() -> new ResourceNotFoundException("Join request not found: " + requestId));
    if (!joinRequest.organization.id.equals(organizationId)) {
      throw new ResourceNotFoundException("Join request does not belong to organization");
    }
    if (joinRequest.status != TournamentJoinRequestStatus.PENDING) {
      throw new BusinessConflictException("Join request was already decided");
    }
    joinRequest.status = status;
    joinRequest.decisionNote = decisionNote == null || decisionNote.isBlank() ? null : decisionNote.trim();
    joinRequest.decidedAt = Instant.now();
    joinRequest.decidedByUser = users.findByIdAndDeletedAtIsNull(permissions.currentActor().userId()).orElse(null);
    if (status == TournamentJoinRequestStatus.APPROVED) {
      ensureAthleteReadiness(joinRequest);
      ensureDelegation(joinRequest);
    }
    return mapper.joinRequest(joinRequest);
  }

  /**
   * Approval makes the member tournament-ready: Person -> Athlete -> ClubRoster, following
   * the core domain chain, so the tournament admin can register entries right away.
   */
  private void ensureAthleteReadiness(TournamentJoinRequest joinRequest) {
    OrganizationMember member = joinRequest.member;
    if (member.person == null) {
      throw new BusinessConflictException("Member has no person profile; complete the profile before approving");
    }
    Athlete athlete = athletes.findByPerson_IdAndDeletedAtIsNull(member.person.id)
        .orElseGet(() -> {
          Athlete created = Athlete.create();
          created.person = member.person;
          created.primaryOrganization = joinRequest.organization;
          created.status = AthleteStatus.ACTIVE;
          return athletes.save(created);
        });
    roster.findByOrganization_IdAndAthlete_IdAndDeletedAtIsNull(joinRequest.organization.id, athlete.id)
        .orElseGet(() -> {
          ClubRoster entry = ClubRoster.create();
          entry.organization = joinRequest.organization;
          entry.athlete = athlete;
          entry.status = ClubRosterStatus.ACTIVE;
          entry.joinedAt = LocalDate.now();
          return roster.save(entry);
        });
  }

  /** Ensures the club has a delegation in the tournament so entries can be attached to it. */
  private void ensureDelegation(TournamentJoinRequest joinRequest) {
    participants.findByTournament_IdAndOrganization_IdAndDeletedAtIsNull(joinRequest.tournament.id, joinRequest.organization.id)
        .orElseGet(() -> {
          TournamentParticipant participant = TournamentParticipant.create();
          participant.tournament = joinRequest.tournament;
          participant.organization = joinRequest.organization;
          participant.displayName = joinRequest.organization.name;
          participant.status = ParticipantStatus.REQUESTED;
          return participants.save(participant);
        });
  }

  private OrganizationMember resolveMembership(UUID userId, UUID organizationId) {
    List<OrganizationMember> memberships = members.findByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
    if (organizationId != null) {
      return memberships.stream()
          .filter(row -> row.organization.id.equals(organizationId))
          .findFirst()
          .orElseThrow(() -> new ResourceNotFoundException("Current user is not a member of this organization"));
    }
    return memberships.stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Current user has no club membership"));
  }
}
