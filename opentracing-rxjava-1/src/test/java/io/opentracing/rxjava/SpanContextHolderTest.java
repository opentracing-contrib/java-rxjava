package io.opentracing.rxjava;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.opentracing.SpanContext;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockSpan.MockContext;
import io.opentracing.mock.MockTracer;
import org.junit.Test;

public class SpanContextHolderTest {

  @Test
  public void test() {
    assertNull(SpanContextHolder.get());
    SpanContextHolder.clear();

    MockTracer tracer = new MockTracer();
    MockSpan span = tracer.buildSpan("test").startManual();

    // Set span
    SpanContextHolder.set(span.context());
    assertNotNull(SpanContextHolder.get());
    SpanContext spanContextFromHolder = SpanContextHolder.get();
    assertNotNull(spanContextFromHolder);
    assertEquals(span.context().spanId(), ((MockContext) spanContextFromHolder).spanId());

    // Clear
    SpanContextHolder.clear();
    assertNull(SpanContextHolder.get());
  }

}