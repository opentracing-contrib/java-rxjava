package io.opentracing.rxjava2;

import io.opentracing.Span;
import io.opentracing.tag.Tags;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;


class TracingObserver implements Observer<Object>, Disposable {

  static final String COMPONENT_NAME = "rxjava-2";
  private Disposable upstream;
  private final Observer observer;
  private final Span span;

  TracingObserver(Observer observer) {
    this.observer = observer;
    span = SpanStackHolder.remove();
  }

  @Override
  public void onSubscribe(Disposable d) {
    upstream = d;
    observer.onSubscribe(this);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onNext(Object o) {
    observer.onNext(o);
  }

  @Override
  public void onError(Throwable t) {
    try {
      observer.onError(t);
    } finally {
      if (span != null) {
        span.finish();
        onError(t, span);
      }
    }
  }

  @Override
  public void onComplete() {
    try {
      observer.onComplete();
    } finally {
      if (span != null) {
        span.finish();
      }
    }
  }

  @Override
  public void dispose() {
    upstream.dispose();
  }

  @Override
  public boolean isDisposed() {
    return upstream.isDisposed();
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
