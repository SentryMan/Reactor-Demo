package com.jojo.demo.advice;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AdviceClass {

  public Mono<ServerResponse> handle(ResponseStatusException exception) {
    log.error("API Exception Occured : {}", exception.getMessage());

    return ServerResponse.status(500).bodyValue("Some Downstream Error Happened");
  }

  public Mono<ServerResponse> handle(Throwable exception) {
    log.error("Exception Occured : {}", exception.getMessage());

    return ServerResponse.status(500).bodyValue("Some error Happened");
  }
}
