package com.karate.tournament.service;

import com.karate.tournament.dto.request.DrawRequest;
import com.karate.tournament.dto.response.DrawResponse;
import java.util.UUID;

public interface DrawService {
  DrawResponse draw(UUID categoryId, DrawRequest request);
}
