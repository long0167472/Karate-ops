package com.karate.tournament.service;

import com.karate.tournament.entity.Athlete;
import com.karate.tournament.dto.request.AthleteCreateRequest;
import com.karate.tournament.dto.response.AthleteResponse;
import com.karate.tournament.dto.request.AthleteUpdateRequest;
import java.util.List;
import java.util.UUID;

public interface AthleteService {
  List<AthleteResponse> list();
  AthleteResponse get(UUID id);
  AthleteResponse create(AthleteCreateRequest request);
  AthleteResponse update(UUID id, AthleteUpdateRequest request);
  void delete(UUID id);
  Athlete requireAthlete(UUID id);
}
