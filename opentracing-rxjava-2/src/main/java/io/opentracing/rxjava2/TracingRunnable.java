package io.opentracing.rxjava2;


import io.opentracing.Span;

class TracingRunnable implements Runnable {

  private final Runnable runnable;
  private final Span span;

  TracingRunnable(Runnable runnable) {
    this.runnable = runnable;
    this.span = SpanHolder.get();
    SpanHolder.clear();
  }

  @Override
  public void run() {
    SpanHolder.set(span);
    runnable.run();
    SpanHolder.clear();
  }
}
