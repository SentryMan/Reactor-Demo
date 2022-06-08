package com.jojo.demo.domain;

import java.util.function.BiFunction;

import reactor.util.retry.Retry.RetrySignal;
import reactor.util.retry.RetryBackoffSpec;

public interface RetryExhaustSpec extends BiFunction<RetryBackoffSpec, RetrySignal, Throwable> {}
