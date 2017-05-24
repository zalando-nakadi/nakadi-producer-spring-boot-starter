package org.zalando.nakadiproducer.flowid;

import lombok.extern.slf4j.Slf4j;

import org.zalando.tracer.Tracer;

@Slf4j
public class NoopFlowIdComponent implements FlowIdComponent {
    private static final String X_FLOW_ID = "X-Flow-ID";

    @Override
    public String getXFlowIdValue() {
        log.debug("No bean of class FlowIdComponent was found. Returning null.");
        return null;
    }
}
