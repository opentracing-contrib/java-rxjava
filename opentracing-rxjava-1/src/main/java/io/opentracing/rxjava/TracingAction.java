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

    if (tracer.activeSpan() != null) {
      continuation = tracer.activeSpan().capture();
    } else {
      continuation = null;
    }
  }

  @Override
  public void call() {
    ActiveSpan span = null;
    if (continuation != null) {
      span = continuation.activate();
    }

    try {
      action0.call();
    } finally {
      if (span != null) {
        span.deactivate();
      }
    }
  }
}


