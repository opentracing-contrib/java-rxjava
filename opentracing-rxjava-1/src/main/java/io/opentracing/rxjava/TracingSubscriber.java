package io.opentracing.rxjava;

import io.opentracing.ActiveSpan;
import io.opentracing.ActiveSpan.Continuation;
import io.opentracing.tag.Tags;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import rx.Producer;
import rx.Subscriber;


class TracingSubscriber extends Subscriber {

  private final Subscriber subscriber;
  private final Continuation continuation;

  TracingSubscriber(Subscriber subscriber, ActiveSpan activeSpan) {
    this.subscriber = subscriber;
    this.continuation = activeSpan.capture();
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onNext(Object o) {
    subscriber.onNext(o);
  }

  @Override
  public void onError(Throwable t) {
    try {
      subscriber.onError(t);
    } finally {
      ActiveSpan activeSpan = continuation.activate();
      onError(t, activeSpan);
      activeSpan.deactivate();
    }
  }

  @Override
  public void onCompleted() {
    try {
      subscriber.onCompleted();
    } finally {
      continuation.activate().deactivate();
    }
  }

  @Override
  public void setProducer(Producer p) {
    subscriber.setProducer(p);
  }

  private static void onError(Throwable throwable, ActiveSpan span) {
    span.setTag(Tags.ERROR.getKey(), Boolean.TRUE);
    span.log(errorLogs(throwable));
  }

  private static Map<String, Object> errorLogs(Throwable throwable) {
    Map<String, Object> errorLogs = new HashMap<>();
    errorLogs.put("event", Tags.ERROR.getKey());
    errorLogs.put("error.kind", throwable.getClass().getName());
    errorLogs.put("error.object", throwable);

    errorLogs.put("message", throwable.getMessage());

    StringWriter sw = new StringWriter();
    throwable.printStackTrace(new PrintWriter(sw));
    errorLogs.put("stack", sw.toString());

    return errorLogs;
  }
}