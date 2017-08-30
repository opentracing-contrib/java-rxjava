package io.opentracing.rxjava;

import io.opentracing.Tracer;
import rx.functions.Action0;
import rx.functions.Func1;
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
  }
}
