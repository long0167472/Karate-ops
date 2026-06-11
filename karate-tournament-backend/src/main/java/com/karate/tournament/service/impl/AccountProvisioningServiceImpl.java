package com.karate.tournament.service.impl;

import com.karate.tournament.dto.request.MemberAccountCreateRequest;
import com.karate.tournament.entity.AppUser;
import com.karate.tournament.entity.Organization;
import com.karate.tournament.entity.OrganizationMember;
import com.karate.tournament.entity.Person;
import com.karate.tournament.entity.enums.ClubMemberRole;
import com.karate.tournament.entity.enums.ClubMemberStatus;
import com.karate.tournament.entity.enums.PaymentStatus;
import com.karate.tournament.exception.BusinessConflictException;
import com.karate.tournament.repository.AppUserRepository;
import com.karate.tournament.repository.OrganizationMemberRepository;
import com.karate.tournament.repository.PersonRepository;
import com.karate.tournament.service.AccountProvisioningService;
import com.karate.tournament.service.UsernameGenerator;
import com.karate.tournament.web.ApiMapper;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountProvisioningServiceImpl implements AccountProvisioningService {
  private final AppUserRepository users;
  private final PersonRepository persons;
  private final OrganizationMemberRepository members;
  private final UsernameGenerator usernameGenerator;
  private final PasswordEncoder passwordEncoder;
  private final ApiMapper mapper;

  public ProvisionedAccount createMemberAccount(Organization organization, MemberAccountCreateRequest request) {
    String email = normalizeEmail(request.email());
    users.findByEmailIgnoreCaseAndDeletedAtIsNull(email).ifPresent(existing -> {
      throw new BusinessConflictException("Email is already registered");
    });

    Person person = Person.create();
    person.displayName = cleanRequired(request.displayName(), "displayName");
    person.email = email;
    person.phone = cleanRequired(request.phone(), "phone");
    person.gender = request.gender();
    person.birthDate = request.birthDate();
    person.currentAddress = trimToNull(request.currentAddress());
    person.emergencyContactName = trimToNull(request.emergencyContactName());
    person.emergencyContactPhone = trimToNull(request.emergencyContactPhone());
    persons.save(person);

    AppUser user = AppUser.create();
    user.displayName = person.displayName;
    user.email = email;
    user.username = usernameGenerator.uniqueUsername(organization, person.displayName);
    user.phone = person.phone;
    user.status = "ACTIVE";
    user.passwordHash = passwordEncoder.encode(DEFAULT_TEMPORARY_PASSWORD);
    user.primaryOrganization = organization;
    users.save(user);

    OrganizationMember member = OrganizationMember.create();
    member.organization = organization;
    member.person = person;
    member.user = user;
    member.role = request.role() == null ? ClubMemberRole.ATHLETE : request.role();
    member.status = request.status() == null ? ClubMemberStatus.ACTIVE : request.status();
    member.joinedAt = LocalDate.now();
    member.student = request.student() == null || request.student();
    member.attendanceViewEnabled = request.attendanceViewEnabled() == null || request.attendanceViewEnabled();
    member.tuitionStatus = request.tuitionStatus() == null ? PaymentStatus.PENDING : request.tuitionStatus();
    if (request.tuitionPaidAmount() != null) member.tuitionPaidAmount = request.tuitionPaidAmount();
    member.otherFeeStatus = request.otherFeeStatus() == null ? PaymentStatus.PENDING : request.otherFeeStatus();
    if (request.otherFeePaidAmount() != null) member.otherFeePaidAmount = request.otherFeePaidAmount();
    member.paymentNote = trimToNull(request.paymentNote());
    member.memberNote = trimToNull(request.memberNote());
    members.save(member);

    return new ProvisionedAccount(mapper.clubMember(member), user, DEFAULT_TEMPORARY_PASSWORD);
  }

  private String normalizeEmail(String email) {
    return cleanRequired(email, "email").toLowerCase();
  }

  private String cleanRequired(String value, String field) {
    String trimmed = trimToNull(value);
    if (trimmed == null) {
      throw new com.karate.tournament.exception.BadRequestException(field + " is required");
    }
    return trimmed;
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isBlank() ? null : trimmed;
  }
}
