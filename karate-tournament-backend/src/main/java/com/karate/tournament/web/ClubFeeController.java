package com.karate.tournament.web;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.ClubFeeService;
import com.karate.tournament.dto.request.ApplyFeeItemRequest;
import com.karate.tournament.dto.request.BulkMemberFeeRoleUpdateRequest;
import com.karate.tournament.dto.request.ClubFinanceExpenseRequest;
import com.karate.tournament.dto.request.ClubFeeItemRequest;
import com.karate.tournament.dto.request.TuitionOverrideBulkRequest;
import com.karate.tournament.dto.response.ClubFinanceExpenseResponse;
import com.karate.tournament.dto.response.ClubFeeItemResponse;
import com.karate.tournament.dto.response.ClubFeeOverviewResponse;
import com.karate.tournament.dto.request.ClubFeeRoleRequest;
import com.karate.tournament.dto.response.ClubFeeRoleResponse;
import com.karate.tournament.dto.response.MemberFeeAssignmentResponse;
import com.karate.tournament.dto.request.MemberFeeAssignmentUpdateRequest;
import com.karate.tournament.dto.response.MemberFeeRoleResponse;
import com.karate.tournament.dto.request.MemberFeeRoleUpdateRequest;
import com.karate.tournament.dto.response.MemberTuitionOverrideResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations/{organizationId}")
@RequiredArgsConstructor
public class ClubFeeController {
  private final ClubFeeService fees;

  @GetMapping("/fees/overview")
  public ClubFeeOverviewResponse overview(@PathVariable UUID organizationId) {
    return fees.overview(organizationId);
  }

  @GetMapping("/finance/overview")
  public ClubFeeOverviewResponse financeOverview(@PathVariable UUID organizationId) {
    return fees.overview(organizationId);
  }

  @PostMapping("/fee-roles")
  @ResponseStatus(HttpStatus.CREATED)
  public ClubFeeRoleResponse createRole(@PathVariable UUID organizationId, @Valid @RequestBody ClubFeeRoleRequest request) {
    return fees.createRole(organizationId, request);
  }

  @PatchMapping("/fee-roles/{roleId}")
  public ClubFeeRoleResponse updateRole(
      @PathVariable UUID organizationId,
      @PathVariable UUID roleId,
      @Valid @RequestBody ClubFeeRoleRequest request
  ) {
    return fees.updateRole(organizationId, roleId, request);
  }

  @DeleteMapping("/fee-roles/{roleId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteRole(@PathVariable UUID organizationId, @PathVariable UUID roleId) {
    fees.deleteRole(organizationId, roleId);
  }

  @PutMapping("/members/{memberId}/fee-roles")
  public MemberFeeRoleResponse setMemberRoles(
      @PathVariable UUID organizationId,
      @PathVariable UUID memberId,
      @RequestBody MemberFeeRoleUpdateRequest request
  ) {
    return fees.setMemberRoles(organizationId, memberId, request);
  }

  @PutMapping("/members/fee-roles/bulk")
  public List<MemberFeeRoleResponse> bulkSetMemberRoles(
      @PathVariable UUID organizationId,
      @RequestBody BulkMemberFeeRoleUpdateRequest request
  ) {
    return fees.bulkSetMemberRoles(organizationId, request);
  }

  @PutMapping("/finance/tuition-overrides/bulk")
  public List<MemberTuitionOverrideResponse> bulkSetTuitionOverrides(
      @PathVariable UUID organizationId,
      @RequestBody TuitionOverrideBulkRequest request
  ) {
    return fees.bulkSetTuitionOverrides(organizationId, request);
  }

  @PostMapping("/fee-items")
  @ResponseStatus(HttpStatus.CREATED)
  public ClubFeeItemResponse createFeeItem(@PathVariable UUID organizationId, @Valid @RequestBody ClubFeeItemRequest request) {
    return fees.createFeeItem(organizationId, request);
  }

  @PatchMapping("/fee-items/{feeItemId}")
  public ClubFeeItemResponse updateFeeItem(
      @PathVariable UUID organizationId,
      @PathVariable UUID feeItemId,
      @Valid @RequestBody ClubFeeItemRequest request
  ) {
    return fees.updateFeeItem(organizationId, feeItemId, request);
  }

  @DeleteMapping("/fee-items/{feeItemId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteFeeItem(@PathVariable UUID organizationId, @PathVariable UUID feeItemId) {
    fees.deleteFeeItem(organizationId, feeItemId);
  }

  @PostMapping("/fee-items/{feeItemId}/apply")
  public List<MemberFeeAssignmentResponse> applyFeeItem(
      @PathVariable UUID organizationId,
      @PathVariable UUID feeItemId,
      @RequestBody ApplyFeeItemRequest request
  ) {
    return fees.applyFeeItem(organizationId, feeItemId, request);
  }

  @PatchMapping("/fee-assignments/{assignmentId}")
  public MemberFeeAssignmentResponse updateAssignment(
      @PathVariable UUID organizationId,
      @PathVariable UUID assignmentId,
      @RequestBody MemberFeeAssignmentUpdateRequest request
  ) {
    return fees.updateAssignment(organizationId, assignmentId, request);
  }

  @DeleteMapping("/fee-assignments/{assignmentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteAssignment(@PathVariable UUID organizationId, @PathVariable UUID assignmentId) {
    fees.deleteAssignment(organizationId, assignmentId);
  }

  @GetMapping("/finance/expenses")
  public List<ClubFinanceExpenseResponse> listExpenses(@PathVariable UUID organizationId) {
    return fees.listExpenses(organizationId);
  }

  @PostMapping("/finance/expenses")
  @ResponseStatus(HttpStatus.CREATED)
  public ClubFinanceExpenseResponse createExpense(
      @PathVariable UUID organizationId,
      @Valid @RequestBody ClubFinanceExpenseRequest request
  ) {
    return fees.createExpense(organizationId, request);
  }

  @PatchMapping("/finance/expenses/{expenseId}")
  public ClubFinanceExpenseResponse updateExpense(
      @PathVariable UUID organizationId,
      @PathVariable UUID expenseId,
      @Valid @RequestBody ClubFinanceExpenseRequest request
  ) {
    return fees.updateExpense(organizationId, expenseId, request);
  }

  @DeleteMapping("/finance/expenses/{expenseId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteExpense(@PathVariable UUID organizationId, @PathVariable UUID expenseId) {
    fees.deleteExpense(organizationId, expenseId);
  }
}
