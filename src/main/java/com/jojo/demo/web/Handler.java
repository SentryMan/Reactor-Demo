package com.jojo.demo.web;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.server.ServerResponse.BodyBuilder;

import com.jojo.demo.service.ServiceClass;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class Handler {

  private final ServiceClass service;

  private static final BodyBuilder OK = ServerResponse.ok();

  Mono<ServerResponse> handleLocal(ServerRequest request) {

    return service.localEndpoint().flatMap(OK::bodyValue);
  }
}
