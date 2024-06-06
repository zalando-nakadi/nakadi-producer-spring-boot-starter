package org.zalando.nakadiproducer.eventlog;

import java.util.UUID;
import org.zalando.nakadiproducer.eventlog.impl.EventLog;

/**
 * Strategy for generating EIDs.
 * <p>
 * EID - is unique identifier for nakadi events and we expect that implementation will generate unique EID for each event.
 */
public interface EidGeneratorStrategy {

    static EidGeneratorStrategy noop() {
        return (EventLog eventLog) -> null;
    }

    static EidGeneratorStrategy random() {
        return (EventLog eventLog) -> UUID.randomUUID();
    }

    UUID generateEid(EventLog eventLog);
}
