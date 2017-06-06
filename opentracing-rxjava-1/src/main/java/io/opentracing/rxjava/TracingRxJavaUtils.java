package io.opentracing.rxjava;


import io.opentracing.ActiveSpan;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.plugins.RxJavaHooks;

/**
 * Utility class to enable tracing via RxJavaHooks
 */
public class TracingRxJavaUtils {


  /**
   * Enable tracing using registered tracer in GlobalTracer
   */
  public static void enableTracing() {
    enableTracing(GlobalTracer.get());
  }

  /**
   * Enable tracing using provided tracer
   *
   * @param tracer Tracer
   */
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
            try (ActiveSpan activeSpan = tracer.buildSpan(onSubscribe.getClass().getSimpleName())
                .startActive()) {
              activeSpan.setTag(Tags.COMPONENT.getKey(), "rxjava-1");
              TracingSubscriber t = new TracingSubscriber(subscriber, activeSpan);
              subscriber.add(t);
              onSubscribe.call(t);
            }
          }
        };
      }
    });
  }

}
