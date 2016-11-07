package de.zalando.wholesale.tarbelaevents.service;

import de.zalando.wholesale.tarbelaevents.api.event.model.BunchOfEventUpdatesDTO;
import de.zalando.wholesale.tarbelaevents.api.event.model.BunchOfEventsDTO;

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
     * Creates snapshot event logs for all objects.
     */
    @Transactional
    void createSnapshotEvents(String flowId);
}
