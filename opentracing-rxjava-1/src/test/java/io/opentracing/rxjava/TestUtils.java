package io.opentracing.rxjava;

import static io.opentracing.rxjava.AbstractTracingSubscriber.COMPONENT_NAME;
import static org.junit.Assert.assertEquals;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

class TestUtils {

  static void checkSpans(List<MockSpan> mockSpans, long traceId) {
    for (MockSpan mockSpan : mockSpans) {
      assertEquals(COMPONENT_NAME, mockSpan.tags().get(Tags.COMPONENT.getKey()));
      assertEquals(0, mockSpan.generatedErrors().size());
      assertEquals(traceId, mockSpan.context().traceId());
    }
  }

  static void sleep() {
    try {
      TimeUnit.MILLISECONDS.sleep(200L);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  static Callable<Integer> reportedSpansSize(final MockTracer mockTracer) {
    return new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        return mockTracer.finishedSpans().size();
      }
    };
  }

  static Observable<Integer> createSequentialObservable() {
    return Observable.range(1, 10)
        .map(new Func1<Integer, Integer>() {
          @Override
          public Integer call(Integer integer) {
            return integer * 3;
          }
        });
  }

  static Observable<Integer> createParallelObservable() {
    return Observable.range(1, 10)
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.computation())
        .map(new Func1<Integer, Integer>() {
          @Override
          public Integer call(Integer integer) {
            sleep();
            return integer * 3;
          }
        }).filter(new Func1<Integer, Boolean>() {
          @Override
          public Boolean call(Integer integer) {
            sleep();
            return integer % 2 == 0;
          }
        });
  }

  static Observable<Long> fromInterval() {
    return Observable.interval(500, TimeUnit.MILLISECONDS, Schedulers.computation())
        .map(new Func1<Long, Long>() {
          @Override
          public Long call(Long value) {
            return value * 2;
          }
        })
        .subscribeOn(Schedulers.io())
        .take(5);
  }
}
