package com.karate.tournament.web;

import lombok.RequiredArgsConstructor;
import com.karate.tournament.dto.request.BeltExamCandidateRequest;
import com.karate.tournament.dto.request.BeltExamCandidateUpdateRequest;
import com.karate.tournament.dto.request.BeltExamCreateRequest;
import com.karate.tournament.dto.request.BeltExamCriterionRequest;
import com.karate.tournament.dto.request.BeltExamScoreRequest;
import com.karate.tournament.dto.request.BeltExamUpdateRequest;
import com.karate.tournament.dto.response.BeltExamCandidateResponse;
import com.karate.tournament.dto.response.BeltExamCriterionResponse;
import com.karate.tournament.dto.response.BeltExamResponse;
import com.karate.tournament.service.BeltExamService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BeltExamController {
  private final BeltExamService beltExams;

  @GetMapping("/organizations/{organizationId}/belt-exams")
  public List<BeltExamResponse> list(@PathVariable UUID organizationId) {
    return beltExams.list(organizationId);
  }

  @PostMapping("/organizations/{organizationId}/belt-exams")
  @ResponseStatus(HttpStatus.CREATED)
  public BeltExamResponse create(
      @PathVariable UUID organizationId,
      @Valid @RequestBody BeltExamCreateRequest request
  ) {
    return beltExams.create(organizationId, request);
  }

  @GetMapping("/belt-exams/{examId}")
  public BeltExamResponse get(@PathVariable UUID examId) {
    return beltExams.get(examId);
  }

  @PatchMapping("/belt-exams/{examId}")
  public BeltExamResponse update(
      @PathVariable UUID examId,
      @Valid @RequestBody BeltExamUpdateRequest request
  ) {
    return beltExams.update(examId, request);
  }

  @DeleteMapping("/belt-exams/{examId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID examId) {
    beltExams.delete(examId);
  }

  @PostMapping("/belt-exams/{examId}/candidates")
  @ResponseStatus(HttpStatus.CREATED)
  public BeltExamCandidateResponse addCandidate(
      @PathVariable UUID examId,
      @Valid @RequestBody BeltExamCandidateRequest request
  ) {
    return beltExams.addCandidate(examId, request);
  }

  @PatchMapping("/belt-exams/{examId}/candidates/{candidateId}")
  public BeltExamCandidateResponse updateCandidate(
      @PathVariable UUID examId,
      @PathVariable UUID candidateId,
      @Valid @RequestBody BeltExamCandidateUpdateRequest request
  ) {
    return beltExams.updateCandidate(examId, candidateId, request);
  }

  @DeleteMapping("/belt-exams/{examId}/candidates/{candidateId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeCandidate(
      @PathVariable UUID examId,
      @PathVariable UUID candidateId
  ) {
    beltExams.removeCandidate(examId, candidateId);
  }

  @PostMapping("/belt-exams/{examId}/apply-results")
  public BeltExamResponse applyResults(@PathVariable UUID examId) {
    return beltExams.applyResults(examId);
  }

  @PostMapping("/belt-exams/{examId}/criteria")
  @ResponseStatus(HttpStatus.CREATED)
  public BeltExamCriterionResponse addCriterion(
      @PathVariable UUID examId,
      @Valid @RequestBody BeltExamCriterionRequest request
  ) {
    return beltExams.addCriterion(examId, request);
  }

  @PatchMapping("/belt-exams/{examId}/criteria/{criterionId}")
  public BeltExamCriterionResponse updateCriterion(
      @PathVariable UUID examId,
      @PathVariable UUID criterionId,
      @Valid @RequestBody BeltExamCriterionRequest request
  ) {
    return beltExams.updateCriterion(examId, criterionId, request);
  }

  @DeleteMapping("/belt-exams/{examId}/criteria/{criterionId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeCriterion(
      @PathVariable UUID examId,
      @PathVariable UUID criterionId
  ) {
    beltExams.removeCriterion(examId, criterionId);
  }

  @PutMapping("/belt-exams/{examId}/candidates/{candidateId}/scores/{criterionId}")
  public BeltExamCandidateResponse scoreCandidate(
      @PathVariable UUID examId,
      @PathVariable UUID candidateId,
      @PathVariable UUID criterionId,
      @Valid @RequestBody BeltExamScoreRequest request
  ) {
    return beltExams.scoreCandidate(examId, candidateId, criterionId, request);
  }
}
