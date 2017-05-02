package org.zalando.nakadiproducer.eventlog;

import lombok.Builder;
import lombok.Setter;

@Setter
@Builder
public class SimpleEventPayload implements EventPayload {

    private String eventType;

    private String dataType;

    private Object data;

    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public String getDataType() {
        return dataType;
    }

    @Override
    public Object getData() {
        return data;
    }
}
