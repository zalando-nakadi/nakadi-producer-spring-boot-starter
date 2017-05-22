package org.zalando.nakadiproducer.snapshots.impl;

import java.util.List;
import java.util.Set;

import org.zalando.nakadiproducer.eventlog.EventLogWriter;
import org.zalando.nakadiproducer.snapshots.SnapshotEventProvider;
import org.zalando.nakadiproducer.snapshots.SnapshotEventProvider.Snapshot;

public class SnapshotCreationService {

    private final SnapshotEventProvider snapshotEventProvider;

    private final EventLogWriter eventLogWriter;

    public SnapshotCreationService(SnapshotEventProvider snapshotEventProvider, EventLogWriter eventLogWriter) {
        this.snapshotEventProvider = snapshotEventProvider;
        this.eventLogWriter = eventLogWriter;
    }

    public void createSnapshotEvents(final String eventType) {
        Object lastProcessedId = null;
        do {
            List<Snapshot> snapshots = snapshotEventProvider.getSnapshot(eventType, lastProcessedId);
            if (snapshots.isEmpty()) {
                break;
            }

            for (Snapshot snapshot : snapshots) {
                eventLogWriter.fireSnapshotEvent(eventType, snapshot.getDataType(), snapshot.getData());
                lastProcessedId = snapshot.getId();
            }
        } while (true);
    }

    public Set<String> getSupportedEventTypes() {
        return snapshotEventProvider.getSupportedEventTypes();
    }
}
