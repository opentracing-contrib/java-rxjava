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
import rx.Observer;

/**
 * Tracing decorator for RxJava {@link Observer}
 */
public class TracingObserverSubscriber<T> extends AbstractTracingSubscriber<T> {

  private final Observer<? super T> observer;

  public TracingObserverSubscriber(Observer<? super T> observer, String operationName,
      Tracer tracer) {
    super(operationName, tracer);

    if (observer == null) {
      throw new NullPointerException("observer is null");
    }

    this.observer = observer;
  }

  @Override
  public void onNext(T t) {
    observer.onNext(t);
  }

  @Override
  public void onError(Throwable e) {
    try {
      observer.onError(e);
    } finally {
      super.onError(e);
    }
  }

  @Override
  public void onCompleted() {
    try {
      observer.onCompleted();
    } finally {
      super.onCompleted();
    }
  }
}
