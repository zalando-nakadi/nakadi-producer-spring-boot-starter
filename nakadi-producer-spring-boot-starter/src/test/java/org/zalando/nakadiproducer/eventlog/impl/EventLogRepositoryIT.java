package org.zalando.nakadiproducer.eventlog.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.zalando.nakadiproducer.BaseMockedExternalCommunicationIT;

import java.util.List;

@Transactional
public class EventLogRepositoryIT extends BaseMockedExternalCommunicationIT {

    @Autowired
    private EventLogRepository eventLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String WAREHOUSE_EVENT_BODY_DATA =
            ("{'self':'http://WAREHOUSE_DOMAIN',"
            + "'code':'WH-DE-EF',"
            + "'name':'Erfurt',"
            + "'address':{'name':'Zalando Logistics SE & Co.KG',"
                       + "'street':'In der Hochstedter Ecke 1',"
                       + "'city':'Erfurt',"
                       + "'zip':'99098',"
                       + "'country':'DE',"
                       + "'additional':null"
            + "},"
            + "'is_allowed_for_shipping':true,"
            + "'is_allowed_for_purchase_order':true,"
            + "'legacy_warehouse_code':'3'"
            + "}").replace('\'', '"');

    private final String WAREHOUSE_EVENT_TYPE = "wholesale.warehouse-change-event";

    public static final String COMPACTION_KEY = "COMPACTED";

    @BeforeEach
    public void setUp() {
        eventLogRepository.deleteAll();

        persistTestEvent("FLOW_ID");
    }

    private void persistTestEvent(String flowId) {
        final EventLog eventLog = EventLog.builder()
                .eventBodyData(WAREHOUSE_EVENT_BODY_DATA)
                .eventType(WAREHOUSE_EVENT_TYPE)
                .compactionKey(COMPACTION_KEY)
                .flowId(flowId)
                .build();
        eventLogRepository.persist(eventLog);
    }

    @Test
    public void testDeleteMultipleEvents() {
        persistTestEvent("second_Flow-ID");
        persistTestEvent("third flow-ID");
        persistTestEvent("fourth flow-ID");
        persistTestEvent("fifth flow-ID");

        List<EventLog> events = findAllEventsInDB();
        assertThat(events, hasSize(5));
        EventLog notDeleted = events.remove(0);

        // now the actual test – delete just 4 of the 5 events from the DB
        eventLogRepository.delete(events);

        List<EventLog> remaining = findAllEventsInDB();
        assertThat(remaining, hasSize(1));
        assertThat(remaining.get(0).getId(), is(notDeleted.getId()));
        assertThat(remaining.get(0).getFlowId(), is(notDeleted.getFlowId()));
    }

    private List<EventLog> findAllEventsInDB() {
        return jdbcTemplate.query(
                "SELECT * FROM nakadi_events.event_log",
                new BeanPropertyRowMapper<>(EventLog.class));
    }

    @Test
    public void testFindEventInRepositoryById() {
        Integer id = jdbcTemplate.queryForObject(
            "SELECT id FROM nakadi_events.event_log WHERE flow_id = 'FLOW_ID'",
            Integer.class);
        final EventLog eventLog = eventLogRepository.findOne(id);
        compareWithPersistedEvent(eventLog);
    }

    private void compareWithPersistedEvent(final EventLog eventLog) {
        assertThat(eventLog.getEventBodyData(), is(WAREHOUSE_EVENT_BODY_DATA));
        assertThat(eventLog.getEventType(), is(WAREHOUSE_EVENT_TYPE));
        assertThat(eventLog.getCompactionKey(), is(COMPACTION_KEY));
    }

}
