package org.zalando.nakadiproducer.eventlog.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.zalando.nakadiproducer.eventlog.EidGeneratorStrategy;
import org.zalando.nakadiproducer.flowid.FlowIdComponent;

public class EventLogMapper {

    private final ObjectMapper objectMapper;
    private final FlowIdComponent flowIdComponent;
    private final EidGeneratorStrategy eidGeneratorStrategy;

    public EventLogMapper(ObjectMapper objectMapper, FlowIdComponent flowIdComponent,
                          EidGeneratorStrategy eidGeneratorStrategy) {
        this.objectMapper = objectMapper;
        this.flowIdComponent = flowIdComponent;
        this.eidGeneratorStrategy = eidGeneratorStrategy;
    }

    public EventLog createEventLog(String eventType, Object eventPayload, String compactionKey) {
        EventLog eventLog = new EventLog();
        eventLog.setEventType(eventType);
        eventLog.setEventBodyData(getEventBodyData(eventPayload));
        eventLog.setCompactionKey(compactionKey);
        eventLog.setFlowId(flowIdComponent.getXFlowIdValue());
        eventLog.setEid(eidGeneratorStrategy.generateEid());
        return eventLog;
    }

    private String getEventBodyData(Object eventPayload) {
        try {
            return objectMapper.writeValueAsString(eventPayload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                "could not map object to json: " + eventPayload.toString(), e
            );
        }
    }
}
