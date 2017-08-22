package io.opentracing.rxjava2;

import io.opentracing.Span;
import java.util.ArrayList;
import java.util.List;

class SpanStackHolder {

  private static final SpanStackHolder holder = new SpanStackHolder();

  private final ThreadLocal<List<Span>> spans = new ThreadLocal<>();

  static Span get() {
    List<Span> spans = holder.spans.get();
    return spans == null || spans.isEmpty() ? null : spans.get(spans.size() - 1);
  }

  static Span remove() {
    List<Span> spans = holder.spans.get();
    if (spans == null) {
      return null;
    } else if (spans.isEmpty()) {
      holder.spans.remove();
      return null;
    } else {
      Span span = spans.remove(spans.size() - 1);
      if (spans.isEmpty()) {
        holder.spans.remove();
      }
      return span;
    }
  }

  static void add(Span span) {
    if (holder.spans.get() == null) {
      holder.spans.set(new ArrayList<Span>());
    }
    holder.spans.get().add(span);
  }
}
