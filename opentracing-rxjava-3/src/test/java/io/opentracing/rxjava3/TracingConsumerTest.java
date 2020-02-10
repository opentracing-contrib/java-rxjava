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

import static io.opentracing.rxjava3.TestUtils.checkSpans;
import static io.opentracing.rxjava3.TestUtils.createParallelObservable;
import static io.opentracing.rxjava3.TestUtils.createSequentialObservable;
import static io.opentracing.rxjava3.TestUtils.reportedSpansSize;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Consumer;
import org.junit.Before;
import org.junit.Test;

public class TracingConsumerTest {

  private static final MockTracer mockTracer = new MockTracer();

  @Before
  public void before() {
    mockTracer.reset();
    TracingRxJavaUtils.enableTracing(mockTracer);
  }

  @Test
  public void sequential() {
    Observable<Integer> observable = createSequentialObservable(mockTracer);
    List<Integer> result = new ArrayList<>();
    Consumer<Integer> onNext = consumer(result);

    observable.subscribe(new TracingConsumer<>(onNext, "sequential", mockTracer));
    assertEquals(5, result.size());

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(1, spans.size());
    checkSpans(spans);

    assertNull(mockTracer.scopeManager().activeSpan());
  }

  @Test
  public void parallel() {
    Observable<Integer> observable = createParallelObservable(mockTracer);

    List<Integer> result = new ArrayList<>();
    Consumer<Integer> onNext = consumer(result);

    observable.subscribe(new TracingConsumer<>(onNext, "sequential", mockTracer));

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(mockTracer), equalTo(1));
    assertEquals(5, result.size());

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(1, spans.size());
    checkSpans(spans);

    assertNull(mockTracer.scopeManager().activeSpan());
  }

  private <T> Consumer<T> consumer(final List<T> result) {
    return new Consumer<T>() {
      @Override
      public void accept(T value) {
        System.out.println(value);
        result.add(value);
      }
    };
  }
}
