package org.zalando.nakadiproducer.eventlog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;

@Setter
@Builder
@AllArgsConstructor
public class BusinessEventPayload implements EventPayload {
    private Object data;

    @Override
    public String getDataType() {
        return null;
    }

    @Override
    public Object getData() {
        return data;
    }
}
