package de.zalando.wholesale.tarbelaevents.service;

import de.zalando.wholesale.tarbelaevents.api.event.model.BunchOfEventUpdatesDTO;
import de.zalando.wholesale.tarbelaevents.api.event.model.BunchOfEventsDTO;

import java.util.Collection;

import javax.transaction.Transactional;

public interface EventLogService {

    @Transactional
    void fireCreateEvent(Object payload, String flowId);

    @Transactional
    void fireUpdateEvent(Object payload, String flowId);

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
    void createSnapshotEvents(Collection<?> snapshotItems, String flowId);
}
