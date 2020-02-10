/*
 * Copyright 2017-2020 The OpenTracing Authors
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
package io.opentracing.rxjava2;

import io.opentracing.Tracer;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.observers.LambdaObserver;

/**
 * Tracing decorator for RxJava {@link Consumer}
 */
public class TracingConsumer<T> implements Observer<T>, Disposable {

  private final RxTracer rxTracer;
  private final LambdaObserver<T> lambdaObserver;

  public TracingConsumer(String operationName, Tracer tracer) {
    this(Functions.emptyConsumer(), operationName, tracer);
  }

  public TracingConsumer(Consumer<? super T> onNext, String operationName, Tracer tracer) {
    this(onNext, Functions.ON_ERROR_MISSING, operationName, tracer);
  }

  public TracingConsumer(Consumer<? super T> onNext, Consumer<? super Throwable> onError,
      String operationName, Tracer tracer) {
    this(onNext, onError, Functions.EMPTY_ACTION, operationName, tracer);
  }

  public TracingConsumer(Consumer<? super T> onNext, Consumer<? super Throwable> onError,
      Action onComplete, String operationName, Tracer tracer) {
    this(onNext, onError, onComplete, Functions.emptyConsumer(),
        operationName, tracer);
  }

  public TracingConsumer(Consumer<? super T> onNext, Consumer<? super Throwable> onError,
      Action onComplete, Consumer<? super Disposable> onSubscribe, String operationName,
      Tracer tracer) {

    rxTracer = new RxTracer(operationName, tracer);

    requireNonNull(onNext, "onNext can not be null");
    requireNonNull(onError, "onError can not be null");
    requireNonNull(onComplete, "onComplete can not be null");
    requireNonNull(onSubscribe, "onSubscribe can not be null");
    requireNonNull(tracer, "tracer can not be null");

    lambdaObserver = new LambdaObserver<>(onNext, onError, onComplete, onSubscribe);
  }

  @Override
  public void onSubscribe(Disposable d) {
    try {
      lambdaObserver.onSubscribe(d);
    } finally {
      rxTracer.onSubscribe();
    }
  }

  @Override
  public void onNext(T t) {
    lambdaObserver.onNext(t);
  }

  @Override
  public void onError(Throwable t) {
    try {
      lambdaObserver.onError(t);
    } finally {
      rxTracer.onError(t);
    }
  }

  @Override
  public void onComplete() {
    try {
      lambdaObserver.onComplete();
    } finally {
      rxTracer.onComplete();
    }
  }

  @Override
  public void dispose() {
    lambdaObserver.dispose();
  }

  @Override
  public boolean isDisposed() {
    return lambdaObserver.isDisposed();
  }

  private static void requireNonNull(Object object, String message) {
    if (object == null) {
      throw new IllegalArgumentException(message);
    }
  }
}
