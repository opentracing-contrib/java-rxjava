/*
 * Copyright 2017-2019 The OpenTracing Authors
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
package io.opentracing.rxjava2;


import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;

class TracingRunnable implements Runnable {

  private final Runnable runnable;
  private final Tracer tracer;
  private final Span span;

  TracingRunnable(Runnable runnable, Tracer tracer) {
    this.runnable = runnable;
    this.tracer = tracer;
    span = getSpan(tracer);
  }

  private Span getSpan(Tracer tracer) {
    if (SpanHolder.getSpan() != null) {
      Span span = SpanHolder.getSpan();
      SpanHolder.clear();
      return span;
    }
    return tracer.activeSpan();
  }

  @Override
  public void run() {
    Scope scope = null;
    if (span != null) {
      scope = tracer.scopeManager().activate(span);
    }
    try {
      runnable.run();
    } finally {
      if (scope != null) {
        scope.close();
      }
    }
  }
}
