package com.karate.tournament.service.impl;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.*;
import com.karate.tournament.exception.BadRequestException;
import com.karate.tournament.exception.BusinessConflictException;
import com.karate.tournament.exception.ResourceNotFoundException;

import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.entity.Athlete;
import com.karate.tournament.entity.Category;
import com.karate.tournament.entity.Entry;
import com.karate.tournament.entity.enums.CategoryDiscipline;
import com.karate.tournament.entity.enums.CompetitionLevel;
import com.karate.tournament.entity.enums.EntryStatus;
import com.karate.tournament.entity.enums.EntryType;
import com.karate.tournament.entity.enums.ParticipantStatus;
import com.karate.tournament.entity.enums.PersonGender;
import com.karate.tournament.entity.enums.RulesetVersion;
import com.karate.tournament.entity.enums.WeighInStatus;
import com.karate.tournament.entity.Tournament;
import com.karate.tournament.entity.TournamentParticipant;
import com.karate.tournament.repository.AthleteRepository;
import com.karate.tournament.repository.CategoryRepository;
import com.karate.tournament.repository.EntryRepository;
import com.karate.tournament.repository.MatchRepository;
import com.karate.tournament.repository.TournamentParticipantRepository;
import com.karate.tournament.web.ApiMapper;
import com.karate.tournament.dto.request.CategoryCreateRequest;
import com.karate.tournament.dto.response.CategoryResponse;
import com.karate.tournament.dto.request.CategoryUpdateRequest;
import com.karate.tournament.dto.request.EntryCreateRequest;
import com.karate.tournament.dto.response.EntryResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
  private final CategoryRepository categories;
  private final EntryRepository entries;
  private final AthleteRepository athletes;
  private final TournamentParticipantRepository participants;
  private final MatchRepository matches;
  private final TournamentService tournaments;
  private final ClubRosterService clubRosterService;
  private final PermissionService permissions;
  private final ApiMapper mapper;

  @Transactional(readOnly = true)
  public List<CategoryResponse> list(UUID tournamentId) {
    Tournament tournament = tournaments.requireTournament(tournamentId);
    permissions.requireViewTournament(tournament);
    return categories.findByTournament_IdAndDeletedAtIsNullOrderByNameAsc(tournamentId)
        .stream()
        .map(mapper::category)
        .toList();
  }

  @Transactional(readOnly = true)
  public CategoryResponse get(UUID id) {
    Category category = requireCategory(id);
    permissions.requireViewTournament(category.tournament);
    return mapper.category(category);
  }

  @Transactional
  public CategoryResponse create(UUID tournamentId, CategoryCreateRequest request) {
    Tournament tournament = tournaments.requireTournament(tournamentId);
    permissions.requireTournamentManage(tournament);
    Category category = Category.create();
    category.tournament = tournament;
    category.name = request.name();
    category.discipline = request.discipline();
    category.gender = request.gender() == null ? PersonGender.OPEN : request.gender();
    category.ageMin = request.ageMin();
    category.ageMax = request.ageMax();
    category.weightMinKg = request.weightMinKg();
    category.weightMaxKg = request.weightMaxKg();
    applyCategoryRules(category, request.competitionLevel(), request.weightLabel(), request.openWeight(),
        request.repechageEnabled(), request.matchDurationSeconds(), request.kataJudgeCount(),
        request.kataRepeatAllowed(), request.entryLimitPerOrganization());
    category.entryType = request.entryType() == null ? EntryType.INDIVIDUAL : request.entryType();
    category.status = request.status() == null ? "DRAFT" : request.status();
    category.rulesetVersion = request.rulesetVersion() == null ? RulesetVersion.WKF_2026 : request.rulesetVersion();
    return mapper.category(categories.save(category));
  }

  @Transactional
  public CategoryResponse update(UUID id, CategoryUpdateRequest request) {
    Category category = requireCategory(id);
    permissions.requireTournamentManage(category.tournament);
    if (request.name() != null) category.name = request.name();
    if (request.discipline() != null) category.discipline = request.discipline();
    if (request.gender() != null) category.gender = request.gender();
    if (request.ageMin() != null) category.ageMin = request.ageMin();
    if (request.ageMax() != null) category.ageMax = request.ageMax();
    if (request.weightMinKg() != null) category.weightMinKg = request.weightMinKg();
    if (request.weightMaxKg() != null) category.weightMaxKg = request.weightMaxKg();
    applyCategoryRules(category, request.competitionLevel(), request.weightLabel(), request.openWeight(),
        request.repechageEnabled(), request.matchDurationSeconds(), request.kataJudgeCount(),
        request.kataRepeatAllowed(), request.entryLimitPerOrganization());
    if (request.entryType() != null) category.entryType = request.entryType();
    if (request.status() != null) category.status = request.status();
    if (request.rulesetVersion() != null) category.rulesetVersion = request.rulesetVersion();
    return mapper.category(category);
  }

  @Transactional
  public void delete(UUID id) {
    Category category = requireCategory(id);
    permissions.requireTournamentManage(category.tournament);
    category.softDelete();
  }

  @Transactional(readOnly = true)
  public List<EntryResponse> listEntries(UUID categoryId) {
    Category category = requireCategory(categoryId);
    permissions.requireViewTournament(category.tournament);
    return entries.findByCategory_IdAndDeletedAtIsNullOrderBySeedNoAscCreatedAtAsc(categoryId)
        .stream()
        .map(mapper::entry)
        .toList();
  }

  @Transactional
  public EntryResponse addEntry(UUID categoryId, EntryCreateRequest request) {
    Category category = requireCategory(categoryId);
    permissions.requireTournamentManage(category.tournament);
    ensureCategoryOpenForEntries(category);
    TournamentParticipant participant = participants.findByIdAndDeletedAtIsNull(request.tournamentParticipantId())
        .orElseThrow(() -> new ResourceNotFoundException("Tournament participant not found: " + request.tournamentParticipantId()));
    if (!participant.tournament.id.equals(category.tournament.id)) {
      throw new BadRequestException("Participant belongs to a different tournament");
    }
    if (participant.status != ParticipantStatus.APPROVED) {
      throw new BusinessConflictException("Participant must be APPROVED before adding entries");
    }
    if (category.entryLimitPerOrganization != null
        && entries.countByCategory_IdAndTournamentParticipant_Organization_IdAndDeletedAtIsNull(category.id, participant.organization.id)
            >= category.entryLimitPerOrganization) {
      throw new BusinessConflictException("Organization reached the entry limit for this category");
    }
    Athlete athlete = null;
    if (request.athleteId() != null) {
      athlete = athletes.findByIdAndDeletedAtIsNull(request.athleteId())
          .orElseThrow(() -> new ResourceNotFoundException("Athlete not found: " + request.athleteId()));
      clubRosterService.requireAthleteBelongsToOrganization(participant.organization.id, athlete);
      entries.findByCategory_IdAndAthlete_IdAndDeletedAtIsNull(category.id, athlete.id)
          .ifPresent(existing -> {
            throw new BusinessConflictException("Athlete is already registered in this category");
          });
      validateAthleteForCategory(category, athlete);
    }
    if (category.entryType == EntryType.INDIVIDUAL && athlete == null) {
      throw new BadRequestException("Individual category entries require athleteId");
    }
    if (category.entryType == EntryType.TEAM && request.teamId() == null && (request.teamName() == null || request.teamName().isBlank())) {
      throw new BadRequestException("Team category entries require teamId or teamName");
    }
    String teamMembers = validateAndSerializeTeamMembers(category, participant, request.teamMemberAthleteIds());
    Entry entry = Entry.create();
    entry.category = category;
    entry.tournamentParticipant = participant;
    entry.athlete = athlete;
    entry.teamId = request.teamId();
    entry.seedNo = request.seedNo();
    entry.status = request.status() == null ? EntryStatus.REGISTERED : request.status();
    entry.registrationWeightKg = request.registrationWeightKg() == null && athlete != null ? athlete.weightKg : request.registrationWeightKg();
    entry.weighInStatus = weighInStatus(category, entry.registrationWeightKg);
    entry.teamName = request.teamName();
    entry.teamMemberAthleteIds = teamMembers;
    entry.validationNotes = validationNotes(category, entry.weighInStatus);
    return mapper.entry(entries.save(entry));
  }

  @Transactional
  public void deleteEntry(UUID categoryId, UUID entryId) {
    Category category = requireCategory(categoryId);
    permissions.requireTournamentManage(category.tournament);
    ensureCategoryOpenForEntries(category);
    Entry entry = requireEntry(entryId);
    if (!entry.category.id.equals(categoryId)) {
      throw new ResourceNotFoundException("Entry does not belong to category");
    }
    entry.softDelete();
  }

  public Category requireCategory(UUID id) {
    return categories.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
  }

  public Entry requireEntry(UUID id) {
    return entries.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new ResourceNotFoundException("Entry not found: " + id));
  }

  private void ensureCategoryOpenForEntries(Category category) {
    if ("DRAWN".equalsIgnoreCase(category.status)
        || !matches.findByCategory_IdAndDeletedAtIsNullOrderByRoundNumberAscBracketPositionAsc(category.id).isEmpty()) {
      throw new BusinessConflictException("Category already has a draw; entry changes require manual correction");
    }
  }

  private void applyCategoryRules(
      Category category,
      CompetitionLevel competitionLevel,
      String weightLabel,
      Boolean openWeight,
      Boolean repechageEnabled,
      Integer matchDurationSeconds,
      Integer kataJudgeCount,
      Boolean kataRepeatAllowed,
      Integer entryLimitPerOrganization
  ) {
    category.competitionLevel = competitionLevel == null ? category.competitionLevel : competitionLevel;
    boolean kumite = category.discipline == CategoryDiscipline.KUMITE || category.discipline == CategoryDiscipline.TEAM_KUMITE;
    boolean noWeightBounds = category.weightMinKg == null && category.weightMaxKg == null;
    category.openWeight = openWeight == null ? kumite && noWeightBounds : openWeight;
    category.weightLabel = weightLabel != null ? weightLabel : defaultWeightLabel(category);
    category.repechageEnabled = repechageEnabled == null ? Boolean.TRUE.equals(category.repechageEnabled) : repechageEnabled;
    category.matchDurationSeconds = matchDurationSeconds == null ? (category.matchDurationSeconds == null ? 180 : category.matchDurationSeconds) : Math.max(30, matchDurationSeconds);
    category.kataJudgeCount = kataJudgeCount == null ? (category.kataJudgeCount == null ? 5 : category.kataJudgeCount) : (kataJudgeCount == 7 ? 7 : 5);
    category.kataRepeatAllowed = kataRepeatAllowed == null ? Boolean.TRUE.equals(category.kataRepeatAllowed) : kataRepeatAllowed;
    if (entryLimitPerOrganization != null) {
      category.entryLimitPerOrganization = Math.max(1, entryLimitPerOrganization);
    }
  }

  private String defaultWeightLabel(Category category) {
    if (Boolean.TRUE.equals(category.openWeight)) {
      return "Vo dich tuyet doi";
    }
    if (category.weightMinKg == null && category.weightMaxKg == null) {
      return null;
    }
    if (category.weightMinKg == null) {
      return "-" + strip(category.weightMaxKg) + "kg";
    }
    if (category.weightMaxKg == null) {
      return "+" + strip(category.weightMinKg) + "kg";
    }
    return strip(category.weightMinKg) + "-" + strip(category.weightMaxKg) + "kg";
  }

  private String strip(BigDecimal value) {
    return value.stripTrailingZeros().toPlainString();
  }

  private void validateAthleteForCategory(Category category, Athlete athlete) {
    if (category.gender != PersonGender.OPEN
        && category.gender != PersonGender.MIXED
        && athlete.person.gender != null
        && athlete.person.gender != category.gender) {
      throw new BadRequestException("Athlete gender does not match category");
    }
    if (athlete.person.birthDate != null && (category.ageMin != null || category.ageMax != null)) {
      int age = Period.between(athlete.person.birthDate, LocalDate.now()).getYears();
      if (category.ageMin != null && age < category.ageMin) {
        throw new BadRequestException("Athlete is younger than category ageMin");
      }
      if (category.ageMax != null && age > category.ageMax) {
        throw new BadRequestException("Athlete is older than category ageMax");
      }
    }
    WeighInStatus status = weighInStatus(category, athlete.weightKg);
    if (status == WeighInStatus.OUT_OF_CLASS) {
      throw new BadRequestException("Athlete weight does not match category");
    }
  }

  private WeighInStatus weighInStatus(Category category, BigDecimal weight) {
    if (!isKumite(category) || Boolean.TRUE.equals(category.openWeight)) {
      return WeighInStatus.VALID;
    }
    if (weight == null) {
      return WeighInStatus.MISSING_WEIGHT;
    }
    if (category.weightMinKg != null && weight.compareTo(category.weightMinKg) < 0) {
      return WeighInStatus.OUT_OF_CLASS;
    }
    if (category.weightMaxKg != null && weight.compareTo(category.weightMaxKg) > 0) {
      return WeighInStatus.OUT_OF_CLASS;
    }
    return WeighInStatus.VALID;
  }

  private String validationNotes(Category category, WeighInStatus status) {
    return switch (status) {
      case VALID -> Boolean.TRUE.equals(category.openWeight) ? "Open weight / Vo dich tuyet doi" : "Eligible";
      case MISSING_WEIGHT -> "Missing registration or official weigh-in value";
      case OUT_OF_CLASS -> "Weight is outside category bounds";
      case NEEDS_ORGANIZER_REVIEW -> "Organizer review required";
    };
  }

  private String validateAndSerializeTeamMembers(Category category, TournamentParticipant participant, List<UUID> athleteIds) {
    if (category.entryType != EntryType.TEAM) {
      return null;
    }
    if (athleteIds == null || athleteIds.isEmpty()) {
      return null;
    }
    for (UUID athleteId : athleteIds) {
      Athlete member = athletes.findByIdAndDeletedAtIsNull(athleteId)
          .orElseThrow(() -> new ResourceNotFoundException("Team athlete not found: " + athleteId));
      clubRosterService.requireAthleteBelongsToOrganization(participant.organization.id, member);
      validateAthleteForCategory(category, member);
    }
    return athleteIds.stream().map(UUID::toString).distinct().reduce((left, right) -> left + "," + right).orElse(null);
  }

  private boolean isKumite(Category category) {
    return category.discipline == CategoryDiscipline.KUMITE || category.discipline == CategoryDiscipline.TEAM_KUMITE;
  }
}
