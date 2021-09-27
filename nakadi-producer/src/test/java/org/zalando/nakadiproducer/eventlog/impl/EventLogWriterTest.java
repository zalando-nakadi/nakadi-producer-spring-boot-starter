package org.zalando.nakadiproducer.eventlog.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.zalando.nakadiproducer.util.Fixture.PUBLISHER_EVENT_TYPE;

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.zalando.nakadiproducer.flowid.FlowIdComponent;
import org.zalando.nakadiproducer.util.Fixture;
import org.zalando.nakadiproducer.util.MockPayload;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(MockitoJUnitRunner.class)
public class EventLogWriterTest {

    @Mock
    private EventLogRepository eventLogRepository;

    @Mock
    private FlowIdComponent flowIdComponent;

    @Captor
    private ArgumentCaptor<EventLog> eventLogCapture;

    @Captor
    private ArgumentCaptor<Collection<EventLog>> eventLogCaptures;

    private EventLogWriterImpl eventLogWriter;

    private MockPayload eventPayload;

    private static final String TRACE_ID = "TRACE_ID";

    private static final String EVENT_BODY_DATA =
            ("{'id':1,"
                    + "'code':'mockedcode',"
                    + "'more':{'info':'some info'},"
                    + "'items':[{'detail':'some detail0'},{'detail':'some detail1'}],"
                    + "'active':true"
                    + "}").replace('\'', '"');

    private static final String DATA_CHANGE_BODY_DATA = ("{'data_op':'{DATA_OP}','data_type':'nakadi:some-publisher','data':" + EVENT_BODY_DATA + "}").replace('\'', '"');
    private static final String PUBLISHER_DATA_TYPE = "nakadi:some-publisher";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Before
    public void setUp() throws Exception {
        eventPayload = Fixture.mockPayload(1, "mockedcode", true,
                Fixture.mockSubClass("some info"), Fixture.mockSubList(2, "some detail"));

        when(flowIdComponent.getXFlowIdValue()).thenReturn(TRACE_ID);

        eventLogWriter = new EventLogWriterImpl(eventLogRepository, new ObjectMapper(), flowIdComponent);
    }

    @Test
    public void testFireCreateEvent() throws Exception {
        eventLogWriter.fireCreateEvent(PUBLISHER_EVENT_TYPE, PUBLISHER_DATA_TYPE, eventPayload);
        verify(eventLogRepository).persist(eventLogCapture.capture());

        assertThat(eventLogCapture.getValue().getEventBodyData(), is(DATA_CHANGE_BODY_DATA.replace("{DATA_OP}", "C")));
        assertThat(eventLogCapture.getValue().getEventType(), is(PUBLISHER_EVENT_TYPE));
        assertThat(eventLogCapture.getValue().getFlowId(), is(TRACE_ID));
        assertThat(eventLogCapture.getValue().getLockedBy(), is(nullValue()));
        assertThat(eventLogCapture.getValue().getLockedUntil(), is(nullValue()));

    }

    @Test
    public void testFireUpdateEvent() throws Exception {

        eventLogWriter.fireUpdateEvent(PUBLISHER_EVENT_TYPE, PUBLISHER_DATA_TYPE, eventPayload);

        verify(eventLogRepository).persist(eventLogCapture.capture());

        assertThat(eventLogCapture.getValue().getEventBodyData(), is(DATA_CHANGE_BODY_DATA.replace("{DATA_OP}", "U")));
        assertThat(eventLogCapture.getValue().getEventType(), is(PUBLISHER_EVENT_TYPE));
        assertThat(eventLogCapture.getValue().getFlowId(), is(TRACE_ID));
        assertThat(eventLogCapture.getValue().getLockedBy(), is(nullValue()));
        assertThat(eventLogCapture.getValue().getLockedUntil(), is(nullValue()));

    }

    @Test
    public void testFireDeleteEvent() throws Exception {

        eventLogWriter.fireDeleteEvent(PUBLISHER_EVENT_TYPE, PUBLISHER_DATA_TYPE, eventPayload);

        verify(eventLogRepository).persist(eventLogCapture.capture());

        assertThat(eventLogCapture.getValue().getEventBodyData(), is(DATA_CHANGE_BODY_DATA.replace("{DATA_OP}", "D")));
        assertThat(eventLogCapture.getValue().getEventType(), is(PUBLISHER_EVENT_TYPE));
        assertThat(eventLogCapture.getValue().getFlowId(), is(TRACE_ID));
        assertThat(eventLogCapture.getValue().getLockedBy(), is(nullValue()));
        assertThat(eventLogCapture.getValue().getLockedUntil(), is(nullValue()));
    }

    @Test
    public void testFireSnapshotEvent() throws Exception {

        eventLogWriter.fireSnapshotEvent(PUBLISHER_EVENT_TYPE, PUBLISHER_DATA_TYPE, eventPayload);

        verify(eventLogRepository).persist(eventLogCapture.capture());

        assertThat(eventLogCapture.getValue().getEventBodyData(), is(DATA_CHANGE_BODY_DATA.replace("{DATA_OP}", "S")));
        assertThat(eventLogCapture.getValue().getEventType(), is(PUBLISHER_EVENT_TYPE));
        assertThat(eventLogCapture.getValue().getFlowId(), is(TRACE_ID));
        assertThat(eventLogCapture.getValue().getLockedBy(), is(nullValue()));
        assertThat(eventLogCapture.getValue().getLockedUntil(), is(nullValue()));
    }

    @Test
    public void testFireBusinessEvent() throws Exception {
        MockPayload mockPayload = Fixture.mockPayload(1, "mockedcode", true,
                Fixture.mockSubClass("some info"), Fixture.mockSubList(2, "some detail"));

        eventLogWriter.fireBusinessEvent(PUBLISHER_EVENT_TYPE, mockPayload);

        verify(eventLogRepository).persist(eventLogCapture.capture());

        assertThat(eventLogCapture.getValue().getEventBodyData(), is(EVENT_BODY_DATA));
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

      verify(eventLogRepository).persist(eventLogCaptures.capture());

      Iterator<EventLog> eventLogIterator = eventLogCaptures.getValue().iterator();
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

}
