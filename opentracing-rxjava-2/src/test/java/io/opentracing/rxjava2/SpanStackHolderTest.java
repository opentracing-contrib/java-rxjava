package io.opentracing.rxjava2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.opentracing.Span;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.junit.Test;

public class SpanStackHolderTest {

  @Test
  public void test() {
    assertNull(SpanStackHolder.remove());
    assertNull(SpanStackHolder.get());

    MockTracer tracer = new MockTracer();
    MockSpan span = tracer.buildSpan("test").startManual();

    // Add span
    SpanStackHolder.add(span);
    assertNotNull(SpanStackHolder.get());
    Span spanFromHolder = SpanStackHolder.get();
    assertNotNull(spanFromHolder);
    assertEquals(span.operationName(), ((MockSpan) spanFromHolder).operationName());

    // Remove span
    spanFromHolder = SpanStackHolder.remove();
    assertNotNull(spanFromHolder);
    assertEquals(span.operationName(), ((MockSpan) spanFromHolder).operationName());

    assertNull(SpanStackHolder.get());
    assertNull(SpanStackHolder.remove());
  }
}