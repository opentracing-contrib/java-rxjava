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
package io.opentracing.rxjava3;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

final class RxTracer {

  static final String COMPONENT_NAME = "rxjava-3";

  private final String operationName;
  private final Tracer tracer;
  private volatile Span span;

  RxTracer(String operationName, Tracer tracer) {
    this.operationName = operationName;
    this.tracer = tracer;
  }

  void onSubscribe() {
    span = tracer.buildSpan(operationName)
        .withTag(Tags.COMPONENT.getKey(), COMPONENT_NAME).start();
    Scope scope = tracer.activateSpan(span);
    SpanHolder.set(scope, span);
  }

  void onError(Throwable t) {
    onError(t, span);
    span.finish();
    SpanHolder.clear();
  }

  void onComplete() {
    span.finish();
    SpanHolder.clear();
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
