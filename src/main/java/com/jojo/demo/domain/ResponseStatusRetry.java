package com.jojo.demo.domain;

import java.time.Duration;

import org.springframework.web.server.ResponseStatusException;

import reactor.util.retry.Retry;

/**
 * Configures a {@link Retry} for Retrying Response Status Exceptions
 *
 * @author nhn485
 * @see <a href="https://projectreactor.io/docs/core/release/reference/#_retrying">Retrying a
 *     Sequence</a>
 */
public class ResponseStatusRetry {

  private static final long RETRY_BACKOFF = 1000;
  private static final long RETRY_MAX = 2;

  public static Retry getRetrySpecification() {

    return Retry.fixedDelay(RETRY_MAX, Duration.ofMillis(RETRY_BACKOFF))
        .filter(ResponseStatusRetry::retryPredicate)
        // throw original exception on retry exhaust
        .onRetryExhaustedThrow((spec, rs) -> rs.failure())
        .doBeforeRetry(
            rs ->
                System.out.println(
                    String.format(
                        "Executing RetrySpec. Total Retries: %s; Total Retries In a Row: %s;",
                        rs.totalRetries(), rs.totalRetriesInARow())));
  }

  // retry if exception is of type responseStatusException
  // And does not have a status of 400
  private static boolean retryPredicate(Throwable ex) {

    return ex instanceof final ResponseStatusException rse
        && !rse.getStatusCode().is4xxClientError();
  }
}
