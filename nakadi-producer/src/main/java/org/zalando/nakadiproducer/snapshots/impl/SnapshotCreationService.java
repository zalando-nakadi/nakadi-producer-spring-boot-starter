package org.zalando.nakadiproducer.snapshots.impl;

import static java.util.Collections.unmodifiableSet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

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

    /**
     * Creates the service.
     *
     * @param snapshotEventGenerators
     *            the event generators. Each of them must have a different
     *            supported event type.
     * @param eventLogWriter
     *            The event log writer to which the newly generated snapshot
     *            events are pushed.
     *            @throws IllegalStateException if two event generators declare to be responsible for the same event type.
     */
    public SnapshotCreationService(List<SnapshotEventGenerator> snapshotEventGenerators,
            EventLogWriter eventLogWriter) {
        this.snapshotEventProviders = snapshotEventGenerators.stream()
                .collect(toMap(SnapshotEventGenerator::getSupportedEventType, identity()));
        this.eventLogWriter = eventLogWriter;
    }

    public void createSnapshotEvents(final String eventType, String filter) {
        final SnapshotEventGenerator snapshotEventGenerator = snapshotEventProviders.get(eventType);
        if (snapshotEventGenerator == null) {
            throw new UnknownEventTypeException(eventType);
        }

        Object lastProcessedId = null;
        do {
            final List<Snapshot> snapshots = snapshotEventGenerator.generateSnapshots(lastProcessedId, filter);
            if (snapshots.isEmpty()) {
                break;
            }

            snapshots.stream()
                    .collect(groupingBy(Snapshot::getDataType, mapping(Snapshot::getData, toList())))
                    .forEach((dataType, snapshotPartition) ->
                            eventLogWriter.fireSnapshotEvents(eventType, dataType, snapshotPartition));

            lastProcessedId = snapshots.get(snapshots.size()-1).getId();
        } while (true);
    }

    public Set<String> getSupportedEventTypes() {
        return unmodifiableSet(snapshotEventProviders.keySet());
    }
}
