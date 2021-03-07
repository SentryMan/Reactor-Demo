package com.jojo.demo.filter;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Slf4j
@Component
public class CustomFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

  // Advices are used in filters with webflux
  @Autowired private AdviceClass advice;

  @Override
  public Mono<ServerResponse> filter(
      ServerRequest serverRequest, HandlerFunction<ServerResponse> handlerFunction) {

    final Instant requestStart = Instant.now();
    final Map<String, String> headers = serverRequest.headers().asHttpHeaders().toSingleValueMap();
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
        // log incoming Request
        .doOnSubscribe(s -> log.info("Request_Details:\n{}", requestDetails))
        // handle exceptions
        .onErrorResume(ResponseStatusException.class, advice::handle)
        .onErrorResume(advice::handle)
        // log response
        .doOnNext(
            serverResponse -> {
              final Map<String, Object> responseDetails = new HashMap<>();
              final Instant end = Instant.now();
              responseDetails.put("Response Time", Duration.between(requestStart, end).toMillis());
              responseDetails.put("HTTP Status", serverResponse.rawStatusCode());
              log.info("Response_Details:\n{}", responseDetails);
            })
        // add request details to reactor context for MDC logging
        .contextWrite(Context.of(requestDetails));
  }
}
