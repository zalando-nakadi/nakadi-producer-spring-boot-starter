package org.zalando.nakadiproducer.snapshots.impl;

import static java.util.Collections.unmodifiableSet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.zalando.nakadiproducer.eventlog.EventLogWriter;
import org.zalando.nakadiproducer.snapshots.SnapshotEventProvider;
import org.zalando.nakadiproducer.snapshots.SnapshotEventProvider.Snapshot;
import org.zalando.nakadiproducer.snapshots.UnknownEventTypeException;

public class SnapshotCreationService {

    private final Map<String, SnapshotEventProvider> snapshotEventProviders;

    private final EventLogWriter eventLogWriter;

    public SnapshotCreationService(List<SnapshotEventProvider> snapshotEventProviders, EventLogWriter eventLogWriter) {
        this.snapshotEventProviders = snapshotEventProviders.stream()
                                                            .collect(
                                                                toMap(
                                                                    SnapshotEventProvider::getSupportedEventType,
                                                                    identity()
                                                                )
                                                            );
        this.eventLogWriter = eventLogWriter;
    }

    public void createSnapshotEvents(final String eventType) {
        SnapshotEventProvider snapshotEventProvider = snapshotEventProviders.get(eventType);
        if (snapshotEventProvider == null) {
            throw new UnknownEventTypeException(eventType);
        }

        Object lastProcessedId = null;
        do {
            List<Snapshot> snapshots = snapshotEventProvider.getSnapshot(lastProcessedId);
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
        return unmodifiableSet(snapshotEventProviders.keySet());
    }
}
