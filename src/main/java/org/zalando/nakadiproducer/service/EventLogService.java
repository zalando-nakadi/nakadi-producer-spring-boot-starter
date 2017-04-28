package org.zalando.nakadiproducer.service;

import java.util.Collection;

import javax.transaction.Transactional;

import org.zalando.nakadiproducer.persistence.entity.EventLog;

public interface EventLogService {
    /**
     * Creates snapshot event logs of given type.
     */
    @Transactional
    void createSnapshotEvents(String eventType, String flowId);

    @Transactional
    Collection<EventLog> lockSomeEvents();

    @Transactional
    void sendEvent(EventLog eventLog);
}
