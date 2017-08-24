package io.opentracing.rxjava;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import rx.Subscriber;

class AbstractTracingSubscriber<T> extends Subscriber<T> {

  static final String COMPONENT_NAME = "rxjava-1";

  private final String operationName;
  private final Tracer tracer;
  private volatile Span span;

  AbstractTracingSubscriber(String operationName, Tracer tracer) {
    if (tracer == null) {
      throw new IllegalArgumentException("tracer can not be null");
    }

    this.operationName = operationName;
    this.tracer = tracer;
  }

  @Override
  public void onStart() {
    span = tracer.buildSpan(operationName)
        .withTag(Tags.COMPONENT.getKey(), COMPONENT_NAME).startManual();
  }

  @Override
  public void onCompleted() {
    span.finish();
  }

  @Override
  public void onError(Throwable e) {
    onError(e, span);
    span.finish();
  }

  @Override
  public void onNext(T t) {
  }

  private static void onError(Throwable throwable, Span span) {
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
