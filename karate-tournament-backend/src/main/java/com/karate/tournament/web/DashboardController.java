package com.karate.tournament.web;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.DashboardService;
import com.karate.tournament.dto.response.CategoryDashboardResponse;
import com.karate.tournament.dto.response.DashboardOverviewResponse;
import com.karate.tournament.dto.response.MedalTableRow;
import com.karate.tournament.dto.response.TatamiDashboardRow;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/tournaments/{tournamentId}")
@RequiredArgsConstructor
public class DashboardController {
  private final DashboardService dashboard;

  @GetMapping("/overview")
  public DashboardOverviewResponse overview(@PathVariable UUID tournamentId) {
    return dashboard.overview(tournamentId);
  }

  @GetMapping("/medals")
  public List<MedalTableRow> medals(@PathVariable UUID tournamentId) {
    return dashboard.medals(tournamentId);
  }

  @GetMapping("/tatamis")
  public List<TatamiDashboardRow> tatamis(@PathVariable UUID tournamentId) {
    return dashboard.tatamis(tournamentId);
  }

  @GetMapping("/categories/{categoryId}")
  public CategoryDashboardResponse category(@PathVariable UUID tournamentId, @PathVariable UUID categoryId) {
    return dashboard.category(tournamentId, categoryId);
  }
}
