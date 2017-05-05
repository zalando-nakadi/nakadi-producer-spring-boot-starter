package org.zalando.nakadiproducer.snapshots;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.zalando.nakadiproducer.eventlog.EventPayload;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * The {@code SnapshotEventProvider} interface should be implemented by any
 * Event Producer that wants to support snapshot events feature. The
 * class must define a method {@code getSnapshot}.
 */
public interface SnapshotEventProvider {

    /**
     * Returns a batch of snapshots of given type (event type is an event channel topic name). The implementation may
     * return an arbitrary amount of results, but it must return at least one element if there are entities
     * matching the parameters.
     *
     * The library will call your implementation like this:
     * Request: getSnapshot(eventType, null) Response: 1,2,3
     * Request: getSnapshot(eventType, 3) Response: 4,5
     * Request: getSnapshot(eventType, 5) Response: emptyList
     * It is your responsibility to make sure that the returned events are ordered by their id asc and that, given you
     * return a list of events for entities with ids {id1, ..., idN}, there exists no entity with an id between id1 and idN, that
     * is not part of the result.
     *
     *
     * @param eventType event type to make a snapshot of
     * @param withIdGreaterThan if not null, only events for entities with an id greater than the given one must be returned
     * @return list of elements ordered by their id
     * @throws UnknownEventTypeException if {@code eventType} is unknown
     */
    List<Snapshot> getSnapshot(@NotNull String eventType, @Nullable Object withIdGreaterThan);

    Set<String> getSupportedEventTypes();

    @AllArgsConstructor
    @Getter
    class Snapshot {
        private Object id;
        private EventPayload eventPayload;
    }

}
