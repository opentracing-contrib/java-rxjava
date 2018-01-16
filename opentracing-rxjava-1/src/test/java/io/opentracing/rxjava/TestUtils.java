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

import static io.opentracing.rxjava.AbstractTracingSubscriber.COMPONENT_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

class TestUtils {

  static void checkSpans(List<MockSpan> mockSpans, long traceId) {
    for (MockSpan mockSpan : mockSpans) {
      assertEquals(COMPONENT_NAME, mockSpan.tags().get(Tags.COMPONENT.getKey()));
      assertEquals(0, mockSpan.generatedErrors().size());
      assertEquals(traceId, mockSpan.context().traceId());
    }
  }

  static void sleep() {
    try {
      TimeUnit.MILLISECONDS.sleep(200L);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  static Callable<Integer> reportedSpansSize(final MockTracer mockTracer) {
    return new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        return mockTracer.finishedSpans().size();
      }
    };
  }

  static Observable<Integer> createSequentialObservable(final MockTracer mockTracer) {
    return Observable.range(1, 10)
        .map(new Func1<Integer, Integer>() {
          @Override
          public Integer call(Integer integer) {
            assertNotNull(mockTracer.scopeManager().active());
            return integer * 3;
          }
        });
  }

  static Observable<Integer> createParallelObservable(final MockTracer mockTracer) {
    return Observable.range(1, 10)
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.computation())
        .map(new Func1<Integer, Integer>() {
          @Override
          public Integer call(Integer integer) {
            sleep();
            assertNotNull(mockTracer.scopeManager().active());
            return integer * 3;
          }
        }).filter(new Func1<Integer, Boolean>() {
          @Override
          public Boolean call(Integer integer) {
            sleep();
            assertNotNull(mockTracer.scopeManager().active());
            return integer % 2 == 0;
          }
        });
  }

  static Observable<Long> fromInterval(final MockTracer mockTracer) {
    return Observable.interval(500, TimeUnit.MILLISECONDS, Schedulers.computation())
        .map(new Func1<Long, Long>() {
          @Override
          public Long call(Long value) {
            assertNotNull(mockTracer.scopeManager().active());
            return value * 2;
          }
        })
        .subscribeOn(Schedulers.io())
        .take(5);
  }
}
