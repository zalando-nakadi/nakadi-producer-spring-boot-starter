package org.zalando.nakadiproducer.opentracing;

import java.io.Closeable;
import java.util.Map;

/**
 * This interface provides an abstraction layer to an OpenTracing implementation (or a noop mock thereof).
 */
public interface OpenTracingComponent {
    interface SpanAndScope extends Closeable {
        @Override
        void close();
        public Map<String, String> exportSpanContext();
    }
    public SpanAndScope startActiveSpan(String name);
}
