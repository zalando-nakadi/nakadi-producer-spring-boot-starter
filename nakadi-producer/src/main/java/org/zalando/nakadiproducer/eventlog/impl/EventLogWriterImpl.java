package org.zalando.nakadiproducer.eventlog.impl;

import static org.zalando.nakadiproducer.eventlog.impl.EventDataOperation.CREATE;
import static org.zalando.nakadiproducer.eventlog.impl.EventDataOperation.DELETE;
import static org.zalando.nakadiproducer.eventlog.impl.EventDataOperation.SNAPSHOT;
import static org.zalando.nakadiproducer.eventlog.impl.EventDataOperation.UPDATE;

import org.zalando.nakadiproducer.flowid.FlowIdComponent;
import org.zalando.nakadiproducer.opentracing.OpenTracingComponent;
import org.zalando.nakadiproducer.opentracing.OpenTracingComponent.SpanAndScope;
import org.zalando.nakadiproducer.eventlog.EventLogWriter;

import javax.transaction.Transactional;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventLogWriterImpl implements EventLogWriter {

    private final EventLogRepository eventLogRepository;

    private final ObjectMapper objectMapper;
    private final FlowIdComponent flowIdComponent;
    private final OpenTracingComponent openTracingComponent;

    public EventLogWriterImpl(EventLogRepository eventLogRepository, ObjectMapper objectMapper, FlowIdComponent flowIdComponent, OpenTracingComponent openTracingComponent) {
        this.eventLogRepository = eventLogRepository;
        this.objectMapper = objectMapper;
        this.flowIdComponent = flowIdComponent;
        this.openTracingComponent = openTracingComponent;
    }

    @Override
    @Transactional
    public void fireCreateEvent(final String eventType, final String dataType, final Object data) {
        try (SpanAndScope span = openTracingComponent.startActiveSpan("fire_create_event")) {
            final EventLog eventLog = createEventLog(eventType, new DataChangeEventEnvelope(CREATE.toString(), dataType, data), span);
            eventLogRepository.persist(eventLog);
        }
    }

    @Override
    @Transactional
    public void fireUpdateEvent(final String eventType, final String dataType, final Object data) {
        try (SpanAndScope span = openTracingComponent.startActiveSpan("fire_update_event")) {
            final EventLog eventLog = createEventLog(eventType,
                    new DataChangeEventEnvelope(UPDATE.toString(), dataType, data), span);
            eventLogRepository.persist(eventLog);
        }
    }

    @Override
    @Transactional
    public void fireDeleteEvent(final String eventType, final String dataType, final Object data) {
        try (SpanAndScope span = openTracingComponent.startActiveSpan("fire_delete_event")) {
            final EventLog eventLog = createEventLog(eventType,
                    new DataChangeEventEnvelope(DELETE.toString(), dataType, data), span);
            eventLogRepository.persist(eventLog);
        }
    }

    @Override
    @Transactional
    public void fireSnapshotEvent(final String eventType, final String dataType, final Object data) {
        try (SpanAndScope span = openTracingComponent.startActiveSpan("fire_snapshot_event")) {
            final EventLog eventLog = createEventLog(eventType,
                    new DataChangeEventEnvelope(SNAPSHOT.toString(), dataType, data), span);
            eventLogRepository.persist(eventLog);
        }
    }

    @Override
    @Transactional
    public void fireBusinessEvent(final String eventType, Object payload) {
        try (SpanAndScope span = openTracingComponent.startActiveSpan("fire_business_event")) {
            final EventLog eventLog = createEventLog(eventType, payload, span);
            eventLogRepository.persist(eventLog);
        }
    }

    private EventLog createEventLog(final String eventType, final Object eventPayload, SpanAndScope span) {
        final EventLog eventLog = new EventLog();
        eventLog.setEventType(eventType);

        try {
            eventLog.setEventBodyData(objectMapper.writeValueAsString(eventPayload));
        } catch (final JsonProcessingException e) {
            throw new IllegalStateException("could not map object to json: " + eventPayload.toString(), e);
        }

        eventLog.setSpanContext(serializeSpanContext(span));
        eventLog.setFlowId(flowIdComponent.getXFlowIdValue());
        return eventLog;
    }

    private String serializeSpanContext(SpanAndScope span) {
        Map<String, String> context = span.exportSpanContext();
        String contextString;
        try {
            contextString = objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            log.error("Could not serialize span context {}", context);
            contextString = null;
        }
        return contextString;
    }

}
