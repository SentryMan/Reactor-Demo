package com.jojo.demo.filter;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AdviceClass {
  Mono<ServerResponse> handle(ResponseStatusException exception) {
    log.info("API Exception Occured: ", exception.getMessage());
    return ServerResponse.status(500).bodyValue("Downstream Error");
  }

  Mono<ServerResponse> handle(Throwable exception) {
    log.info("API Exception Occured: ", exception.getMessage());
    return ServerResponse.status(500).bodyValue("Some Error Happened");
  }
}
