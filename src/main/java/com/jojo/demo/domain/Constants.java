package com.jojo.demo.domain;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.function.Predicate;

import org.springframework.web.server.ResponseStatusException;

public class Constants {
  private Constants() {}

  // Header Constants
  public static final String CORRELATION_ID = "Client-Correlation-Id";
  public static final String MESSAGE_ID = "x-message-id";
  public static final String API_KEY = "Api-Key";

  /** Predicate for HTTP 404 Exceptions */
  public static final Predicate<Throwable> HTTP_404_ERROR =
      ex ->
          ex instanceof ResponseStatusException
              && ((ResponseStatusException) ex).getStatusCode() == NOT_FOUND;

  /** Bifunction to rethrow original exception when a RetrySpec exhausts */
  public static final RetryExhaustSpec RETRY_EXHAUST_SPEC = (spec, rs) -> rs.failure();
}
