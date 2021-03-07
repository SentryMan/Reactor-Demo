package com.jojo.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

// @Configuration
public class OauthConfig {

  @Value("${app.oauth2.uri}")
  private String oauthEndPoint;

  @Value("${app.client-id}")
  private String clientId;

  @Value("${app.client-secret}")
  private String clientSecret;

  @Value("${app.oauth2.app}")
  private String oauth2App;

  // Add OAuth to WebClient
  /**
   * @param oauthFilterFunction the OAuth function that will add credentials to every HTTP request
   */
  @Bean
  public WebClientCustomizer customizeOauth(
      ServerOAuth2AuthorizedClientExchangeFilterFunction oauthFilterFunction) {

    return webClientBuilder -> webClientBuilder.filter(oauthFilterFunction);
  }

  @Bean
  ReactiveClientRegistrationRepository reactiveClientRegistrationRepository() {
    final ClientRegistration clientRegistration =
        ClientRegistration.withRegistrationId(oauth2App)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri(oauthEndPoint)
            .build();

    return new InMemoryReactiveClientRegistrationRepository(clientRegistration);
  }

  @Bean
  ServerOAuth2AuthorizedClientExchangeFilterFunction ouathConfig(
      ReactiveClientRegistrationRepository clientRegistrationRepo) {

    final ReactiveOAuth2AuthorizedClientService oauthClientService =
        new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrationRepo);

    final AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager oauthManager =
        new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
            clientRegistrationRepo, oauthClientService);

    final ServerOAuth2AuthorizedClientExchangeFilterFunction oauthFilterFunction =
        new ServerOAuth2AuthorizedClientExchangeFilterFunction(oauthManager);

    oauthFilterFunction.setDefaultClientRegistrationId(oauth2App);

    return oauthFilterFunction;
  }
}
