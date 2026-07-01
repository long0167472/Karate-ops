package com.karate.tournament.dto.request;

import jakarta.validation.constraints.Size;

public record DecisionNoteRequest(
    @Size(max = 500) String decisionNote
) {
}
