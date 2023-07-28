package org.zalando.nakadiproducer.eventlog.impl;

import java.time.Instant;
import java.util.Collection;

public interface EventLogRepository {
    Collection<EventLog> findByLockedByAndLockedUntilGreaterThan(String lockedBy, Instant lockedUntil);

    void lockSomeMessages(String lockId, Instant now, Instant lockExpires);

    void delete(EventLog eventLog);

    default void delete(Collection<EventLog> eventLogs) {
        eventLogs.forEach(this::delete);
    }

    void persist(EventLog eventLog);

    default void persist(Collection<EventLog> eventLogs) {
        eventLogs.forEach(this::persist);
    }

    /**
     * Persists and immediately deletes some event log entries.
     * This is meant to be used together with infrastructure listening to a logical DB replication stream.
     * <p><b>Implementer's note:</b> The default implementation just calls {@link #persist(Collection)}
     *   and {@link #delete(Collection)} and will only work if {@code persist} stores back the IDs into the objects
     *   (which is not part of the contract). If that's not the case, this methods needs to be overridden to
     *   implement it in a different way.
     * </p>
     *  (The implementation in Nakadi-Producer Spring Boot starter reimplements this method.)
     * @param eventLogs the event log entries to be persisted and deleted.
     */
    default void persistAndDelete(Collection<EventLog> eventLogs) {
        persist(eventLogs);
        delete(eventLogs);
    };

    void deleteAll();

    EventLog findOne(Integer id);
}
