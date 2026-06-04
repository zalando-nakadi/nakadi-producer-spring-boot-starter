package org.zalando.nakadiproducer.eventlog.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class EventLogRepositoryImpl implements EventLogRepository {

    private NamedParameterJdbcTemplate jdbcTemplate;
    private int lockSize;

    public EventLogRepositoryImpl(NamedParameterJdbcTemplate jdbcTemplate, int lockSize) {
        this.jdbcTemplate = jdbcTemplate;
        this.lockSize = lockSize;
    }

    @Override
    public Collection<EventLog> findByLockedByAndLockedUntilGreaterThan(String lockedBy, Instant lockedUntil) {
        Map<String, Object> namedParameterMap = new HashMap<>();
        namedParameterMap.put("lockedBy", lockedBy);
        namedParameterMap.put("lockedUntil", toSqlTimestamp(lockedUntil));
        return jdbcTemplate.query(
            "SELECT * FROM nakadi_events.event_log where locked_by = :lockedBy and locked_until > :lockedUntil",
            namedParameterMap,
            new BeanPropertyRowMapper<>(EventLog.class)
        );
    }

    @Override
    public void lockSomeMessages(String lockId, Instant now, Instant lockExpires) {
        Map<String, Object> namedParameterMap = new HashMap<>();
        namedParameterMap.put("lockId", lockId);
        namedParameterMap.put("now", toSqlTimestamp(now));
        namedParameterMap.put("lockExpires", toSqlTimestamp(lockExpires));

        StringBuilder optionalLockSizeClause = new StringBuilder();
        if (lockSize > 0) {
          optionalLockSizeClause.append("LIMIT :lockSize");
          namedParameterMap.put("lockSize", lockSize);
        }

        jdbcTemplate.update(
            "UPDATE nakadi_events.event_log "
                + "SET locked_by = :lockId, locked_until = :lockExpires "
                + "WHERE id IN (SELECT id "
                + "             FROM nakadi_events.event_log "
                + "             WHERE locked_until IS null OR locked_until < :now "
                + optionalLockSizeClause
                + "             FOR UPDATE SKIP LOCKED) ",
            namedParameterMap
        );
    }

    @Override
    public void delete(EventLog eventLog) {
        delete(Collections.singleton(eventLog));
    }

    @Override
    public void delete(Collection<EventLog> eventLogs) {
        MapSqlParameterSource[] namedParameterMaps = eventLogs.stream()
                .map(eventLog ->
                    new MapSqlParameterSource().addValue("id", eventLog.getId())
                ).toArray(MapSqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(
                "DELETE FROM nakadi_events.event_log where id = :id",
                namedParameterMaps
        );
    }

    @Override
    public void persist(EventLog eventLog) {
        persist(Collections.singleton(eventLog));
    }

    @Override
    public void persist(Collection<EventLog> eventLogs) {
        MapSqlParameterSource[] namedParameterMaps = eventLogs.stream()
            .map(eventLog -> {
              Timestamp now = toSqlTimestamp(Instant.now());
              MapSqlParameterSource namedParameterMap = new MapSqlParameterSource();
              namedParameterMap.addValue("eventType", eventLog.getEventType());
              namedParameterMap.addValue("eventBodyData", eventLog.getEventBodyData());
              namedParameterMap.addValue("flowId", eventLog.getFlowId());
              namedParameterMap.addValue("created", now);
              namedParameterMap.addValue("lastModified", now);
              namedParameterMap.addValue("lockedBy", eventLog.getLockedBy());
              namedParameterMap.addValue("lockedUntil", eventLog.getLockedUntil());
              namedParameterMap.addValue("compactionKey", eventLog.getCompactionKey());
              namedParameterMap.addValue("eid", eventLog.getEid());
              return namedParameterMap;
            })
            .toArray(MapSqlParameterSource[]::new);

      jdbcTemplate.batchUpdate(
          "INSERT INTO " +
              "    nakadi_events.event_log " +
              "    (event_type, event_body_data, flow_id, created, last_modified, locked_by, locked_until, compaction_key, eid)" +
              "VALUES " +
              "    (:eventType, :eventBodyData, :flowId, :created, :lastModified, :lockedBy, :lockedUntil, :compactionKey, :eid)",
          namedParameterMaps
      );
    }

    private Timestamp toSqlTimestamp(Instant now) {
        if (now == null) {
            return null;
        }
        return Timestamp.from(now);
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE from nakadi_events.event_log", new HashMap<>());
    }

    @Override
    public EventLog findOne(Integer id) {
        Map<String, Object> namedParameterMap = new HashMap<>();
        namedParameterMap.put("id", id);
        try {
            return jdbcTemplate.queryForObject(
                "SELECT * FROM nakadi_events.event_log where id = :id",
                namedParameterMap,
                new BeanPropertyRowMapper<>(EventLog.class)
            );
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }
}
