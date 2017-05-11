package org.zalando.nakadiproducer.eventlog.impl;

import org.zalando.nakadiproducer.flowid.FlowIdComponent;
import org.zalando.nakadiproducer.eventlog.EventLogWriter;
import org.zalando.nakadiproducer.eventlog.DataChangeEventPayload;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EventLogWriterImpl implements EventLogWriter {

    private final EventLogRepository eventLogRepository;

    private final ObjectMapper objectMapper;
    private final FlowIdComponent flowIdComponent;

    @Autowired
    public EventLogWriterImpl(EventLogRepository eventLogRepository, ObjectMapper objectMapper, FlowIdComponent flowIdComponent) {
        this.eventLogRepository = eventLogRepository;
        this.objectMapper = objectMapper;
        this.flowIdComponent = flowIdComponent;
    }

    @Override
    @Transactional
    public void fireCreateEvent(final String eventType, final DataChangeEventPayload payload) {
        final EventLog eventLog = createEventLog(eventType, EventDataOperation.CREATE, payload);
        eventLogRepository.save(eventLog);
    }

    @Override
    @Transactional
    public void fireUpdateEvent(final String eventType, final DataChangeEventPayload payload) {
        final EventLog eventLog = createEventLog(eventType, EventDataOperation.UPDATE, payload);
        eventLogRepository.save(eventLog);
    }

    @Override
    @Transactional
    public void fireDeleteEvent(final String eventType, final DataChangeEventPayload payload) {
        final EventLog eventLog = createEventLog(eventType, EventDataOperation.DELETE, payload);
        eventLogRepository.save(eventLog);
    }

    @Override
    @Transactional
    public void fireSnapshotEvent(final String eventType, final DataChangeEventPayload payload) {
        final EventLog eventLog = createEventLog(eventType, EventDataOperation.SNAPSHOT, payload);
        eventLogRepository.save(eventLog);
    }

    @Override
    @Transactional
    public void fireBusinessEvent(final String eventType, Object payload) {
        final EventLog eventLog = createBusinessEventLog(eventType, payload);
        eventLogRepository.save(eventLog);
    }

    private EventLog createEventLog(final String eventType, final EventDataOperation dataOp, final DataChangeEventPayload eventPayload) {
        final EventLog eventLog = new EventLog();
        eventLog.setEventType(eventType);
        try {
            eventLog.setEventBodyData(objectMapper.writeValueAsString(eventPayload.getData()));
        } catch (final JsonProcessingException e) {
            throw new IllegalStateException("could not map object to json: " + eventPayload.getData().toString(), e);
        }

        eventLog.setDataOp(dataOp != null ? dataOp.toString() : null);
        eventLog.setDataType(eventPayload.getDataType());
        eventLog.setFlowId(flowIdComponent.getXFlowIdValue());
        return eventLog;
    }

    private EventLog createBusinessEventLog(final String eventType, final Object eventPayload) {
        final EventLog eventLog = new EventLog();
        eventLog.setEventType(eventType);
        try {
            eventLog.setEventBodyData(objectMapper.writeValueAsString(eventPayload));
        } catch (final JsonProcessingException e) {
            throw new IllegalStateException("could not map object to json: " + eventPayload.toString(), e);
        }

        eventLog.setDataOp(null);
        eventLog.setDataType(null);
        eventLog.setFlowId(flowIdComponent.getXFlowIdValue());
        return eventLog;
    }

}
