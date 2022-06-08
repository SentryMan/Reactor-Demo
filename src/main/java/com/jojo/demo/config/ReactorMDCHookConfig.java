package com.jojo.demo.config;

import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.reactivestreams.Subscription;
import org.slf4j.MDC;

import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

/**
 * Adds a Hook to every Reactor Operator <br>
 * This Hook adds values from the Reactor {@link Context} to the MDC map slower because it affects
 * every reactive stream, if speed is the name of the game, use the reactor logging util instead of
 * using this class like a crutch
 *
 * @author nhn485
 * @see <a href="https://projectreactor.io/docs/core/release/reference/#hooks">Reactor Hooks</a>
 * @see <a href="https://projectreactor.io/docs/core/release/reference/#context">Adding Context to a
 *     reactive Sequence</a>
 */
// @Configuration
public class ReactorMDCHookConfig {
  private static final String MDC_CONTEXT_REACTOR_KEY = ReactorMDCHookConfig.class.getName();

  // adds mdc logging functionality to every mono/flux operator
  @PostConstruct
  private void contextOperatorHook() {
    Hooks.onEachOperator(MDC_CONTEXT_REACTOR_KEY, Operators.lift(MdcContextLifter::new));
  }

  @PreDestroy
  private void cleanupHook() {
    Hooks.resetOnEachOperator(MDC_CONTEXT_REACTOR_KEY);
  }
}

// class that copies the state of Reactor Context to MDC on the onNext and onError function.
record MdcContextLifter<T>(Scannable scannable, CoreSubscriber<T> coreSubscriber)
    implements CoreSubscriber<T> {

  @Override
  public void onSubscribe(Subscription subscription) {

    coreSubscriber.onSubscribe(subscription);
  }

  @Override
  public void onNext(T obj) {
    copyToMdc(currentContext());
    coreSubscriber.onNext(obj);
  }

  @Override
  public void onError(Throwable t) {
    copyToMdc(currentContext());
    coreSubscriber.onError(t);
  }

  @Override
  public void onComplete() {
    coreSubscriber.onComplete();
  }

  @Override
  public Context currentContext() {
    return coreSubscriber.currentContext();
  }

  /**
   * Extracts values from reactor context and adds to MDC
   *
   * @param context The current Reactor {@link Context}
   * @see <a href="https://projectreactor.io/docs/core/release/reference/#context">Adding Context to
   *     a reactive Sequence</a>
   */
  private void copyToMdc(Context context) {

    if (!context.isEmpty()) {
      final Map<String, String> map =
          context.stream()
              .collect(Collectors.toMap(k -> k.getKey().toString(), v -> v.getValue().toString()));

      MDC.setContextMap(map);
    } else MDC.clear();
  }
}
