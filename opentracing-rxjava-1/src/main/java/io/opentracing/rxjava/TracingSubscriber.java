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

import io.opentracing.Tracer;
import rx.Producer;
import rx.Subscriber;


/**
 * Tracing decorator for RxJava {@link Subscriber}
 */
public class TracingSubscriber<T> extends AbstractTracingSubscriber<T> {

  private final Subscriber<T> subscriber;

  public TracingSubscriber(Subscriber<T> subscriber, String operationName, Tracer tracer) {
    super(operationName, tracer);

    if (subscriber == null) {
      throw new IllegalArgumentException("subscriber can not be null");
    }

    this.subscriber = subscriber;
    subscriber.add(this);
  }

  @Override
  public void onStart() {
    super.onStart();
    subscriber.onStart();
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
      super.onError(t);
    }
  }

  @Override
  public void onCompleted() {
    try {
      subscriber.onCompleted();
    } finally {
      super.onCompleted();
    }
  }

  @Override
  public void setProducer(Producer p) {
    subscriber.setProducer(p);
  }
}