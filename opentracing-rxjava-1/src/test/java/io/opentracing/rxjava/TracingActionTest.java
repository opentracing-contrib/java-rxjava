package io.opentracing.rxjava;

import static com.jayway.awaitility.Awaitility.await;
import static io.opentracing.rxjava.TestUtils.checkSpans;
import static io.opentracing.rxjava.TestUtils.createParallelObservable;
import static io.opentracing.rxjava.TestUtils.createSequentialObservable;
import static io.opentracing.rxjava.TestUtils.reportedSpansSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalScopeManager;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.functions.Action1;

public class TracingActionTest {

  private static final MockTracer mockTracer = new MockTracer(MockTracer.Propagator.TEXT_MAP);

  @Before
  public void beforeClass() {
    mockTracer.setScopeManager(new ThreadLocalScopeManager());
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

    assertNull(mockTracer.scopeManager().active());
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

    assertNull(mockTracer.scopeManager().active());
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

    assertNull(mockTracer.scopeManager().active());
  }

  private static <T> Action1<T> action1() {
    return new Action1<T>() {
      @Override
      public void call(T value) {
        System.out.println(value);
      }
    };
  }
}
