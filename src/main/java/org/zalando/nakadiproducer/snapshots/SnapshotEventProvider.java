package org.zalando.nakadiproducer.snapshots;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.zalando.nakadiproducer.eventlog.EventPayload;

import java.util.List;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * The {@code SnapshotEventProvider} interface should be implemented by any
 * Event Producer that wants to support snapshot events feature. The
 * class must define a method {@code getSnapshot}.
 */
public interface SnapshotEventProvider {

    /**
     * Returns a stream consisting of elements for creating a snapshot of events of given type (event type is an event
     * channel topic name). The implementation may return an arbitrary amount of results, but it must return at least
     * one element if there are entities matching the parameters.
     *
     * @param eventType event type to make a snapshot of
     * @param withIdGreaterThan if not null, only events for entities with an id greater than the given one must be returned
     * @return stream of elements ordered by their id to create a snapshot from
     * @throws UnknownEventTypeException if {@code eventType} is unknown
     */
    List<Snapshot> getSnapshot(@NotNull String eventType, @Nullable Object withIdGreaterThan);

    @AllArgsConstructor
    @Getter
    class Snapshot {
        private Object id;
        private EventPayload eventPayload;
    }

}
