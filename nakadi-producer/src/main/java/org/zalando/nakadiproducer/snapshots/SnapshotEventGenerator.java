package org.zalando.nakadiproducer.snapshots;


import org.springframework.lang.Nullable;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The {@code SnapshotEventGenerator} interface should be implemented by any
 * event producer that wants to support the snapshot events feature. The class
 * must define a method {@link #generateSnapshots}, as well as
 * {@link #getSupportedEventType()}.
 *
 * The {@link #of} methods can be used for creating an implementation from method references or lambdas:
 * <pre>{@code
 *  @Bean
 *  public SnapshotEventGenerator snapshotEventGenerator(MyService service) {
 *     return SnapshotEventGenerator.of("event type", service::createSnapshotEvents);
 *  }
 * }</pre>
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
    List<Snapshot> generateSnapshots(@Nullable Object withIdGreaterThan, String filter);

    /**
     * The name of the event type supported by this snapshot generator.
     */
    String getSupportedEventType();

    /**
     * Creates a SnapshotEventGenerator for an event type, which doesn't support
     * filtering.
     *
     * @param eventType
     *            the eventType that this SnapShotEventProvider will support.
     * @param generator
     *            a snapshot event factory function conforming to the
     *            specification of
     *            {@link SnapshotEventGenerator#generateSnapshots(Object, String)}
     *            (but without the filter parameter). Any filter provided by the
     *            caller will be thrown away.
     * @since 21.1.0
     */
    static SnapshotEventGenerator of(String eventType, Function<Object, List<Snapshot>> generator) {
        return new SimpleSnapshotEventGenerator(eventType, generator);
    }

    /**
     * Creates a SnapshotEventGenerator for an event type, with filtering
     * support.
     *
     * @param eventType
     *            the eventType that this SnapShotEventProvider will support.
     * @param generator
     *            a snapshot event factory function conforming to the
     *            specification of
     *            {@link SnapshotEventGenerator#generateSnapshots(Object, String)}.
     * @since 21.1.0
     */
    static SnapshotEventGenerator of(String eventType, BiFunction<Object, String, List<Snapshot>> generator) {
        return new SimpleSnapshotEventGenerator(eventType, generator);
    }
}
