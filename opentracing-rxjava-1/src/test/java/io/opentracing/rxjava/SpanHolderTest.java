package io.opentracing.rxjava;


import static com.jayway.awaitility.Awaitility.await;
import static io.opentracing.rxjava.TestUtils.reportedSpansSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.opentracing.Scope;
import io.opentracing.Scope.Observer;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalScopeManager;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class SpanHolderTest {

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
  public void traced() throws InterruptedException {

    Observable<Integer> ob = Observable.range(1, 10)
        .observeOn(Schedulers.io())
        .subscribeOn(Schedulers.computation())
        .map(new Func1<Integer, Integer>() {
          @Override
          public Integer call(Integer integer) {
            //System.out.println("map: " + Thread.currentThread().getName());
            assertNotNull(mockTracer.scopeManager().active());
            mockTracer.scopeManager().active().span().setTag(String.valueOf(integer), integer);
            return integer * 2;

          }
        })
        .observeOn(Schedulers.computation())
        .filter(new Func1<Integer, Boolean>() {
          @Override
          public Boolean call(Integer integer) {
            //System.out.println("filter: " + Thread.currentThread().getName());
            return integer % 2 == 0;
          }
        });

    Action1<Integer> action1 = new Action1<Integer>() {
      @Override
      public void call(Integer integer) {
        assertNotNull(mockTracer.scopeManager().active());
        System.out.println(integer);
      }
    };

    ob.subscribe(new TracingActionSubscriber<>(action1, "test", mockTracer));
    ob.subscribe(new TracingActionSubscriber<>(action1, "test2", mockTracer));

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(mockTracer), equalTo(2));

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(2, spans.size());

    assertEquals(10, spans.get(0).tags().get(String.valueOf(10)));
    assertEquals(10, spans.get(1).tags().get(String.valueOf(10)));

    assertNull(mockTracer.scopeManager().active());
  }

  @Test
  public void not_traced() throws Exception {

    final CountDownLatch latch = new CountDownLatch(10);

    Observable<Integer> ob = Observable.range(1, 10)
        .observeOn(Schedulers.io())
        .subscribeOn(Schedulers.computation())
        .map(new Func1<Integer, Integer>() {
          @Override
          public Integer call(Integer integer) {
            assertNull(mockTracer.scopeManager().active());
            return integer * 2;
          }
        })
        .filter(new Func1<Integer, Boolean>() {
          @Override
          public Boolean call(Integer integer) {
            assertNull(mockTracer.scopeManager().active());
            latch.countDown();
            return integer % 2 == 0;

          }
        });

    Action1<Integer> action1 = new Action1<Integer>() {
      @Override
      public void call(Integer integer) {
        assertNull(mockTracer.scopeManager().active());
        System.out.println(integer);
      }
    };

    ob.subscribe(action1);
    latch.await(10, TimeUnit.SECONDS);

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(0, spans.size());

    assertNull(mockTracer.scopeManager().active());
  }

  @Test
  public void not_traced_with_parent() throws Exception {

    final CountDownLatch latch = new CountDownLatch(10);

    Scope scope = mockTracer.buildSpan("parent").startActive(Observer.FINISH_ON_CLOSE);

    Observable<Integer> ob = Observable.range(1, 10)
        .observeOn(Schedulers.io())
        .subscribeOn(Schedulers.computation())
        .map(new Func1<Integer, Integer>() {
          @Override
          public Integer call(Integer integer) {
            assertNotNull(mockTracer.scopeManager().active());
            return integer * 2;
          }
        })
        .filter(new Func1<Integer, Boolean>() {
          @Override
          public Boolean call(Integer integer) {
            latch.countDown();
            assertNotNull(mockTracer.scopeManager().active());
            return integer % 2 == 0;
          }
        });

    Action1<Integer> action1 = new Action1<Integer>() {
      @Override
      public void call(Integer integer) {
        assertNotNull(mockTracer.scopeManager().active());
        System.out.println(integer);
      }
    };

    ob.subscribe(action1);
    latch.await(10, TimeUnit.SECONDS);
    scope.close();

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(1, spans.size());

    assertNull(mockTracer.scopeManager().active());
  }

  @Test
  public void trace_only_observable() throws Exception {
    final CountDownLatch latch = new CountDownLatch(10);

    final SpanHolder spanHolder = new SpanHolder(mockTracer);

    Observable<Integer> ob = Observable.range(1, 10)
        .observeOn(Schedulers.io())
        .subscribeOn(Schedulers.computation())
        .map(new Func1<Integer, Integer>() {
          @Override
          public Integer call(Integer integer) {
            try (Scope scope = spanHolder.activate("map")) {
              assertNotNull(mockTracer.scopeManager().active());
              return integer * 2;
            }
          }
        })
        .filter(new Func1<Integer, Boolean>() {
          @Override
          public Boolean call(Integer integer) {
            assertNull(mockTracer.scopeManager().active());
            latch.countDown();
            return integer % 2 == 0;

          }
        });

    Action1<Integer> action1 = new Action1<Integer>() {
      @Override
      public void call(Integer integer) {
        assertNull(mockTracer.scopeManager().active());
        System.out.println(integer);
      }
    };

    ob.subscribe(action1);
    latch.await(10, TimeUnit.SECONDS);

    List<MockSpan> spans = mockTracer.finishedSpans();

    assertEquals(10, spans.size());

    assertNull(mockTracer.scopeManager().active());
  }

  @Test
  public void trace_only_observable_with_parent() throws Exception {
    Scope scope = mockTracer.buildSpan("parent").startActive(Observer.FINISH_ON_CLOSE);
    final SpanHolder spanHolder = new SpanHolder(mockTracer);

    Observable<Integer> ob = Observable.range(1, 10)
        .observeOn(Schedulers.io())
        .subscribeOn(Schedulers.computation())
        .map(new Func1<Integer, Integer>() {
          @Override
          public Integer call(Integer integer) {
            try (Scope scope = spanHolder.activate("map")) {
              assertNotNull(mockTracer.scopeManager().active());
              return integer * 2;
            }
          }
        })
        .filter(new Func1<Integer, Boolean>() {
          @Override
          public Boolean call(Integer integer) {
            return integer % 2 == 0;
          }
        });

    Action1<Integer> action1 = new Action1<Integer>() {
      @Override
      public void call(Integer integer) {
        //System.out.println("call: " + Thread.currentThread().getName());
        assertNotNull(mockTracer.scopeManager().active());
        MockSpan mockSpan = (MockSpan) mockTracer.scopeManager().active().span();
        // here parent

        System.out.println(integer);
      }
    };

    ob.subscribe(action1);
    scope.close();

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(mockTracer), equalTo(11));
    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(11, spans.size());

    for (int i = 1; i < spans.size(); i++) {
      assertEquals(spans.get(0).context().traceId(), spans.get(i).context().traceId());
    }

    assertNull(mockTracer.scopeManager().active());
  }
}
