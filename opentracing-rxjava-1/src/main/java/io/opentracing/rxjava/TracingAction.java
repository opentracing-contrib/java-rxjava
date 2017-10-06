package io.opentracing.rxjava;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import rx.functions.Action0;


class TracingAction implements Action0 {

  private final Action0 action0;
  private final Tracer tracer;
  private final Span span;

  TracingAction(Action0 action0, Tracer tracer) {
    this.action0 = action0;
    this.tracer = tracer;

    if (tracer.scopeManager().active() != null) {
      span = tracer.scopeManager().active().span();
    } else {
      span = null;
    }
  }

  @Override
  public void call() {
    Scope scope = null;
    if (span != null) {
      scope = tracer.scopeManager().activate(span, false);
    }

    try {
      action0.call();
    } finally {
      if (scope != null) {
        scope.close();
      }
    }
  }
}
