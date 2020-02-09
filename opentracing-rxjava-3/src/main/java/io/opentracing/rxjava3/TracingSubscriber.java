/*
 * Copyright 2017-2019 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.rxjava3;

import io.opentracing.Tracer;
import io.reactivex.rxjava3.core.FlowableSubscriber;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.functions.ObjectHelper;
import io.reactivex.rxjava3.internal.operators.flowable.FlowableInternalHelper;
import io.reactivex.rxjava3.internal.subscribers.LambdaSubscriber;
import org.reactivestreams.Subscription;

import static java.util.Objects.requireNonNull;

/**
 * Tracing decorator for RxJava {@link FlowableSubscriber}
 */
public class TracingSubscriber<T> implements FlowableSubscriber<T>, Subscription {

  private Subscription upstream;
  private final RxTracer rxTracer;
  private final FlowableSubscriber<T> subscriber;

  private TracingSubscriber(FlowableSubscriber<T> subscriber, String operationName, Tracer tracer) {
    rxTracer = new RxTracer(operationName, tracer);
    this.subscriber = subscriber;
  }

  @Override
  public void request(long l) {
    upstream.request(l);
  }

  @Override
  public void cancel() {
    upstream.cancel();
  }

  @Override
  public void onSubscribe(Subscription s) {
    upstream = s;
    try {
      subscriber.onSubscribe(this);
    } finally {
      rxTracer.onSubscribe();
    }
  }

  @Override
  public void onNext(T o) {
    subscriber.onNext(o);
  }

  @Override
  public void onError(Throwable t) {
    try {
      subscriber.onError(t);
    } finally {
      rxTracer.onError(t);
    }
  }

  @Override
  public void onComplete() {
    try {
      subscriber.onComplete();
    } finally {
      rxTracer.onComplete();
    }
  }

  public static <T> FlowableSubscriber<T> create(
      String operationName,
      Tracer tracer) {
    return create(Functions.emptyConsumer(), Functions.ON_ERROR_MISSING, Functions.EMPTY_ACTION,
        FlowableInternalHelper.RequestMax.INSTANCE, operationName, tracer);
  }

  public static <T> FlowableSubscriber<T> create(
      Consumer<? super T> onNext,
      String operationName,
      Tracer tracer) {
    return create(onNext, Functions.ON_ERROR_MISSING, Functions.EMPTY_ACTION,
        FlowableInternalHelper.RequestMax.INSTANCE, operationName, tracer);
  }

  public static <T> FlowableSubscriber<T> create(
      Consumer<? super T> onNext,
      Consumer<? super Throwable> onError,
      String operationName,
      Tracer tracer) {
    return create(onNext, onError, Functions.EMPTY_ACTION,
        FlowableInternalHelper.RequestMax.INSTANCE,
        operationName, tracer);
  }

  public static <T> FlowableSubscriber<T> create(
      Consumer<? super T> onNext,
      Consumer<? super Throwable> onError,
      Action onComplete,
      String operationName,
      Tracer tracer) {
    return create(onNext, onError, onComplete, FlowableInternalHelper.RequestMax.INSTANCE,
        operationName, tracer);
  }

  public static <T> FlowableSubscriber<T> create(
      Consumer<? super T> onNext,
      Consumer<? super Throwable> onError,
      Action onComplete,
      Consumer<? super Subscription> onSubscribe,
      String operationName,
      Tracer tracer) {

    requireNonNull(onError, "onError is null");
    requireNonNull(onComplete, "onComplete is null");
    requireNonNull(onSubscribe, "onSubscribe is null");
    requireNonNull(tracer, "tracer can not be null");

    return create(new LambdaSubscriber<>(onNext, onError, onComplete, onSubscribe), operationName,
        tracer);
  }

  public static <T> FlowableSubscriber<T> create(
      FlowableSubscriber<T> subscriber,
      String operationName,
      Tracer tracer) {

    return new TracingSubscriber<>(subscriber, operationName, tracer);
  }
}
