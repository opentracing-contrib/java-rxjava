package io.opentracing.rxjava2;


import static com.jayway.awaitility.Awaitility.await;
import static io.opentracing.rxjava2.TestUtils.checkSpans;
import static io.opentracing.rxjava2.TestUtils.createParallelObservable;
import static io.opentracing.rxjava2.TestUtils.createSequentialObservable;
import static io.opentracing.rxjava2.TestUtils.reportedSpansSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.opentracing.ActiveSpan;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalActiveSpanSource;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;

public class TracingObserverTest {

  private static final MockTracer mockTracer = new MockTracer(new ThreadLocalActiveSpanSource(),
      MockTracer.Propagator.TEXT_MAP);

  @Before
  public void before() throws Exception {
    mockTracer.reset();
  }

  @Test
  public void sequential() {
    executeSequentialObservable("sequential");

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(1, spans.size());
    checkSpans(spans);

    assertNull(mockTracer.activeSpan());
  }

  @Test
  public void two_sequential() {
    executeSequentialObservable("two_sequential first");
    executeSequentialObservable("two_sequential second");

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(2, spans.size());

    assertNotEquals(spans.get(0).context().traceId(), spans.get(1).context().traceId());

    assertNull(mockTracer.activeSpan());
  }

  @Test
  public void sequential_with_parent() {
    try (ActiveSpan parent = mockTracer.buildSpan("parent").startActive()) {
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

    assertNull(mockTracer.activeSpan());
  }

  @Test
  public void parallel() throws Exception {
    executeParallelObservable("parallel");

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(mockTracer), equalTo(1));

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(1, spans.size());
    checkSpans(spans);

    assertNull(mockTracer.activeSpan());
  }

  @Test
  public void two_parallel() throws Exception {
    executeParallelObservable("first_parallel");
    executeParallelObservable("second_parallel");

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(mockTracer), equalTo(2));
    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(2, spans.size());

    assertNotEquals(spans.get(0).context().traceId(), spans.get(1).context().traceId());

    assertNull(mockTracer.activeSpan());
  }

  @Test
  public void parallel_with_parent() throws Exception {
    try (ActiveSpan parent = mockTracer.buildSpan("parallel_parent").startActive()) {
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

    assertNull(mockTracer.activeSpan());
  }

  private void executeSequentialObservable(String name) {
    Observable<Integer> observable = createSequentialObservable();

    Observer<Integer> observer = observer(name);

    observable.subscribe(new TracingObserver<>(observer, "sequential", mockTracer));

  }

  private void executeParallelObservable(final String name) {
    Observable<Integer> observable = createParallelObservable();

    Observer<Integer> observer = observer(name);

    observable.subscribe(new TracingObserver<>(observer, "parallel", mockTracer));
  }

  private static <T> Observer<T> observer(final String name) {
    return new Observer<T>() {
      @Override
      public void onSubscribe(Disposable d) {

      }

      @Override
      public void onNext(T value) {
        System.out.println(name + ": " + value);
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
}
