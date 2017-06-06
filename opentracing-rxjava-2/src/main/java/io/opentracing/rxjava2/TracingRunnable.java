package io.opentracing.rxjava2;


import io.opentracing.ActiveSpan;
import io.opentracing.ActiveSpan.Continuation;
import io.opentracing.Tracer;

class TracingRunnable implements Runnable {

  private final Runnable runnable;
  private final Continuation continuation;

  TracingRunnable(Runnable runnable, Tracer tracer) {
    this.runnable = runnable;
    ActiveSpan activeSpan = tracer.activeSpan();
    if (activeSpan != null) {
      this.continuation = activeSpan.capture();
    } else {
      this.continuation = null;
    }
  }

  @Override
  public void run() {
    ActiveSpan activeSpan = null;
    if (continuation != null) {
      activeSpan = continuation.activate();
    }
    try {
      runnable.run();
    } finally {
      if (activeSpan != null) {
        activeSpan.deactivate();
      }
    }
  }
}
