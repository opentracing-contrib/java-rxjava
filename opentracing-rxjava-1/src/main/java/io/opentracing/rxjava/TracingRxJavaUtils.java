package io.opentracing.rxjava;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observers.SafeSubscriber;
import rx.plugins.RxJavaHooks;

public class TracingRxJavaUtils {

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
                scope = tracer.scopeManager().activate(span);
              }
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
