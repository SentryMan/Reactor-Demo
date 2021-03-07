package com.jojo.demo.web;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import com.jojo.demo.filter.CustomFilter;

@Component
public class RouterConfigs {

  // Request Predicates determine whether to route or not
  @Bean
  public RouterFunction<ServerResponse> demoRoute(Handler handler, CustomFilter filter) {

    return RouterFunctions.route(GET("/hello-world-demo"), handler::handle).filter(filter);
  }

  // You can have multiple routers
  @Bean
  public RouterFunction<ServerResponse> helloWorldRoutes() {

    return RouterFunctions.route(GET("/hello"), r -> ServerResponse.ok().bodyValue("hello"))
        .andRoute(GET("/world"), r -> ServerResponse.ok().bodyValue(" world"));
  }
}
