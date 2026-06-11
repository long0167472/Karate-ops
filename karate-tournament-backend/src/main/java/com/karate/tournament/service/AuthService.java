package com.karate.tournament.service;

import com.karate.tournament.auth.CurrentActor;
import com.karate.tournament.dto.response.AuthResponse;
import com.karate.tournament.dto.response.AuthUserResponse;
import com.karate.tournament.dto.request.LoginRequest;
import com.karate.tournament.dto.request.RegisterClubManagerRequest;

public interface AuthService {
  AuthResponse login(LoginRequest request);
  AuthResponse registerClubManager(RegisterClubManagerRequest request);
  AuthUserResponse me(CurrentActor actor);
}
