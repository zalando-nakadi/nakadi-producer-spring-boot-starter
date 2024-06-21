package org.zalando.nakadiproducer.eventlog.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.hamcrest.core.Is.is;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.zalando.nakadiproducer.BaseMockedExternalCommunicationIT;

import java.util.List;

public class EventLogRepositoryIT extends BaseMockedExternalCommunicationIT {

    @Autowired
    private EventLogRepository eventLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

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
        jdbcTemplate.execute("alter sequence nakadi_events.event_log_id_seq restart with 1");
    }

    private void persistTestEvent(String flowId) {
        final EventLog eventLog = buildEventLog(flowId);
        eventLogRepository.persist(eventLog);
    }

    @Test
    @Transactional
    public void testDeleteMultipleEvents() {
        persistTestEvent("FLOW_ID");
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
        assertThat(remaining.get(0), Matchers.samePropertyValuesAs(notDeleted));
    }

    private List<EventLog> findAllEventsInDB() {
        return jdbcTemplate.query(
                "SELECT * FROM nakadi_events.event_log",
                new BeanPropertyRowMapper<>(EventLog.class));
    }

    @Test
    @Transactional
    public void testFindEventInRepositoryById() {
        persistTestEvent("FLOW_ID");
        Integer id = jdbcTemplate.queryForObject(
            "SELECT id FROM nakadi_events.event_log WHERE flow_id = 'FLOW_ID'",
            Integer.class);
        final EventLog eventLog = eventLogRepository.findOne(id);
        compareWithPersistedEvent(eventLog);
    }

    @Test
    @Transactional
    public void testInsertEventWithDefaultEid() {
        persistTestEvent("FLOW_ID");
        EventLog actual = findAllEventsInDB().get(0);

        EventLog expected = buildEventLog("FLOW_ID", 1, buildEid(1));
        assertEvent(actual, expected);
    }

    @Test
    @Transactional
    public void testInsertEventWithDefinedEid() {
        EventLog testEvent = buildEventLog("flow-id", 1, UUID.randomUUID());
        eventLogRepository.persist(testEvent);

        EventLog actual = findAllEventsInDB().get(0);
        assertEvent(actual, testEvent);
    }

    @Test
    @Transactional
    public void testInsertEventWithNegativeId() {
        jdbcTemplate.execute("ALTER SEQUENCE nakadi_events.event_log_id_seq " +
            " MINVALUE " + Integer.MIN_VALUE +
            " START " + Integer.MIN_VALUE +
            " RESTART " + Integer.MIN_VALUE);
        persistTestEvent("FLOW_ID");
        EventLog actual = findAllEventsInDB().get(0);

        EventLog expected = buildEventLog("FLOW_ID", Integer.MIN_VALUE, buildEid(Integer.MIN_VALUE));
        assertEvent(actual, expected);
    }

    /**
     * This test checks that the default eid is generated correctly when multiple transactions are running in parallel.
     * The test creates three events in two parallel transactions.
     * Execution order of the transactions is the following:
     * -- / T1 starts -- Event1 ----------------------------------------- Event3 --- T1 ends -/--
     * -------------------------- / T2 starts --- Event2 --- T2 ends -/--------------------------
     */
    @Test
    public void testInsertEventWithDefaultEidWithParallelTransactions() throws Exception {
        EventLog firstExpected = buildEventLog("first flow-id", 1, buildEid(1));
        EventLog secondExpected = buildEventLog("second flow-id", 2, buildEid(2));
        EventLog thirdExpected = buildEventLog("third flow-id", 3, buildEid(3));

        CountDownLatch latchInsideTransaction = new CountDownLatch(1);
        CountDownLatch latchOutsideTransaction = new CountDownLatch(1);

        CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
            transactionTemplate.executeWithoutResult(
                (s) -> {
                    // We persist first event with default eid in the start of first transaction
                    // It should be first event in the table.
                    persistTestEvent("first flow-id");

                    latchInsideTransaction.countDown();
                    // Waiting for the second transaction to complete
                    try {
                        latchOutsideTransaction.await(2, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    // We persist second event in the end of first transaction.
                    // It should be third event in the table.
                    persistTestEvent("third flow-id");
                }
            )
        );

        // Waiting for the first transaction to start
        latchInsideTransaction.await(1, TimeUnit.SECONDS);

        // We persist third event in the second transaction.
        transactionTemplate.executeWithoutResult(
            (s) -> persistTestEvent("second flow-id")
        );
        // We check that the first and third events haven't visible yet
        List<EventLog> events = findAllEventsInDB();
        assertThat(events, hasSize(1));
        assertEvent(events.get(0), secondExpected);

        // Waiting for the first transaction to complete
        latchOutsideTransaction.countDown();
        future.get(1, TimeUnit.SECONDS);

        // We check that the all three events are in the table
        events = findAllEventsInDB();
        assertThat(events, hasSize(3));
        assertEvent(events.get(0), firstExpected);
        assertEvent(events.get(1), secondExpected);
        assertEvent(events.get(2), thirdExpected);
    }

    private void assertEvent(EventLog actual, EventLog expected) {
        assertThat(actual,
            samePropertyValuesAs(expected, "created", "lastModified")
        );
    }

    private void compareWithPersistedEvent(final EventLog eventLog) {
        assertThat(eventLog.getEventBodyData(), is(WAREHOUSE_EVENT_BODY_DATA));
        assertThat(eventLog.getEventType(), is(WAREHOUSE_EVENT_TYPE));
        assertThat(eventLog.getCompactionKey(), is(COMPACTION_KEY));
    }

    private EventLog buildEventLog(String flowId) {
        return buildEventLog(flowId, null, null);
    }

    private EventLog buildEventLog(String flowId, Integer id, UUID eid) {
        return EventLog.builder()
            .id(id)
            .eventBodyData(WAREHOUSE_EVENT_BODY_DATA)
            .eventType(WAREHOUSE_EVENT_TYPE)
            .compactionKey(COMPACTION_KEY)
            .flowId(flowId)
            .eid(eid)
            .build();
    }

    private UUID buildEid(int id) {
        return new UUID(0, id);
    }
}
