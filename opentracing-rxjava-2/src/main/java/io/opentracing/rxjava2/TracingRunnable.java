package io.opentracing.rxjava2;


import io.opentracing.Span;

class TracingRunnable implements Runnable {

  private final Runnable runnable;
  private final Span span;

  TracingRunnable(Runnable runnable) {
    this.runnable = runnable;
    this.span = SpanStackHolder.remove();
  }

  @Override
  public void run() {
    if (span != null) {
      SpanStackHolder.add(span);
    }
    runnable.run();
  }
}
