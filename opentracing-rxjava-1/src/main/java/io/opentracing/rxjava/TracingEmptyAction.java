package io.opentracing.rxjava;

import rx.functions.Action0;
import rx.functions.Action1;

class TracingEmptyAction<T0> implements Action0, Action1<T0> {

  private static final TracingEmptyAction EMPTY_ACTION = new TracingEmptyAction();

  @SuppressWarnings("unchecked")
  static <T0> TracingEmptyAction<T0> empty() {
    return EMPTY_ACTION;
  }

  @Override
  public void call() {
  }

  @Override
  public void call(T0 t0) {
  }
}
