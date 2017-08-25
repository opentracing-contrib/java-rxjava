package io.opentracing.rxjava2;

import io.opentracing.Tracer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.observers.LambdaObserver;

/**
 * Tracing decorator for RxJava {@link Consumer}
 */
public class TracingConsumer<T> extends AbstractTracingObserver<T> implements Disposable {

  private final Consumer<? super T> onNext;
  private final Consumer<? super Throwable> onError;
  private final Action onComplete;
  private final Consumer<? super Disposable> onSubscribe;
  private final LambdaObserver<T> lambdaObserver;

  public TracingConsumer(String operationName, Tracer tracer) {
    this(Functions.emptyConsumer(), operationName, tracer);
  }

  public TracingConsumer(Consumer<? super T> onNext, String operationName, Tracer tracer) {
    this(onNext, Functions.ON_ERROR_MISSING, operationName, tracer);
  }

  public TracingConsumer(Consumer<? super T> onNext, Consumer<? super Throwable> onError,
      String operationName, Tracer tracer) {
    this(onNext, onError, Functions.EMPTY_ACTION, operationName, tracer);
  }

  public TracingConsumer(Consumer<? super T> onNext, Consumer<? super Throwable> onError,
      Action onComplete, String operationName, Tracer tracer) {
    this(onNext, onError, onComplete, Functions.emptyConsumer(),
        operationName, tracer);
  }

  public TracingConsumer(Consumer<? super T> onNext, Consumer<? super Throwable> onError,
      Action onComplete, Consumer<? super Disposable> onSubscribe, String operationName,
      Tracer tracer) {

    super(operationName, tracer);

    requireNonNull(onNext, "onNext can not be null");
    requireNonNull(onError, "onError can not be null");
    requireNonNull(onComplete, "onComplete can not be null");
    requireNonNull(onSubscribe, "onSubscribe can not be null");
    requireNonNull(tracer, "tracer can not be null");

    this.onNext = onNext;
    this.onError = onError;
    this.onComplete = onComplete;
    this.onSubscribe = onSubscribe;

    lambdaObserver = new LambdaObserver<>(onNext, onError, onComplete, onSubscribe);
  }

  @Override
  public void onSubscribe(Disposable d) {
    try {
      lambdaObserver.onSubscribe(d);
    } finally {
      super.onSubscribe(d);
    }
  }

  @Override
  public void onNext(T t) {
    lambdaObserver.onNext(t);
  }

  @Override
  public void onError(Throwable t) {
    try {
      lambdaObserver.onError(t);
    } finally {
      super.onError(t);
    }

  }

  @Override
  public void onComplete() {
    try {
      lambdaObserver.onComplete();
    } finally {
      super.onComplete();
    }
  }

  @Override
  public void dispose() {
    lambdaObserver.dispose();
  }

  @Override
  public boolean isDisposed() {
    return lambdaObserver.isDisposed();
  }

  private static void requireNonNull(Object object, String message) {
    if (object == null) {
      throw new IllegalArgumentException(message);
    }
  }
}
