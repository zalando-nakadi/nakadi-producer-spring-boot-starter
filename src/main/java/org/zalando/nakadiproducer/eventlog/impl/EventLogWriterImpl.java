package org.zalando.nakadiproducer.eventlog.impl;

import org.zalando.nakadiproducer.flowid.FlowIdComponent;
import org.zalando.nakadiproducer.eventlog.EventLogWriter;
import org.zalando.nakadiproducer.eventlog.EventPayload;

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
    public void fireCreateEvent(final EventPayload payload) {
        final EventLog eventLog = createEventLog(EventDataOperation.CREATE, payload);
        eventLogRepository.save(eventLog);
    }

    @Override
    @Transactional
    public void fireUpdateEvent(final EventPayload payload) {
        final EventLog eventLog = createEventLog(EventDataOperation.UPDATE, payload);
        eventLogRepository.save(eventLog);
    }

    @Override
    @Transactional
    public void fireDeleteEvent(final EventPayload payload) {
        final EventLog eventLog = createEventLog(EventDataOperation.DELETE, payload);
        eventLogRepository.save(eventLog);
    }

    @Override
    @Transactional
    public void fireSnapshotEvent(final EventPayload payload) {
        final EventLog eventLog = createEventLog(EventDataOperation.SNAPSHOT, payload);
        eventLogRepository.save(eventLog);
    }

    @Override
    public void fireBusinessEvent(EventPayload payload) {
        // data_op doesn't make sense for business events, so just nulify it
        final EventLog eventLog = createEventLog(null, payload);
        eventLogRepository.save(eventLog);
    }

    private EventLog createEventLog(final EventDataOperation dataOp, final EventPayload eventPayload) {
        final EventLog eventLog = new EventLog();
        eventLog.setEventType(eventPayload.getEventType());
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

}
