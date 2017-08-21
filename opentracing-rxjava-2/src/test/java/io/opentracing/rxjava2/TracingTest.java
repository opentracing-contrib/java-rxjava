package io.opentracing.rxjava2;


import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.opentracing.ActiveSpan;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.ThreadLocalActiveSpanSource;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TracingTest {

  private static final MockTracer mockTracer = new MockTracer(new ThreadLocalActiveSpanSource(),
      MockTracer.Propagator.TEXT_MAP);

  @BeforeClass
  public static void init() {
    GlobalTracer.register(mockTracer);
    TracingRxJava2Utils.enableTracing();
  }

  @Before
  public void before() throws Exception {
    mockTracer.reset();
  }

  @Test
  public void sequential() {
    executeSequentialObservable();

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(3, spans.size());
    checkSpans(spans);
    checkParentIds(spans);

    assertNull(mockTracer.activeSpan());

    assertNull(SpanHolder.get());
  }

  @Test
  public void two_sequential() {
    executeSequentialObservable();
    executeSequentialObservable();

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(6, spans.size());

    for (int i = 1; i < 3; i++) {
      assertEquals(spans.get(0).context().traceId(), spans.get(i).context().traceId());
    }
    for (int i = 4; i < 6; i++) {
      assertEquals(spans.get(3).context().traceId(), spans.get(i).context().traceId());
    }
    assertNotEquals(spans.get(0).context().traceId(), spans.get(3).context().traceId());

    checkParentIds(spans.subList(0, 3));
    checkParentIds(spans.subList(3, 6));

    assertNull(mockTracer.activeSpan());
  }

  @Test
  public void sequential_with_parent() {
    try (ActiveSpan parent = mockTracer.buildSpan("parent").startActive()) {
      executeSequentialObservable();
      executeSequentialObservable();
    }

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(7, spans.size());

    MockSpan parent = getOneSpanByOperationName(spans, "parent");
    assertNotNull(parent);

    for (MockSpan span : spans) {
      assertEquals(parent.context().traceId(), span.context().traceId());
    }

    assertNull(mockTracer.activeSpan());
  }

  @Test
  public void parallel() {
    executeParallelObservable("parallel");

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(), equalTo(5));

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(5, spans.size());
    checkSpans(spans);
    checkParentIds(spans);

    assertNull(mockTracer.activeSpan());

    assertNull(SpanHolder.get());
  }

  @Test
  public void two_parallel() {
    executeParallelObservable("first_parallel");
    executeParallelObservable("second_parallel");

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(), equalTo(10));
    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(10, spans.size());

    Collections.sort(spans, new Comparator<MockSpan>() {
      @Override
      public int compare(MockSpan o1, MockSpan o2) {
        return Long.compare(o1.context().traceId(), o2.context().traceId());
      }
    });

    for (int i = 1; i < 5; i++) {
      assertEquals(spans.get(0).context().traceId(), spans.get(i).context().traceId());
    }
    for (int i = 6; i < 10; i++) {
      assertEquals(spans.get(5).context().traceId(), spans.get(i).context().traceId());
    }

    assertNotEquals(spans.get(0).context().traceId(), spans.get(5).context().traceId());

    checkParentIds(spans.subList(0, 5));
    checkParentIds(spans.subList(5, 10));

    assertNull(mockTracer.activeSpan());
  }

  @Test
  public void parallel_with_parent() throws Exception {
    try (ActiveSpan parent = mockTracer.buildSpan("parallel_parent").startActive()) {
      executeParallelObservable("first_parallel_with_parent");
      executeParallelObservable("second_parallel_with_parent");
    }

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(), equalTo(11));
    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(11, spans.size());

    MockSpan parent = getOneSpanByOperationName(spans, "parallel_parent");
    assertNotNull(parent);

    for (MockSpan span : spans) {
      assertEquals(parent.context().traceId(), span.context().traceId());
    }

    assertNull(mockTracer.activeSpan());
  }

  private void executeSequentialObservable() {
    Observable<Integer> observable = Observable.range(1, 10)
        .map(new Function<Integer, Integer>() {
          @Override
          public Integer apply(Integer integer) throws Exception {
            return integer * 3;
          }
        })
        .filter(new Predicate<Integer>() {
          @Override
          public boolean test(Integer integer) throws Exception {
            return integer % 2 == 0;
          }
        });

    observable.subscribe(new Consumer<Integer>() {
      @Override
      public void accept(Integer integer) throws Exception {
        System.out.println(integer);
      }
    });
  }

  private void executeParallelObservable(final String name) {
    Observable<Integer> observable = Observable.range(1, 10)
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.computation())
        .map(new Function<Integer, Integer>() {
          @Override
          public Integer apply(Integer integer) throws Exception {
            sleep();
            return integer * 3;
          }
        })
        .filter(new Predicate<Integer>() {
          @Override
          public boolean test(Integer integer) throws Exception {
            sleep();
            return integer % 2 == 0;
          }
        });

    observable.subscribe(new Consumer<Integer>() {
      @Override
      public void accept(Integer integer) throws Exception {
        System.out.println(name + ": " + integer);
      }
    });
  }

  private Callable<Integer> reportedSpansSize() {
    return new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        return mockTracer.finishedSpans().size();
      }
    };
  }

  private void checkSpans(List<MockSpan> mockSpans) {
    for (MockSpan mockSpan : mockSpans) {
      assertEquals(TracingObserver.COMPONENT_NAME, mockSpan.tags().get(Tags.COMPONENT.getKey()));
      assertEquals(0, mockSpan.generatedErrors().size());
    }
  }

  /**
   * check that span parentId is equal to previous span spanId
   */
  private void checkParentIds(List<MockSpan> mockSpans) {
    Collections.sort(mockSpans, new Comparator<MockSpan>() {
      @Override
      public int compare(MockSpan o1, MockSpan o2) {
        return Long.compare(o1.parentId(), o2.parentId());
      }
    });

    for (int i = 1; i < mockSpans.size(); i++) {
      assertEquals(mockSpans.get(i - 1).context().spanId(), mockSpans.get(i).parentId());
    }
  }

  private void sleep() {
    try {
      TimeUnit.MILLISECONDS.sleep(200L);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
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
