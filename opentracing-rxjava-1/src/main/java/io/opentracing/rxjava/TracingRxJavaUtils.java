package io.opentracing.rxjava;


import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import rx.Observable.OnSubscribe;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.plugins.RxJavaHooks;

/**
 * Utility class to enable tracing via RxJavaHooks
 */
public class TracingRxJavaUtils {

  static final String COMPONENT_NAME = "rxjava-1";

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

    RxJavaHooks.setOnObservableCreate(new Func1<OnSubscribe, OnSubscribe>() {
      @Override
      @SuppressWarnings("unchecked")
      public OnSubscribe call(OnSubscribe onSubscribe) {
        return new TracingOnSubscribe(onSubscribe, tracer);
      }
    });
  }

}
