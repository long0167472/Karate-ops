package com.karate.tournament.service.impl;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.*;
import com.karate.tournament.exception.BadRequestException;
import com.karate.tournament.exception.BusinessConflictException;
import com.karate.tournament.exception.ResourceNotFoundException;

import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.entity.AppUser;
import com.karate.tournament.entity.enums.ClubMemberRole;
import com.karate.tournament.entity.enums.ClubMemberStatus;
import com.karate.tournament.entity.Organization;
import com.karate.tournament.entity.OrganizationMember;
import com.karate.tournament.entity.enums.PaymentStatus;
import com.karate.tournament.entity.Person;
import com.karate.tournament.repository.AppUserRepository;
import com.karate.tournament.repository.OrganizationMemberRepository;
import com.karate.tournament.repository.OrganizationRepository;
import com.karate.tournament.repository.PersonRepository;
import com.karate.tournament.web.ApiMapper;
import com.karate.tournament.dto.request.ClubMemberCreateRequest;
import com.karate.tournament.dto.response.ClubMemberResponse;
import com.karate.tournament.dto.request.ClubMemberUpdateRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationMemberServiceImpl implements OrganizationMemberService {
  private final OrganizationMemberRepository members;
  private final OrganizationRepository organizations;
  private final PersonRepository persons;
  private final AppUserRepository users;
  private final PermissionService permissions;
  private final ApiMapper mapper;

  @Transactional(readOnly = true)
  public List<ClubMemberResponse> list(UUID organizationId) {
    Organization organization = requireOrganization(organizationId);
    permissions.requireClubView(organization.id);
    return members.findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId)
        .stream()
        .map(mapper::clubMember)
        .toList();
  }

  @Transactional
  public ClubMemberResponse create(UUID organizationId, ClubMemberCreateRequest request) {
    Organization organization = requireOrganization(organizationId);
    permissions.requireRosterManage(organization.id);
    if (request.personId() == null && request.userId() == null) {
      throw new BadRequestException("personId or userId is required");
    }
    Person person = null;
    if (request.personId() != null) {
      person = persons.findByIdAndDeletedAtIsNull(request.personId())
          .orElseThrow(() -> new ResourceNotFoundException("Person not found: " + request.personId()));
      members.findByOrganization_IdAndPerson_IdAndDeletedAtIsNull(organization.id, person.id)
          .ifPresent(existing -> {
            throw new BusinessConflictException("Person is already a member of this organization");
          });
    }
    AppUser user = null;
    if (request.userId() != null) {
      user = users.findByIdAndDeletedAtIsNull(request.userId())
          .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userId()));
    }
    OrganizationMember member = OrganizationMember.create();
    member.organization = organization;
    member.person = person;
    member.user = user;
    member.role = request.role() == null ? ClubMemberRole.ATHLETE : request.role();
    member.status = request.status() == null ? ClubMemberStatus.ACTIVE : request.status();
    member.joinedAt = request.joinedAt() == null ? LocalDate.now() : request.joinedAt();
    member.student = request.student() != null && request.student();
    member.attendanceViewEnabled = request.attendanceViewEnabled() == null || request.attendanceViewEnabled();
    member.tuitionStatus = request.tuitionStatus() == null ? PaymentStatus.PENDING : request.tuitionStatus();
    if (request.tuitionPaidAmount() != null) member.tuitionPaidAmount = request.tuitionPaidAmount();
    member.otherFeeStatus = request.otherFeeStatus() == null ? PaymentStatus.PENDING : request.otherFeeStatus();
    if (request.otherFeePaidAmount() != null) member.otherFeePaidAmount = request.otherFeePaidAmount();
    member.paymentNote = request.paymentNote();
    member.memberNote = request.memberNote();
    return mapper.clubMember(members.save(member));
  }

  @Transactional
  public ClubMemberResponse update(UUID organizationId, UUID memberId, ClubMemberUpdateRequest request) {
    OrganizationMember member = requireMemberInOrganization(organizationId, memberId);
    permissions.requireRosterManage(organizationId);
    if (request.personId() != null && (member.person == null || !member.person.id.equals(request.personId()))) {
      Person person = persons.findByIdAndDeletedAtIsNull(request.personId())
          .orElseThrow(() -> new ResourceNotFoundException("Person not found: " + request.personId()));
      members.findByOrganization_IdAndPerson_IdAndDeletedAtIsNull(organizationId, person.id)
          .filter(existing -> !existing.id.equals(member.id))
          .ifPresent(existing -> {
            throw new BusinessConflictException("Person is already a member of this organization");
          });
      member.person = person;
    }
    if (request.userId() != null) {
      member.user = users.findByIdAndDeletedAtIsNull(request.userId())
          .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userId()));
    }
    if (request.role() != null) member.role = request.role();
    if (request.status() != null) member.status = request.status();
    if (request.joinedAt() != null) member.joinedAt = request.joinedAt();
    if (request.student() != null) member.student = request.student();
    if (request.attendanceViewEnabled() != null) member.attendanceViewEnabled = request.attendanceViewEnabled();
    if (request.tuitionStatus() != null) member.tuitionStatus = request.tuitionStatus();
    if (request.tuitionPaidAmount() != null) member.tuitionPaidAmount = request.tuitionPaidAmount();
    if (request.otherFeeStatus() != null) member.otherFeeStatus = request.otherFeeStatus();
    if (request.otherFeePaidAmount() != null) member.otherFeePaidAmount = request.otherFeePaidAmount();
    if (request.paymentNote() != null) member.paymentNote = request.paymentNote();
    if (request.memberNote() != null) member.memberNote = request.memberNote();
    return mapper.clubMember(member);
  }

  @Transactional
  public void delete(UUID organizationId, UUID memberId) {
    OrganizationMember member = requireMemberInOrganization(organizationId, memberId);
    permissions.requireRosterManage(organizationId);
    member.softDelete();
  }

  public OrganizationMember requireMemberInOrganization(UUID organizationId, UUID memberId) {
    OrganizationMember member = members.findByIdAndDeletedAtIsNull(memberId)
        .orElseThrow(() -> new ResourceNotFoundException("Organization member not found: " + memberId));
    if (!member.organization.id.equals(organizationId)) {
      throw new ResourceNotFoundException("Member does not belong to organization");
    }
    return member;
  }

  private Organization requireOrganization(UUID id) {
    return organizations.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
  }
}
