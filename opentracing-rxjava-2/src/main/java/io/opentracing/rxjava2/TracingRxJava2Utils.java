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


import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import io.reactivex.functions.Function;
import io.reactivex.plugins.RxJavaPlugins;


public class TracingRxJava2Utils {

  /**
   * GlobalTracer is used to get tracer
   */
  public static void enableTracing() {
    enableTracing(GlobalTracer.get());
  }

  public static void enableTracing(final Tracer tracer) {

    RxJavaPlugins.setScheduleHandler(new Function<Runnable, Runnable>() {
      @Override
      public Runnable apply(Runnable runnable) {
        return new TracingRunnable(runnable, tracer);
      }
    });
  }
}
