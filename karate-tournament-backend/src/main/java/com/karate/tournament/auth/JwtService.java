package com.karate.tournament.auth;

public interface JwtService {
  long expiresSeconds();
  String createToken(AuthenticatedPrincipal principal);
  AuthenticatedPrincipal verify(String token);
}
