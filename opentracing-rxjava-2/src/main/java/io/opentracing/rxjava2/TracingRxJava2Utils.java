package io.opentracing.rxjava2;


import io.opentracing.Tracer;
import io.reactivex.functions.Function;
import io.reactivex.plugins.RxJavaPlugins;


public class TracingRxJava2Utils {

  public static void enableTracing(final Tracer tracer) {

    RxJavaPlugins.setScheduleHandler(new Function<Runnable, Runnable>() {
      @Override
      public Runnable apply(Runnable runnable) {
        return new TracingRunnable(runnable, tracer);
      }
    });
  }
}
