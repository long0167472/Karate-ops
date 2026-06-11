package com.karate.tournament.web;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.CategoryService;
import com.karate.tournament.service.DrawService;
import com.karate.tournament.dto.request.CategoryCreateRequest;
import com.karate.tournament.dto.response.CategoryResponse;
import com.karate.tournament.dto.request.CategoryUpdateRequest;
import com.karate.tournament.dto.request.DrawRequest;
import com.karate.tournament.dto.response.DrawResponse;
import com.karate.tournament.dto.request.EntryCreateRequest;
import com.karate.tournament.dto.response.EntryResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CategoryController {
  private final CategoryService categories;
  private final DrawService drawService;

  @GetMapping("/tournaments/{tournamentId}/categories")
  public List<CategoryResponse> list(@PathVariable UUID tournamentId) {
    return categories.list(tournamentId);
  }

  @PostMapping("/tournaments/{tournamentId}/categories")
  @ResponseStatus(HttpStatus.CREATED)
  public CategoryResponse create(
      @PathVariable UUID tournamentId,
      @Valid @RequestBody CategoryCreateRequest request
  ) {
    return categories.create(tournamentId, request);
  }

  @GetMapping("/categories/{id}")
  public CategoryResponse get(@PathVariable UUID id) {
    return categories.get(id);
  }

  @PatchMapping("/categories/{id}")
  public CategoryResponse update(@PathVariable UUID id, @Valid @RequestBody CategoryUpdateRequest request) {
    return categories.update(id, request);
  }

  @DeleteMapping("/categories/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    categories.delete(id);
  }

  @GetMapping("/categories/{id}/entries")
  public List<EntryResponse> entries(@PathVariable UUID id) {
    return categories.listEntries(id);
  }

  @PostMapping("/categories/{id}/entries")
  @ResponseStatus(HttpStatus.CREATED)
  public EntryResponse addEntry(@PathVariable UUID id, @Valid @RequestBody EntryCreateRequest request) {
    return categories.addEntry(id, request);
  }

  @DeleteMapping("/categories/{id}/entries/{entryId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteEntry(@PathVariable UUID id, @PathVariable UUID entryId) {
    categories.deleteEntry(id, entryId);
  }

  @PostMapping("/categories/{id}/draw")
  public DrawResponse draw(@PathVariable UUID id, @RequestBody(required = false) DrawRequest request) {
    return drawService.draw(id, request);
  }
}
