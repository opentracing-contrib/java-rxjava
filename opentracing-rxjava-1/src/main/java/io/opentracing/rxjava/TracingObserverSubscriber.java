package io.opentracing.rxjava;

import io.opentracing.Tracer;
import rx.Observer;

/**
 * Tracing decorator for RxJava {@link Observer}
 */
public class TracingObserverSubscriber<T> extends AbstractTracingSubscriber<T> {

  private final Observer<? super T> observer;

  public TracingObserverSubscriber(Observer<? super T> observer, String operationName,
      Tracer tracer) {
    super(operationName, tracer);

    if (observer == null) {
      throw new NullPointerException("observer is null");
    }

    this.observer = observer;
  }

  @Override
  public void onNext(T t) {
    observer.onNext(t);
  }

  @Override
  public void onError(Throwable e) {
    try {
      observer.onError(e);
    } finally {
      super.onError(e);
    }
  }

  @Override
  public void onCompleted() {
    try {
      observer.onCompleted();
    } finally {
      super.onCompleted();
    }
  }
}
