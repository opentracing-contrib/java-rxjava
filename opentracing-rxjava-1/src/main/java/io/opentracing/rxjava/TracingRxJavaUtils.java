package io.opentracing.rxjava;

import io.opentracing.Scope;
import io.opentracing.Scope.Observer;
import io.opentracing.Span;
import io.opentracing.Tracer;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
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
              } else if (tracer.scopeManager().active() != null) {
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
