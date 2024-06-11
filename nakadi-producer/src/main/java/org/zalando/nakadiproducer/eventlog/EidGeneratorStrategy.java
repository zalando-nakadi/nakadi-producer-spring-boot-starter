package org.zalando.nakadiproducer.eventlog;

import java.util.UUID;
import org.zalando.nakadiproducer.eventlog.impl.EventLog;

/**
 * Strategy for generating EIDs.
 * <p>
 * EID - is unique identifier for nakadi events and we expect that implementation will generate unique EID for each event.
 */
public interface EidGeneratorStrategy {

    /**
     * A built-in strategy which does not generate an eid (which means the library will fall back
     * to converting the sequential DB ID into an UUID).
     * (This is the default strategy, and equivalent to the behavior before this interface was introduced.)
     */
    static EidGeneratorStrategy noop() {
        return (EventLog eventLog) -> null;
    }

    /**
     * A built-in strategy which will assign a random (type 4) UUID, ignoring the data.
     * You should only use this if your consumers don't depend on the eid for ordering
     * of events.
     */
    static EidGeneratorStrategy random() {
        return (EventLog eventLog) -> UUID.randomUUID();
    }

    UUID generateEid(EventLog eventLog);
}
