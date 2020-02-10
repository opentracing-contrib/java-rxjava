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

import static io.opentracing.rxjava.TestUtils.checkSpans;
import static io.opentracing.rxjava.TestUtils.createParallelObservable;
import static io.opentracing.rxjava.TestUtils.createSequentialObservable;
import static io.opentracing.rxjava.TestUtils.reportedSpansSize;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.functions.Action1;

public class TracingActionTest {

  private static final MockTracer mockTracer = new MockTracer();

  @Before
  public void beforeClass() {
    TracingRxJavaUtils.enableTracing(mockTracer);
  }

  @Before
  public void before() {
    mockTracer.reset();
  }

  @Test
  public void sequential() {
    Observable<Integer> observable = createSequentialObservable(mockTracer);

    Action1<Integer> onNext = action1();

    observable.subscribe(new TracingActionSubscriber<>(onNext, "sequential", mockTracer));

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(1, spans.size());
    checkSpans(spans, spans.get(0).context().traceId());

    assertNull(mockTracer.scopeManager().activeSpan());
  }

  @Test
  public void parallel() {
    Observable<Integer> observable = createParallelObservable(mockTracer);

    Action1<Integer> onNext = action1();

    observable.subscribe(new TracingActionSubscriber<>(onNext, "parallel", mockTracer));

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(mockTracer), equalTo(1));

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(1, spans.size());
    checkSpans(spans, spans.get(0).context().traceId());

    assertNull(mockTracer.scopeManager().activeSpan());
  }

  @Test
  public void fromInterval() {
    Observable<Long> observable = TestUtils.fromInterval(mockTracer);

    Action1<Long> onNext = action1();

    observable.subscribe(new TracingActionSubscriber<>(onNext, "from_interval", mockTracer));

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(mockTracer), equalTo(1));

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(1, spans.size());
    checkSpans(spans, spans.get(0).context().traceId());

    assertNull(mockTracer.scopeManager().activeSpan());
  }

  private static <T> Action1<T> action1() {
    return new Action1<T>() {
      @Override
      public void call(T value) {
        assertNotNull(mockTracer.activeSpan());
        System.out.println(value);
      }
    };
  }
}
