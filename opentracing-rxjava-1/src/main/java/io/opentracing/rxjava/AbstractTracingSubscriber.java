/*
 * Copyright 2017-2020 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
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
  private final String componentName;
  private final Tracer tracer;
  private volatile Span span;

  AbstractTracingSubscriber(String operationName, String componentName, Tracer tracer) {
    if (tracer == null) {
      throw new IllegalArgumentException("tracer can not be null");
    }

    this.operationName = operationName;
    this.componentName = componentName;
    this.tracer = tracer;
  }

  public Span getSpan() {
    return span;
  }

  @Override
  public void onStart() {
    span = tracer.buildSpan(operationName)
        .withTag(Tags.COMPONENT.getKey(), componentName).start();
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
