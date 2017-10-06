package io.opentracing.rxjava2;

import static io.opentracing.rxjava2.AbstractTracingObserver.COMPONENT_NAME;
import static org.junit.Assert.assertEquals;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

class TestUtils {

  static Observable<Integer> createSequentialObservable() {
    return Observable.range(1, 10)
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
  }

  static Observable<Integer> createParallelObservable() {
    return Observable.range(1, 10)
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
  }

  private static void sleep() {
    try {
      TimeUnit.MILLISECONDS.sleep(200L);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  static void checkSpans(List<MockSpan> mockSpans) {
    for (MockSpan mockSpan : mockSpans) {
      assertEquals(COMPONENT_NAME, mockSpan.tags().get(Tags.COMPONENT.getKey()));
      assertEquals(0, mockSpan.generatedErrors().size());
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
}
