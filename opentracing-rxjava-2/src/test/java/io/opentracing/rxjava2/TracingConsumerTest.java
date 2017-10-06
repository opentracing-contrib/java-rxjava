package io.opentracing.rxjava2;

import static com.jayway.awaitility.Awaitility.await;
import static io.opentracing.rxjava2.TestUtils.checkSpans;
import static io.opentracing.rxjava2.TestUtils.createParallelObservable;
import static io.opentracing.rxjava2.TestUtils.createSequentialObservable;
import static io.opentracing.rxjava2.TestUtils.reportedSpansSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalScopeManager;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;

public class TracingConsumerTest {

  private static final MockTracer mockTracer = new MockTracer(new ThreadLocalScopeManager(),
      MockTracer.Propagator.TEXT_MAP);

  @Before
  public void before() throws Exception {
    mockTracer.reset();
  }

  @Test
  public void sequential() {
    Observable<Integer> observable = createSequentialObservable();
    Consumer<Integer> onNext = consumer();

    observable.subscribe(new TracingConsumer<>(onNext, "sequential", mockTracer));

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(1, spans.size());
    checkSpans(spans);

    assertNull(mockTracer.scopeManager().active());
  }

  @Test
  public void parallel() {
    Observable<Integer> observable = createParallelObservable();

    Consumer<Integer> onNext = consumer();

    observable.subscribe(new TracingConsumer<>(onNext, "sequential", mockTracer));

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(mockTracer), equalTo(1));

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(1, spans.size());
    checkSpans(spans);

    assertNull(mockTracer.scopeManager().active());
  }

  private <T> Consumer<T> consumer() {
    return new Consumer<T>() {
      @Override
      public void accept(T value) throws Exception {
        System.out.println(value);
      }
    };
  }
}
