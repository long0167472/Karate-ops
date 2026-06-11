package com.karate.tournament.web;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.karate.tournament.web")
public class ApiResponseBodyAdvice implements ResponseBodyAdvice<Object> {
  @Override
  public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter returnType,
      MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest request,
      ServerHttpResponse response
  ) {
    if (body == null || body instanceof ApiResponse<?>) {
      return body;
    }
    HttpStatusCode status = status(response);
    if (status.value() == HttpStatus.NO_CONTENT.value()) {
      return null;
    }
    return ApiResponse.success(status, body, request.getURI().getPath());
  }

  private HttpStatusCode status(ServerHttpResponse response) {
    if (response instanceof ServletServerHttpResponse servletResponse) {
      return HttpStatusCode.valueOf(servletResponse.getServletResponse().getStatus());
    }
    return HttpStatus.OK;
  }
}
