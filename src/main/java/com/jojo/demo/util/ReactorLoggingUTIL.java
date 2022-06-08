package com.jojo.demo.util;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.MDC;

import reactor.core.publisher.Signal;
import reactor.util.context.ContextView;

public interface ReactorLoggingUTIL {

  static <T> Consumer<Signal<T>> logOnNext(Consumer<T> logStatement) {
    return signal -> {
      if (!signal.isOnNext()) return;
      MDC.setContextMap(ctx2Map(signal.getContextView()));
      logStatement.accept(signal.get());
      MDC.clear();
    };
  }

  static <T> Consumer<Signal<T>> logOnError(Consumer<Throwable> logStatement) {
    return signal -> {
      if (!signal.isOnError()) return;
      MDC.setContextMap(ctx2Map(signal.getContextView()));
      logStatement.accept(signal.getThrowable());
      MDC.clear();
    };
  }

  static <T> Consumer<Signal<T>> logOnTerminate(Runnable logStatement) {
    return signal -> {
      if (!signal.isOnComplete() && !signal.isOnError()) return;
      MDC.setContextMap(ctx2Map(signal.getContextView()));
      logStatement.run();
      MDC.clear();
    };
  }

  static Map<String, String> ctx2Map(ContextView ctx) {
    return ctx.stream()
        .collect(Collectors.toMap(k -> k.getKey().toString(), v -> v.getValue().toString()));
  }
}
