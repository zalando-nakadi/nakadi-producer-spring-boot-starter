package org.zalando.nakadiproducer.eventlog.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.zalando.nakadiproducer.util.Fixture.PUBLISHER_EVENT_TYPE;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import org.zalando.nakadiproducer.flowid.FlowIdComponent;
import org.zalando.nakadiproducer.util.Fixture;
import org.zalando.nakadiproducer.util.MockPayload;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class EventLogWriterTest {

  @Mock
  private EventLogRepository eventLogRepository;

  @Mock
  private FlowIdComponent flowIdComponent;

  @Captor
  private ArgumentCaptor<EventLog> eventLogCapture;

  @Captor
  private ArgumentCaptor<Collection<EventLog>> eventLogsCapture;

  private EventLogWriterImpl eventLogWriter;

  private MockPayload eventPayload1;
  private MockPayload eventPayload2;
  private MockPayload eventPayload3;

  private static final String TRACE_ID = "TRACE_ID";

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

  @BeforeEach
  public void setUp() {
    Mockito.reset(eventLogRepository, flowIdComponent);

    eventPayload1 = Fixture.mockPayload(1, "mockedcode1", true,
        Fixture.mockSubClass("some info"), Fixture.mockSubList(2, "some detail"));

    eventPayload2 = Fixture.mockPayload(2, "mockedcode2", true,
        Fixture.mockSubClass("some info"), Fixture.mockSubList(2, "some detail"));

    eventPayload3 = Fixture.mockPayload(3, "mockedcode3", true,
        Fixture.mockSubClass("some info"), Fixture.mockSubList(2, "some detail"));

    when(flowIdComponent.getXFlowIdValue()).thenReturn(TRACE_ID);

    eventLogWriter = new EventLogWriterImpl(eventLogRepository, new ObjectMapper(),
        flowIdComponent);
  }

  @Test
  public void testFireCreateEvent() {
    eventLogWriter.fireCreateEvent(PUBLISHER_EVENT_TYPE, PUBLISHER_DATA_TYPE_1, eventPayload1);

    verify(eventLogRepository).persist(eventLogCapture.capture());

    assertThat(eventLogCapture.getValue().getEventBodyData(), is(
        DATA_CHANGE_BODY_DATA_1.replace("{DATA_OP}", "C")));
    assertThat(eventLogCapture.getValue().getEventType(), is(PUBLISHER_EVENT_TYPE));
    assertThat(eventLogCapture.getValue().getFlowId(), is(TRACE_ID));
    assertThat(eventLogCapture.getValue().getLockedBy(), is(nullValue()));
    assertThat(eventLogCapture.getValue().getLockedUntil(), is(nullValue()));
  }

  @Test
  public void testFireCreateEvents() {
    eventLogWriter.fireCreateEvents(
        PUBLISHER_EVENT_TYPE,
        PUBLISHER_DATA_TYPE_1,
        Arrays.asList(eventPayload1, eventPayload2, eventPayload3)
    );
    verify(eventLogRepository).persist(eventLogsCapture.capture());

    verifyEventLogs("C", new HashSet<>(eventLogsCapture.getValue()));
  }

  @Test
  public void testFireUpdateEvent() {
    eventLogWriter.fireUpdateEvent(PUBLISHER_EVENT_TYPE, PUBLISHER_DATA_TYPE_1, eventPayload1);

    verify(eventLogRepository).persist(eventLogCapture.capture());

    assertThat(eventLogCapture.getValue().getEventBodyData(), is(
        DATA_CHANGE_BODY_DATA_1.replace("{DATA_OP}", "U")));
    assertThat(eventLogCapture.getValue().getEventType(), is(PUBLISHER_EVENT_TYPE));
    assertThat(eventLogCapture.getValue().getFlowId(), is(TRACE_ID));
    assertThat(eventLogCapture.getValue().getLockedBy(), is(nullValue()));
    assertThat(eventLogCapture.getValue().getLockedUntil(), is(nullValue()));
  }

  @Test
  public void testFireUpdateEvents() {
    eventLogWriter.fireUpdateEvents(
        PUBLISHER_EVENT_TYPE,
        PUBLISHER_DATA_TYPE_1,
        Arrays.asList(eventPayload1, eventPayload2, eventPayload3)
    );
    verify(eventLogRepository).persist(eventLogsCapture.capture());

    verifyEventLogs("U", new HashSet<>(eventLogsCapture.getValue()));
  }

  @Test
  public void testFireDeleteEvent() {
    eventLogWriter.fireDeleteEvent(PUBLISHER_EVENT_TYPE, PUBLISHER_DATA_TYPE_1, eventPayload1);

    verify(eventLogRepository).persist(eventLogCapture.capture());

    assertThat(eventLogCapture.getValue().getEventBodyData(), is(
        DATA_CHANGE_BODY_DATA_1.replace("{DATA_OP}", "D")));
    assertThat(eventLogCapture.getValue().getEventType(), is(PUBLISHER_EVENT_TYPE));
    assertThat(eventLogCapture.getValue().getFlowId(), is(TRACE_ID));
    assertThat(eventLogCapture.getValue().getLockedBy(), is(nullValue()));
    assertThat(eventLogCapture.getValue().getLockedUntil(), is(nullValue()));
  }

  @Test
  public void testFireDeleteEvents() {
    eventLogWriter.fireDeleteEvents(
        PUBLISHER_EVENT_TYPE,
        PUBLISHER_DATA_TYPE_1,
        Arrays.asList(eventPayload1, eventPayload2, eventPayload3));
    verify(eventLogRepository).persist(eventLogsCapture.capture());

    verifyEventLogs("D", new HashSet<>(eventLogsCapture.getValue()));
  }

  @Test
  public void testFireSnapshotEvent() throws Exception {
    eventLogWriter.fireSnapshotEvent(PUBLISHER_EVENT_TYPE, PUBLISHER_DATA_TYPE_1, eventPayload1);

    verify(eventLogRepository).persist(eventLogCapture.capture());

    assertThat(eventLogCapture.getValue().getEventBodyData(), is(
        DATA_CHANGE_BODY_DATA_1.replace("{DATA_OP}", "S")));
    assertThat(eventLogCapture.getValue().getEventType(), is(PUBLISHER_EVENT_TYPE));
    assertThat(eventLogCapture.getValue().getFlowId(), is(TRACE_ID));
    assertThat(eventLogCapture.getValue().getLockedBy(), is(nullValue()));
    assertThat(eventLogCapture.getValue().getLockedUntil(), is(nullValue()));
  }

  @Test
  public void testFireSnapshotEvents() {
    eventLogWriter.fireSnapshotEvents(
        PUBLISHER_EVENT_TYPE,
        PUBLISHER_DATA_TYPE_1,
        Arrays.asList(eventPayload1, eventPayload2, eventPayload3));
    verify(eventLogRepository).persist(eventLogsCapture.capture());

    verifyEventLogs("S", new HashSet<>(eventLogsCapture.getValue()));
  }

  @Test
  public void testFireBusinessEvent() throws Exception {
    MockPayload mockPayload = Fixture.mockPayload(1, "mockedcode1", true,
        Fixture.mockSubClass("some info"), Fixture.mockSubList(2, "some detail"));

    eventLogWriter.fireBusinessEvent(PUBLISHER_EVENT_TYPE, mockPayload);

    verify(eventLogRepository).persist(eventLogCapture.capture());

    assertThat(eventLogCapture.getValue().getEventBodyData(), is(EVENT_BODY_DATA_1));
    assertThat(eventLogCapture.getValue().getEventType(), is(PUBLISHER_EVENT_TYPE));
    assertThat(eventLogCapture.getValue().getFlowId(), is(TRACE_ID));
    assertThat(eventLogCapture.getValue().getLockedBy(), is(nullValue()));
    assertThat(eventLogCapture.getValue().getLockedUntil(), is(nullValue()));
  }

  @Test
  public void testFireBusinessEvents() throws Exception {
    MockPayload mockPayload1 = Fixture.mockPayload(1, "mockedcode1", true,
        Fixture.mockSubClass("some info 1_0"), Fixture.mockSubList(2, "some detail 1_2"));
    MockPayload mockPayload2 = Fixture.mockPayload(2, "mockedcode2", true,
        Fixture.mockSubClass("some info 2_0"), Fixture.mockSubList(2, "some detail 2_1"));

    eventLogWriter.fireBusinessEvents(PUBLISHER_EVENT_TYPE,
        Stream.of(mockPayload1, mockPayload2).collect(Collectors.toList()));

    verify(eventLogRepository).persist(eventLogsCapture.capture());

    Iterator<EventLog> eventLogIterator = eventLogsCapture.getValue().iterator();
    EventLog eventLog1 = eventLogIterator.next();
    assertThat(eventLog1.getEventBodyData(), is(OBJECT_MAPPER.writeValueAsString(mockPayload1)));
    assertThat(eventLog1.getEventType(), is(PUBLISHER_EVENT_TYPE));
    assertThat(eventLog1.getFlowId(), is(TRACE_ID));
    assertThat(eventLog1.getLockedBy(), is(nullValue()));
    assertThat(eventLog1.getLockedUntil(), is(nullValue()));

    EventLog eventLog2 = eventLogIterator.next();
    assertThat(eventLog2.getEventBodyData(), is(OBJECT_MAPPER.writeValueAsString(mockPayload2)));
    assertThat(eventLog2.getEventType(), is(PUBLISHER_EVENT_TYPE));
    assertThat(eventLog2.getFlowId(), is(TRACE_ID));
    assertThat(eventLog2.getLockedBy(), is(nullValue()));
    assertThat(eventLog2.getLockedUntil(), is(nullValue()));
  }

  private void verifyEventLogs(String dataOp, Set<EventLog> eventLogs) {
    Optional<EventLog> firstEventLog = eventLogs.stream().filter(
        eventLog -> eventLog.getEventBodyData().contains("mockedcode1")).findFirst();
    assertThat(firstEventLog.isPresent(), is(true));
    assertThat(firstEventLog.get().getEventBodyData(), is(
        DATA_CHANGE_BODY_DATA_1.replace("{DATA_OP}", dataOp)));
    assertThat(firstEventLog.get().getEventType(), is(PUBLISHER_EVENT_TYPE));
    assertThat(firstEventLog.get().getFlowId(), is(TRACE_ID));
    assertThat(firstEventLog.get().getLockedBy(), is(nullValue()));
    assertThat(firstEventLog.get().getLockedUntil(), is(nullValue()));

    Optional<EventLog> secondEventLog = eventLogs.stream().filter(
        eventLog -> eventLog.getEventBodyData().contains("mockedcode2")).findFirst();
    assertThat(secondEventLog.isPresent(), is(true));
    assertThat(secondEventLog.get().getEventBodyData(), is(
        DATA_CHANGE_BODY_DATA_2.replace("{DATA_OP}", dataOp)));
    assertThat(secondEventLog.get().getEventType(), is(PUBLISHER_EVENT_TYPE));
    assertThat(secondEventLog.get().getFlowId(), is(TRACE_ID));
    assertThat(secondEventLog.get().getLockedBy(), is(nullValue()));
    assertThat(secondEventLog.get().getLockedUntil(), is(nullValue()));

    Optional<EventLog> thirdEventLog = eventLogs.stream().filter(
        eventLog -> eventLog.getEventBodyData().contains("mockedcode3")).findFirst();
    assertThat(thirdEventLog.isPresent(), is(true));
    assertThat(thirdEventLog.get().getEventBodyData(), is(
        DATA_CHANGE_BODY_DATA_3.replace("{DATA_OP}", dataOp)));
    assertThat(thirdEventLog.get().getEventType(), is(PUBLISHER_EVENT_TYPE));
    assertThat(thirdEventLog.get().getFlowId(), is(TRACE_ID));
    assertThat(thirdEventLog.get().getLockedBy(), is(nullValue()));
    assertThat(thirdEventLog.get().getLockedUntil(), is(nullValue()));
  }

}
