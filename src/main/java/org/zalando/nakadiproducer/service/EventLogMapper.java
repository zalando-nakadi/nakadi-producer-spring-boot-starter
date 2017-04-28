package org.zalando.nakadiproducer.service;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zalando.nakadiproducer.persistence.entity.EventDataOperation;
import org.zalando.nakadiproducer.persistence.entity.EventLog;
import org.zalando.nakadiproducer.persistence.entity.EventStatus;
import org.zalando.nakadiproducer.service.model.EventPayload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class EventLogMapper {

    private ObjectMapper objectMapper;

    @Autowired
    public EventLogMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EventLog createEventLog(final EventDataOperation dataOp, final EventPayload eventPayload, @Nullable final String flowId) {
        final EventLog eventLog = new EventLog();
        eventLog.setStatus(EventStatus.NEW.toString());
        eventLog.setEventType(eventPayload.getEventType());
        try {
            eventLog.setEventBodyData(objectMapper.writeValueAsString(eventPayload.getData()));
        } catch (final JsonProcessingException e) {
            throw new IllegalStateException("could not map object to json: " + eventPayload.getData().toString(), e);
        }

        eventLog.setDataOp(dataOp.toString());
        eventLog.setDataType(eventPayload.getDataType());
        eventLog.setFlowId(flowId);
        return eventLog;
    }

}
