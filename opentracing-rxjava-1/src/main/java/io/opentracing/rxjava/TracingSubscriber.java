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
import rx.Producer;
import rx.Subscriber;


/**
 * Tracing decorator for RxJava {@link Subscriber}
 */
public class TracingSubscriber<T> extends AbstractTracingSubscriber<T> {

  private final Subscriber<T> subscriber;
  private final Tracer tracer;

  public TracingSubscriber(Subscriber<T> subscriber, String operationName, Tracer tracer) {
    this(subscriber, operationName, AbstractTracingSubscriber.COMPONENT_NAME, tracer);
  }

  public TracingSubscriber(Subscriber<T> subscriber, String operationName, String componentName,
      Tracer tracer) {
    super(operationName, componentName, tracer);

    if (subscriber == null) {
      throw new IllegalArgumentException("subscriber can not be null");
    }

    this.subscriber = subscriber;
    this.tracer = tracer;
    subscriber.add(this);
  }

  @Override
  public void onStart() {
    super.onStart();
    Span span = getSpan();
    Span activeSpan = tracer.activeSpan();
    if (span != null && (activeSpan == null || !span.equals(activeSpan))) {
      try (Scope ignore = tracer.scopeManager().activate(getSpan(), false)) {
        subscriber.onStart();
      }
    } else {
      subscriber.onStart();
    }
  }

  @Override
  public void onNext(T o) {
    Span span = getSpan();
    Span activeSpan = tracer.activeSpan();
    if (span != null && (activeSpan == null || !span.equals(activeSpan))) {
      try (Scope ignore = tracer.scopeManager().activate(getSpan(), false)) {
        subscriber.onNext(o);
      }
    } else {
      subscriber.onNext(o);
    }
  }

  @Override
  public void onError(Throwable t) {
    try {
      Span span = getSpan();
      Span activeSpan = tracer.activeSpan();
      if (span != null && (activeSpan == null || !span.equals(activeSpan))) {
        try (Scope ignore = tracer.scopeManager().activate(getSpan(), false)) {
          subscriber.onError(t);
        }
      } else {
        subscriber.onError(t);
      }
    } finally {
      super.onError(t);
    }
  }

  @Override
  public void onCompleted() {
    try {
      Span span = getSpan();
      Span activeSpan = tracer.activeSpan();
      if (span != null && (activeSpan == null || !span.equals(activeSpan))) {
        try (Scope ignore = tracer.scopeManager().activate(getSpan(), false)) {
          subscriber.onCompleted();
        }
      } else {
        subscriber.onCompleted();
      }
    } finally {
      super.onCompleted();
    }
  }

  @Override
  public void setProducer(Producer p) {
    subscriber.setProducer(p);
  }
}