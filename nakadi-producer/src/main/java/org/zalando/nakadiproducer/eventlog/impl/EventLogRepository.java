package org.zalando.nakadiproducer.eventlog.impl;

import java.time.Instant;
import java.util.Collection;

/**
 * This interface represents the database table where event log entries are stored.
 * Normal users of the library don't need to care about this, it's implemented in
 * nakadi-producer-spring-boot-starter and used in nakadi-producer.
 * Only if you are using nakadi-producer without the spring-boot-starter, you'll have to implement it yourself.
 */
public interface EventLogRepository {
    /**
     * Fetched events which were locked by the given lock ID, and whose lock is not yet expired.
     * @param lockId the lock ID used for locking. This should be the same value as previously used
     *              in {@link #lockSomeMessages(String, Instant, Instant)} for locking the event log entries.
     * @param lockedUntil A cut-off for the expiry time. Use a time here where you are reasonably confident
     *                    that you can send out the fetched events until this time.
     * @return the fetched events.
     */
    Collection<EventLog> findByLockedByAndLockedUntilGreaterThan(String lockId, Instant lockedUntil);

    /**
     * Lock some event log entries, so that other instances won't try to send them out.
     * You can later retrieve the locked entries with {@link #findByLockedByAndLockedUntilGreaterThan(String, Instant)}.
     * @param lockId a unique identifier for this instance / job run / etc. The same value should
     *             later be used for fetching them in {@link #findByLockedByAndLockedUntilGreaterThan(String, Instant)}.
     * @param now         existing locked event logs whose lock expiry time is before this value can be locked again.
     * @param lockExpires an expiry time to use for the new locks.
     */
    void lockSomeMessages(String lockId, Instant now, Instant lockExpires);

    /**
     * Deletes a single event log entry from the database.
     * @param eventLog the event log entry. Only its {@code id} property is used.
     */
    void delete(EventLog eventLog);

    /**
     * Deletes multiple event log entries.
     * @param eventLogs A collection of event log entries.
     *                  Only their {@code id} properties are be used.
     */
    default void delete(Collection<EventLog> eventLogs) {
        eventLogs.forEach(this::delete);
    }

    /**
     * Persists a single eventlog entry.
     * @param eventLog the event log entry to insert into the database.
     *                 It's not part of the contract to fill the {@code id} property
     *                 with the generated identifier, but an implementation is free to do so.
     *                 (The implementation in Nakadi-Producer Spring Boot Starter doesn't do so.)
     */
    void persist(EventLog eventLog);

    /**
     * Persist multiple event log entries at once.
     * @param eventLogs A collection of event logs entries.
     *                 It's not part of the contract to fill the {@code id} property
     *                 with the generated identifier, but an implementation is free to do so.
     *                 (The implementation in Nakadi-Producer Spring Boot Starter doesn't do so.)
     */
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

    /**
     * Deletes all event log entries. This is only meant for cleanup in tests.
     */
    void deleteAll();

    /**
     * Fetches a specific event log by its ID. This is only meant to be used in tests.
     * @param id  the id attribute.
     * @return the event log entry with the given ID, or null if there is none.
     */
    EventLog findOne(Integer id);
}
