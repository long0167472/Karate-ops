package com.karate.tournament.exception;

public class UnauthorizedException extends ApiException {
  public UnauthorizedException(String message) {
    super("UNAUTHORIZED", message);
  }
}
