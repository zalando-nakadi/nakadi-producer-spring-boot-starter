package org.zalando.nakadiproducer.flowid;

public interface FlowIdComponent {
    String getXFlowIdValue();

    void startTraceIfNoneExists();
}
