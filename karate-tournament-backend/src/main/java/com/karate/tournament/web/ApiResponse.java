package com.karate.tournament.web;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public record ApiResponse<T>(
    boolean success,
    int status,
    String code,
    String message,
    T data,
    Instant at,
    String path,
    List<ApiErrorDetail> details
) {
  public static <T> ApiResponse<T> success(HttpStatusCode status, T data, String path) {
    return new ApiResponse<>(
        true,
        status.value(),
        "SUCCESS",
        reason(status),
        data,
        Instant.now(),
        path,
        List.of()
    );
  }

  public static ApiResponse<Void> error(HttpStatusCode status, String code, String message, String path) {
    return error(status, code, message, path, List.of());
  }

  public static ApiResponse<Void> error(
      HttpStatusCode status,
      String code,
      String message,
      String path,
      List<ApiErrorDetail> details
  ) {
    return new ApiResponse<>(
        false,
        status.value(),
        code,
        message,
        null,
        Instant.now(),
        path,
        details
    );
  }

  private static String reason(HttpStatusCode status) {
    HttpStatus resolved = HttpStatus.resolve(status.value());
    return resolved == null ? "Success" : resolved.getReasonPhrase();
  }

  public record ApiErrorDetail(String field, String message) {
  }
}
