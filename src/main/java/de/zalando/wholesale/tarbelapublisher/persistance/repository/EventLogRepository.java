package de.zalando.wholesale.tarbelapublisher.persistance.repository;

import de.zalando.wholesale.tarbelapublisher.persistance.entity.event.EventLog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventLogRepository extends JpaRepository<EventLog, Integer>,
        EventLogCustomRepository {

    List<EventLog> findByIdIn(List<Integer> eventIds);
}
