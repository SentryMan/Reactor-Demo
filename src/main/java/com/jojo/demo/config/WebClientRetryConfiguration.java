package com.jojo.demo.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ResponseStatusException;
import reactor.util.retry.Retry;

/**
 * Configures a {@link Retry} for use in Webclient Flux/Monos
 *
 * @author nhn485
 * @see <a href="https://projectreactor.io/docs/core/release/reference/#_retrying">Retrying a
 *     Sequence</a>
 */
@Configuration
public class WebClientRetryConfiguration {

  private static final long RETRY_BACKOFF = 1000;
  private static final long RETRY_MAX = 2;

  @Bean("WebClient Retry")
  public Retry getRetrySpecification() {

    return Retry.fixedDelay(RETRY_MAX, Duration.ofMillis(RETRY_BACKOFF))
        .filter(this::retryPredicate)
        .onRetryExhaustedThrow((spec, rs) -> rs.failure());
  }

  private boolean retryPredicate(Throwable ex) {
    // retry if exception is of type responseStatusException
    // And does not have a status of 400
    if (ex instanceof ResponseStatusException)
      return !((ResponseStatusException) ex).getStatus().is4xxClientError();
    else return false;
  }
}
