package com.karate.tournament.service;

import com.karate.tournament.dto.response.CategoryDashboardResponse;
import com.karate.tournament.dto.response.DashboardOverviewResponse;
import com.karate.tournament.dto.response.MedalTableRow;
import com.karate.tournament.dto.response.TatamiDashboardRow;
import java.util.List;
import java.util.UUID;

public interface DashboardService {
  DashboardOverviewResponse overview(UUID tournamentId);
  List<MedalTableRow> medals(UUID tournamentId);
  List<TatamiDashboardRow> tatamis(UUID tournamentId);
  CategoryDashboardResponse category(UUID tournamentId, UUID categoryId);
}
