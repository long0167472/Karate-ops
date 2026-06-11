package com.karate.tournament.service.impl;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.*;
import com.karate.tournament.exception.BadRequestException;
import com.karate.tournament.exception.ResourceNotFoundException;

import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.entity.enums.BillingCycle;
import com.karate.tournament.entity.ClubFinanceExpense;
import com.karate.tournament.entity.ClubFeeItem;
import com.karate.tournament.entity.ClubFeeItemRoleAmount;
import com.karate.tournament.entity.ClubFeeRole;
import com.karate.tournament.entity.enums.ClubMemberStatus;
import com.karate.tournament.entity.enums.ExpenseDisbursementStatus;
import com.karate.tournament.entity.enums.FeeAssignmentSource;
import com.karate.tournament.entity.enums.FeeItemKind;
import com.karate.tournament.entity.enums.FeeItemStatus;
import com.karate.tournament.entity.enums.FeeItemType;
import com.karate.tournament.entity.MemberFeeAssignment;
import com.karate.tournament.entity.Organization;
import com.karate.tournament.entity.OrganizationMember;
import com.karate.tournament.entity.OrganizationMemberFeeRole;
import com.karate.tournament.entity.OrganizationMemberTuitionOverride;
import com.karate.tournament.entity.enums.PaymentStatus;
import com.karate.tournament.repository.ClubFinanceExpenseRepository;
import com.karate.tournament.repository.ClubFeeItemRepository;
import com.karate.tournament.repository.ClubFeeItemRoleAmountRepository;
import com.karate.tournament.repository.ClubFeeRoleRepository;
import com.karate.tournament.repository.MemberFeeAssignmentRepository;
import com.karate.tournament.repository.OrganizationMemberFeeRoleRepository;
import com.karate.tournament.repository.OrganizationMemberRepository;
import com.karate.tournament.repository.OrganizationMemberTuitionOverrideRepository;
import com.karate.tournament.repository.OrganizationRepository;
import com.karate.tournament.dto.request.ApplyFeeItemRequest;
import com.karate.tournament.dto.request.BulkMemberFeeRoleUpdateRequest;
import com.karate.tournament.dto.request.ClubFinanceExpenseRequest;
import com.karate.tournament.dto.request.ClubFeeItemRequest;
import com.karate.tournament.dto.request.TuitionOverrideBulkRequest;
import com.karate.tournament.dto.response.ClubFinanceExpenseResponse;
import com.karate.tournament.dto.response.ClubFinanceSummaryResponse;
import com.karate.tournament.dto.response.ClubFeeItemResponse;
import com.karate.tournament.dto.response.ClubFeeOverviewResponse;
import com.karate.tournament.dto.request.ClubFeeRoleRequest;
import com.karate.tournament.dto.response.ClubFeeRoleResponse;
import com.karate.tournament.dto.response.FeeRoleAmountResponse;
import com.karate.tournament.dto.response.MemberFeeAssignmentResponse;
import com.karate.tournament.dto.request.MemberFeeAssignmentUpdateRequest;
import com.karate.tournament.dto.response.MemberFeeRoleResponse;
import com.karate.tournament.dto.request.MemberFeeRoleUpdateRequest;
import com.karate.tournament.dto.response.MemberTuitionOverrideResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClubFeeServiceImpl implements ClubFeeService {
  private final OrganizationRepository organizations;
  private final OrganizationMemberRepository members;
  private final ClubFeeRoleRepository roles;
  private final OrganizationMemberFeeRoleRepository memberRoles;
  private final ClubFeeItemRepository feeItems;
  private final ClubFeeItemRoleAmountRepository roleAmounts;
  private final MemberFeeAssignmentRepository assignments;
  private final OrganizationMemberTuitionOverrideRepository tuitionOverrides;
  private final ClubFinanceExpenseRepository expenses;
  private final PermissionService permissions;

  @Transactional
  public ClubFeeOverviewResponse overview(UUID organizationId) {
    Organization organization = requireOrganization(organizationId);
    permissions.requireClubView(organizationId);
    ensureDefaultTuitionItem(organization);
    List<ClubFeeRoleResponse> roleResponses = roles.findByOrganization_IdAndDeletedAtIsNullOrderByPriorityAscNameAsc(organizationId)
        .stream().map(this::roleResponse).toList();
    List<MemberFeeRoleResponse> memberRoleResponses = memberRoles.findByOrganization_IdAndDeletedAtIsNull(organizationId)
        .stream()
        .collect(Collectors.groupingBy(link -> link.member.id))
        .entrySet()
        .stream()
        .map(entry -> new MemberFeeRoleResponse(entry.getKey(), entry.getValue().stream().map(link -> roleResponse(link.feeRole)).toList()))
        .toList();
    return new ClubFeeOverviewResponse(
        roleResponses,
        memberRoleResponses,
        feeItems.findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId).stream().map(this::feeItemResponse).toList(),
        assignments.findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId).stream().map(this::assignmentResponse).toList(),
        tuitionOverrides.findByOrganization_IdAndDeletedAtIsNull(organizationId).stream().map(this::tuitionOverrideResponse).toList(),
        expenses.findByOrganization_IdAndDeletedAtIsNullOrderByExpenseDateDescCreatedAtDesc(organizationId).stream().map(this::expenseResponse).toList(),
        financeSummary(organizationId)
    );
  }

  @Transactional
  public ClubFeeRoleResponse createRole(UUID organizationId, ClubFeeRoleRequest request) {
    Organization organization = requireOrganization(organizationId);
    permissions.requireRosterManage(organizationId);
    ClubFeeRole role = ClubFeeRole.create();
    role.organization = organization;
    applyRole(role, request);
    return roleResponse(roles.save(role));
  }

  @Transactional
  public ClubFeeRoleResponse updateRole(UUID organizationId, UUID roleId, ClubFeeRoleRequest request) {
    ClubFeeRole role = requireRole(organizationId, roleId);
    permissions.requireRosterManage(organizationId);
    applyRole(role, request);
    return roleResponse(role);
  }

  @Transactional
  public void deleteRole(UUID organizationId, UUID roleId) {
    ClubFeeRole role = requireRole(organizationId, roleId);
    permissions.requireRosterManage(organizationId);
    role.softDelete();
    memberRoles.findByOrganization_IdAndDeletedAtIsNull(organizationId).stream()
        .filter(link -> link.feeRole.id.equals(role.id))
        .forEach(OrganizationMemberFeeRole::softDelete);
  }

  @Transactional
  public MemberFeeRoleResponse setMemberRoles(UUID organizationId, UUID memberId, MemberFeeRoleUpdateRequest request) {
    OrganizationMember member = requireMember(organizationId, memberId);
    permissions.requireRosterManage(organizationId);
    return replaceMemberRoles(member, distinctIds(request.feeRoleIds()));
  }

  @Transactional
  public List<MemberFeeRoleResponse> bulkSetMemberRoles(UUID organizationId, BulkMemberFeeRoleUpdateRequest request) {
    permissions.requireRosterManage(organizationId);
    BulkMemberFeeRoleUpdateRequest.Mode mode = request.mode() == null
        ? BulkMemberFeeRoleUpdateRequest.Mode.REPLACE
        : request.mode();
    List<UUID> roleIds = distinctIds(request.feeRoleIds());
    return distinctIds(request.memberIds()).stream()
        .map(memberId -> {
          OrganizationMember member = requireMember(organizationId, memberId);
          List<UUID> currentRoleIds = currentMemberRoleIds(member.id);
          List<UUID> nextRoleIds = switch (mode) {
            case ADD -> mergeIds(currentRoleIds, roleIds);
            case REMOVE -> currentRoleIds.stream().filter(id -> !roleIds.contains(id)).toList();
            case REPLACE -> roleIds;
          };
          return replaceMemberRoles(member, nextRoleIds);
        })
        .toList();
  }

  @Transactional
  public List<MemberTuitionOverrideResponse> bulkSetTuitionOverrides(UUID organizationId, TuitionOverrideBulkRequest request) {
    permissions.requireRosterManage(organizationId);
    ClubFeeItem overrideItem = null;
    if (request.feeItemId() != null) {
      overrideItem = requireFeeItem(organizationId, request.feeItemId());
      if (overrideItem.feeKind == FeeItemKind.MONTHLY_TUITION_DEFAULT) {
        overrideItem = null;
      } else if (overrideItem.feeKind != FeeItemKind.MONTHLY_TUITION_OVERRIDE || overrideItem.billingCycle != BillingCycle.MONTHLY) {
        throw new BadRequestException("Fee item is not a monthly tuition override");
      }
    }
    ClubFeeItem finalOverrideItem = overrideItem;
    return distinctIds(request.memberIds()).stream()
        .map(memberId -> {
          OrganizationMember member = requireMember(organizationId, memberId);
          tuitionOverrides.findByMember_IdAndDeletedAtIsNull(member.id).forEach(OrganizationMemberTuitionOverride::softDelete);
          if (finalOverrideItem != null) {
            OrganizationMemberTuitionOverride link = OrganizationMemberTuitionOverride.create();
            link.organization = member.organization;
            link.member = member;
            link.feeItem = finalOverrideItem;
            tuitionOverrides.save(link);
          }
          return tuitionOverrideResponse(member, finalOverrideItem);
        })
        .toList();
  }

  @Transactional
  public ClubFeeItemResponse createFeeItem(UUID organizationId, ClubFeeItemRequest request) {
    Organization organization = requireOrganization(organizationId);
    permissions.requireRosterManage(organizationId);
    ClubFeeItem item = ClubFeeItem.create();
    item.organization = organization;
    applyFeeItem(item, request);
    ClubFeeItem saved = feeItems.save(item);
    replaceRoleAmounts(organizationId, saved, request);
    return feeItemResponse(saved);
  }

  @Transactional
  public ClubFeeItemResponse updateFeeItem(UUID organizationId, UUID feeItemId, ClubFeeItemRequest request) {
    ClubFeeItem item = requireFeeItem(organizationId, feeItemId);
    permissions.requireRosterManage(organizationId);
    applyFeeItem(item, request);
    replaceRoleAmounts(organizationId, item, request);
    return feeItemResponse(item);
  }

  @Transactional
  public void deleteFeeItem(UUID organizationId, UUID feeItemId) {
    ClubFeeItem item = requireFeeItem(organizationId, feeItemId);
    permissions.requireRosterManage(organizationId);
    item.softDelete();
  }

  @Transactional
  public List<MemberFeeAssignmentResponse> applyFeeItem(UUID organizationId, UUID feeItemId, ApplyFeeItemRequest request) {
    ClubFeeItem item = requireFeeItem(organizationId, feeItemId);
    permissions.requireRosterManage(organizationId);
    List<OrganizationMember> targetMembers = targetMembers(organizationId, request);
    List<MemberFeeAssignment> existing = assignments.findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId);
    Map<UUID, MemberFeeAssignment> existingByMember = existing.stream()
        .filter(assignment -> assignment.feeItem.id.equals(item.id))
        .collect(Collectors.toMap(assignment -> assignment.member.id, Function.identity(), (a, b) -> a));
    for (OrganizationMember member : targetMembers) {
      if (existingByMember.containsKey(member.id)) continue;
      ResolvedAmount resolved = resolveAmount(member, item);
      MemberFeeAssignment assignment = MemberFeeAssignment.create();
      assignment.organization = member.organization;
      assignment.member = member;
      assignment.feeItem = item;
      assignment.assignedRole = resolved.role();
      assignment.amountDue = resolved.amount();
      assignment.status = resolved.amount().compareTo(BigDecimal.ZERO) == 0 ? PaymentStatus.WAIVED : PaymentStatus.PENDING;
      assignment.dueDate = request.dueDate() != null ? request.dueDate() : defaultDueDate(item);
      assignment.note = request.note();
      assignments.save(assignment);
    }
    return assignments.findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId).stream()
        .filter(assignment -> assignment.feeItem.id.equals(item.id))
        .map(this::assignmentResponse)
        .toList();
  }

  @Transactional
  public MemberFeeAssignmentResponse updateAssignment(UUID organizationId, UUID assignmentId, MemberFeeAssignmentUpdateRequest request) {
    MemberFeeAssignment assignment = assignments.findByIdAndDeletedAtIsNull(assignmentId)
        .orElseThrow(() -> new ResourceNotFoundException("Fee assignment not found: " + assignmentId));
    if (!assignment.organization.id.equals(organizationId)) throw new ResourceNotFoundException("Fee assignment does not belong to organization");
    permissions.requireRosterManage(organizationId);
    if (request.amountDue() != null) assignment.amountDue = request.amountDue();
    if (request.paidAmount() != null) assignment.paidAmount = request.paidAmount();
    if (request.status() != null) assignment.status = request.status();
    if (request.dueDate() != null) assignment.dueDate = request.dueDate();
    if (request.note() != null) assignment.note = request.note();
    return assignmentResponse(assignment);
  }

  @Transactional
  public void deleteAssignment(UUID organizationId, UUID assignmentId) {
    MemberFeeAssignment assignment = assignments.findByIdAndDeletedAtIsNull(assignmentId)
        .orElseThrow(() -> new ResourceNotFoundException("Fee assignment not found: " + assignmentId));
    if (!assignment.organization.id.equals(organizationId)) throw new ResourceNotFoundException("Fee assignment does not belong to organization");
    permissions.requireRosterManage(organizationId);
    assignment.softDelete();
  }

  private void applyRole(ClubFeeRole role, ClubFeeRoleRequest request) {
    role.code = request.code().trim().toUpperCase();
    role.name = request.name().trim();
    role.description = request.description();
    role.priority = request.priority() == null ? role.priority : request.priority();
    role.active = request.active() == null || request.active();
  }

  private void applyFeeItem(ClubFeeItem item, ClubFeeItemRequest request) {
    item.name = request.name().trim();
    item.feeType = request.feeType() == null ? FeeItemType.TUITION : request.feeType();
    item.feeKind = request.feeKind() == null ? inferFeeKind(item.feeType, request.billingCycle()) : request.feeKind();
    item.billingCycle = request.billingCycle() != null ? request.billingCycle()
        : (item.feeKind == FeeItemKind.ONE_TIME_INCOME ? BillingCycle.ONE_TIME : BillingCycle.MONTHLY);
    item.status = request.status() == null ? FeeItemStatus.ACTIVE : request.status();
    item.defaultAmount = request.defaultAmount() == null ? BigDecimal.ZERO : request.defaultAmount();
    item.dueDay = request.dueDay();
    item.description = request.description();
  }

  private void replaceRoleAmounts(UUID organizationId, ClubFeeItem item, ClubFeeItemRequest request) {
    roleAmounts.findByFeeItem_IdAndDeletedAtIsNull(item.id).forEach(ClubFeeItemRoleAmount::softDelete);
    if (request.roleAmounts() == null) return;
    request.roleAmounts().forEach(row -> {
      ClubFeeItemRoleAmount amount = ClubFeeItemRoleAmount.create();
      amount.feeItem = item;
      amount.feeRole = requireRole(organizationId, row.feeRoleId());
      amount.amount = row.amount() == null ? BigDecimal.ZERO : row.amount();
      amount.exempt = row.exempt() != null && row.exempt();
      roleAmounts.save(amount);
    });
  }

  private List<OrganizationMember> targetMembers(UUID organizationId, ApplyFeeItemRequest request) {
    Map<UUID, OrganizationMember> targets = new LinkedHashMap<>();
    if (request.applyToAllActive() != null && request.applyToAllActive()) {
      members.findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId).stream()
          .filter(member -> member.status == ClubMemberStatus.ACTIVE)
          .forEach(member -> targets.put(member.id, member));
    }
    distinctIds(request.memberIds()).stream()
        .map(id -> requireMember(organizationId, id))
        .forEach(member -> targets.put(member.id, member));
    List<UUID> feeRoleIds = distinctIds(request.feeRoleIds());
    feeRoleIds.forEach(roleId -> requireRole(organizationId, roleId));
    if (!feeRoleIds.isEmpty()) {
      memberRoles.findByOrganization_IdAndDeletedAtIsNull(organizationId).stream()
          .filter(link -> feeRoleIds.contains(link.feeRole.id))
          .map(link -> link.member)
          .filter(member -> member.deletedAt == null && member.status == ClubMemberStatus.ACTIVE)
          .forEach(member -> targets.put(member.id, member));
    }
    return List.copyOf(targets.values());
  }

  private MemberFeeRoleResponse replaceMemberRoles(OrganizationMember member, List<UUID> roleIds) {
    memberRoles.findByMember_IdAndDeletedAtIsNull(member.id).forEach(OrganizationMemberFeeRole::softDelete);
    for (UUID roleId : roleIds) {
      OrganizationMemberFeeRole link = OrganizationMemberFeeRole.create();
      link.organization = member.organization;
      link.member = member;
      link.feeRole = requireRole(member.organization.id, roleId);
      memberRoles.save(link);
    }
    return new MemberFeeRoleResponse(member.id, memberRoles.findByMember_IdAndDeletedAtIsNull(member.id).stream().map(link -> roleResponse(link.feeRole)).toList());
  }

  private List<UUID> currentMemberRoleIds(UUID memberId) {
    return memberRoles.findByMember_IdAndDeletedAtIsNull(memberId).stream()
        .map(link -> link.feeRole.id)
        .toList();
  }

  private List<UUID> distinctIds(List<UUID> ids) {
    return ids == null ? List.of() : ids.stream().filter(Objects::nonNull).distinct().toList();
  }

  private List<UUID> mergeIds(List<UUID> existing, List<UUID> added) {
    Map<UUID, Boolean> ids = new LinkedHashMap<>();
    existing.forEach(id -> ids.put(id, true));
    added.forEach(id -> ids.put(id, true));
    return ids.keySet().stream().toList();
  }

  private ResolvedAmount resolveAmount(OrganizationMember member, ClubFeeItem item) {
    if (item.feeKind == FeeItemKind.ONE_TIME_INCOME) {
      return new ResolvedAmount(null, item.defaultAmount);
    }
    List<ClubFeeItemRoleAmount> amounts = roleAmounts.findByFeeItem_IdAndDeletedAtIsNull(item.id);
    List<UUID> memberRoleIds = memberRoles.findByMember_IdAndDeletedAtIsNull(member.id).stream().map(link -> link.feeRole.id).toList();
    return amounts.stream()
        .filter(amount -> memberRoleIds.contains(amount.feeRole.id))
        .min(Comparator.comparing(amount -> amount.exempt ? BigDecimal.ZERO : amount.amount))
        .map(amount -> new ResolvedAmount(amount.feeRole, amount.exempt ? BigDecimal.ZERO : amount.amount))
        .orElse(new ResolvedAmount(null, item.defaultAmount));
  }

  private Organization requireOrganization(UUID id) {
    return organizations.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
  }

  private OrganizationMember requireMember(UUID organizationId, UUID memberId) {
    OrganizationMember member = members.findByIdAndDeletedAtIsNull(memberId)
        .orElseThrow(() -> new ResourceNotFoundException("Organization member not found: " + memberId));
    if (!member.organization.id.equals(organizationId)) throw new ResourceNotFoundException("Member does not belong to organization");
    return member;
  }

  private ClubFeeRole requireRole(UUID organizationId, UUID roleId) {
    ClubFeeRole role = roles.findByIdAndDeletedAtIsNull(roleId)
        .orElseThrow(() -> new ResourceNotFoundException("Fee role not found: " + roleId));
    if (!role.organization.id.equals(organizationId)) throw new ResourceNotFoundException("Fee role does not belong to organization");
    return role;
  }

  private ClubFeeItem requireFeeItem(UUID organizationId, UUID feeItemId) {
    ClubFeeItem item = feeItems.findByIdAndDeletedAtIsNull(feeItemId)
        .orElseThrow(() -> new ResourceNotFoundException("Fee item not found: " + feeItemId));
    if (!item.organization.id.equals(organizationId)) throw new ResourceNotFoundException("Fee item does not belong to organization");
    return item;
  }

  private ClubFeeRoleResponse roleResponse(ClubFeeRole role) {
    return new ClubFeeRoleResponse(role.id, role.organization.id, role.code, role.name, role.description, role.priority, role.active);
  }

  private ClubFeeItemResponse feeItemResponse(ClubFeeItem item) {
    return new ClubFeeItemResponse(
        item.id,
        item.organization.id,
        item.name,
        item.feeType,
        item.feeKind,
        item.billingCycle,
        item.status,
        item.defaultAmount,
        item.dueDay,
        item.description,
        roleAmounts.findByFeeItem_IdAndDeletedAtIsNull(item.id).stream().map(this::roleAmountResponse).toList()
    );
  }

  private FeeRoleAmountResponse roleAmountResponse(ClubFeeItemRoleAmount amount) {
    return new FeeRoleAmountResponse(amount.id, amount.feeRole.id, amount.feeRole.name, amount.amount, amount.exempt);
  }

  private MemberFeeAssignmentResponse assignmentResponse(MemberFeeAssignment assignment) {
    return new MemberFeeAssignmentResponse(
        assignment.id,
        assignment.organization.id,
        assignment.member.id,
        assignment.member.person == null ? null : assignment.member.person.displayName,
        assignment.feeItem.id,
        assignment.feeItem.name,
        assignment.assignedRole == null ? null : assignment.assignedRole.id,
        assignment.assignedRole == null ? null : assignment.assignedRole.name,
        assignment.amountDue,
        assignment.paidAmount,
        assignment.status,
        assignment.dueDate,
        assignment.source,
        assignment.note
    );
  }

  @Transactional(readOnly = true)
  public List<ClubFinanceExpenseResponse> listExpenses(UUID organizationId) {
    requireOrganization(organizationId);
    permissions.requireClubView(organizationId);
    return expenses.findByOrganization_IdAndDeletedAtIsNullOrderByExpenseDateDescCreatedAtDesc(organizationId)
        .stream()
        .map(this::expenseResponse)
        .toList();
  }

  @Transactional
  public ClubFinanceExpenseResponse createExpense(UUID organizationId, ClubFinanceExpenseRequest request) {
    Organization organization = requireOrganization(organizationId);
    permissions.requireRosterManage(organizationId);
    ClubFinanceExpense expense = ClubFinanceExpense.create();
    expense.organization = organization;
    applyExpense(expense, request);
    return expenseResponse(expenses.save(expense));
  }

  @Transactional
  public ClubFinanceExpenseResponse updateExpense(UUID organizationId, UUID expenseId, ClubFinanceExpenseRequest request) {
    ClubFinanceExpense expense = requireExpense(organizationId, expenseId);
    permissions.requireRosterManage(organizationId);
    applyExpense(expense, request);
    return expenseResponse(expense);
  }

  @Transactional
  public void deleteExpense(UUID organizationId, UUID expenseId) {
    ClubFinanceExpense expense = requireExpense(organizationId, expenseId);
    permissions.requireRosterManage(organizationId);
    expense.softDelete();
  }

  private LocalDate defaultDueDate(ClubFeeItem item) {
    int dueDay = item.dueDay == null ? 10 : Math.max(1, Math.min(28, item.dueDay));
    return LocalDate.now().withDayOfMonth(dueDay);
  }

  private FeeItemKind inferFeeKind(FeeItemType feeType, BillingCycle billingCycle) {
    BillingCycle cycle = billingCycle == null ? BillingCycle.MONTHLY : billingCycle;
    if (feeType == FeeItemType.TUITION && cycle == BillingCycle.MONTHLY) {
      return FeeItemKind.MONTHLY_TUITION_OVERRIDE;
    }
    return FeeItemKind.ONE_TIME_INCOME;
  }

  private void ensureDefaultTuitionItem(Organization organization) {
    boolean hasDefault = feeItems.findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(organization.id).stream()
        .anyMatch(item -> item.feeKind == FeeItemKind.MONTHLY_TUITION_DEFAULT);
    if (hasDefault) return;
    ClubFeeItem item = ClubFeeItem.create();
    item.organization = organization;
    item.name = "Học phí";
    item.feeType = FeeItemType.TUITION;
    item.feeKind = FeeItemKind.MONTHLY_TUITION_DEFAULT;
    item.billingCycle = BillingCycle.MONTHLY;
    item.status = FeeItemStatus.ACTIVE;
    item.defaultAmount = BigDecimal.ZERO;
    item.dueDay = 10;
    item.description = "Khoản học phí tháng mặc định của CLB.";
    feeItems.save(item);
  }

  private ClubFeeItem defaultTuitionItem(UUID organizationId) {
    return feeItems.findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId).stream()
        .filter(item -> item.feeKind == FeeItemKind.MONTHLY_TUITION_DEFAULT)
        .min(Comparator.comparing(item -> item.createdAt))
        .orElseThrow(() -> new ResourceNotFoundException("Default tuition item not found"));
  }

  private ClubFeeItem monthlyTuitionForMember(OrganizationMember member, ClubFeeItem defaultItem) {
    return tuitionOverrides.findFirstByMember_IdAndDeletedAtIsNullOrderByCreatedAtDesc(member.id)
        .map(link -> link.feeItem)
        .filter(item -> item.deletedAt == null && item.status == FeeItemStatus.ACTIVE)
        .orElse(defaultItem);
  }

  private ClubFinanceSummaryResponse financeSummary(UUID organizationId) {
    List<OrganizationMember> activeMembers = members.findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId).stream()
        .filter(member -> member.status == ClubMemberStatus.ACTIVE)
        .toList();
    ClubFeeItem defaultItem = defaultTuitionItem(organizationId);
    BigDecimal monthlyExpected = activeMembers.stream()
        .map(member -> monthlyTuitionForMember(member, defaultItem).defaultAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    List<MemberFeeAssignment> assignmentRows = assignments.findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId);
    BigDecimal totalDue = assignmentRows.stream().map(row -> row.amountDue).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalPaid = assignmentRows.stream().map(row -> row.paidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal oneTimeDue = assignmentRows.stream()
        .filter(row -> row.feeItem.feeKind == FeeItemKind.ONE_TIME_INCOME)
        .map(row -> row.amountDue)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    List<ClubFinanceExpense> expenseRows = expenses.findByOrganization_IdAndDeletedAtIsNullOrderByExpenseDateDescCreatedAtDesc(organizationId);
    BigDecimal expensesTotal = expenseRows.stream().map(row -> row.amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal expensesDisbursed = expenseRows.stream()
        .filter(row -> row.status == ExpenseDisbursementStatus.DISBURSED)
        .map(row -> row.amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal expensesPending = expenseRows.stream()
        .filter(row -> row.status == ExpenseDisbursementStatus.PENDING_DISBURSEMENT)
        .map(row -> row.amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new ClubFinanceSummaryResponse(
        activeMembers.size(),
        monthlyExpected,
        oneTimeDue,
        totalDue,
        totalPaid,
        totalDue.subtract(totalPaid).max(BigDecimal.ZERO),
        expensesTotal,
        expensesDisbursed,
        expensesPending,
        totalPaid.subtract(expensesDisbursed)
    );
  }

  private void applyExpense(ClubFinanceExpense expense, ClubFinanceExpenseRequest request) {
    expense.name = request.name().trim();
    expense.amount = request.amount() == null ? BigDecimal.ZERO : request.amount();
    expense.expenseDate = request.expenseDate() == null ? LocalDate.now() : request.expenseDate();
    expense.status = request.status() == null ? ExpenseDisbursementStatus.PENDING_DISBURSEMENT : request.status();
    expense.note = request.note();
  }

  private ClubFinanceExpense requireExpense(UUID organizationId, UUID expenseId) {
    ClubFinanceExpense expense = expenses.findByIdAndDeletedAtIsNull(expenseId)
        .orElseThrow(() -> new ResourceNotFoundException("Expense not found: " + expenseId));
    if (!expense.organization.id.equals(organizationId)) throw new ResourceNotFoundException("Expense does not belong to organization");
    return expense;
  }

  private MemberTuitionOverrideResponse tuitionOverrideResponse(OrganizationMemberTuitionOverride override) {
    return tuitionOverrideResponse(override.member, override.feeItem);
  }

  private MemberTuitionOverrideResponse tuitionOverrideResponse(OrganizationMember member, ClubFeeItem item) {
    if (item == null) {
      ClubFeeItem defaultItem = defaultTuitionItem(member.organization.id);
      return new MemberTuitionOverrideResponse(
          member.id,
          member.person == null ? null : member.person.displayName,
          null,
          defaultItem.name,
          defaultItem.defaultAmount
      );
    }
    return new MemberTuitionOverrideResponse(
        member.id,
        member.person == null ? null : member.person.displayName,
        item.id,
        item.name,
        item.defaultAmount
    );
  }

  private ClubFinanceExpenseResponse expenseResponse(ClubFinanceExpense expense) {
    return new ClubFinanceExpenseResponse(
        expense.id,
        expense.organization.id,
        expense.name,
        expense.amount,
        expense.expenseDate,
        expense.status,
        expense.note
    );
  }

  private record ResolvedAmount(ClubFeeRole role, BigDecimal amount) {
  }
}
