package com.karate.tournament.web;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.auth.CurrentActorProvider;
import com.karate.tournament.service.AuthService;
import com.karate.tournament.dto.response.AuthResponse;
import com.karate.tournament.dto.response.AuthUserResponse;
import com.karate.tournament.dto.request.LoginRequest;
import com.karate.tournament.dto.request.RegisterClubManagerRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService auth;
  private final CurrentActorProvider currentActorProvider;

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return auth.login(request);
  }

  @PostMapping("/register-club-manager")
  @ResponseStatus(HttpStatus.CREATED)
  public AuthResponse registerClubManager(@Valid @RequestBody RegisterClubManagerRequest request) {
    return auth.registerClubManager(request);
  }

  @GetMapping("/me")
  public AuthUserResponse me() {
    return auth.me(currentActorProvider.currentActor());
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout() {
  }
}
