package org.zalando.nakadiproducer.service;

import static java.util.Collections.singletonList;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zalando.fahrschein.NakadiClient;
import org.zalando.nakadiproducer.NakadiProperties;
import org.zalando.nakadiproducer.SnapshotEventProvider;
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
    public void sendMessages() {
        log.info("This would transmit a bunch of events");
        eventLogRepository.findAll().stream().forEach(eventLog -> {
                try {
                    nakadiClient.publish(eventLog.getEventType(), singletonList(eventLogMapper.mapToNakadiPayload(eventLog)));
                } catch (IOException e) {
                    throw new IllegalStateException();
                }
            }
         );
    }

}
