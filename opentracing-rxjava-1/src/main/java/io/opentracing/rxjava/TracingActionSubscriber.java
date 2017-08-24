package io.opentracing.rxjava;

import io.opentracing.Tracer;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.internal.util.InternalObservableUtils;

/**
 * Tracing decorator for RxJava {@link rx.functions.Action}
 */
public class TracingActionSubscriber<T> extends AbstractTracingSubscriber<T> {

  private final Action1<? super T> onNext;
  private final Action1<Throwable> onError;
  private final Action0 onCompleted;

  public TracingActionSubscriber(String operationName, Tracer tracer) {
    this(TracingEmptyAction.empty(), operationName, tracer);
  }

  public TracingActionSubscriber(Action1<? super T> onNext, String operationName, Tracer tracer) {
    this(onNext, InternalObservableUtils.ERROR_NOT_IMPLEMENTED, operationName, tracer);
  }

  public TracingActionSubscriber(Action1<? super T> onNext, Action1<Throwable> onError,
      String operationName, Tracer tracer) {
    this(onNext, onError, TracingEmptyAction.empty(), operationName, tracer);
  }

  public TracingActionSubscriber(Action1<? super T> onNext, Action1<Throwable> onError,
      Action0 onCompleted, String operationName, Tracer tracer) {
    super(operationName, tracer);

    if (onNext == null) {
      throw new IllegalArgumentException("onNext can not be null");
    }
    if (onError == null) {
      throw new IllegalArgumentException("onError can not be null");
    }
    if (onCompleted == null) {
      throw new IllegalArgumentException("onComplete can not be null");
    }

    this.onNext = onNext;
    this.onError = onError;
    this.onCompleted = onCompleted;
  }

  @Override
  public void onNext(T t) {
    onNext.call(t);
  }

  @Override
  public void onError(Throwable e) {
    try {
      onError.call(e);
    } finally {
      super.onError(e);
    }
  }

  @Override
  public void onCompleted() {
    try {
      onCompleted.call();
    } finally {
      super.onCompleted();
    }
  }
}
