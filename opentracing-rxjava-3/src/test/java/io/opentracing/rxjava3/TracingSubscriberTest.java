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
import static io.opentracing.rxjava3.TestUtils.createParallelFlowable;
import static io.opentracing.rxjava3.TestUtils.createSequentialFlowable;
import static io.opentracing.rxjava3.TestUtils.reportedSpansSize;
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

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.junit.Before;
import org.junit.Test;

public class TracingSubscriberTest {

  private static final MockTracer mockTracer = new MockTracer();

  @Before
  public void before() {
    mockTracer.reset();
    TracingRxJavaUtils.enableTracing(mockTracer);
  }

  @Test
  public void sequential() {
    TestSubscriber<Integer> testSubscriber = executeSequentialFlowable();
    testSubscriber.assertValueCount(5);
    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(1, spans.size());
    checkSpans(spans);

    assertNull(mockTracer.scopeManager().activeSpan());
  }

  @Test
  public void two_sequential() {
    TestSubscriber<Integer> testSubscriber1 = executeSequentialFlowable();
    TestSubscriber<Integer> testSubscriber2 = executeSequentialFlowable();

    testSubscriber1.assertValueCount(5);
    testSubscriber2.assertValueCount(5);

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(2, spans.size());

    assertNotEquals(spans.get(0).context().traceId(), spans.get(1).context().traceId());

    assertNull(mockTracer.scopeManager().activeSpan());
  }

  @Test
  public void sequential_with_parent() {
    final MockSpan parent = mockTracer.buildSpan("parent").start();
    try (Scope ignored = mockTracer.activateSpan(parent)) {
      TestSubscriber<Integer> testSubscriber1 = executeSequentialFlowable();
      TestSubscriber<Integer> testSubscriber2 = executeSequentialFlowable();

      testSubscriber1.assertValueCount(5);
      testSubscriber2.assertValueCount(5);
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
  public void parallel() {
    TestSubscriber<Integer> testSubscriber = executeParallelFlowable();

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(mockTracer), equalTo(1));

    testSubscriber.assertValueCount(5);

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(1, spans.size());
    checkSpans(spans);

    assertNull(mockTracer.scopeManager().activeSpan());
  }

  @Test
  public void two_parallel() {
    TestSubscriber<Integer> testSubscriber1 = executeParallelFlowable();
    TestSubscriber<Integer> testSubscriber2 = executeParallelFlowable();

    testSubscriber1.awaitDone(60, TimeUnit.SECONDS);
    testSubscriber2.awaitDone(60, TimeUnit.SECONDS);

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(mockTracer), equalTo(2));

    testSubscriber1.assertValueCount(5);
    testSubscriber2.assertValueCount(5);

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(2, spans.size());

    assertNotEquals(spans.get(0).context().traceId(), spans.get(1).context().traceId());

    assertNull(mockTracer.scopeManager().activeSpan());
  }

  @Test
  public void parallel_with_parent() {
    final MockSpan parent = mockTracer.buildSpan("parallel_parent").start();
    try (Scope ignored = mockTracer.activateSpan(parent)) {
      TestSubscriber<Integer> testSubscriber1 = executeParallelFlowable();
      TestSubscriber<Integer> testSubscriber2 = executeParallelFlowable();

      testSubscriber1.awaitDone(60, TimeUnit.SECONDS);
      testSubscriber2.awaitDone(60, TimeUnit.SECONDS);

      testSubscriber1.assertValueCount(5);
      testSubscriber2.assertValueCount(5);
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

  private TestSubscriber<Integer> executeSequentialFlowable() {
    Flowable<Integer> Flowable = createSequentialFlowable(mockTracer);

    TestSubscriber<Integer> subscriber = new TestSubscriber<>();

    Flowable.subscribe(TracingSubscriber.create(subscriber, "sequential", mockTracer));

    return subscriber;
  }

  private TestSubscriber<Integer> executeParallelFlowable() {
    Flowable<Integer> flowable = createParallelFlowable(mockTracer);

    TestSubscriber<Integer> subscriber = new TestSubscriber<>();

    flowable.subscribe(TracingSubscriber.create(subscriber, "parallel", mockTracer));

    return subscriber;
  }
}
