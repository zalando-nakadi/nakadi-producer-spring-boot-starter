package org.zalando.nakadiproducer.snapshots;

import java.util.List;

/**
 * The {@code SnapshotEventGenerator} interface should be implemented by any
 * event producer that wants to support the snapshot events feature. The class
 * must define a method {@link #generateSnapshots}, as well as
 * {@link #getSupportedEventType()}.
 */
public interface SnapshotEventGenerator {

    /**
     * <p>
     * Returns a batch of snapshots of given type (event type is an event
     * channel topic name). The implementation may return an arbitrary amount of
     * results, but it must return at least one element if there are entities
     * matching the parameters.
     * </p>
     * <p>
     * Calling this method must have no side effects.
     * </p>
     * The library will call your implementation like this:
     * <ul>
     * <li>Request: generateSnapshots(null, filter), Response: 1,2,3</li>
     * <li>Request: generateSnapshots(3, filter), Response: 4,5</li>
     * <li>Request: generateSnapshots(5, filter), Response: emptyList</li>
     * </ul>
     * <p>
     * It is your responsibility to make sure that the returned events are
     * ordered by their ID ascending and that, given you return a list of events
     * for entities with IDs {id<sub>1</sub>, ..., id<sub>N</sub>}, there exists
     * no entity with an ID between id<sub>1</sub> and id<sub>N</sub>, that is
     * not part of the result.
     * </p>
     *
     * @param withIdGreaterThan
     *            if not null, only events for entities with an ID greater than
     *            the given one must be returned
     *
     * @param filter
     *            a filter for the snapshot generation mechanism. This value is
     *            simply passed through from the request body of the REST
     *            endpoint (or from any other triggering mechanism). If there
     *            was no request body, this will be {@code null}.
     *
     *            Implementors can interpret it in whatever way they want (even
     *            ignore it). All calls for one snapshot generation will receive
     *            the same string.
     *
     * @return list of elements (wrapped in Snapshot objects) ordered by their
     *         ID.
     */
    List<Snapshot> generateSnapshots(Object withIdGreaterThan, String filter);

    /**
     * The name of the event type supported by this snapshot generator.
     */
    String getSupportedEventType();
}