package io.opentracing.rxjava2;

import io.opentracing.Tracer;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * Tracing decorator for RxJava {@link Observer}
 */
public class TracingObserver<T> extends AbstractTracingObserver<T> implements Disposable {

  private Disposable upstream;
  private final Observer<T> observer;


  public TracingObserver(Observer<T> observer, String operationName, Tracer tracer) {
    super(operationName, tracer);
    this.observer = observer;
  }

  @Override
  public void dispose() {
    upstream.dispose();
  }

  @Override
  public boolean isDisposed() {
    return upstream.isDisposed();
  }

  @Override
  public void onSubscribe(Disposable d) {
    upstream = d;
    try {
      observer.onSubscribe(this);
    } finally {
      super.onSubscribe(d);
    }
  }

  @Override
  public void onNext(T o) {
    observer.onNext(o);
  }

  @Override
  public void onError(Throwable t) {
    try {
      observer.onError(t);
    } finally {
      super.onError(t);
    }
  }

  @Override
  public void onComplete() {
    try {
      observer.onComplete();
    } finally {
      super.onComplete();
    }
  }
}
