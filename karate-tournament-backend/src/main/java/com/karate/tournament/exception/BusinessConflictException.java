package com.karate.tournament.exception;

public class BusinessConflictException extends ApiException {
  public BusinessConflictException(String message) {
    super("BUSINESS_CONFLICT", message);
  }
}
