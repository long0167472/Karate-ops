package com.karate.tournament.auth.impl;

import com.karate.tournament.auth.*;
import com.karate.tournament.exception.UnauthorizedException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karate.tournament.entity.enums.SystemRole;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {
  private final ObjectMapper objectMapper;
  @Value("${app.security.jwt.secret}")
  private final String secret;
  @Value("${app.security.jwt.expires-minutes}")
  private final long expiresMinutes;

  public long expiresSeconds() {
    return expiresMinutes * 60;
  }

  public String createToken(AuthenticatedPrincipal principal) {
    Instant now = Instant.now();
    Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("sub", principal.userId().toString());
    payload.put("email", principal.email());
    payload.put("name", principal.displayName());
    payload.put("primaryOrganizationId", principal.primaryOrganizationId() == null ? null : principal.primaryOrganizationId().toString());
    payload.put("roles", principal.roles().stream().map(Enum::name).sorted().toList());
    payload.put("iat", now.getEpochSecond());
    payload.put("exp", now.plusSeconds(expiresSeconds()).getEpochSecond());
    String unsigned = encode(header) + "." + encode(payload);
    return unsigned + "." + sign(unsigned);
  }

  public AuthenticatedPrincipal verify(String token) {
    try {
      String[] parts = token.split("\\.");
      if (parts.length != 3) throw new UnauthorizedException("Invalid token");
      String unsigned = parts[0] + "." + parts[1];
      if (!constantTimeEquals(sign(unsigned), parts[2])) throw new UnauthorizedException("Invalid token signature");
      Map<String, Object> payload = objectMapper.readValue(
          Base64.getUrlDecoder().decode(parts[1]),
          new TypeReference<Map<String, Object>>() {}
      );
      long exp = ((Number) payload.get("exp")).longValue();
      if (Instant.now().getEpochSecond() >= exp) throw new UnauthorizedException("Token expired");
      UUID orgId = payload.get("primaryOrganizationId") == null ? null : UUID.fromString(String.valueOf(payload.get("primaryOrganizationId")));
      @SuppressWarnings("unchecked")
      List<String> roleNames = (List<String>) payload.get("roles");
      Set<SystemRole> roles = roleNames.stream().map(SystemRole::valueOf).collect(Collectors.toCollection(LinkedHashSet::new));
      roles.add(SystemRole.MEMBER);
      return new AuthenticatedPrincipal(
          UUID.fromString(String.valueOf(payload.get("sub"))),
          orgId,
          String.valueOf(payload.get("email")),
          String.valueOf(payload.get("name")),
          roles
      );
    } catch (UnauthorizedException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new UnauthorizedException("Invalid token");
    }
  }

  private String encode(Object value) {
    try {
      return Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(value));
    } catch (Exception exception) {
      throw new UnauthorizedException("Could not create token");
    }
  }

  private String sign(String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256"));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new UnauthorizedException("Could not sign token");
    }
  }

  private boolean constantTimeEquals(String a, String b) {
    byte[] left = a.getBytes(StandardCharsets.UTF_8);
    byte[] right = b.getBytes(StandardCharsets.UTF_8);
    if (left.length != right.length) return false;
    int result = 0;
    for (int index = 0; index < left.length; index += 1) {
      result |= left[index] ^ right[index];
    }
    return result == 0;
  }
}
