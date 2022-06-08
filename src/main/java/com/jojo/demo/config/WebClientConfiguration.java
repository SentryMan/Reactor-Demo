package com.jojo.demo.config;

import static com.jojo.demo.domain.Constants.CORRELATION_ID;
import static com.jojo.demo.domain.Constants.MESSAGE_ID;
import static com.jojo.demo.domain.Constants.RETRY_EXHAUST_SPEC;
import static com.jojo.demo.util.ReactorLoggingUTIL.logOnTerminate;

import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.server.ResponseStatusException;

import io.netty.channel.ChannelOption;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

/**
 * Configures a {@link WebClient} with OAuth and request logging
 *
 * @author nhn485,
 */
@Slf4j
@Configuration
public class WebClientConfiguration {

  @Value("${base.url}")
  private String baseUrl;

  @Value("${connect.timeout:1000}")
  private Integer connectTimeOut;

  @Value("${read.timeout:1000}")
  private long readTimeOut;

  @Value("${request.retry.backoff:500}")
  private Integer retryBackoff;

  @Value("${request.retry.max:2}")
  private Integer maxRetries;

  @Getter private Retry retrySpec;

  @PostConstruct
  public void setRetrySpec() {

    // Retry Spec that Retries 5xx and 429 Exceptions
    retrySpec =
        Retry.fixedDelay(maxRetries, Duration.ofMillis(retryBackoff))
            .filter(
                ex -> {
                  if (ex instanceof final ResponseStatusException rse) {
                    final var status = rse.getStatusCode();
                    return !status.is4xxClientError()
                        || status == HttpStatus.TOO_MANY_REQUESTS
                        || status == HttpStatusCode.valueOf(499);
                  }
                  return false;
                })
            .onRetryExhaustedThrow(RETRY_EXHAUST_SPEC);
  }

  // constructs webclient from spring builder
  // all instances of webclient that use ReactorClientHttpConnector share the same thread pool
  // so it's better to use one webclient everywhere to simplify tests
  @Bean
  public WebClient buildWebClient(WebClient.Builder webClientBuilder) {

    final var connector =
    		new ReactorClientHttpConnector(
    		        HttpClient.create()
    		            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeOut)
    		            .responseTimeout(Duration.ofMillis(readTimeOut)));

    return webClientBuilder
        .baseUrl(baseUrl)
        .clientConnector(connector)
        .filter(this::contextHeaderFilter)
        .filter(this::logRequestFilter)
        .filter(this::exceptionRetryFilter)
        .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  // Filter That retries failed requests
  private final Mono<ClientResponse> exceptionRetryFilter(
      ClientRequest clientRequest, ExchangeFunction next) {

    return next.exchange(clientRequest)
        .timeout(Duration.ofMillis(readTimeOut + connectTimeOut))
        .flatMap(
            response ->
                response.statusCode() != null && response.statusCode().isError()
                    ? convertStatus2ErrorSignal(response)
                    : Mono.just(response))
        .retryWhen(retrySpec)
        .onErrorMap(
            TimeoutException.class,
            ex ->
                new ResponseStatusException(
                    HttpStatusCode.valueOf(499), "Request Aborted: Exceeded Read Timeout", ex))
        .onErrorMap(
            WebClientRequestException.class,
            ex ->
                new ResponseStatusException(
                    HttpStatusCode.valueOf(500), "Request Failed for some exception", ex));
  }

  // converts error status to error Signal
  private static Mono<ClientResponse> convertStatus2ErrorSignal(ClientResponse clientResponse) {

    return clientResponse
        // get Error Response Body As Mono<String>
        .bodyToMono(String.class)
        // if no body return empty string
        .switchIfEmpty(Mono.just(""))
        // map string and status to an exception
        .map(
            errorResponseBody ->
                new ResponseStatusException(clientResponse.statusCode(), errorResponseBody))
        // flatmap into Error Signal
        .flatMap(Mono::error);
  }

  // augments outgoing requests with messageId/correlation if values are found in context
  private final Mono<ClientResponse> contextHeaderFilter(
      ClientRequest clientRequestt, ExchangeFunction next) {

    return Mono.deferContextual(
            reactorContext -> {
              final var modifiedRequest =
                  ClientRequest.from(clientRequestt)
                      .headers(
                          headers -> {
                            reactorContext
                                .getOrEmpty(CORRELATION_ID)
                                .map(Object::toString)
                                .ifPresent(id -> headers.add(CORRELATION_ID, id));

                            reactorContext
                                .getOrEmpty("message_id")
                                .map(Object::toString)
                                .ifPresent(id -> headers.add(MESSAGE_ID, id));
                          })
                      .build();

              return Mono.just(modifiedRequest);
            })
        .flatMap(next::exchange);
  }

  // logs current webclient request
  private final Mono<ClientResponse> logRequestFilter(
      ClientRequest clientRequest, ExchangeFunction next) {

    final var startTime = System.currentTimeMillis();

    final var entries = new HashMap<String, Object>();

    entries.put("HTTP Method", clientRequest.method());
    entries.put("URI", clientRequest.url());
    entries.putAll(clientRequest.headers().toSingleValueMap());
    entries.remove("Authorization");

    return next.exchange(clientRequest)
        .doOnNext(r -> entries.put("HTTP Status", r.statusCode().value()))
        .doOnError(
            ResponseStatusException.class, rse -> entries.put("HTTP Status", rse.getStatusCode().value()))
        .doOnEach(
            logOnTerminate(
                () -> {
                  entries.put("Response Time", System.currentTimeMillis() - startTime);

                  clientRequest
                      .attribute("API-Name")
                      .ifPresentOrElse(
                          name -> {
                            MDC.put(name + "_Response_Time", entries.get("Response Time") + "");
                            MDC.put(name + "_HTTP_Status", entries.get("HTTP Status") + "");

                            log.info("{} downstream request: \n {}", name, entries);
                          },
                          () -> log.info("Downstream request: \n {}", entries));
                }));
  }
}
