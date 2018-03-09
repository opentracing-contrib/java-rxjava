/*
 * Copyright 2017-2018 The OpenTracing Authors
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
package io.opentracing.rxjava;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.internal.util.InternalObservableUtils;

/**
 * Tracing decorator for RxJava {@link rx.functions.Action}
 */
public class TracingActionSubscriber<T> extends AbstractTracingSubscriber<T> {

  private final Action1<? super T> onNext;
  private final Action1<Throwable> onError;
  private final Action0 onCompleted;
  private final Tracer tracer;

  public TracingActionSubscriber(String operationName, Tracer tracer) {
    this(operationName, AbstractTracingSubscriber.COMPONENT_NAME, tracer);
  }

  public TracingActionSubscriber(String operationName, String componentName, Tracer tracer) {
    this(TracingEmptyAction.empty(), operationName, componentName, tracer);
  }

  public TracingActionSubscriber(Action1<? super T> onNext, String operationName, Tracer tracer) {
    this(onNext, operationName, AbstractTracingSubscriber.COMPONENT_NAME, tracer);
  }

  public TracingActionSubscriber(Action1<? super T> onNext, String operationName,
      String componentName, Tracer tracer) {
    this(onNext, InternalObservableUtils.ERROR_NOT_IMPLEMENTED, operationName, componentName,
        tracer);
  }

  public TracingActionSubscriber(Action1<? super T> onNext, Action1<Throwable> onError,
      String operationName, Tracer tracer) {
    this(onNext, onError, operationName, AbstractTracingSubscriber.COMPONENT_NAME, tracer);
  }

  public TracingActionSubscriber(Action1<? super T> onNext, Action1<Throwable> onError,
      String operationName, String componentName, Tracer tracer) {
    this(onNext, onError, TracingEmptyAction.empty(), operationName, componentName, tracer);
  }

  public TracingActionSubscriber(Action1<? super T> onNext, Action1<Throwable> onError,
      Action0 onCompleted, String operationName, Tracer tracer) {
    this(onNext, onError, onCompleted, operationName, AbstractTracingSubscriber.COMPONENT_NAME,
        tracer);
  }

  public TracingActionSubscriber(Action1<? super T> onNext, Action1<Throwable> onError,
      Action0 onCompleted, String operationName, String componentName, Tracer tracer) {
    super(operationName, componentName, tracer);

    if (onNext == null) {
      throw new IllegalArgumentException("onNext can not be null");
    }
    if (onError == null) {
      throw new IllegalArgumentException("onError can not be null");
    }
    if (onCompleted == null) {
      throw new IllegalArgumentException("onComplete can not be null");
    }

    this.onNext = onNext;
    this.onError = onError;
    this.onCompleted = onCompleted;
    this.tracer = tracer;
  }

  @Override
  public void onNext(T t) {
    Span span = getSpan();
    Span activeSpan = tracer.activeSpan();
    if (span != null && (activeSpan == null || !span.equals(activeSpan))) {
      try (Scope ignore = tracer.scopeManager().activate(getSpan(), false)) {
        onNext.call(t);
      }
    } else {
      onNext.call(t);
    }
  }

  @Override
  public void onError(Throwable e) {
    try {
      Span span = getSpan();
      Span activeSpan = tracer.activeSpan();
      if (span != null && (activeSpan == null || !span.equals(activeSpan))) {
        try (Scope ignore = tracer.scopeManager().activate(getSpan(), false)) {
          onError.call(e);
        }
      } else {
        onError.call(e);
      }
    } finally {
      super.onError(e);
    }
  }

  @Override
  public void onCompleted() {
    try {
      Span span = getSpan();
      Span activeSpan = tracer.activeSpan();
      if (span != null && (activeSpan == null || !span.equals(activeSpan))) {
        try (Scope ignore = tracer.scopeManager().activate(getSpan(), false)) {
          onCompleted.call();
        }
      } else {
        onCompleted.call();
      }
    } finally {
      super.onCompleted();
    }
  }
}
