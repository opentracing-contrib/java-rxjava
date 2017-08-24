package io.opentracing.rxjava;

import io.opentracing.Tracer;
import rx.Producer;
import rx.Subscriber;


/**
 * Tracing decorator for RxJava {@link Subscriber}
 */
class TracingSubscriber<T> extends AbstractTracingSubscriber<T> {

  private final Subscriber<T> subscriber;

  TracingSubscriber(Subscriber<T> subscriber, String operationName, Tracer tracer) {
    super(operationName, tracer);

    if (subscriber == null) {
      throw new IllegalArgumentException("subscriber can not be null");
    }

    this.subscriber = subscriber;
    subscriber.add(this);
  }

  @Override
  public void onStart() {
    super.onStart();
    subscriber.onStart();
  }

  @Override
  public void onNext(T o) {
    subscriber.onNext(o);
  }

  @Override
  public void onError(Throwable t) {
    try {
      subscriber.onError(t);
    } finally {
      super.onError(t);
    }
  }

  @Override
  public void onCompleted() {
    try {
      subscriber.onCompleted();
    } finally {
      super.onCompleted();
    }
  }

  @Override
  public void setProducer(Producer p) {
    subscriber.setProducer(p);
  }
}