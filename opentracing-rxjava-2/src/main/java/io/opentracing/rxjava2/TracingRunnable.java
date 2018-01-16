package io.opentracing.rxjava2;


import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;

class TracingRunnable implements Runnable {

  private final Runnable runnable;
  private final Tracer tracer;
  private final Span span;

  TracingRunnable(Runnable runnable, Tracer tracer) {
    this.runnable = runnable;
    this.tracer = tracer;
    span = getSpan(tracer);
  }

  private Span getSpan(Tracer tracer) {
    if (SpanHolder.get() != null) {
      Scope scope = SpanHolder.get();
      SpanHolder.clear();
      return scope.span();
    }
    return tracer.activeSpan();
  }

  @Override
  public void run() {
    Scope scope = null;
    if (span != null) {
      scope = tracer.scopeManager().activate(span, false);
    }
    try {
      runnable.run();
    } finally {
      if (scope != null) {
        scope.close();
      }
    }
  }
}
