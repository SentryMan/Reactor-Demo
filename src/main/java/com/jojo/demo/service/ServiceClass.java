package com.jojo.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
@RequiredArgsConstructor
public class ServiceClass {

  public static final String ENDPOINT_LOCAL = "http://localhost:8080";

  private final WebClient client;

  Retry retrySpec;

  // hardcoded for local
  public Mono<String> localEndpoint() {

    final var helloMono =
        client
            .get()
            .uri(ENDPOINT_LOCAL + "/hello")
            .retrieve()
            .bodyToMono(String.class)
            .checkpoint("Hello CheckPoint");

    final var worldMono =
        client
            .get()
            .uri(ENDPOINT_LOCAL + "/world")
            .retrieve()
            .bodyToMono(String.class)
            .checkpoint("World CheckPoint");

    return Mono.zip(helloMono, worldMono, (h, w) -> h + w);
  }

  // how you'd normally call webclient
  public Mono<String> testClient(String value) {
    return client
        .get()
        // no http in path. so will default to base url defined in the config class
        // in test props we can set to local wiremock host for easy testing
        .uri("/hello?param=" + value)
        .retrieve()
        .bodyToMono(String.class)
        .checkpoint("checky");
  }
}
