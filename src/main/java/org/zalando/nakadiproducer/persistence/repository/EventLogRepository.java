package org.zalando.nakadiproducer.persistence.repository;

import org.zalando.nakadiproducer.persistence.entity.EventLog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventLogRepository extends JpaRepository<EventLog, Integer>,
        EventLogCustomRepository {

    List<EventLog> findByIdIn(List<Integer> eventIds);
}
