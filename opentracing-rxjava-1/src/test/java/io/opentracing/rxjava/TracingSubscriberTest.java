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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.opentracing.Scope;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.Subscriber;

public class TracingSubscriberTest {

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
    executeSequentialObservable("sequential");

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(1, spans.size());
    checkSpans(spans, spans.get(0).context().traceId());

    assertNull(mockTracer.scopeManager().activeSpan());
  }

  @Test
  public void two_sequential() {
    executeSequentialObservable("two_sequential first");
    executeSequentialObservable("two_sequential second");

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(2, spans.size());

    assertNotEquals(spans.get(0).context().traceId(), spans.get(1).context().traceId());

    assertNull(mockTracer.scopeManager().activeSpan());
  }

  @Test
  public void sequential_with_parent() {
    final MockSpan parent = mockTracer.buildSpan("parent").start();
    try (Scope ignored = mockTracer.activateSpan(parent)) {
      executeSequentialObservable("sequential_with_parent first");
      executeSequentialObservable("sequential_with_parent second");
    }
    parent.finish();

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(3, spans.size());

    assertNotNull(parent);

    for (MockSpan span : spans) {
      assertEquals(parent.context().traceId(), span.context().traceId());
    }

    assertNull(mockTracer.scopeManager().activeSpan());
  }

  @Test
  public void from_interval() {
    Observable<Long> observable = TestUtils.fromInterval(mockTracer);

    Subscriber<Long> subscriber = subscriber("from_interval");

    observable.subscribe(new TracingSubscriber<>(subscriber, "from_interval", mockTracer));

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(mockTracer), equalTo(1));

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(1, spans.size());
    checkSpans(spans, spans.get(0).context().traceId());

    assertNull(mockTracer.scopeManager().activeSpan());
  }

  @Test
  public void parallel() {
    executeParallelObservable("parallel");

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(mockTracer), equalTo(1));

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(1, spans.size());
    checkSpans(spans, spans.get(0).context().traceId());

    assertNull(mockTracer.scopeManager().activeSpan());
  }

  @Test
  public void two_parallel() {
    executeParallelObservable("first_parallel");
    executeParallelObservable("second_parallel");

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(mockTracer), equalTo(2));
    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(2, spans.size());

    assertNotEquals(spans.get(0).context().traceId(), spans.get(1).context().traceId());

    assertNull(mockTracer.scopeManager().activeSpan());
  }

  @Test
  public void parallel_with_parent() {
    final MockSpan parent = mockTracer.buildSpan("parallel_parent").start();
    try (Scope ignored = mockTracer.activateSpan(parent)) {
      executeParallelObservable("first_parallel_with_parent");
      executeParallelObservable("second_parallel_with_parent");
    }
    parent.finish();

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(mockTracer), equalTo(3));
    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(3, spans.size());

    assertNotNull(parent);

    for (MockSpan span : spans) {
      assertEquals(parent.context().traceId(), span.context().traceId());
    }

    assertNull(mockTracer.scopeManager().activeSpan());
  }

  private void executeSequentialObservable(final String name) {
    Observable<Integer> observable = createSequentialObservable(mockTracer);

    Subscriber<Integer> subscriber = subscriber(name);

    observable.subscribe(new TracingSubscriber<>(subscriber, "sequential", mockTracer));
  }

  private void executeParallelObservable(final String name) {
    Observable<Integer> observable = createParallelObservable(mockTracer);

    Subscriber<Integer> subscriber = subscriber(name);

    observable.subscribe(new TracingSubscriber<>(subscriber, "parallel", mockTracer));
  }

  private static <T> Subscriber<T> subscriber(final String name) {
    return new Subscriber<T>() {

      public void onStart() {
        assertNotNull(mockTracer.activeSpan());
        System.out.println(name + ": onStart");
      }

      @Override
      public void onCompleted() {
        assertNotNull(mockTracer.activeSpan());
        System.out.println(name + ": onCompleted");
      }

      @Override
      public void onError(Throwable e) {
        e.printStackTrace();
      }

      @Override
      public void onNext(T value) {
        assertNotNull(mockTracer.activeSpan());
        System.out.println(name + ": " + value);
      }
    };
  }
}
