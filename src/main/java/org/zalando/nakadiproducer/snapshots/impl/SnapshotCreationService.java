package org.zalando.nakadiproducer.snapshots.impl;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zalando.nakadiproducer.eventlog.impl.EventLogWriterImpl;
import org.zalando.nakadiproducer.snapshots.SnapshotEventProvider;
import org.zalando.nakadiproducer.snapshots.SnapshotEventProvider.Snapshot;

@Service
public class SnapshotCreationService {

    private final SnapshotEventProvider snapshotEventProvider;

    private final EventLogWriterImpl eventLogWriter;

    @Autowired
    public SnapshotCreationService(SnapshotEventProvider snapshotEventProvider, EventLogWriterImpl eventLogWriter) {
        this.snapshotEventProvider = snapshotEventProvider;
        this.eventLogWriter = eventLogWriter;
    }

    @Transactional
    public void createSnapshotEvents(final String eventType) {
        Object lastProcessedId = null;
        do {
            List<Snapshot> snapshots = snapshotEventProvider.getSnapshot(eventType, lastProcessedId);
            if (snapshots.isEmpty()) {
                break;
            }

            for (Snapshot snapshot : snapshots) {
                eventLogWriter.fireSnapshotEvent(snapshot.getEventPayload());
                lastProcessedId = snapshot.getId();
            }
        } while (true);
    }
}
