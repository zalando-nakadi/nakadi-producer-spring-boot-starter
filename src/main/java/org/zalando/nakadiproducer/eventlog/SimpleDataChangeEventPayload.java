package org.zalando.nakadiproducer.eventlog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;

@Setter
@Builder
@AllArgsConstructor
public class SimpleDataChangeEventPayload implements DataChangeEventPayload {
    private String dataType;

    private Object data;

    @Override
    public String getDataType() {
        return dataType;
    }

    @Override
    public Object getData() {
        return data;
    }
}