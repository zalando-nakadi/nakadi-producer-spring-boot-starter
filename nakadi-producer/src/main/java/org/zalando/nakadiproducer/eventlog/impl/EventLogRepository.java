package org.zalando.nakadiproducer.eventlog.impl;

import java.time.Instant;
import java.util.Collection;

public interface EventLogRepository {
    Collection<EventLog> findByLockedByAndLockedUntilGreaterThan(String lockedBy, Instant lockedUntil);

    void lockSomeMessages(String lockId, Instant now, Instant lockExpires);

    void delete(EventLog eventLog);

    void persist(EventLog eventLog);

    void deleteAll();

    EventLog findOne(Integer id);
}
