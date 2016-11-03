package de.zalando.wholesale.tarbelapublisher.service.event;

import de.zalando.wholesale.tarbelapublisher.api.event.model.BunchOfEventUpdatesDTO;
import de.zalando.wholesale.tarbelapublisher.api.event.model.BunchOfEventsDTO;

import java.util.Collection;

import javax.transaction.Transactional;

public interface EventLogService {

    @Transactional
    void fireCreateEvent(Object payload, String eventType, String dataType, String flowId);

    @Transactional
    void fireUpdateEvent(Object payload, String eventType, String dataType, String flowId);

    /**
     * Searches for events for given cursor, status and limit.
     */
    BunchOfEventsDTO searchEvents(String cursor, String status, Integer limit,
                                  String eventType, String sinkId);

    /**
     * Updates the event logs.
     */
    void updateEvents(BunchOfEventUpdatesDTO updates);

    /**
     * Creates snapshot event logs for all objects.
     */
    @Transactional
    void createSnapshotEvents(Collection<?> snapshotItems, String eventType, String dataType, String flowId);
}
