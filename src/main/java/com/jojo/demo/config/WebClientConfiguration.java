package com.jojo.demo.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * Configures a {@link WebClient} with OAuth and request logging
 *
 * @author nhn485,
 */
@Slf4j
@Profile("!test")
@Configuration
public class WebClientConfiguration {

  @Value("${connect.timeout}")
  private Integer connectionTimeOut;

  @Value("${read.timeout}")
  private Integer readTimeOut;

 
  /** Customizer that will configure the default WebClient.Builder */
  @Bean
  public WebClientCustomizer customize() {

    return webClientBuilder ->
        webClientBuilder.clientConnector(createConnector()).filter(this::logRequest);
  }

  /**
   * Sets up Connection Timeouts
   *
   * @return connector with timeout configuration
   */
  private ClientHttpConnector createConnector() {

    return new ReactorClientHttpConnector(
        HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeOut)
            .responseTimeout(Duration.ofMillis(readTimeOut)));
  }

  /** log request/response details */
  private final Mono<ClientResponse> logRequest(
      ClientRequest clientRequest, ExchangeFunction next) {
    long startTime = System.currentTimeMillis();
    Mono<ClientResponse> nextExchange = next.exchange(clientRequest);

    Map<String, Object> entries = new HashMap<>();
    entries.put("HTTP Method", clientRequest.method());
    entries.put("URI", clientRequest.url());
    entries.putAll(clientRequest.headers().toSingleValueMap());
    entries.remove("Authorization");

    return nextExchange.doOnNext(
        clientResponse -> {
          entries.put("Response Time", System.currentTimeMillis() - startTime);
          entries.put("HTTP Status", clientResponse.rawStatusCode());

          log.info("Downstream_Request:\n{}", entries);
        });
  }
}
