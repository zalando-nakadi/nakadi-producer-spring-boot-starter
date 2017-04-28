package org.zalando.nakadiproducer;

import org.zalando.nakadiproducer.service.exception.UnknownEventTypeException;
import org.zalando.nakadiproducer.service.model.EventPayload;

import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

/**
 * The {@code SnapshotEventProvider} interface should be implemented by any
 * Event Producer that wants to support snapshot events feature. The
 * class must define a method {@code getSnapshot}.
 */
public interface SnapshotEventProvider {

    /**
     * Returns a stream consisting of elements for creating a snapshot of events
     * of given type (event type is an event channel topic name).
     * @param eventType event type to make a snapshot of
     * @return stream of elements to create a snapshot from
     * @throws UnknownEventTypeException if {@code eventType} is unknown
     */
    Stream<EventPayload> getSnapshot(@NotNull String eventType);

}
