package org.zalando.nakadiproducer.service;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.singletonList;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zalando.fahrschein.NakadiClient;
import org.zalando.nakadiproducer.NakadiProperties;
import org.zalando.nakadiproducer.snapshots.SnapshotEventProvider;
import org.zalando.nakadiproducer.persistence.entity.EventDataOperation;
import org.zalando.nakadiproducer.persistence.entity.EventLog;
import org.zalando.nakadiproducer.persistence.repository.EventLogRepository;
import org.zalando.nakadiproducer.service.model.EventPayload;

import com.google.common.collect.Iterators;

@Service
@Slf4j
public class EventLogServiceImpl implements EventLogService {

    @Autowired
    private EventLogRepository eventLogRepository;

    @Autowired
    private EventLogMapper eventLogMapper;

    @Autowired
    private NakadiProperties nakadiProperties;

    @Autowired
    private SnapshotEventProvider snapshotEventProvider;

    @Autowired
    private NakadiClient nakadiClient;

    @Override
    @Transactional
    public void createSnapshotEvents(final String eventType, final String flowId) {

        Stream<EventPayload> snapshotItemsStream = snapshotEventProvider.getSnapshot(eventType);

        Iterators.partition(snapshotItemsStream.iterator(), nakadiProperties.getSnapshotBatchSize())
                .forEachRemaining(batch -> {
                    final List<EventLog> events = batch.stream()
                            .map(item -> eventLogMapper.createEventLog(EventDataOperation.SNAPSHOT, item, flowId))
                            .collect(Collectors.toList());
                    eventLogRepository.save(events);
                });

        eventLogRepository.flush();
    }

    @Override
    @Transactional
    public Collection<EventLog> lockSomeEvents() {
        String lockId = UUID.randomUUID().toString();
        log.info("Locking events for replcation with lockId {}", lockId);
        eventLogRepository.lockSomeMessages(lockId, Instant.now(), Instant.now().plus(10, MINUTES));
        return eventLogRepository.findByLockedByAndLockedUntilGreaterThan(lockId, Instant.now());
    }

    @Override
    @Transactional
    public void sendEvent(EventLog eventLog) {
        try {
            nakadiClient.publish(eventLog.getEventType(), singletonList(eventLogMapper.mapToNakadiPayload(eventLog)));
            log.info("Event {} locked by {} was sucessfully transmitted to nakadi", eventLog.getId(), eventLog.getLockedBy());
            eventLogRepository.delete(eventLog);
        } catch (IOException e) {
            log.error("Event {} locked by {} could not be transmitted to nakadi", eventLog.getId(), eventLog.getLockedBy(), e);
        }

    }

}
