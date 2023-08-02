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
        for (EventLog eventLog : eventLogs) {
            persist(eventLog);
        }
    }

    void deleteAll();

    EventLog findOne(Integer id);
}
