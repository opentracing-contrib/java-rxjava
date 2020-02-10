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
package io.opentracing.rxjava3;

import static io.opentracing.rxjava3.RxTracer.COMPONENT_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.functions.Predicate;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

class TestUtils {

  static Observable<Integer> createSequentialObservable(final MockTracer mockTracer) {
    return Observable.range(1, 10)
        .map(new Function<Integer, Integer>() {
          @Override
          public Integer apply(Integer integer) {
            assertNotNull(mockTracer.scopeManager().activeSpan());
            return integer * 3;
          }
        })
        .filter(new Predicate<Integer>() {
          @Override
          public boolean test(Integer integer) {
            assertNotNull(mockTracer.scopeManager().activeSpan());
            return integer % 2 == 0;
          }
        });
  }

  static Observable<Integer> createParallelObservable(final MockTracer mockTracer) {
    return Observable.range(1, 10)
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.computation())
        .map(new Function<Integer, Integer>() {
          @Override
          public Integer apply(Integer integer) {
            sleep();
            assertNotNull(mockTracer.scopeManager().activeSpan());
            return integer * 3;
          }
        })
        .filter(new Predicate<Integer>() {
          @Override
          public boolean test(Integer integer) {
            sleep();
            assertNotNull(mockTracer.scopeManager().activeSpan());
            return integer % 2 == 0;
          }
        });
  }

  static Flowable<Integer> createSequentialFlowable(final MockTracer mockTracer) {
    return createSequentialObservable(mockTracer).toFlowable(BackpressureStrategy.ERROR);
  }

  static Flowable<Integer> createParallelFlowable(final MockTracer mockTracer) {
    return createParallelObservable(mockTracer).toFlowable(BackpressureStrategy.ERROR);
  }

  private static void sleep() {
    try {
      TimeUnit.MILLISECONDS.sleep(200L);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  static void checkSpans(List<MockSpan> mockSpans) {
    for (MockSpan mockSpan : mockSpans) {
      assertEquals(COMPONENT_NAME, mockSpan.tags().get(Tags.COMPONENT.getKey()));
      assertEquals(0, mockSpan.generatedErrors().size());
    }
  }

  static Callable<Integer> reportedSpansSize(final MockTracer mockTracer) {
    return new Callable<Integer>() {
      @Override
      public Integer call() {
        return mockTracer.finishedSpans().size();
      }
    };
  }
}
