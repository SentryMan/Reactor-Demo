package com.jojo.demo.filter;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;

import com.jojo.demo.advice.AdviceClass;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Slf4j
@Component
public class ContextFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

  // Advices are used in filters with webflux
  @Autowired private AdviceClass advice;

  @Override
  public Mono<ServerResponse> filter(
      ServerRequest serverRequest, HandlerFunction<ServerResponse> handlerFunction) {

    final var requestStart = Instant.now();
    final var headers = serverRequest.headers().asHttpHeaders().toSingleValueMap();
    final Map<String, Object> requestDetails = new HashMap<>();

    // create map of request details
    requestDetails.put("HTTP Method", serverRequest.method());
    requestDetails.put("URI", serverRequest);
    requestDetails.putAll(serverRequest.pathVariables());
    requestDetails.putAll(serverRequest.queryParams());
    requestDetails.putAll(headers);

    return handlerFunction
        // call handler Method
        .handle(serverRequest)
        // handle exceptions with advice
        .onErrorResume(ResponseStatusException.class, advice::handle)
        .onErrorResume(advice::handle)
        // log response
        .doOnNext(
            serverResponse -> {
              final var responseTime = Duration.between(requestStart, Instant.now()).toMillis();

              final var responseDetails =
                  Map.of(
                      "Response Time", responseTime, "HTTP Status", serverResponse.rawStatusCode());

              MDC.put("Response Time", responseTime + "");
              MDC.put("HTTP Status", serverResponse.statusCode().value() + "");

              log.info("Response Details:\n{}", responseDetails);
            })
        // add request details to reactor context for MDC logging
        .contextWrite(Context.of(requestDetails));
  }
}
