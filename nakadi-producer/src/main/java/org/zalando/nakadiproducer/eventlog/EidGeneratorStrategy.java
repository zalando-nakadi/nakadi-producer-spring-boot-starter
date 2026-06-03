package org.zalando.nakadiproducer.eventlog;

import java.util.UUID;

/**
 * Strategy for generating EIDs.
 * <p>
 * EID is the unique identifier for nakadi events, and we expect that implementation will generate a unique EID
 * (at least per event type) for each event.
 */
public interface EidGeneratorStrategy {

    /**
     * A built-in strategy which does not generate an eid (which means the library will fall back
     * to converting the sequential DB ID into a UUID).
     * (This is the default strategy, and equivalent to the behavior before this interface was introduced.)
     */
    static EidGeneratorStrategy noop() {
        return () -> null;
    }

    /**
     * A built-in strategy which will assign a random (version 4) UUID, ignoring the data.
     * You should only use this if your consumers don't depend on the eid for ordering
     * of events.
     */
    static EidGeneratorStrategy random() {
        return UUID::randomUUID;
    }

    /**
     * Generates an event ID for a Nakadi event.
     * @return a {@code UUID}, or {@code null} – in the latter case, the library will fall back to
     * converting an internal counter into UUID format.
     */
    UUID generateEid();
}
