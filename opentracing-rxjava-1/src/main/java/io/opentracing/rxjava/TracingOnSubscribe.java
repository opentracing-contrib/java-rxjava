package io.opentracing.rxjava;


import static io.opentracing.rxjava.TracingRxJavaUtils.COMPONENT_NAME;

import io.opentracing.ActiveSpan;
import io.opentracing.References;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.tag.Tags;
import rx.Observable.OnSubscribe;
import rx.Subscriber;


class TracingOnSubscribe<T> implements OnSubscribe<T> {

  private final OnSubscribe<T> onSubscribe;
  private final Tracer tracer;
  private final Span span;

  TracingOnSubscribe(OnSubscribe<T> onSubscribe, final Tracer tracer) {
    this.onSubscribe = onSubscribe;
    this.tracer = tracer;

    SpanBuilder spanBuilder = tracer.buildSpan(onSubscribe.getClass().getSimpleName());
    SpanContext parentContext = SpanContextHolder.get();
    if (parentContext != null) {
      spanBuilder.addReference(References.CHILD_OF, parentContext);
    }
    span = spanBuilder.startManual();
    if (onSubscribe.getClass().getSimpleName().isEmpty()) {
      span.setOperationName(onSubscribe.getClass().getName());
    }

    SpanContextHolder.set(span.context());
  }

  @Override
  @SuppressWarnings("unchecked")
  public void call(Subscriber<? super T> subscriber) {
    SpanContextHolder.clear();
    try (ActiveSpan activeSpan = tracer.makeActive(span)) {
      if (onSubscribe.getClass().getSimpleName().isEmpty()) {
        activeSpan.setOperationName(onSubscribe.getClass().getName());
      }

      activeSpan.setTag(Tags.COMPONENT.getKey(), COMPONENT_NAME);
      TracingSubscriber t = new TracingSubscriber(subscriber, activeSpan);
      subscriber.add(t);
      onSubscribe.call(t);
    }
  }
}
