package io.opentracing.rxjava2;


import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
  public void test() {
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

    assertNull(mockTracer.activeSpan());
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
      assertEquals("rxjava-2", mockSpan.tags().get(Tags.COMPONENT.getKey()));
      assertEquals(0, mockSpan.generatedErrors().size());
    }
  }
}
