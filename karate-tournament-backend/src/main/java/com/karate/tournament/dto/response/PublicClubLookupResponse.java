package com.karate.tournament.dto.response;

import java.util.UUID;

public record PublicClubLookupResponse(
    UUID id,
    String name,
    String shortName,
    String code,
    String province,
    String address,
    String contactEmail,
    String contactPhone
) {
}
