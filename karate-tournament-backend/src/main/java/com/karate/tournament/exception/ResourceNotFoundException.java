package com.karate.tournament.exception;

public class ResourceNotFoundException extends ApiException {
  public ResourceNotFoundException(String message) {
    super("RESOURCE_NOT_FOUND", message);
  }
}
