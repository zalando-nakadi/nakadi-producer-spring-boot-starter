package org.zalando.nakadiproducer.opentracing;

import io.opentracing.Scope;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapInjectAdapter;

import java.util.HashMap;
import java.util.Map;

public class TracerOpenTracingComponent implements OpenTracingComponent {

    private class SpanAndScopeImpl implements SpanAndScope {

        private Scope scope;

        public SpanAndScopeImpl(Scope scope) {
            this.scope = scope;
        }

        @Override
        public void close() {
            scope.close();
        }

        @Override
        public Map<String, String> exportSpanContext() {
            Map<String, String> map = new HashMap<>();
            SpanContext context = scope.span().context();
            tracer.inject(context, Format.Builtin.TEXT_MAP, new TextMapInjectAdapter(map));
            return map;
        }
    }

    private Tracer tracer;

    public TracerOpenTracingComponent(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public SpanAndScope startActiveSpan(String name) {
        return new SpanAndScopeImpl(tracer.buildSpan(name).startActive(true));
    }



}
