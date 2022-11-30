package org.zalando.nakadiproducer.eventlog.impl;

import static java.util.stream.Collectors.toList;
import static org.zalando.nakadiproducer.eventlog.impl.EventDataOperation.CREATE;
import static org.zalando.nakadiproducer.eventlog.impl.EventDataOperation.DELETE;
import static org.zalando.nakadiproducer.eventlog.impl.EventDataOperation.SNAPSHOT;
import static org.zalando.nakadiproducer.eventlog.impl.EventDataOperation.UPDATE;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import org.zalando.nakadiproducer.eventlog.CompactionKeyExtractor;
import org.zalando.nakadiproducer.flowid.FlowIdComponent;
import org.zalando.nakadiproducer.eventlog.EventLogWriter;

import javax.transaction.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EventLogWriterImpl implements EventLogWriter {

    private final EventLogRepository eventLogRepository;

    private final ObjectMapper objectMapper;
    private final FlowIdComponent flowIdComponent;

    private final Map<String, CompactionKeyExtractor<Object>> extractors;

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
        CompactionKeyExtractor<Object> extractor = getExtractorFor(eventType);
        return eventPayloads.stream()
                .map(payload -> createEventLog(eventType, payload, extractor.getCompactionKeyFor(payload)))
                .collect(toList());
    }

    private Collection<EventLog> createDataEventLogs(
            final String eventType,
            final EventDataOperation eventDataOperation,
            final String dataType,
            final Collection<?> data
    ) {
        CompactionKeyExtractor<Object> extractor = getExtractorFor(eventType);
        return data.stream()
                .map(payload -> createEventLog(
                                    eventType,
                                    new DataChangeEventEnvelope(eventDataOperation.toString(), dataType, payload),
                                    extractor.getCompactionKeyFor(payload)))
                .collect(toList());
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

        eventLog.setCompactionKey(compactionKey);
        eventLog.setFlowId(flowIdComponent.getXFlowIdValue());
        return eventLog;
    }

    private String getCompactionKeyFor(String eventType, Object payload) {
        return getExtractorFor(eventType).getCompactionKeyFor(payload);
    }

    private CompactionKeyExtractor<Object> getExtractorFor(String eventType) {
        return extractors.getOrDefault(eventType, NOOP_EXTRACTOR);
    }

    /**
     * A key extractor which always returns null. (This is used as the terminator in a list,
     * and the default value when there is no extractor.)
     */
    private static final CompactionKeyExtractor<Object> NOOP_EXTRACTOR = (o -> null);
    /**
     * This is a linked list of extractors (with type information), all for the same event type
     * (but possibly different Java types).
     *
     * @param <X> the type of objects for which the head of the list can identify the compaction key.
     */
    @AllArgsConstructor
    private static class TypedCompactionExtractorWrapper<X> implements CompactionKeyExtractor<Object> {
        final CompactionKeyExtractor<X> extractor;
        final Class<X> type;
        final CompactionKeyExtractor<Object> next;

        @Override
        public String getCompactionKeyFor(Object o) {
            if (type.isInstance(o)) {
                return extractor.getCompactionKeyFor(type.cast(o));
            } else {
                return next.getCompactionKeyFor(o);
            }
        }
    }

    @Override
    public <X> void registerCompactionKeyExtractor(String eventType, Class<X> dataType, CompactionKeyExtractor<X> extractor) {
        extractors.put(eventType, new TypedCompactionExtractorWrapper<X>(extractor, dataType, getExtractorFor(eventType)));
    }
}
