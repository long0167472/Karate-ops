package com.karate.tournament.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.karate.tournament.exception.BadRequestException;
import com.karate.tournament.exception.BusinessConflictException;
import com.karate.tournament.exception.ForbiddenException;
import com.karate.tournament.exception.ResourceNotFoundException;
import com.karate.tournament.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class ApiExceptionHandlerTest {
  private final MockMvc mvc = MockMvcBuilders
      .standaloneSetup(new ExceptionController())
      .setControllerAdvice(new ApiExceptionHandler())
      .build();

  @Test
  void mapsCustomExceptionsToStableErrorResponses() throws Exception {
    assertError("/exceptions/bad-request", 400, "BAD_REQUEST");
    assertError("/exceptions/unauthorized", 401, "UNAUTHORIZED");
    assertError("/exceptions/forbidden", 403, "FORBIDDEN");
    assertError("/exceptions/not-found", 404, "RESOURCE_NOT_FOUND");
    assertError("/exceptions/conflict", 409, "BUSINESS_CONFLICT");
  }

  private void assertError(String path, int expectedStatus, String expectedCode) throws Exception {
    mvc.perform(get(path))
        .andExpect(status().is(expectedStatus))
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.status").value(expectedStatus))
        .andExpect(jsonPath("$.code").value(expectedCode))
        .andExpect(jsonPath("$.path").value(path));
  }

  @RestController
  @RequestMapping("/exceptions")
  static class ExceptionController {
    @GetMapping("/bad-request")
    void badRequest() {
      throw new BadRequestException("Bad request");
    }

    @GetMapping("/unauthorized")
    void unauthorized() {
      throw new UnauthorizedException("Unauthorized");
    }

    @GetMapping("/forbidden")
    void forbidden() {
      throw new ForbiddenException("Forbidden");
    }

    @GetMapping("/not-found")
    void notFound() {
      throw new ResourceNotFoundException("Not found");
    }

    @GetMapping("/conflict")
    void conflict() {
      throw new BusinessConflictException("Conflict");
    }
  }
}
