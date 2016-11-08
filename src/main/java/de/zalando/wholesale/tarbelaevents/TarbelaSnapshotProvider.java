package de.zalando.wholesale.tarbelaevents;

import de.zalando.wholesale.tarbelaevents.service.model.EventPayload;

import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

/**
 * The {@code TarbelaSnapshotProvider} interface should be implemented by any
 * Tarbela Event Producer that wants to support snapshot events feature. The
 * class must define a method of no arguments called {@code getSnapshot}.
 */
public interface TarbelaSnapshotProvider {

    /**
     * Returns a stream consisting of elements for creating a snapshot of events
     * of given type (event type is an event channel topic name).
     * @return stream of elements to create a snapshot from
     */
    Stream<EventPayload> getSnapshot(@NotNull String eventType);

}
