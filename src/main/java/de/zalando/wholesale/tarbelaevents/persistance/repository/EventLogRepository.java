package de.zalando.wholesale.tarbelaevents.persistance.repository;

import de.zalando.wholesale.tarbelaevents.persistance.entity.EventLog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventLogRepository extends JpaRepository<EventLog, Integer>,
        EventLogCustomRepository {

    List<EventLog> findByIdIn(List<Integer> eventIds);
}
