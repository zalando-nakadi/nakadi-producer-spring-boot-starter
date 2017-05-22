package org.zalando.nakadiproducer.eventlog.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.zalando.nakadiproducer.util.Fixture.PUBLISHER_EVENT_TYPE;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
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

}
