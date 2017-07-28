package io.opentracing.rxjava2;


import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TracingTest {

  private static final MockTracer mockTracer = new MockTracer(MockTracer.Propagator.TEXT_MAP);

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
    Observable<Integer> observable = Observable.range(1, 2)
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

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(), equalTo(3));

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(3, spans.size());
    checkSpans(spans);
    checkParentIds(spans);

    assertNull(mockTracer.activeSpan());

    assertNull(SpanHolder.get());
  }

  @Test
  public void parallel() {
    Observable<Integer> observable = Observable.range(1, 2)
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.computation())
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

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(), equalTo(5));

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(5, spans.size());
    checkSpans(spans);
    checkParentIds(spans);

    assertNull(mockTracer.activeSpan());

    assertNull(SpanHolder.get());
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
}
