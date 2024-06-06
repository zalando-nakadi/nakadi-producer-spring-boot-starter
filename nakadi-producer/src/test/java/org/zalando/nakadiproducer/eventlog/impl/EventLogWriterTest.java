package org.zalando.nakadiproducer.eventlog.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.zalando.nakadiproducer.eventlog.impl.EventDataOperation.CREATE;
import static org.zalando.nakadiproducer.eventlog.impl.EventDataOperation.DELETE;
import static org.zalando.nakadiproducer.eventlog.impl.EventDataOperation.SNAPSHOT;
import static org.zalando.nakadiproducer.eventlog.impl.EventDataOperation.UPDATE;
import static org.zalando.nakadiproducer.util.Fixture.PUBLISHER_EVENT_TYPE;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import org.zalando.nakadiproducer.eventlog.CompactionKeyExtractor;
import org.zalando.nakadiproducer.util.Fixture;
import org.zalando.nakadiproducer.util.MockPayload;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * With JUnit4, this would have been a parameterized test.
 * With JUnit 5, we have those on method level, but I want it for the whole class.
 * As a workaround, we make this class abstract, with subclasses (which also are static nested classes)
 * which have the different compaction key logic.
 */
@ExtendWith(MockitoExtension.class)
public abstract class EventLogWriterTest {

    public static class NoCompactionKey extends EventLogWriterTest {
        public NoCompactionKey() {
            super((key, data) -> assertThat(key, is(nullValue())));
        }
    }

    public static class DummyCompactionKey extends EventLogWriterTest {
        public DummyCompactionKey() {
            super((key, data) -> assertThat(key, is("dummy")),
                    CompactionKeyExtractor.of(PUBLISHER_EVENT_TYPE, MockPayload.class, m -> "dummy")
            );
        }
    }

    public static class CodeCompactionKey extends EventLogWriterTest {
        public CodeCompactionKey() {
            super((key, data) -> {
                assertThat(key, startsWith("mockedcode"));
                assertThat(data, containsString("code\":\"" + key));
            }, CompactionKeyExtractor.of(PUBLISHER_EVENT_TYPE, MockPayload.class,
                            m -> m.getCode())
            );
        }
    }

    public static class IdCompactionKey extends EventLogWriterTest {
        public IdCompactionKey() {
            super((key, data) -> {
                assertThat(key, isOneOf("1", "2", "3"));
                assertThat(data, containsString("\"id\":" + key + ","));
            },
                    CompactionKeyExtractor.of(PUBLISHER_EVENT_TYPE, MockPayload.class,
                            m -> m.getId().toString())
            );
        }
    }

    @Mock
    private EventLogRepository eventLogRepository;

    @Mock
    private EventLogMapper eventLogMapper;

    @Captor
    private ArgumentCaptor<EventLog> eventLogCapture;

    @Captor
    private ArgumentCaptor<Collection<EventLog>> eventLogsCapture;

    private EventLogWriterImpl eventLogWriter;

    private MockPayload eventPayload1;
    private MockPayload eventPayload2;
    private MockPayload eventPayload3;

    private static final String TRACE_ID = "TRACE_ID";
    private static final UUID EID = UUID.fromString("558a8fe5-330e-4d89-ae6c-d58432b2dde0");


    private static final String EVENT_BODY_DATA_1 =
        ("{'id':1,"
            + "'code':'mockedcode1',"
            + "'more':{'info':'some info'},"
            + "'items':[{'detail':'some detail0'},{'detail':'some detail1'}],"
            + "'active':true"
            + "}").replace('\'', '"');

    private static final String EVENT_BODY_DATA_2 =
        ("{'id':2,"
            + "'code':'mockedcode2',"
            + "'more':{'info':'some info'},"
            + "'items':[{'detail':'some detail0'},{'detail':'some detail1'}],"
            + "'active':true"
            + "}").replace('\'', '"');

    private static final String EVENT_BODY_DATA_3 =
        ("{'id':3,"
            + "'code':'mockedcode3',"
            + "'more':{'info':'some info'},"
            + "'items':[{'detail':'some detail0'},{'detail':'some detail1'}],"
            + "'active':true"
            + "}").replace('\'', '"');

    private static final String PUBLISHER_DATA_TYPE_1 = "nakadi:some-publisher";

    private static final String DATA_CHANGE_BODY_DATA_1 = ("{'data_op':'{DATA_OP}','data_type':'" +
        PUBLISHER_DATA_TYPE_1 + "','data':" + EVENT_BODY_DATA_1
        + "}").replace('\'', '"');

    private static final String DATA_CHANGE_BODY_DATA_2 = ("{'data_op':'{DATA_OP}','data_type':'" +
        PUBLISHER_DATA_TYPE_1 + "','data':" + EVENT_BODY_DATA_2
        + "}").replace('\'', '"');

    private static final String DATA_CHANGE_BODY_DATA_3 = ("{'data_op':'{DATA_OP}','data_type':'" +
        PUBLISHER_DATA_TYPE_1 + "','data':" + EVENT_BODY_DATA_3
        + "}").replace('\'', '"');

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();


