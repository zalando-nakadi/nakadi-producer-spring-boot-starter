package org.zalando.nakadiproducer.snapshots.impl;

import static java.util.Collections.unmodifiableSet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.zalando.nakadiproducer.eventlog.EventLogWriter;
import org.zalando.nakadiproducer.snapshots.Snapshot;
import org.zalando.nakadiproducer.snapshots.SnapshotEventGenerator;
import org.zalando.nakadiproducer.snapshots.UnknownEventTypeException;

public class SnapshotCreationService {

    private final Map<String, SnapshotEventGenerator> snapshotEventProviders;

    private final EventLogWriter eventLogWriter;

    public SnapshotCreationService(List<SnapshotEventGenerator> snapshotEventGenerators, EventLogWriter eventLogWriter) {
        this.snapshotEventProviders = snapshotEventGenerators.stream()
                                                             .collect(
                                                                toMap(
                                                                    SnapshotEventGenerator::getSupportedEventType,
                                                                    identity()
                                                                )
                                                            );
        this.eventLogWriter = eventLogWriter;
    }

    public void createSnapshotEvents(final String eventType) {
        SnapshotEventGenerator snapshotEventGenerator = snapshotEventProviders.get(eventType);
        if (snapshotEventGenerator == null) {
            throw new UnknownEventTypeException(eventType);
        }

        Object lastProcessedId = null;
        do {
            List<Snapshot> snapshots = snapshotEventGenerator.generateSnapshots(lastProcessedId);
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
