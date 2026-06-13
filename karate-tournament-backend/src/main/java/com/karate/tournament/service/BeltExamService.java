package com.karate.tournament.service;

import com.karate.tournament.dto.request.BeltExamCandidateRequest;
import com.karate.tournament.dto.request.BeltExamCandidateUpdateRequest;
import com.karate.tournament.dto.request.BeltExamCreateRequest;
import com.karate.tournament.dto.request.BeltExamUpdateRequest;
import com.karate.tournament.dto.response.BeltExamCandidateResponse;
import com.karate.tournament.dto.response.BeltExamResponse;
import java.util.List;
import java.util.UUID;

public interface BeltExamService {
  List<BeltExamResponse> list(UUID organizationId);
  BeltExamResponse get(UUID examId);
  BeltExamResponse create(UUID organizationId, BeltExamCreateRequest request);
  BeltExamResponse update(UUID examId, BeltExamUpdateRequest request);
  void delete(UUID examId);
  BeltExamCandidateResponse addCandidate(UUID examId, BeltExamCandidateRequest request);
  BeltExamCandidateResponse updateCandidate(UUID examId, UUID candidateId, BeltExamCandidateUpdateRequest request);
  void removeCandidate(UUID examId, UUID candidateId);
  BeltExamResponse applyResults(UUID examId);
}
