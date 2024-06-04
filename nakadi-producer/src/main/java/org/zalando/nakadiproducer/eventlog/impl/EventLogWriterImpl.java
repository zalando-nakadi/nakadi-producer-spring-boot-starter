package org.zalando.nakadiproducer.eventlog.impl;

import static java.util.stream.Collectors.*;
import static org.zalando.nakadiproducer.eventlog.impl.EventDataOperation.CREATE;
import static org.zalando.nakadiproducer.eventlog.impl.EventDataOperation.DELETE;
import static org.zalando.nakadiproducer.eventlog.impl.EventDataOperation.SNAPSHOT;
import static org.zalando.nakadiproducer.eventlog.impl.EventDataOperation.UPDATE;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.zalando.fahrschein.Preconditions;
import org.zalando.nakadiproducer.eventlog.CompactionKeyExtractor;
import org.zalando.nakadiproducer.eventlog.EventLogWriter;

import javax.transaction.Transactional;

public class EventLogWriterImpl implements EventLogWriter {

    private static final CompactionKeyExtractor NOOP_EXTRACTOR =
            CompactionKeyExtractor.ofOptional("doesn't matter", o -> Optional.empty());
    private final EventLogRepository eventLogRepository;

    private final EventLogMapper eventLogMapper;

    private final Map<String, CompactionKeyExtractor> extractorsByEventType;

    public EventLogWriterImpl(EventLogRepository eventLogRepository,
                              EventLogMapper eventLogMapper,
                              List<CompactionKeyExtractor> keyExtractors) {
        this.eventLogRepository = eventLogRepository;
        this.eventLogMapper = eventLogMapper;
        this.extractorsByEventType = keyExtractors.stream()
                .collect(groupingBy(
                        CompactionKeyExtractor::getEventType,
                        collectingAndThen(toList(), EventLogWriterImpl::joinCompactors)));
    }

    /**
     * Helper function (used in the constructor) to join a (non-empty) list of compaction key extractors
     * (for the same event type) into a single one. If that list has just one element, it is returned.
     * Otherwise, a new extractor is created whose retrieval method will just iterate through all the
     * extractors, ask them for the key and returns any that is non-empty.
     *
     * @param list a list of extractors, non-empty.
     * @return a single extractor based on the list which will return a key when any of the extractors returns one.
     */
    private static CompactionKeyExtractor joinCompactors(List<CompactionKeyExtractor> list) {
        Preconditions.checkArgument(!list.isEmpty());
        if(list.size() == 1) {
            // the most common case: just one extractor per event type.
            return list.get(0);
        } else {
            return CompactionKeyExtractor.ofOptional(list.get(0).getEventType(),
                    o -> list.stream()
                            .flatMap(ex -> ex.tryGetKeyFor(o).stream())
                            .findAny()
            );
        }
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
        final EventLog eventLog = eventLogMapper.createEventLog(eventType, payload, getCompactionKeyFor(eventType, payload));
        eventLogRepository.persist(eventLog);
    }

    @Override
    @Transactional
    public void fireBusinessEvents(final String eventType, final Collection<?> payload) {
        final Collection<EventLog> eventLogs = createBusinessEventLogs(eventType, payload);
        eventLogRepository.persist(eventLogs);
    }

    private Collection<EventLog> createBusinessEventLogs(final String eventType,
                                                     final Collection<?> eventPayloads) {
        CompactionKeyExtractor extractor = getKeyExtractorFor(eventType);
        return eventPayloads.stream()
                .map(payload -> eventLogMapper.createEventLog(eventType, payload,
                        extractor.getKeyOrNull(payload)))
                .collect(toList());
    }

    private Collection<EventLog> createDataEventLogs(
            final String eventType,
            final EventDataOperation eventDataOperation,
            final String dataType,
            final Collection<?> data
    ) {
        CompactionKeyExtractor extractor = getKeyExtractorFor(eventType);
        String dataOp = eventDataOperation.toString();
        return data.stream()
                .map(payload -> eventLogMapper.createEventLog(
                        eventType,
                        new DataChangeEventEnvelope(dataOp, dataType, payload),
                        extractor.getKeyOrNull(payload)))
                .collect(toList());
    }

    private EventLog createDataEventLog(String eventType, EventDataOperation dataOp, String dataType, Object data) {
        return eventLogMapper.createEventLog(eventType,
                new DataChangeEventEnvelope(dataOp.toString(), dataType, data),
                getCompactionKeyFor(eventType, data));
    }

    private String getCompactionKeyFor(String eventType, Object payload) {
        return getKeyExtractorFor(eventType).getKeyOrNull(payload);
    }

    private CompactionKeyExtractor getKeyExtractorFor(String eventType) {
        return extractorsByEventType.getOrDefault(eventType, NOOP_EXTRACTOR);
    }
}
