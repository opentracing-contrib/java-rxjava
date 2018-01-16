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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.opentracing.util.ThreadLocalScopeManager;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.Subscriber;

public class TracingSubscriberTest {

  private static final MockTracer mockTracer = new MockTracer(new ThreadLocalScopeManager(),
          MockTracer.Propagator.TEXT_MAP);

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

    assertNull(mockTracer.scopeManager().active());
  }

  @Test
  public void two_sequential() {
    executeSequentialObservable("two_sequential first");
    executeSequentialObservable("two_sequential second");

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(2, spans.size());

    assertNotEquals(spans.get(0).context().traceId(), spans.get(1).context().traceId());

    assertNull(mockTracer.scopeManager().active());
  }

  @Test
  public void sequential_with_parent() {
    try (Scope parent = mockTracer.buildSpan("parent").startActive(true)) {
      executeSequentialObservable("sequential_with_parent first");
      executeSequentialObservable("sequential_with_parent second");
    }

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(3, spans.size());

    MockSpan parent = getOneSpanByOperationName(spans, "parent");
    assertNotNull(parent);

    for (MockSpan span : spans) {
      assertEquals(parent.context().traceId(), span.context().traceId());
    }

    assertNull(mockTracer.scopeManager().active());
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

    assertNull(mockTracer.scopeManager().active());
  }

  @Test
  public void parallel() {
    executeParallelObservable("parallel");

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(mockTracer), equalTo(1));

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(1, spans.size());
    checkSpans(spans, spans.get(0).context().traceId());

    assertNull(mockTracer.scopeManager().active());
  }

  @Test
  public void two_parallel() {
    executeParallelObservable("first_parallel");
    executeParallelObservable("second_parallel");

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(mockTracer), equalTo(2));
    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(2, spans.size());

    assertNotEquals(spans.get(0).context().traceId(), spans.get(1).context().traceId());

    assertNull(mockTracer.scopeManager().active());
  }

  @Test
  public void parallel_with_parent() throws Exception {
    try (Scope parent = mockTracer.buildSpan("parallel_parent")
        .startActive(true)) {
      executeParallelObservable("first_parallel_with_parent");
      executeParallelObservable("second_parallel_with_parent");
    }

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(mockTracer), equalTo(3));
    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(3, spans.size());

    MockSpan parent = getOneSpanByOperationName(spans, "parallel_parent");
    assertNotNull(parent);

    for (MockSpan span : spans) {
      assertEquals(parent.context().traceId(), span.context().traceId());
    }

    assertNull(mockTracer.scopeManager().active());
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

  private MockSpan getOneSpanByOperationName(List<MockSpan> spans, String operationName) {
    List<MockSpan> found = new ArrayList<>();
    for (MockSpan span : spans) {
      if (operationName.equals(span.operationName())) {
        found.add(span);
      }
    }
    if (found.size() > 1) {
      throw new RuntimeException(
          "Ups, too many spans (" + found.size() + ") with operation name " + operationName);
    }
    return found.isEmpty() ? null : spans.get(0);
  }

  private static <T> Subscriber<T> subscriber(final String name) {
    return new Subscriber<T>() {

      public void onStart() {
        System.out.println(name + ": onStart");
      }

      @Override
      public void onCompleted() {
        System.out.println(name + ": onCompleted");
      }

      @Override
      public void onError(Throwable e) {
        e.printStackTrace();
      }

      @Override
      public void onNext(T value) {
        System.out.println(name + ": " + value);
      }
    };
  }
}
