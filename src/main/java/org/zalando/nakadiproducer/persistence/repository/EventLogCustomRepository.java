package org.zalando.nakadiproducer.persistence.repository;


import org.zalando.nakadiproducer.persistence.entity.EventLog;

import java.util.List;

public interface EventLogCustomRepository {

    /**
     * Searches for event logs based on cursor, status and limit.
     */
    List<EventLog> search(Integer cursor, String status, int limit);

}
