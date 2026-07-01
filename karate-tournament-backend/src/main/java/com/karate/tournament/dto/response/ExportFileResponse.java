package com.karate.tournament.dto.response;

public record ExportFileResponse(
    String filename,
    String contentType,
    String content
) {
}
