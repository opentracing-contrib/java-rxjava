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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.opentracing.Scope;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import org.junit.Before;
import org.junit.Test;

public class TracingObserverTest {

  private static final MockTracer mockTracer = new MockTracer();

  @Before
  public void before() {
    mockTracer.reset();
    TracingRxJavaUtils.enableTracing(mockTracer);
  }

  @Test
  public void sequential() {
    List<Integer> result = new ArrayList<>();
    executeSequentialObservable("sequential", result);

    assertEquals(5, result.size());

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(1, spans.size());
    checkSpans(spans);

    assertNull(mockTracer.scopeManager().activeSpan());
  }

  @Test
  public void two_sequential() {
    List<Integer> result = new ArrayList<>();
    executeSequentialObservable("two_sequential first", result);
    executeSequentialObservable("two_sequential second", result);

    assertEquals(10, result.size());

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(2, spans.size());

    assertNotEquals(spans.get(0).context().traceId(), spans.get(1).context().traceId());

    assertNull(mockTracer.scopeManager().activeSpan());
  }

  @Test
  public void sequential_with_parent() {
    List<Integer> result = new ArrayList<>();
    final MockSpan parent = mockTracer.buildSpan("parent").start();
    try (Scope ignored = mockTracer.activateSpan(parent)) {
      executeSequentialObservable("sequential_with_parent first", result);
      executeSequentialObservable("sequential_with_parent second", result);
    }
    parent.finish();

    assertEquals(10, result.size());

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
    List<Integer> result = new ArrayList<>();
    executeParallelObservable("parallel", result);

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(mockTracer), equalTo(1));

    assertEquals(5, result.size());

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(1, spans.size());
    checkSpans(spans);

    assertNull(mockTracer.scopeManager().activeSpan());
  }

  @Test
  public void two_parallel() {
    List<Integer> result = new CopyOnWriteArrayList<>();
    executeParallelObservable("first_parallel", result);
    executeParallelObservable("second_parallel", result);

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(mockTracer), equalTo(2));

    assertEquals(10, result.size());

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(2, spans.size());

    assertNotEquals(spans.get(0).context().traceId(), spans.get(1).context().traceId());

    assertNull(mockTracer.scopeManager().activeSpan());
  }

  @Test
  public void parallel_with_parent() {
    List<Integer> result = new CopyOnWriteArrayList<>();
    final MockSpan parent = mockTracer.buildSpan("parallel_parent").start();
    try (Scope ignored = mockTracer.activateSpan(parent)) {
      executeParallelObservable("first_parallel_with_parent", result);
      executeParallelObservable("second_parallel_with_parent", result);
    }
    parent.finish();

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(mockTracer), equalTo(3));

    assertEquals(10, result.size());

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(3, spans.size());

    assertNotNull(parent);

    for (MockSpan span : spans) {
      assertEquals(parent.context().traceId(), span.context().traceId());
    }

    assertNull(mockTracer.scopeManager().activeSpan());
  }

  private void executeSequentialObservable(String name, List<Integer> result) {
    Observable<Integer> observable = createSequentialObservable(mockTracer);

    Observer<Integer> observer = observer(name, result);

    observable.subscribe(new TracingObserver<>(observer, "sequential", mockTracer));

  }

  private void executeParallelObservable(final String name, List<Integer> result) {
    Observable<Integer> observable = createParallelObservable(mockTracer);

    Observer<Integer> observer = observer(name, result);

    observable.subscribe(new TracingObserver<>(observer, "parallel", mockTracer));
  }

  private static <T> Observer<T> observer(final String name, final List<T> result) {
    return new Observer<T>() {
      @Override
      public void onSubscribe(Disposable d) {

      }

      @Override
      public void onNext(T value) {
        System.out.println(name + ": " + value);
        result.add(value);
      }

      @Override
      public void onError(Throwable e) {
        e.printStackTrace();
      }

      @Override
      public void onComplete() {
        System.out.println(name + ": onComplete");
      }
    };
  }

}
