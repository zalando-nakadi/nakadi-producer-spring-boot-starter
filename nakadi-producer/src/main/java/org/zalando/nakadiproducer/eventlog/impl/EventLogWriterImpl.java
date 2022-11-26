package org.zalando.nakadiproducer.eventlog.impl;

import static java.util.stream.Collectors.toList;
import static org.zalando.nakadiproducer.eventlog.impl.EventDataOperation.CREATE;
import static org.zalando.nakadiproducer.eventlog.impl.EventDataOperation.DELETE;
import static org.zalando.nakadiproducer.eventlog.impl.EventDataOperation.SNAPSHOT;
import static org.zalando.nakadiproducer.eventlog.impl.EventDataOperation.UPDATE;

import java.util.Collection;

import lombok.AllArgsConstructor;
import org.zalando.nakadiproducer.eventlog.CompactionKeyExtractor;
import org.zalando.nakadiproducer.flowid.FlowIdComponent;
import org.zalando.nakadiproducer.eventlog.EventLogWriter;

import javax.transaction.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class EventLogWriterImpl implements EventLogWriter {

    private final EventLogRepository eventLogRepository;

    private final ObjectMapper objectMapper;
    private final FlowIdComponent flowIdComponent;

    private final Map<String, CompactionExtractorWrapper<?>> extractors;

    public EventLogWriterImpl(EventLogRepository eventLogRepository, ObjectMapper objectMapper, FlowIdComponent flowIdComponent) {
        this.eventLogRepository = eventLogRepository;
        this.objectMapper = objectMapper;
        this.flowIdComponent = flowIdComponent;
        this.extractors = new HashMap<>();
    }

    @Override
    @Transactional
    public void fireCreateEvent(final String eventType, final String dataType, final Object data) {
        final EventLog eventLog = createDataEventLog(eventType, CREATE, dataType, data);
        eventLogRepository.persist(eventLog);
    }

    @Override
    @Transactional
    public void fireCreateEvents(final String eventType, final String dataType, final Collection<?> data) {
        eventLogRepository.persist(createDataEventLogs(eventType, CREATE, dataType, data));
    }

    @Override
    @Transactional
    public void fireUpdateEvent(final String eventType, final String dataType, final Object data) {
        final EventLog eventLog = createDataEventLog(eventType, UPDATE, dataType, data);
        eventLogRepository.persist(eventLog);
    }

    @Override
    @Transactional
    public void fireUpdateEvents(final String eventType, final String dataType, final Collection<?> data) {
        eventLogRepository.persist(createDataEventLogs(eventType, UPDATE, dataType, data));
    }

    @Override
    @Transactional
    public void fireDeleteEvent(final String eventType, final String dataType, final Object data) {
        final EventLog eventLog = createDataEventLog(eventType, DELETE, dataType, data);
        eventLogRepository.persist(eventLog);
    }

    @Override
    @Transactional
    public void fireDeleteEvents(final String eventType, final String dataType, final Collection<?> data) {
        eventLogRepository.persist(createDataEventLogs(eventType, DELETE, dataType, data));
    }

    @Override
    @Transactional
    public void fireSnapshotEvent(final String eventType, final String dataType, final Object data) {
        final EventLog eventLog = createDataEventLog(eventType, SNAPSHOT, dataType, data);
        eventLogRepository.persist(eventLog);
    }

    @Override
    @Transactional
    public void fireSnapshotEvents(final String eventType, final String dataType, final Collection<?> data) {
        eventLogRepository.persist(createDataEventLogs(eventType, SNAPSHOT, dataType, data));
    }

    @Override
    @Transactional
    public void fireBusinessEvent(final String eventType, Object payload) {
        final EventLog eventLog = createEventLog(eventType, payload, getCompactionKeyFor(eventType, payload));
        eventLogRepository.persist(eventLog);
    }

    @Override
    @Transactional
    public void fireBusinessEvents(final String eventType, final Collection<Object> payload) {
        final Collection<EventLog> eventLogs = createBusinessEventLogs(eventType, payload);
        eventLogRepository.persist(eventLogs);
    }

    private Collection<EventLog> createBusinessEventLogs(final String eventType,
                                                     final Collection<Object> eventPayloads) {
        return eventPayloads.stream()
                .map(payload -> createEventLog(eventType, payload, getCompactionKeyFor(eventType, payload)))
                .collect(toList());
    }

    private String getCompactionKeyFor(String eventType, Object payload) {
        CompactionExtractorWrapper<?> extractorWrapper = extractors.get(eventType);
        if (extractorWrapper != null) {
            return extractorWrapper.extract(payload);
        } else {
            return null;
        }
    }


    private EventLog createDataEventLog(String eventType, EventDataOperation dataOp, String dataType, Object data) {
        return createEventLog(eventType, new DataChangeEventEnvelope(dataOp.toString(), dataType, data),
                getCompactionKeyFor(eventType, data));
    }

    private EventLog createEventLog(final String eventType, final Object eventPayload, String compactionKey) {
        final EventLog eventLog = new EventLog();
        eventLog.setEventType(eventType);
        try {
            eventLog.setEventBodyData(objectMapper.writeValueAsString(eventPayload));
        } catch (final JsonProcessingException e) {
            throw new IllegalStateException("could not map object to json: " + eventPayload.toString(), e);
        }

        eventLog.setFlowId(flowIdComponent.getXFlowIdValue());
        return eventLog;
    }

    private Collection<EventLog> createDataEventLogs(
            final String eventType,
            final EventDataOperation eventDataOperation,
            final String dataType,
            final Collection<?> data
    ) {
        return data.stream()
                .map(payload -> createEventLog(
                                    eventType,
                                    new DataChangeEventEnvelope(eventDataOperation.toString(), dataType, payload),
                                    getCompactionKeyFor(eventType, payload)))
                .collect(toList());
    }

    /**
     * This is a linked list of extractors (with type information), all for the same event type.
     *
     * @param <X>
     */
    @AllArgsConstructor
    private static class CompactionExtractorWrapper<X> {
        CompactionKeyExtractor<X> extractor;
        Class<X> type;
        CompactionExtractorWrapper<?> next;

        String extract(Object o) {
            if (o == null) {
                return null;
            } else if (type.isInstance(o)) {
                return extractor.getCompactionKeyFor(type.cast(o));
            } else if (next != null) {
                return next.extract(o);
            }
            return null;
        }
    }

    @Override
    public <X> void registerCompactionKeyExtractor(String eventType, Class<X> dataType, CompactionKeyExtractor<X> extractor) {
        CompactionExtractorWrapper<?> next = extractors.get(eventType);
        extractors.put(eventType, new CompactionExtractorWrapper<X>(extractor, dataType, next));
    }

}
