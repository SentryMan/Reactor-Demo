package com.jojo.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;

// @Profile("!test")
// @Configuration
public class OauthConfig {

  @Value("${oauth.app}")
  private String oauthApp;

  @Bean
  public WebClientCustomizer customizeOauth(
      ServerOAuth2AuthorizedClientExchangeFilterFunction oauthFilterFunction) {

    return webClientBuilder -> webClientBuilder.filter(oauthFilterFunction);
  }

  @Bean
  public ServerOAuth2AuthorizedClientExchangeFilterFunction createOauthExchangeFunc(
      ReactiveClientRegistrationRepository oauthRepo,
      ReactiveOAuth2AuthorizedClientService oauthservice) {

    final var oauthFilterFunction =
        new ServerOAuth2AuthorizedClientExchangeFilterFunction(
            new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                oauthRepo, oauthservice));
    // globally set default oauth app for webclient
    oauthFilterFunction.setDefaultClientRegistrationId(oauthApp);
    return oauthFilterFunction;
  }
}
