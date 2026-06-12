package com.karate.tournament.service.impl;

import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.dto.request.RegisterAthleteRequest;
import com.karate.tournament.dto.request.RegisterClubRequest;
import com.karate.tournament.dto.response.TournamentRegistrationResponse;
import com.karate.tournament.dto.response.TournamentRegistrationResponse.RegistrationEntryItem;
import com.karate.tournament.entity.Athlete;
import com.karate.tournament.entity.Category;
import com.karate.tournament.entity.Entry;
import com.karate.tournament.entity.Organization;
import com.karate.tournament.entity.Tournament;
import com.karate.tournament.entity.TournamentParticipant;
import com.karate.tournament.entity.enums.BtcApprovalStatus;
import com.karate.tournament.entity.enums.ParticipantStatus;
import com.karate.tournament.entity.enums.TournamentStatus;
import com.karate.tournament.exception.BadRequestException;
import com.karate.tournament.exception.BusinessConflictException;
import com.karate.tournament.exception.ResourceNotFoundException;
import com.karate.tournament.repository.AthleteRepository;
import com.karate.tournament.repository.CategoryRepository;
import com.karate.tournament.repository.EntryRepository;
import com.karate.tournament.repository.OrganizationRepository;
import com.karate.tournament.repository.TournamentParticipantRepository;
import com.karate.tournament.repository.TournamentRepository;
import com.karate.tournament.service.TournamentRegistrationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TournamentRegistrationServiceImpl implements TournamentRegistrationService {

  private final TournamentRepository tournaments;
  private final TournamentParticipantRepository participants;
  private final EntryRepository entries;
  private final CategoryRepository categories;
  private final AthleteRepository athletes;
  private final OrganizationRepository organizations;
  private final PermissionService permissions;

  @Override
  @Transactional
  public TournamentRegistrationResponse registerClub(UUID tournamentId, RegisterClubRequest req) {
    UUID orgId = permissions.currentActor().primaryOrganizationId();

    Tournament tournament = requireTournament(tournamentId);

    if (tournament.status != TournamentStatus.REGISTRATION_OPEN) {
      throw new BadRequestException("Tournament is not open for registration");
    }
    if (tournament.registrationDeadline != null && Instant.now().isAfter(tournament.registrationDeadline)) {
      throw new BadRequestException("Registration deadline has passed");
    }

    participants.findByTournament_IdAndOrganization_IdAndDeletedAtIsNull(tournamentId, orgId)
        .ifPresent(existing -> {
          throw new BusinessConflictException("Organization is already registered for this tournament");
        });

    Organization organization = organizations.findByIdAndDeletedAtIsNull(orgId)
        .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + orgId));

    TournamentParticipant participant = TournamentParticipant.create();
    participant.tournament = tournament;
    participant.organization = organization;
    String displayName = (req.displayName() == null || req.displayName().isBlank())
        ? organization.name
        : req.displayName();
    participant.displayName = (displayName == null || displayName.isBlank()) ? orgId.toString() : displayName;
    participant.status = ParticipantStatus.REQUESTED;

    TournamentParticipant saved = participants.save(participant);
    return toResponse(saved, List.of());
  }

  @Override
  @Transactional
  public void withdrawClub(UUID tournamentId) {
    UUID orgId = permissions.currentActor().primaryOrganizationId();

    Tournament tournament = requireTournament(tournamentId);

    if (tournament.step > 1) {
      throw new BadRequestException("Cannot withdraw after tournament has progressed past registration");
    }

    TournamentParticipant participant = requireParticipant(tournamentId, orgId);

    List<Entry> participantEntries = findEntriesByParticipant(participant.id);
    participantEntries.forEach(Entry::softDelete);

    participant.softDelete();
  }

  @Override
  @Transactional(readOnly = true)
  public TournamentRegistrationResponse getMyRegistration(UUID tournamentId) {
    UUID orgId = permissions.currentActor().primaryOrganizationId();

    requireTournament(tournamentId);

    TournamentParticipant participant = requireParticipant(tournamentId, orgId);
    List<Entry> participantEntries = findEntriesByParticipant(participant.id);
    return toResponse(participant, participantEntries);
  }

  @Override
  @Transactional
  public TournamentRegistrationResponse addAthlete(UUID tournamentId, RegisterAthleteRequest req) {
    UUID orgId = permissions.currentActor().primaryOrganizationId();

    requireTournament(tournamentId);

    TournamentParticipant participant = requireParticipant(tournamentId, orgId);

    Category category = categories.findByIdAndDeletedAtIsNull(req.categoryId())
        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + req.categoryId()));

    if (!category.tournament.id.equals(tournamentId)) {
      throw new BadRequestException("Category does not belong to this tournament");
    }

    Athlete athlete = athletes.findByIdAndDeletedAtIsNull(req.athleteId())
        .orElseThrow(() -> new ResourceNotFoundException("Athlete not found: " + req.athleteId()));

    Entry entry = Entry.create();
    entry.category = category;
    entry.tournamentParticipant = participant;
    entry.athlete = athlete;
    entry.registrationWeightKg = req.registrationWeightKg();
    entry.btcApprovalStatus = BtcApprovalStatus.PENDING;

    entries.save(entry);

    List<Entry> participantEntries = findEntriesByParticipant(participant.id);
    return toResponse(participant, participantEntries);
  }

  @Override
  @Transactional
  public void removeAthlete(UUID tournamentId, UUID entryId) {
    UUID orgId = permissions.currentActor().primaryOrganizationId();

    requireTournament(tournamentId);

    TournamentParticipant participant = requireParticipant(tournamentId, orgId);

    Entry entry = entries.findByIdAndDeletedAtIsNull(entryId)
        .orElseThrow(() -> new ResourceNotFoundException("Entry not found: " + entryId));

    if (!entry.tournamentParticipant.id.equals(participant.id)) {
      throw new BadRequestException("Entry does not belong to your registration");
    }

    entry.softDelete();
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private Tournament requireTournament(UUID id) {
    return tournaments.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new ResourceNotFoundException("Tournament not found: " + id));
  }

  private TournamentParticipant requireParticipant(UUID tournamentId, UUID orgId) {
    return participants.findByTournament_IdAndOrganization_IdAndDeletedAtIsNull(tournamentId, orgId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Your organization is not registered for tournament: " + tournamentId));
  }

  private List<Entry> findEntriesByParticipant(UUID participantId) {
    // EntryRepository has no direct findByParticipantId method.
    // We load all entries for the participant's tournament, then filter by participantId in memory.
    // The participant entity is already loaded (EAGER) so we get tournamentId from it directly.
    TournamentParticipant participant = participants.findByIdAndDeletedAtIsNull(participantId)
        .orElseThrow(() -> new ResourceNotFoundException("Participant not found: " + participantId));
    UUID tournamentId = participant.tournament.id;
    return entries.findByTournament(tournamentId).stream()
        .filter(e -> e.tournamentParticipant.id.equals(participantId))
        .toList();
  }

  private TournamentRegistrationResponse toResponse(TournamentParticipant participant, List<Entry> entryList) {
    List<RegistrationEntryItem> items = entryList.stream()
        .map(e -> new RegistrationEntryItem(
            e.id,
            e.category.id,
            e.category.name,
            e.athlete != null ? e.athlete.id : null,
            e.athlete != null && e.athlete.person != null ? e.athlete.person.displayName : null,
            e.registrationWeightKg,
            e.btcApprovalStatus != null ? e.btcApprovalStatus.name() : null
        ))
        .toList();

    return new TournamentRegistrationResponse(
        participant.id,
        participant.organization.id,
        participant.displayName,
        participant.status.name(),
        participant.createdAt,
        items
    );
  }
}