    private List<CompactionKeyExtractor> extractorList;
    private BiConsumer<String, String> keyAsserter;

    private EventLogWriterTest(BiConsumer<String, String> keyAsserter, CompactionKeyExtractor... extractors) {
        this.keyAsserter = keyAsserter;
        this.extractorList = List.copyOf(Arrays.asList(extractors));
    }

    @BeforeEach
    public void setUp() {
        Mockito.reset(eventLogRepository, eventLogMapper);

        eventPayload1 = Fixture.mockPayload(1, "mockedcode1", true,
            Fixture.mockSubClass("some info"), Fixture.mockSubList(2, "some detail"));

        eventPayload2 = Fixture.mockPayload(2, "mockedcode2", true,
            Fixture.mockSubClass("some info"), Fixture.mockSubList(2, "some detail"));

        eventPayload3 = Fixture.mockPayload(3, "mockedcode3", true,
            Fixture.mockSubClass("some info"), Fixture.mockSubList(2, "some detail"));

        eventLogWriter = new EventLogWriterImpl(eventLogRepository, eventLogMapper, extractorList);
    }

    @Test
    public void testFireCreateEvent() {
        mockCreateEventLog(eventPayload1, CREATE);

        eventLogWriter.fireCreateEvent(PUBLISHER_EVENT_TYPE, PUBLISHER_DATA_TYPE_1, eventPayload1);

        verifyPersistedDataEventLog("C");
    }

    @Test
    public void testFireCreateEvents() {
        mockCreateEventLog(eventPayload1, CREATE);
        mockCreateEventLog(eventPayload2, CREATE);
        mockCreateEventLog(eventPayload3, CREATE);

        eventLogWriter.fireCreateEvents(
            PUBLISHER_EVENT_TYPE,
            PUBLISHER_DATA_TYPE_1,
            Arrays.asList(eventPayload1, eventPayload2, eventPayload3)
        );
        verifyPersistedEventLogs("C");
    }

    @Test
    public void testFireUpdateEvent() {
        mockCreateEventLog(eventPayload1, UPDATE);

        eventLogWriter.fireUpdateEvent(PUBLISHER_EVENT_TYPE, PUBLISHER_DATA_TYPE_1, eventPayload1);

        verifyPersistedDataEventLog("U");
    }

    @Test
    public void testFireUpdateEvents() {
        mockCreateEventLog(eventPayload1, UPDATE);
        mockCreateEventLog(eventPayload2, UPDATE);
        mockCreateEventLog(eventPayload3, UPDATE);

        eventLogWriter.fireUpdateEvents(
            PUBLISHER_EVENT_TYPE,
            PUBLISHER_DATA_TYPE_1,
            Arrays.asList(eventPayload1, eventPayload2, eventPayload3)
        );
        verifyPersistedEventLogs("U");
    }

    @Test
    public void testFireDeleteEvent() {
        mockCreateEventLog(eventPayload1, DELETE);

        eventLogWriter.fireDeleteEvent(PUBLISHER_EVENT_TYPE, PUBLISHER_DATA_TYPE_1, eventPayload1);

        verifyPersistedDataEventLog("D");
    }

    @Test
    public void testFireDeleteEvents() {
        mockCreateEventLog(eventPayload1, DELETE);
        mockCreateEventLog(eventPayload2, DELETE);
        mockCreateEventLog(eventPayload3, DELETE);

        eventLogWriter.fireDeleteEvents(
            PUBLISHER_EVENT_TYPE,
            PUBLISHER_DATA_TYPE_1,
            Arrays.asList(eventPayload1, eventPayload2, eventPayload3));
        verifyPersistedEventLogs("D");
    }

    @Test
    public void testFireSnapshotEvent() throws Exception {
        mockCreateEventLog(eventPayload1, SNAPSHOT);

        eventLogWriter.fireSnapshotEvent(PUBLISHER_EVENT_TYPE, PUBLISHER_DATA_TYPE_1,
            eventPayload1);

        verifyPersistedDataEventLog("S");
    }

    @Test
    public void testFireSnapshotEvents() {
        mockCreateEventLog(eventPayload1, SNAPSHOT);
        mockCreateEventLog(eventPayload2, SNAPSHOT);
        mockCreateEventLog(eventPayload3, SNAPSHOT);

        eventLogWriter.fireSnapshotEvents(
            PUBLISHER_EVENT_TYPE,
            PUBLISHER_DATA_TYPE_1,
            Arrays.asList(eventPayload1, eventPayload2, eventPayload3));
        verifyPersistedEventLogs("S");
    }

    @Test
    public void testFireBusinessEvent() throws Exception {
        MockPayload mockPayload = Fixture.mockPayload(1, "mockedcode1", true,
            Fixture.mockSubClass("some info"), Fixture.mockSubList(2, "some detail"));

        mockCreateBusinessEventLog(mockPayload);

        eventLogWriter.fireBusinessEvent(PUBLISHER_EVENT_TYPE, mockPayload);

        verify(eventLogRepository).persist(eventLogCapture.capture());

        assertEventLog(eventLogCapture.getValue(), EVENT_BODY_DATA_1);
    }

