package com.jojo.demo.web;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
public class Handler {

  public static final String ENDPOINT = "http://localhost:8080";

  private final WebClient client;

  @Qualifier("WebClient Retry")
  Retry retrySpec;

  Handler(WebClient.Builder builder, @Qualifier("WebClient Retry") Retry retrySpec) {
    this.client = builder.build();
    this.retrySpec = retrySpec;
  }

  Mono<ServerResponse> handle(ServerRequest request) {

    final Mono<String> helloMono =
        client
            .get()
            .uri(ENDPOINT + "/hello")
            .retrieve()
            .onStatus(HttpStatus::isError, this::convertErrorStatus2ErrorSignal)
            .bodyToMono(String.class)
            .checkpoint("Hello CheckPoint")
            .retryWhen(retrySpec);

    final Mono<String> worldMono =
        client
            .get()
            .uri(ENDPOINT + "/world")
            .retrieve()
            .onStatus(HttpStatus::isError, this::convertErrorStatus2ErrorSignal)
            .bodyToMono(String.class)
            .checkpoint("World CheckPoint")
            .retryWhen(retrySpec);

    return Mono.zip(helloMono, worldMono, (h, w) -> h + w)
        .flatMap(s -> ServerResponse.ok().bodyValue(s));
  }

  private Mono<ResponseStatusException> convertErrorStatus2ErrorSignal(
      ClientResponse clientResponse) {

    return clientResponse
        // get Error Response Body As Mono<String>
        .bodyToMono(String.class)
        // flatmap into Error Signal
        .flatMap(
            errorResponseBody ->
                Mono.error(
                    new ResponseStatusException(clientResponse.statusCode(), errorResponseBody)));
  }
}
