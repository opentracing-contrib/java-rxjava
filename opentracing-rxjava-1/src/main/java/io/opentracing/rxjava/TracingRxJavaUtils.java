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
import io.opentracing.util.GlobalTracer;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observers.SafeSubscriber;
import rx.plugins.RxJavaHooks;

public class TracingRxJavaUtils {

  /**
   * GlobalTracer is used to get tracer
   */
  public static void enableTracing() {
    enableTracing(GlobalTracer.get());
  }

  @SuppressWarnings("unchecked")
  public static void enableTracing(final Tracer tracer) {

    RxJavaHooks.setOnScheduleAction(new Func1<Action0, Action0>() {
      @Override
      public Action0 call(final Action0 action0) {
        return new TracingAction(action0, tracer);
      }
    });

    RxJavaHooks.setOnObservableStart(new Func2<Observable, OnSubscribe, OnSubscribe>() {
      @Override
      public OnSubscribe call(final Observable observable, final OnSubscribe onSubscribe) {

        return new OnSubscribe<Subscriber>() {

          @Override
          public void call(Subscriber subscriber) {
            Scope scope = null;
            if (subscriber instanceof SafeSubscriber) {
              SafeSubscriber safeSubscriber = (SafeSubscriber) subscriber;
              Subscriber subscriber2 = safeSubscriber.getActual();
              if (subscriber2 instanceof AbstractTracingSubscriber) {
                AbstractTracingSubscriber tracingSubscriber = (AbstractTracingSubscriber) subscriber2;
                Span span = tracingSubscriber.getSpan();
                scope = tracer.scopeManager().activate(span, false);
              } /* else if (tracer.scopeManager().active() != null) {
                // if there is no parent don't create new span

                final Scope scope2 = tracer.buildSpan("observable")
                    .startActive(Observer.FINISH_ON_CLOSE);
                subscriber2.add(new Subscription() {
                  private volatile boolean unsubscribed;

                  @Override
                  public void unsubscribe() {
                    scope2.close();
                    unsubscribed = true;
                  }

                  @Override
                  public boolean isUnsubscribed() {
                    return unsubscribed;
                  }
                });
              }*/
            }

            onSubscribe.call(subscriber);

            if (scope != null) {
              scope.close();
            }
          }
        };
      }
    });
  }
}
