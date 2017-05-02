package org.zalando.nakadiproducer.snapshots.impl;

import java.util.stream.Stream;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zalando.nakadiproducer.NakadiProperties;
import org.zalando.nakadiproducer.eventlog.EventPayload;
import org.zalando.nakadiproducer.eventlog.impl.EventLogWriterImpl;
import org.zalando.nakadiproducer.snapshots.SnapshotEventProvider;

import com.google.common.collect.Iterators;

@Service
public class SnapshotCreationService {

    private final NakadiProperties nakadiProperties;

    private final SnapshotEventProvider snapshotEventProvider;

    private final EventLogWriterImpl eventLogWriter;

    @Autowired
    public SnapshotCreationService(NakadiProperties nakadiProperties, SnapshotEventProvider snapshotEventProvider, EventLogWriterImpl eventLogWriter) {
        this.nakadiProperties = nakadiProperties;
        this.snapshotEventProvider = snapshotEventProvider;
        this.eventLogWriter = eventLogWriter;
    }

    @Transactional
    public void createSnapshotEvents(final String eventType, final String flowId) {

        Stream<EventPayload> snapshotItemsStream = snapshotEventProvider.getSnapshot(eventType);

        Iterators.partition(snapshotItemsStream.iterator(), nakadiProperties.getSnapshotBatchSize())
                 .forEachRemaining(batch -> batch.forEach((item) -> eventLogWriter.fireSnapshotEvent(item, flowId)));
    }
}
