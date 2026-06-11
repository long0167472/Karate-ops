package com.karate.tournament.service;

import com.karate.tournament.entity.Organization;
import com.karate.tournament.repository.AppUserRepository;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UsernameGenerator {
  private final AppUserRepository users;

  public String uniqueUsername(Organization organization, String displayName) {
    String prefix = normalizeToken(organization.code != null && !organization.code.isBlank() ? organization.code : organization.name);
    if (prefix.isBlank()) {
      prefix = "club";
    }
    String nameToken = nameToken(displayName);
    String base = trimBase(prefix + "." + nameToken);
    String candidate = base;
    int suffix = 1;
    while (users.existsByUsernameIgnoreCaseAndDeletedAtIsNull(candidate)) {
      candidate = trimBase(base, String.valueOf(suffix));
      suffix += 1;
    }
    return candidate;
  }

  private String nameToken(String displayName) {
    String[] parts = Arrays.stream(stripDiacritics(displayName).toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
        .filter(part -> !part.isBlank())
        .toArray(String[]::new);
    if (parts.length == 0) {
      return "user";
    }
    if (parts.length == 1) {
      return parts[0];
    }
    StringBuilder builder = new StringBuilder();
    for (int index = 0; index < parts.length - 1; index += 1) {
      builder.append(parts[index].charAt(0));
    }
    builder.append(parts[parts.length - 1]);
    return builder.toString();
  }

  private String normalizeToken(String value) {
    return stripDiacritics(value)
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "")
        .trim();
  }

  private String stripDiacritics(String value) {
    String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD);
    return normalized.replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D');
  }

  private String trimBase(String base) {
    return base.length() <= 112 ? base : base.substring(0, 112);
  }

  private String trimBase(String base, String suffix) {
    int maxBaseLength = 120 - suffix.length();
    String trimmed = base.length() <= maxBaseLength ? base : base.substring(0, maxBaseLength);
    return trimmed + suffix;
  }
}
