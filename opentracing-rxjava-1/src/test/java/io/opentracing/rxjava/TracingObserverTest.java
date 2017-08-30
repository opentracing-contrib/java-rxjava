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
import io.opentracing.util.ThreadLocalActiveSpanSource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Observable;
import rx.Observer;

public class TracingObserverTest {

  private static final MockTracer mockTracer = new MockTracer(new ThreadLocalActiveSpanSource(),
      MockTracer.Propagator.TEXT_MAP);

  @BeforeClass
  public static void beforeClass() {
    TracingRxJavaUtils.enableTracing(mockTracer);
  }

  @Before
  public void before() throws Exception {
    mockTracer.reset();
  }

  @Test
  public void sequential() {
    Observable<Integer> observable = createSequentialObservable();
    Observer<Integer> observer = observer("sequential");

    TracingObserverSubscriber<Integer> tracingObserverSubscriber =
        new TracingObserverSubscriber<>(observer, "sequential", mockTracer);

    observable.subscribe(tracingObserverSubscriber);

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(1, spans.size());
    checkSpans(spans, spans.get(0).context().traceId());

    assertNull(mockTracer.activeSpan());
  }

  @Test
  public void parallel() {
    Observable<Integer> observable = createParallelObservable();

    Observer<Integer> observer = observer("parallel");

    TracingObserverSubscriber<Integer> tracingObserverSubscriber =
        new TracingObserverSubscriber<>(observer, "parallel", mockTracer);

    observable.subscribe(tracingObserverSubscriber);

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(mockTracer), equalTo(1));

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(1, spans.size());
    checkSpans(spans, spans.get(0).context().traceId());

    assertNull(mockTracer.activeSpan());
  }

  private static <T> Observer<T> observer(final String name) {
    return new Observer<T>() {
      @Override
      public void onCompleted() {
        System.out.println(name + ": onCompleted");
      }

      @Override
      public void onError(Throwable e) {
        e.printStackTrace();
      }

      @Override
      public void onNext(T t) {
        System.out.println(name + ": " + t);
      }
    };
  }
}
