package com.karate.tournament.web;

import com.karate.tournament.exception.ApiException;
import com.karate.tournament.exception.BadRequestException;
import com.karate.tournament.exception.BusinessConflictException;
import com.karate.tournament.exception.ForbiddenException;
import com.karate.tournament.exception.ResourceNotFoundException;
import com.karate.tournament.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(ResourceNotFoundException.class)
  ResponseEntity<ApiResponse<Void>> notFound(ResourceNotFoundException exception, HttpServletRequest request) {
    return error(HttpStatus.NOT_FOUND, exception, request);
  }

  @ExceptionHandler(BusinessConflictException.class)
  ResponseEntity<ApiResponse<Void>> conflict(BusinessConflictException exception, HttpServletRequest request) {
    return error(HttpStatus.CONFLICT, exception, request);
  }

  @ExceptionHandler(ForbiddenException.class)
  ResponseEntity<ApiResponse<Void>> forbidden(ForbiddenException exception, HttpServletRequest request) {
    return error(HttpStatus.FORBIDDEN, exception, request);
  }

  @ExceptionHandler(UnauthorizedException.class)
  ResponseEntity<ApiResponse<Void>> unauthorized(UnauthorizedException exception, HttpServletRequest request) {
    return error(HttpStatus.UNAUTHORIZED, exception, request);
  }

  @ExceptionHandler(BadRequestException.class)
  ResponseEntity<ApiResponse<Void>> badRequest(BadRequestException exception, HttpServletRequest request) {
    return error(HttpStatus.BAD_REQUEST, exception, request);
  }

  @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
  ResponseEntity<ApiResponse<Void>> badRequest(RuntimeException exception, HttpServletRequest request) {
    return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
    List<ApiResponse.ApiErrorDetail> details = exception.getBindingResult().getFieldErrors().stream()
        .map(error -> new ApiResponse.ApiErrorDetail(error.getField(), error.getDefaultMessage()))
        .toList();
    String message = exception.getBindingResult().getFieldErrors().stream()
        .findFirst()
        .map(error -> error.getField() + " " + error.getDefaultMessage())
        .orElse("Validation failed");
    return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, request, details);
  }

  private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, ApiException exception, HttpServletRequest request) {
    return error(status, exception.code(), exception.getMessage(), request);
  }

  private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String code, String message, HttpServletRequest request) {
    return error(status, code, message, request, List.of());
  }

  private ResponseEntity<ApiResponse<Void>> error(
      HttpStatus status,
      String code,
      String message,
      HttpServletRequest request,
      List<ApiResponse.ApiErrorDetail> details
  ) {
    return ResponseEntity.status(status)
        .body(ApiResponse.error(status, code, message, request.getRequestURI(), details));
  }
}
