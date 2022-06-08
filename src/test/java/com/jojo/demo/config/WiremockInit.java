package com.jojo.demo.config;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;

public class WiremockInit implements ApplicationContextInitializer<GenericApplicationContext> {

  // registers a wiremockConfig Bean to spring test context
  @Override
  public void initialize(GenericApplicationContext applicationContext) {

    applicationContext.registerBean(WiremockConfig.class, WiremockConfig::new);
  }

  @Configuration
  static class WiremockConfig {

    @Value("${wiremockserver.port:6969}")
    private int portNo;

    private static WireMockServer wireMockServer;

    // destroy wiremock server before spring context
    @PreDestroy
    public static void stopWiremock() {
      wireMockServer.stop();
    }
    // create a local wiremock server for junits
    @PostConstruct
    public void startWireMock() {
      if (Objects.isNull(wireMockServer))
        wireMockServer =
            new WireMockServer(
                wireMockConfig().port(portNo).extensions(new ResponseTemplateTransformer(true)));

      if (!wireMockServer.isRunning()) wireMockServer.start();

      // Test
      wireMockServer.stubFor(
          WireMock.get("/hello?param=good200")
              .willReturn(
                  WireMock.aResponse()
                      .withStatus(200)
                      .withHeader("Content-Type", "application/json")
                      .withBodyFile(TestConstants.SUCCESS_FILE)));

      // Test MultiAccount
      wireMockServer.stubFor(
          WireMock.get("/hello?param=bad500")
              .willReturn(
                  WireMock.aResponse()
                      .withStatus(500)
                      .withHeader("Content-Type", "application/json")
                      .withBodyFile(TestConstants.ERROR_FILE)));
    }
  }
}
