package io.opentracing.rxjava;

import io.opentracing.Scope;
import io.opentracing.Scope.Observer;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;

public class SpanHolder {

  private final Tracer tracer;
  private final SpanContext parentContext;

  public SpanHolder(Tracer tracer) {
    this.tracer = tracer;
    Scope scope = tracer.scopeManager().active();
    if (scope != null) {
      parentContext = scope.span().context();
    } else {
      parentContext = null;
    }
  }

  public Scope activate(String operationName) {
    // build a new span, activate it and it will be finished when span is deactivated
    SpanBuilder spanBuilder = tracer.buildSpan(operationName);
    if (parentContext != null) {
      spanBuilder.asChildOf(parentContext);
    }
    return spanBuilder.startActive(Observer.FINISH_ON_CLOSE);
  }
}
