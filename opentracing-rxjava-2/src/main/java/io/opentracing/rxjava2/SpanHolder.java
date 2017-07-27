package io.opentracing.rxjava2;

import io.opentracing.Span;

class SpanHolder {

  private static final SpanHolder holder = new SpanHolder();

  private final ThreadLocal<Span> spanThreadLocal = new ThreadLocal<>();

  static Span get() {
    return holder.spanThreadLocal.get();
  }

  static void set(Span span) {
    holder.spanThreadLocal.set(span);
  }

  static void clear() {
    holder.spanThreadLocal.remove();
  }
}
