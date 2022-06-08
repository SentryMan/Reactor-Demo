package com.jojo.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ResponseStatusException;

import com.jojo.demo.config.WebClientConfiguration;
import com.jojo.demo.config.WiremockInit;

import reactor.test.StepVerifier;

@SpringBootTest(
    classes = {
      WebClientAutoConfiguration.class,
      WebClientConfiguration.class,
      ServiceClass.class,
    })
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = WiremockInit.class)
class ServiceClassTest {

  @Autowired ServiceClass service;

  @BeforeEach
  void setUp() throws Exception {}

  @Test
  void test200() {

    StepVerifier.create(service.testClient("good200")).expectNextCount(1).verifyComplete();
  }

  @Test
  void test500() {
    StepVerifier.create(service.testClient("bad500"))
        .verifyErrorSatisfies(
            ex -> {
              assert ex instanceof ResponseStatusException;
            });
  }
}
