package org.zalando.nakadiproducer.eventlog.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.zalando.nakadiproducer.util.Fixture.PUBLISHER_DATA_TYPE;
import static org.zalando.nakadiproducer.util.Fixture.PUBLISHER_EVENT_TYPE;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.zalando.nakadiproducer.FlowIdComponent;
import org.zalando.nakadiproducer.eventlog.EventPayload;
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

    private EventPayload eventPayload;

    private static final String TRACE_ID = "TRACE_ID";

    private static final String EVENT_BODY_DATA =
            ("{'id':1,"
                    + "'code':'mockedcode',"
                    + "'more':{'info':'some info'},"
                    + "'items':[{'detail':'some detail0'},{'detail':'some detail1'}],"
                    + "'active':true"
                    + "}").replace('\'', '"');

    @Before
    public void setUp() throws Exception {

        MockPayload mockPayload = Fixture.mockPayload(1, "mockedcode", true,
                Fixture.mockSubClass("some info"), Fixture.mockSubList(2, "some detail"));

        eventPayload = Fixture.mockEventPayload(mockPayload);

        when(flowIdComponent.getXFlowIdValue()).thenReturn(TRACE_ID);

        eventLogWriter = new EventLogWriterImpl(eventLogRepository, new ObjectMapper(), flowIdComponent);
    }

    @Test
    public void testFireCreateEvent() throws Exception {

        eventLogWriter.fireCreateEvent(eventPayload);
        verify(eventLogRepository).save(eventLogCapture.capture());

        assertThat(eventLogCapture.getValue().getDataOp(), is("C"));
        assertThat(eventLogCapture.getValue().getDataType(), is(PUBLISHER_DATA_TYPE));
        assertThat(eventLogCapture.getValue().getEventBodyData(), is(EVENT_BODY_DATA));
        assertThat(eventLogCapture.getValue().getEventType(), is(PUBLISHER_EVENT_TYPE));
        assertThat(eventLogCapture.getValue().getFlowId(), is(TRACE_ID));
        assertThat(eventLogCapture.getValue().getLockedBy(), is(nullValue()));
        assertThat(eventLogCapture.getValue().getLockedUntil(), is(nullValue()));

    }

    @Test
    public void testFireUpdateEvent() throws Exception {

        eventLogWriter.fireUpdateEvent(eventPayload);

        verify(eventLogRepository).save(eventLogCapture.capture());

        assertThat(eventLogCapture.getValue().getDataOp(), is("U"));
        assertThat(eventLogCapture.getValue().getDataType(), is(PUBLISHER_DATA_TYPE));
        assertThat(eventLogCapture.getValue().getEventBodyData(), is(EVENT_BODY_DATA));
        assertThat(eventLogCapture.getValue().getEventType(), is(PUBLISHER_EVENT_TYPE));
        assertThat(eventLogCapture.getValue().getFlowId(), is(TRACE_ID));
        assertThat(eventLogCapture.getValue().getLockedBy(), is(nullValue()));
        assertThat(eventLogCapture.getValue().getLockedUntil(), is(nullValue()));

    }

    @Test
    public void testFireDeleteEvent() throws Exception {

        eventLogWriter.fireDeleteEvent(eventPayload);

        verify(eventLogRepository).save(eventLogCapture.capture());

        assertThat(eventLogCapture.getValue().getDataOp(), is("D"));
        assertThat(eventLogCapture.getValue().getDataType(), is(PUBLISHER_DATA_TYPE));
        assertThat(eventLogCapture.getValue().getEventBodyData(), is(EVENT_BODY_DATA));
        assertThat(eventLogCapture.getValue().getEventType(), is(PUBLISHER_EVENT_TYPE));
        assertThat(eventLogCapture.getValue().getFlowId(), is(TRACE_ID));
        assertThat(eventLogCapture.getValue().getLockedBy(), is(nullValue()));
        assertThat(eventLogCapture.getValue().getLockedUntil(), is(nullValue()));
    }

    @Test
    public void testFireSnapshotEvent() throws Exception {

        eventLogWriter.fireSnapshotEvent(eventPayload);

        verify(eventLogRepository).save(eventLogCapture.capture());

        assertThat(eventLogCapture.getValue().getDataOp(), is("S"));
        assertThat(eventLogCapture.getValue().getDataType(), is(PUBLISHER_DATA_TYPE));
        assertThat(eventLogCapture.getValue().getEventBodyData(), is(EVENT_BODY_DATA));
        assertThat(eventLogCapture.getValue().getEventType(), is(PUBLISHER_EVENT_TYPE));
        assertThat(eventLogCapture.getValue().getFlowId(), is(TRACE_ID));
        assertThat(eventLogCapture.getValue().getLockedBy(), is(nullValue()));
        assertThat(eventLogCapture.getValue().getLockedUntil(), is(nullValue()));
    }

}
