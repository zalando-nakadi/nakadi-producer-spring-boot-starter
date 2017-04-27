package org.zalando.tarbelaproducer.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.zalando.tarbelaproducer.persistance.entity.EventDataOperation;
import org.zalando.tarbelaproducer.persistance.entity.EventLog;
import org.zalando.tarbelaproducer.persistance.entity.EventStatus;
import org.zalando.tarbelaproducer.persistance.repository.EventLogRepository;
import org.zalando.tarbelaproducer.service.model.EventPayload;
import org.zalando.tarbelaproducer.util.Fixture;
import org.zalando.tarbelaproducer.util.MockPayload;

import java.util.Random;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.zalando.tarbelaproducer.util.Fixture.PUBLISHER_DATA_TYPE;
import static org.zalando.tarbelaproducer.util.Fixture.PUBLISHER_EVENT_TYPE;

@RunWith(MockitoJUnitRunner.class)
public class EventLogWriterTest {

    @Mock
    private EventLogRepository eventLogRepository;

    @Mock
    private EventLogMapper eventLogMapper;

    @InjectMocks
    private EventLogWriterImpl eventLogWriter;

    private EventPayload eventPayload;

    private String traceId;

    private EventLog eventLog;

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

        traceId = "TRACE_ID";

        final Random rand = new Random();
        eventLog = EventLog.builder().id(rand.nextInt()).eventBodyData(EVENT_BODY_DATA)
                .eventType(PUBLISHER_EVENT_TYPE)
                .dataType(PUBLISHER_DATA_TYPE)
                .dataOp(EventDataOperation.CREATE.toString())
                .status(EventStatus.NEW.name()).flowId("FLOW_ID").errorCount(0).build();

        when(eventLogMapper.createEventLog(any(), any(), any())).thenReturn(eventLog);

    }

    @Test
    public void testFireCreateEvent() throws Exception {

        eventLogWriter.fireCreateEvent(eventPayload, traceId);

        verify(eventLogMapper).createEventLog(eq(EventDataOperation.CREATE), eq(eventPayload), eq(traceId));

        verify(eventLogRepository).save(eq(eventLog));

    }

    @Test
    public void testFireUpdateEvent() throws Exception {

        eventLogWriter.fireUpdateEvent(eventPayload, traceId);

        verify(eventLogMapper).createEventLog(eq(EventDataOperation.UPDATE), eq(eventPayload), eq(traceId));

        verify(eventLogRepository).save(eq(eventLog));

    }

    @Test
    public void testFireDeleteEvent() throws Exception {

        eventLogWriter.fireDeleteEvent(eventPayload, traceId);

        verify(eventLogMapper).createEventLog(eq(EventDataOperation.DELETE), eq(eventPayload), eq(traceId));

        verify(eventLogRepository).save(eq(eventLog));

    }

}
