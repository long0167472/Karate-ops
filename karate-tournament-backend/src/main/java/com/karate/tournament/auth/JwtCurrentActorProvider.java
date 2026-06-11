package com.karate.tournament.auth;

import com.karate.tournament.exception.UnauthorizedException;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class JwtCurrentActorProvider implements CurrentActorProvider {
  @Override
  public CurrentActor currentActor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal principal)) {
      throw new UnauthorizedException("Authentication is required");
    }
    return principal.actor();
  }
}
