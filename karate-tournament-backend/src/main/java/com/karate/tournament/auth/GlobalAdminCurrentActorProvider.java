package com.karate.tournament.auth;

import com.karate.tournament.entity.enums.SystemRole;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class GlobalAdminCurrentActorProvider implements CurrentActorProvider {
  public static final UUID GLOBAL_ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  public static final UUID GLOBAL_ADMIN_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
  private static final ThreadLocal<CurrentActor> TEST_ACTOR = new ThreadLocal<>();

  public static void setTestActor(CurrentActor actor) {
    TEST_ACTOR.set(actor);
  }

  public static void clearTestActor() {
    TEST_ACTOR.remove();
  }

  @Override
  public CurrentActor currentActor() {
    CurrentActor testActor = TEST_ACTOR.get();
    if (testActor != null) {
      return testActor;
    }
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedPrincipal principal) {
      return principal.actor();
    }
    return new CurrentActor(GLOBAL_ADMIN_USER_ID, GLOBAL_ADMIN_ORG_ID, Set.of(SystemRole.GLOBAL_ADMIN));
  }
}
