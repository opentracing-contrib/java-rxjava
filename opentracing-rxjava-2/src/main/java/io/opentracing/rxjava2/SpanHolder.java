package io.opentracing.rxjava2;

import io.opentracing.Scope;

class SpanHolder {

  private static final SpanHolder holder = new SpanHolder();
  private final ThreadLocal<Scope> scope = new ThreadLocal<>();

  static Scope get() {
    return holder.scope.get();
  }

  static void set(Scope scope) {
    holder.scope.set(scope);
  }

  static void clear() {
    Scope scope = holder.scope.get();
    if (scope != null) {
      scope.close();
    }
    holder.scope.remove();
  }

}
