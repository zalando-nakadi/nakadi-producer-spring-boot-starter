package de.zalando.wholesale.tarbelaevents.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zalando.tracer.Tracer;

@Component
public class FlowIdComponent {
    
    private static final String X_FLOW_ID = "X-Flow-ID";
    
    private final Tracer tracer;
    
    @Autowired
    public FlowIdComponent(Tracer tracer) {
        this.tracer = tracer;
    }

    public String getXFlowIdKey() {
        return X_FLOW_ID;
    }

    public String getXFlowIdValue() {
        return tracer.get(X_FLOW_ID).getValue();
    }
}
