package com.karate.tournament.service;

import com.karate.tournament.entity.Category;
import com.karate.tournament.entity.Entry;
import com.karate.tournament.dto.request.CategoryCreateRequest;
import com.karate.tournament.dto.response.CategoryResponse;
import com.karate.tournament.dto.request.CategoryUpdateRequest;
import com.karate.tournament.dto.request.EntryCreateRequest;
import com.karate.tournament.dto.response.EntryResponse;
import java.util.List;
import java.util.UUID;

public interface CategoryService {
  List<CategoryResponse> list(UUID tournamentId);
  CategoryResponse get(UUID id);
  CategoryResponse create(UUID tournamentId, CategoryCreateRequest request);
  CategoryResponse update(UUID id, CategoryUpdateRequest request);
  void delete(UUID id);
  List<EntryResponse> listEntries(UUID categoryId);
  EntryResponse addEntry(UUID categoryId, EntryCreateRequest request);
  void deleteEntry(UUID categoryId, UUID entryId);
  Category requireCategory(UUID id);
  Entry requireEntry(UUID id);
}
