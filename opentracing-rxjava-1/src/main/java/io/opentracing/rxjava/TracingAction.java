/*
 * Copyright 2017-2018 The OpenTracing Authors
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

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import rx.functions.Action0;


class TracingAction implements Action0 {

  private final Action0 action0;
  private final Tracer tracer;
  private final Span span;

  TracingAction(Action0 action0, Tracer tracer) {
    this.action0 = action0;
    this.tracer = tracer;
    span = tracer.activeSpan();
  }

  @Override
  public void call() {
    Scope scope = null;
    if (span != null) {
      scope = tracer.scopeManager().activate(span, false);
    }

    try {
      action0.call();
    } finally {
      if (scope != null) {
        scope.close();
      }
    }
  }
}
