package org.zalando.nakadiproducer.opentracing;

import java.util.Collections;
import java.util.Map;

public class NoopOpenTracingComponent implements OpenTracingComponent {

    private static final SpanAndScope NOOP_SPAN_SCOPE = new SpanAndScope() {

        @Override
        public void close() {
        }

        @Override
        public Map<String, String> exportSpanContext() {
            return Collections.emptyMap();
        }
    };

    @Override
    public SpanAndScope startActiveSpan(String name) {
        return NOOP_SPAN_SCOPE;
    }


}
