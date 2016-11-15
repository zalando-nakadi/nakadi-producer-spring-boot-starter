package org.zalando.tarbelaproducer.persistance.repository;


import org.zalando.tarbelaproducer.persistance.entity.EventLog;

import java.util.List;

public interface EventLogCustomRepository {

    /**
     * Searches for event logs based on cursor, status and limit.
     */
    List<EventLog> search(Integer cursor, String status, int limit);

}
