package org.zalando.nakadiproducer.eventlog.impl;

import org.zalando.nakadiproducer.eventlog.EventLogWriter;
import org.zalando.nakadiproducer.eventlog.EventPayload;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.transaction.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EventLogWriterImpl implements EventLogWriter {

    private final EventLogRepository eventLogRepository;

    private final ObjectMapper objectMapper;

    @Autowired
    public EventLogWriterImpl(EventLogRepository eventLogRepository, ObjectMapper objectMapper) {
        this.eventLogRepository = eventLogRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void fireCreateEvent(final EventPayload payload, @Nullable final String flowId) {
        final EventLog eventLog = createEventLog(EventDataOperation.CREATE, payload, flowId);
        eventLogRepository.save(eventLog);
    }

    @Override
    @Transactional
    public void fireUpdateEvent(final EventPayload payload, @Nullable final String flowId) {
        final EventLog eventLog = createEventLog(EventDataOperation.UPDATE, payload, flowId);
        eventLogRepository.save(eventLog);
    }

    @Override
    @Transactional
    public void fireDeleteEvent(final EventPayload payload, @Nullable final String flowId) {
        final EventLog eventLog = createEventLog(EventDataOperation.DELETE, payload, flowId);
        eventLogRepository.save(eventLog);
    }

    @Transactional
    public void fireSnapshotEvent(final EventPayload payload, @Nullable final String flowId) {
        final EventLog eventLog = createEventLog(EventDataOperation.SNAPSHOT, payload, flowId);
        eventLogRepository.save(eventLog);
    }

    private EventLog createEventLog(final EventDataOperation dataOp, final EventPayload eventPayload, @Nullable final String flowId) {
        final EventLog eventLog = new EventLog();
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
