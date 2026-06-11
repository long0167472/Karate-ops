package com.karate.tournament.service;

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
import java.util.List;
import java.util.UUID;

public interface ClubFeeService {
  ClubFeeOverviewResponse overview(UUID organizationId);
  ClubFeeRoleResponse createRole(UUID organizationId, ClubFeeRoleRequest request);
  ClubFeeRoleResponse updateRole(UUID organizationId, UUID roleId, ClubFeeRoleRequest request);
  void deleteRole(UUID organizationId, UUID roleId);
  MemberFeeRoleResponse setMemberRoles(UUID organizationId, UUID memberId, MemberFeeRoleUpdateRequest request);
  List<MemberFeeRoleResponse> bulkSetMemberRoles(UUID organizationId, BulkMemberFeeRoleUpdateRequest request);
  List<MemberTuitionOverrideResponse> bulkSetTuitionOverrides(UUID organizationId, TuitionOverrideBulkRequest request);
  ClubFeeItemResponse createFeeItem(UUID organizationId, ClubFeeItemRequest request);
  ClubFeeItemResponse updateFeeItem(UUID organizationId, UUID feeItemId, ClubFeeItemRequest request);
  void deleteFeeItem(UUID organizationId, UUID feeItemId);
  List<MemberFeeAssignmentResponse> applyFeeItem(UUID organizationId, UUID feeItemId, ApplyFeeItemRequest request);
  MemberFeeAssignmentResponse updateAssignment(UUID organizationId, UUID assignmentId, MemberFeeAssignmentUpdateRequest request);
  void deleteAssignment(UUID organizationId, UUID assignmentId);
  List<ClubFinanceExpenseResponse> listExpenses(UUID organizationId);
  ClubFinanceExpenseResponse createExpense(UUID organizationId, ClubFinanceExpenseRequest request);
  ClubFinanceExpenseResponse updateExpense(UUID organizationId, UUID expenseId, ClubFinanceExpenseRequest request);
  void deleteExpense(UUID organizationId, UUID expenseId);
}
