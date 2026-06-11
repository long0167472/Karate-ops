package com.karate.tournament.dto.request;

import java.util.List;
import java.util.UUID;

public record BulkMemberFeeRoleUpdateRequest(
    List<UUID> memberIds,
    List<UUID> feeRoleIds,
    Mode mode
) {
  public enum Mode {
    ADD,
    REPLACE,
    REMOVE
  }
}
