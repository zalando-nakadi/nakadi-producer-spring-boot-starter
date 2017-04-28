package org.zalando.nakadiproducer.service;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.UUID;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zalando.nakadiproducer.persistence.entity.EventDataOperation;
import org.zalando.nakadiproducer.persistence.entity.EventLog;
import org.zalando.nakadiproducer.persistence.entity.EventStatus;
import org.zalando.nakadiproducer.service.model.EventPayload;
import org.zalando.nakadiproducer.service.model.NakadiEvent;
import org.zalando.nakadiproducer.service.model.NakadiMetadata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

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



    public NakadiEvent mapToNakadiPayload(final EventLog event) {

        final NakadiEvent nakadiEvent = new NakadiEvent();

        final NakadiMetadata metadata = new NakadiMetadata();
        metadata.setEid(convertToUUID(event.getId()));
        metadata.setOccuredAt(event.getCreated());
        nakadiEvent.setMetadata(metadata);

        nakadiEvent.setDataOperation(event.getDataOp());
        nakadiEvent.setDataType(event.getDataType());

        HashMap<String, Object> payloadDTO;
        try {
            payloadDTO = objectMapper.readValue(event.getEventBodyData(), Maps.newLinkedHashMap().getClass());
        } catch (IOException e) {
            log.error("An error occurred at JSON deserialization", e);
            throw new UncheckedIOException(e);
        }

        nakadiEvent.setData(payloadDTO);

        return nakadiEvent;
    }



    /**
     * Converts a number in UUID format.
     *
     * <p>For instance 213 will be converted to "00000000-0000-0000-0000-0000000000d5"</p>
     */
    private String convertToUUID(final int number) {
        return new UUID(0, number).toString();
    }

}
