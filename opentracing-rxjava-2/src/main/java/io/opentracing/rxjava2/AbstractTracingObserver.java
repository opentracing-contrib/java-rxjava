package io.opentracing.rxjava2;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

class AbstractTracingObserver<T> implements Observer<T> {

  static final String COMPONENT_NAME = "rxjava-2";

  private final String operationName;
  private final Tracer tracer;
  private volatile Span span;

  AbstractTracingObserver(String operationName, Tracer tracer) {
    this.operationName = operationName;
    this.tracer = tracer;
  }

  @Override
  public void onSubscribe(Disposable d) {
    span = tracer.buildSpan(operationName)
        .withTag(Tags.COMPONENT.getKey(), COMPONENT_NAME).startManual();
  }

  @Override
  public void onNext(T t) {
  }

  @Override
  public void onError(Throwable t) {
    onError(t, span);
    span.finish();
  }

  @Override
  public void onComplete() {
    span.finish();
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
