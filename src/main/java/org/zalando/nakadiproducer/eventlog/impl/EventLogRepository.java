package org.zalando.nakadiproducer.eventlog.impl;

import java.time.Instant;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EventLogRepository extends JpaRepository<EventLog, Integer> {

    Collection<EventLog> findByLockedByAndLockedUntilGreaterThan(String lockedBy, Instant lockedUntil);

    @Modifying
    @Query("UPDATE EventLog SET lockedBy = ?1, lockedUntil = ?3 where lockedUntil is null or lockedUntil < ?2")
    void lockSomeMessages(String lockId, Instant now, Instant lockExpires);
}
