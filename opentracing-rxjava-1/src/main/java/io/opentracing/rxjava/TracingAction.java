package io.opentracing.rxjava;

import io.opentracing.ActiveSpan;
import io.opentracing.ActiveSpan.Continuation;
import io.opentracing.Tracer;
import rx.functions.Action0;

class TracingAction implements Action0 {

  private final Action0 action0;
  private final Continuation continuation;

  TracingAction(Action0 action0, Tracer tracer) {
    this.action0 = action0;
    ActiveSpan activeSpan = tracer.activeSpan();
    if (activeSpan != null) {
      this.continuation = activeSpan.capture();
    } else {
      this.continuation = null;
    }
  }

  @Override
  public void call() {
    ActiveSpan activeSpan = null;
    if (continuation != null) {
      activeSpan = continuation.activate();
    }
    try {
      action0.call();
    } finally {
      if (activeSpan != null) {
        activeSpan.deactivate();
      }
    }
  }
}
