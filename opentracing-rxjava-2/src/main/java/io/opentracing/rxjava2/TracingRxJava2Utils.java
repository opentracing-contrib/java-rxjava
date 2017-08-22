package io.opentracing.rxjava2;


import io.opentracing.References;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.plugins.RxJavaPlugins;


public class TracingRxJava2Utils {

  static final String COMPONENT_NAME = "rxjava-2";

  public static void enableTracing() {
    enableTracing(GlobalTracer.get());
  }

  public static void enableTracing(final Tracer tracer) {

    RxJavaPlugins.setScheduleHandler(new Function<Runnable, Runnable>() {
      @Override
      public Runnable apply(Runnable runnable) throws Exception {
        return new TracingRunnable(runnable);
      }
    });

    RxJavaPlugins.setOnObservableAssembly(new Function<Observable, Observable>() {
      @Override
      public Observable apply(Observable observable) throws Exception {
        SpanBuilder spanBuilder = tracer.buildSpan(observable.getClass().getSimpleName())
            .withTag(Tags.COMPONENT.getKey(), COMPONENT_NAME);
        Span parent = SpanStackHolder.get();
        if (parent != null) {
          spanBuilder.addReference(References.CHILD_OF, parent.context());
        }
        Span span = spanBuilder.startManual();
        if (observable.getClass().getSimpleName().isEmpty()) {
          span.setOperationName(observable.getClass().getName());
        }
        SpanStackHolder.add(span);

        return observable;
      }
    });

    RxJavaPlugins.setOnObservableSubscribe(new BiFunction<Observable, Observer, Observer>() {
      @Override
      public Observer apply(Observable observable, Observer observer) throws Exception {
        return new TracingObserver(observer);
      }
    });
  }
}
