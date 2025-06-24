package org.zalando.nakadiproducer.eventlog.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.zalando.nakadiproducer.eventlog.impl.batcher.QueryStatementBatcher;

import static java.util.stream.Collectors.toList;

public class EventLogRepositoryImpl implements EventLogRepository {

    private final static QueryStatementBatcher<Integer> INSERT_RETURNING_BATCHER = new QueryStatementBatcher<>(
            "INSERT INTO nakadi_events.event_log " +
                    "    (event_type, event_body_data, flow_id, created, last_modified, locked_by, locked_until, compaction_key)" +
                    " VALUES ",
            "  (:eventType#, :eventBodyData#, :flowId#, :created#, :lastModified#, :lockedBy#, :lockedUntil#, :compactionKey#)",
            " RETURNING id",
            (row, n) -> row.getInt("id")
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final int lockSize;

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
        delete(List.of(eventLog));
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
        persist(List.of(eventLog));
    }

    @Override
    public void persist(Collection<EventLog> eventLogs) {
        MapSqlParameterSource[] namedParameterMaps = mapToParameterSourceStream(eventLogs)
                .toArray(MapSqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(
                "INSERT INTO nakadi_events.event_log " +
                        "    ( event_type, event_body_data,  flow_id,   created," +
                        "      last_modified,  locked_by,  locked_until, compaction_key)" +
                        " VALUES " +
                        // the parameter names here need to match the names in toSqlParameterSource
                        "    (:eventType#, :eventBodyData#, :flowId#, :created#," +
                        "     :lastModified#, :lockedBy#, :lockedUntil#, :compactionKey#)",
                namedParameterMaps);
    }

    /**
     * A variant of {@link #persist(Collection)} which will store the generated IDs back into the objects.
     * @param eventLogs
     */
    public void persistWithIds(Collection<EventLog> eventLogs) {
        List<EventLog> orderedLogs = eventLogs instanceof List ?
                (List<EventLog>)eventLogs :
                new ArrayList<>(eventLogs);
        List<Integer> createdIds = INSERT_RETURNING_BATCHER
                .queryForStream(jdbcTemplate, mapToParameterSourceStream(orderedLogs))
                .collect(toList());
        for(int i = 0; i < createdIds.size(); i++) {
            orderedLogs.get(i).setId(createdIds.get(i));
        }
    }

    @Override
    public void persistAndDelete(Collection<EventLog> eventLogs) {
        this.persistWithIds(eventLogs);
        this.delete(eventLogs);
    }

    private Stream<MapSqlParameterSource> mapToParameterSourceStream(Collection<EventLog> eventLogs) {
        return eventLogs.stream().map(this::toSqlParameterSource);
    }

    private MapSqlParameterSource toSqlParameterSource(EventLog eventLog) {
        Timestamp now = toSqlTimestamp(Instant.now());
        return new MapSqlParameterSource()
                .addValue("eventType#", eventLog.getEventType())
                .addValue("eventBodyData#", eventLog.getEventBodyData())
                .addValue("flowId#", eventLog.getFlowId())
                .addValue("created#", now)
                .addValue("lastModified#", now)
                .addValue("lockedBy#", eventLog.getLockedBy())
                .addValue("lockedUntil#", eventLog.getLockedUntil())
                .addValue("compactionKey#", eventLog.getCompactionKey());
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
        SqlParameterSource namedParameterMap = new MapSqlParameterSource().addValue("id", id);
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
