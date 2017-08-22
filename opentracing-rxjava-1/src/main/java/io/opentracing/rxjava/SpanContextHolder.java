package io.opentracing.rxjava;

import io.opentracing.SpanContext;

class SpanContextHolder {

  private static final SpanContextHolder holder = new SpanContextHolder();

  private final ThreadLocal<SpanContext> spanContext = new ThreadLocal<>();

  static SpanContext get() {
    return holder.spanContext.get();
  }

  static void set(SpanContext spanContext) {
    holder.spanContext.set(spanContext);
  }

  static void clear() {
    holder.spanContext.remove();
  }
}
