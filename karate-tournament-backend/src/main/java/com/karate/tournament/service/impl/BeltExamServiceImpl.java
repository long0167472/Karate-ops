package com.karate.tournament.service.impl;

import lombok.RequiredArgsConstructor;
import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.dto.request.BeltExamCandidateRequest;
import com.karate.tournament.dto.request.BeltExamCandidateUpdateRequest;
import com.karate.tournament.dto.request.BeltExamCreateRequest;
import com.karate.tournament.dto.request.BeltExamUpdateRequest;
import com.karate.tournament.dto.response.BeltExamCandidateResponse;
import com.karate.tournament.dto.response.BeltExamResponse;
import com.karate.tournament.entity.Athlete;
import com.karate.tournament.entity.BeltExam;
import com.karate.tournament.entity.BeltExamCandidate;
import com.karate.tournament.entity.Organization;
import com.karate.tournament.entity.OrganizationMember;
import com.karate.tournament.entity.Person;
import com.karate.tournament.entity.enums.BeltExamResult;
import com.karate.tournament.entity.enums.BeltExamStatus;
import com.karate.tournament.entity.enums.ClubMemberStatus;
import com.karate.tournament.exception.BadRequestException;
import com.karate.tournament.exception.BusinessConflictException;
import com.karate.tournament.exception.ResourceNotFoundException;
import com.karate.tournament.repository.AthleteRepository;
import com.karate.tournament.repository.BeltExamCandidateRepository;
import com.karate.tournament.repository.BeltExamRepository;
import com.karate.tournament.repository.OrganizationMemberRepository;
import com.karate.tournament.repository.OrganizationRepository;
import com.karate.tournament.service.BeltExamService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BeltExamServiceImpl implements BeltExamService {
  private final BeltExamRepository exams;
  private final BeltExamCandidateRepository candidates;
  private final OrganizationRepository organizations;
  private final OrganizationMemberRepository members;
  private final AthleteRepository athletes;
  private final PermissionService permissions;

  @Transactional(readOnly = true)
  public List<BeltExamResponse> list(UUID organizationId) {
    Organization org = requireOrganization(organizationId);
    permissions.requireClubView(org.id);
    return exams.findByOrganization_IdAndDeletedAtIsNullOrderByExamDateDescCreatedAtDesc(organizationId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public BeltExamResponse get(UUID examId) {
    BeltExam exam = requireExam(examId);
    permissions.requireClubView(exam.organization.id);
    return toResponse(exam);
  }

  @Transactional
  public BeltExamResponse create(UUID organizationId, BeltExamCreateRequest request) {
    Organization org = requireOrganization(organizationId);
    permissions.requireAttendanceManage(org.id);
    BeltExam exam = BeltExam.create();
    exam.organization = org;
    exam.name = request.name();
    exam.status = request.status() == null ? BeltExamStatus.DRAFT : request.status();
    exam.examDate = request.examDate();
    exam.location = request.location();
    exam.examinerName = request.examinerName();
    exam.notes = request.notes();
    return toResponse(exams.save(exam));
  }

  @Transactional
  public BeltExamResponse update(UUID examId, BeltExamUpdateRequest request) {
    BeltExam exam = requireExam(examId);
    permissions.requireAttendanceManage(exam.organization.id);
    if (exam.status == BeltExamStatus.COMPLETED) {
      throw new BusinessConflictException("Cannot update a completed belt exam");
    }
    if (request.name() != null) exam.name = request.name();
    if (request.status() != null) exam.status = request.status();
    if (request.examDate() != null) exam.examDate = request.examDate();
    if (request.location() != null) exam.location = request.location();
    if (request.examinerName() != null) exam.examinerName = request.examinerName();
    if (request.notes() != null) exam.notes = request.notes();
    return toResponse(exam);
  }

  @Transactional
  public void delete(UUID examId) {
    BeltExam exam = requireExam(examId);
    permissions.requireAttendanceManage(exam.organization.id);
    if (exam.status == BeltExamStatus.COMPLETED) {
      throw new BusinessConflictException("Cannot delete a completed belt exam");
    }
    exam.softDelete();
  }

  @Transactional
  public BeltExamCandidateResponse addCandidate(UUID examId, BeltExamCandidateRequest request) {
    BeltExam exam = requireExam(examId);
    permissions.requireAttendanceManage(exam.organization.id);
    if (exam.status == BeltExamStatus.COMPLETED || exam.status == BeltExamStatus.CANCELLED) {
      throw new BusinessConflictException("Cannot add candidates to a " + exam.status.name().toLowerCase() + " exam");
    }
    ResolvedSubject subject = resolveSubject(exam.organization.id, request.organizationMemberId(), request.athleteId());
    checkNoDuplicate(exam.id, subject);
    BeltExamCandidate candidate = BeltExamCandidate.create();
    candidate.exam = exam;
    candidate.organizationMember = subject.member();
    candidate.athlete = subject.athlete();
    candidate.currentBelt = request.currentBelt() != null ? request.currentBelt()
        : subject.athlete() != null ? subject.athlete().belt : null;
    candidate.targetBelt = request.targetBelt();
    candidate.result = BeltExamResult.PENDING;
    return toResponse(candidates.save(candidate));
  }

  @Transactional
  public BeltExamCandidateResponse updateCandidate(UUID examId, UUID candidateId, BeltExamCandidateUpdateRequest request) {
    BeltExam exam = requireExam(examId);
    permissions.requireAttendanceManage(exam.organization.id);
    BeltExamCandidate candidate = requireCandidate(candidateId);
    if (!candidate.exam.id.equals(examId)) {
      throw new ResourceNotFoundException("Candidate does not belong to this exam");
    }
    if (request.result() != null) candidate.result = request.result();
    if (request.examinerNote() != null) candidate.examinerNote = request.examinerNote();
    return toResponse(candidate);
  }

  @Transactional
  public void removeCandidate(UUID examId, UUID candidateId) {
    BeltExam exam = requireExam(examId);
    permissions.requireAttendanceManage(exam.organization.id);
    if (exam.status == BeltExamStatus.COMPLETED) {
      throw new BusinessConflictException("Cannot remove candidates from a completed exam");
    }
    BeltExamCandidate candidate = requireCandidate(candidateId);
    if (!candidate.exam.id.equals(examId)) {
      throw new ResourceNotFoundException("Candidate does not belong to this exam");
    }
    candidate.softDelete();
  }

  @Transactional
  public BeltExamResponse applyResults(UUID examId) {
    BeltExam exam = requireExam(examId);
    permissions.requireAttendanceManage(exam.organization.id);
    if (exam.status != BeltExamStatus.COMPLETED) {
      throw new BusinessConflictException("Exam must be COMPLETED before applying results");
    }
    List<BeltExamCandidate> passList = candidates.findByExam_IdAndDeletedAtIsNullOrderByCreatedAtAsc(examId)
        .stream()
        .filter(c -> c.result == BeltExamResult.PASS && !c.beltApplied && c.athlete != null)
        .toList();
    for (BeltExamCandidate c : passList) {
      c.athlete.belt = c.targetBelt;
      athletes.save(c.athlete);
      c.beltApplied = true;
    }
    return toResponse(exam);
  }

  private BeltExamResponse toResponse(BeltExam exam) {
    List<BeltExamCandidateResponse> candidateList = candidates
        .findByExam_IdAndDeletedAtIsNullOrderByCreatedAtAsc(exam.id)
        .stream()
        .map(this::toResponse)
        .toList();
    return new BeltExamResponse(
        exam.id,
        exam.organization.id,
        exam.organization.name,
        exam.name,
        exam.status,
        exam.examDate,
        exam.location,
        exam.examinerName,
        exam.notes,
        candidateList
    );
  }

  private BeltExamCandidateResponse toResponse(BeltExamCandidate c) {
    Athlete athlete = c.athlete;
    OrganizationMember member = c.organizationMember;
    Person person = athlete != null ? athlete.person : member == null ? null : member.person;
    String displayName = athlete != null ? athlete.person.displayName
        : person == null ? null : person.displayName;
    return new BeltExamCandidateResponse(
        c.id,
        c.exam.id,
        member == null ? null : member.id,
        athlete == null ? null : athlete.id,
        person == null ? null : person.id,
        displayName,
        c.currentBelt,
        c.targetBelt,
        c.result,
        c.examinerNote,
        c.beltApplied
    );
  }

  private void checkNoDuplicate(UUID examId, ResolvedSubject subject) {
    if (subject.member() != null) {
      candidates.findByExam_IdAndOrganizationMember_IdAndDeletedAtIsNull(examId, subject.member().id)
          .ifPresent(existing -> { throw new BusinessConflictException("Member is already registered in this exam"); });
    }
    if (subject.athlete() != null) {
      candidates.findByExam_IdAndAthlete_IdAndDeletedAtIsNull(examId, subject.athlete().id)
          .ifPresent(existing -> { throw new BusinessConflictException("Athlete is already registered in this exam"); });
    }
  }

  private ResolvedSubject resolveSubject(UUID organizationId, UUID memberId, UUID athleteId) {
    if (memberId == null && athleteId == null) {
      throw new BadRequestException("organizationMemberId or athleteId is required");
    }
    OrganizationMember member = null;
    if (memberId != null) {
      member = members.findByIdAndDeletedAtIsNull(memberId)
          .orElseThrow(() -> new ResourceNotFoundException("Organization member not found: " + memberId));
      if (!member.organization.id.equals(organizationId)) {
        throw new BadRequestException("Member belongs to another organization");
      }
    }
    Athlete athlete = null;
    if (athleteId != null) {
      athlete = athletes.findByIdAndDeletedAtIsNull(athleteId)
          .orElseThrow(() -> new ResourceNotFoundException("Athlete not found: " + athleteId));
      if (member == null) {
        member = members.findByOrganization_IdAndPerson_IdAndStatusAndDeletedAtIsNull(
            organizationId, athlete.person.id, ClubMemberStatus.ACTIVE
        ).orElse(null);
      }
    }
    return new ResolvedSubject(member, athlete);
  }

  private BeltExam requireExam(UUID id) {
    return exams.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new ResourceNotFoundException("Belt exam not found: " + id));
  }

  private BeltExamCandidate requireCandidate(UUID id) {
    return candidates.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new ResourceNotFoundException("Belt exam candidate not found: " + id));
  }

  private Organization requireOrganization(UUID id) {
    return organizations.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
  }

  private record ResolvedSubject(OrganizationMember member, Athlete athlete) {
  }
}
