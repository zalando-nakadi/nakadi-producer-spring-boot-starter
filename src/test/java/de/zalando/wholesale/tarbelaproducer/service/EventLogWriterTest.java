package de.zalando.wholesale.tarbelaproducer.service;

import de.zalando.wholesale.tarbelaproducer.persistance.entity.EventDataOperation;
import de.zalando.wholesale.tarbelaproducer.persistance.entity.EventLog;
import de.zalando.wholesale.tarbelaproducer.persistance.entity.EventStatus;
import de.zalando.wholesale.tarbelaproducer.persistance.repository.EventLogRepository;
import de.zalando.wholesale.tarbelaproducer.service.model.EventPayload;
import de.zalando.wholesale.tarbelaproducer.util.Fixture;
import de.zalando.wholesale.tarbelaproducer.util.MockPayload;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Random;

import static de.zalando.wholesale.tarbelaproducer.util.Fixture.PUBLISHER_DATA_TYPE;
import static de.zalando.wholesale.tarbelaproducer.util.Fixture.PUBLISHER_EVENT_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private final ArgumentCaptor<EventLog> saveArgCapture = ArgumentCaptor.forClass(EventLog.class);
    private final ArgumentCaptor<EventDataOperation> eventDataArgCapture = ArgumentCaptor.forClass(EventDataOperation.class);
    private final ArgumentCaptor<EventPayload> eventPayloadCapture = ArgumentCaptor.forClass(EventPayload.class);
    private final ArgumentCaptor<String> strArgCapture = ArgumentCaptor.forClass(String.class);

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

        verify(eventLogMapper, times(1)).createEventLog(eventDataArgCapture.capture(), eventPayloadCapture.capture(), strArgCapture.capture());
        assertThat(eventDataArgCapture.getValue(), is(EventDataOperation.CREATE));
        assertThat(eventPayloadCapture.getValue(), is(eventPayload));
        assertThat(strArgCapture.getValue(), is(traceId));

        verify(eventLogRepository, times(1)).save(saveArgCapture.capture());
        assertThat(saveArgCapture.getValue(), is(eventLog));

    }

    @Test
    public void testFireUpdateEvent() throws Exception {

        eventLogWriter.fireUpdateEvent(eventPayload, traceId);

        verify(eventLogMapper, times(1)).createEventLog(eventDataArgCapture.capture(), eventPayloadCapture.capture(), strArgCapture.capture());
        assertThat(eventDataArgCapture.getValue(), is(EventDataOperation.UPDATE));
        assertThat(eventPayloadCapture.getValue(), is(eventPayload));
        assertThat(strArgCapture.getValue(), is(traceId));

        verify(eventLogRepository, times(1)).save(saveArgCapture.capture());
        assertThat(saveArgCapture.getValue(), is(eventLog));

    }

}