    @Test
    public void testFireBusinessEvents() throws Exception {
        MockPayload mockPayload1 = Fixture.mockPayload(1, "mockedcode1", true,
            Fixture.mockSubClass("some info 1_0"), Fixture.mockSubList(2, "some detail 1_2"));
        MockPayload mockPayload2 = Fixture.mockPayload(2, "mockedcode2", true,
            Fixture.mockSubClass("some info 2_0"), Fixture.mockSubList(2, "some detail 2_1"));

        mockCreateBusinessEventLog(mockPayload1);
        mockCreateBusinessEventLog(mockPayload2);

        eventLogWriter.fireBusinessEvents(PUBLISHER_EVENT_TYPE,
            Stream.of(mockPayload1, mockPayload2).collect(Collectors.toList()));

        verify(eventLogRepository).persist(eventLogsCapture.capture());

        Iterator<EventLog> eventLogIterator = eventLogsCapture.getValue().iterator();
        EventLog eventLog1 = eventLogIterator.next();
        assertEventLog(eventLog1, OBJECT_MAPPER.writeValueAsString(mockPayload1));

        EventLog eventLog2 = eventLogIterator.next();
        assertEventLog(eventLog2, OBJECT_MAPPER.writeValueAsString(mockPayload2));
    }

    private void verifyPersistedEventLogs(String expectedDataOp) {
        verify(eventLogRepository).persist(eventLogsCapture.capture());

        Collection<EventLog> eventLogs = eventLogsCapture.getValue();

        Optional<EventLog> firstEventLog = eventLogs.stream().filter(
            eventLog -> eventLog.getEventBodyData().contains("mockedcode1")).findFirst();
        assertThat(firstEventLog.isPresent(), is(true));
        assertDataEventLog(expectedDataOp, firstEventLog.get(), DATA_CHANGE_BODY_DATA_1);

        Optional<EventLog> secondEventLog = eventLogs.stream().filter(
            eventLog -> eventLog.getEventBodyData().contains("mockedcode2")).findFirst();
        assertThat(secondEventLog.isPresent(), is(true));
        assertDataEventLog(expectedDataOp, secondEventLog.get(), DATA_CHANGE_BODY_DATA_2);

        Optional<EventLog> thirdEventLog = eventLogs.stream().filter(
            eventLog -> eventLog.getEventBodyData().contains("mockedcode3")).findFirst();
        assertThat(thirdEventLog.isPresent(), is(true));
        assertDataEventLog(expectedDataOp, thirdEventLog.get(), DATA_CHANGE_BODY_DATA_3);
    }

    private void verifyPersistedDataEventLog(String expectedDataOp) {
        verify(eventLogRepository).persist(eventLogCapture.capture());
        EventLog storedEventLog = eventLogCapture.getValue();
        assertDataEventLog(expectedDataOp, storedEventLog, DATA_CHANGE_BODY_DATA_1);
    }

    private void assertDataEventLog(String expectedDataOp, EventLog storedEventLog, String bodyTemplate) {
        assertEventLog(storedEventLog, bodyTemplate.replace("{DATA_OP}", expectedDataOp));
    }

    private void assertEventLog(EventLog eventLog, String expectedBody) {
        assertThat(eventLog.getEventBodyData(), is(expectedBody));
        assertThat(eventLog.getEventType(), is(PUBLISHER_EVENT_TYPE));
        assertThat(eventLog.getFlowId(), is(TRACE_ID));
        assertThat(eventLog.getLockedBy(), is(nullValue()));
        assertThat(eventLog.getLockedUntil(), is(nullValue()));
        assertThat(eventLog.getEid(), is(EID));
        keyAsserter.accept(eventLog.getCompactionKey(), expectedBody);
    }

    private void mockCreateBusinessEventLog(Object payload) {
        String compactionKey =
            extractorList.isEmpty() ? null : extractorList.get(0).getKeyOrNull(payload);

        mockCreateEventLog(payload, compactionKey);
    }

    private void mockCreateEventLog(Object payload,
                                    EventDataOperation dataOp) {
        String compactionKey =
            extractorList.isEmpty() ? null : extractorList.get(0).getKeyOrNull(payload);

        DataChangeEventEnvelope payloadData =
            new DataChangeEventEnvelope(dataOp.toString(), PUBLISHER_DATA_TYPE_1, payload);

        mockCreateEventLog(payloadData, compactionKey);
    }

    private void mockCreateEventLog(Object payload, String compactionKey) {

        when(
            eventLogMapper.createEventLog(
                eq(PUBLISHER_EVENT_TYPE),
                eq(payload),
                eq(compactionKey))
        ).thenReturn(getEventLog(payload, compactionKey));
    }

    @SneakyThrows
    private EventLog getEventLog(Object payloadString, String compactionKey) {
        return EventLog.builder()
            .eventType(PUBLISHER_EVENT_TYPE)
            .eventBodyData(OBJECT_MAPPER.writeValueAsString(payloadString))
            .flowId(TRACE_ID)
            .compactionKey(compactionKey)
            .eid(EID)
            .build();
    }

}
