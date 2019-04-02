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
package io.opentracing.rxjava;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import rx.Observer;

/**
 * Tracing decorator for RxJava {@link Observer}
 */
public class TracingObserverSubscriber<T> extends AbstractTracingSubscriber<T> {

  private final Observer<? super T> observer;
  private final Tracer tracer;

  public TracingObserverSubscriber(Observer<? super T> observer, String operationName,
      Tracer tracer) {
    this(observer, operationName, AbstractTracingSubscriber.COMPONENT_NAME, tracer);
  }

  public TracingObserverSubscriber(Observer<? super T> observer, String operationName,
      String componentName, Tracer tracer) {
    super(operationName, componentName, tracer);

    if (observer == null) {
      throw new NullPointerException("observer is null");
    }

    this.observer = observer;
    this.tracer = tracer;
  }

  @Override
  public void onNext(T t) {
    Span span = getSpan();
    Span activeSpan = tracer.activeSpan();
    if (span != null && (!span.equals(activeSpan))) {
      try (Scope ignore = tracer.scopeManager().activate(getSpan())) {
        observer.onNext(t);
      }
    } else {
      observer.onNext(t);
    }
  }

  @Override
  public void onError(Throwable e) {
    try {
      Span span = getSpan();
      Span activeSpan = tracer.activeSpan();
      if (span != null && (!span.equals(activeSpan))) {
        try (Scope ignore = tracer.scopeManager().activate(getSpan())) {
          observer.onError(e);
        }
      } else {
        observer.onError(e);
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
      if (span != null && (!span.equals(activeSpan))) {
        try (Scope ignore = tracer.scopeManager().activate(getSpan())) {
          observer.onCompleted();
        }
      } else {
        observer.onCompleted();
      }
    } finally {
      super.onCompleted();
    }
  }
}
