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

import rx.functions.Action0;
import rx.functions.Action1;

class TracingEmptyAction<T0> implements Action0, Action1<T0> {

  private static final TracingEmptyAction EMPTY_ACTION = new TracingEmptyAction();

  @SuppressWarnings("unchecked")
  static <T0> TracingEmptyAction<T0> empty() {
    return EMPTY_ACTION;
  }

  @Override
  public void call() {
  }

  @Override
  public void call(T0 t0) {
  }
}
