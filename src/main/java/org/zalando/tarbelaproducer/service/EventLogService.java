package org.zalando.tarbelaproducer.service;

import org.zalando.tarbelaproducer.api.event.model.BunchOfEventUpdatesDTO;
import org.zalando.tarbelaproducer.api.event.model.BunchOfEventsDTO;

import javax.transaction.Transactional;

public interface EventLogService {

    /**
     * Searches for events for given cursor, status and limit.
     */
    BunchOfEventsDTO searchEvents(String cursor, String status, Integer limit);

    /**
     * Updates the event logs.
     */
    void updateEvents(BunchOfEventUpdatesDTO updates);

    /**
     * Creates snapshot event logs of given type.
     */
    @Transactional
    void createSnapshotEvents(String eventType, String flowId);
}
