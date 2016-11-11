package de.zalando.wholesale.tarbelaproducer.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.zalando.wholesale.tarbelaproducer.TarbelaProperties;
import de.zalando.wholesale.tarbelaproducer.TarbelaSnapshotProvider;
import de.zalando.wholesale.tarbelaproducer.api.event.model.BunchOfEventUpdatesDTO;
import de.zalando.wholesale.tarbelaproducer.api.event.model.BunchOfEventsDTO;
import de.zalando.wholesale.tarbelaproducer.api.event.model.EventUpdateDTO;
import de.zalando.wholesale.tarbelaproducer.persistance.entity.EventDataOperation;
import de.zalando.wholesale.tarbelaproducer.persistance.entity.EventLog;
import de.zalando.wholesale.tarbelaproducer.persistance.entity.EventStatus;
import de.zalando.wholesale.tarbelaproducer.persistance.repository.EventLogRepository;
import de.zalando.wholesale.tarbelaproducer.service.exception.InvalidCursorException;
import de.zalando.wholesale.tarbelaproducer.service.exception.InvalidEventIdException;
import de.zalando.wholesale.tarbelaproducer.service.exception.UnknownEventIdException;
import de.zalando.wholesale.tarbelaproducer.service.exception.ValidationException;
import de.zalando.wholesale.tarbelaproducer.service.model.EventPayload;
import de.zalando.wholesale.tarbelaproducer.util.Fixture;
import de.zalando.wholesale.tarbelaproducer.util.MockPayload;
import de.zalando.wholesale.tarbelaproducer.web.FlowIdComponent;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static de.zalando.wholesale.tarbelaproducer.service.EventLogServiceImpl.DEFAULT_LIMIT;
import static de.zalando.wholesale.tarbelaproducer.util.Fixture.PUBLISHER_DATA_TYPE;
import static de.zalando.wholesale.tarbelaproducer.util.Fixture.PUBLISHER_EVENT_TYPE;
import static de.zalando.wholesale.tarbelaproducer.util.Fixture.SINK_ID;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EventLogServiceTest {

    public static final int CURSOR = 0;
    public static final int LIMIT = 5;
    public static final int SNAPSHOT_BATCH_SIZE = 5;

    @Mock
    private EventLogRepository eventLogRepository;

    @Mock
    private EventLogMapper eventLogMapper;

    @Mock
    private FlowIdComponent flowIdComponent;

    @Mock
    private TarbelaProperties tarbelaProperties;

    @Mock
    private TarbelaSnapshotProvider tarbelaSnapshotProvider;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Spy
    private EventLogServiceImpl.ValidationErrorMessages errorMessages = new EventLogServiceImpl.ValidationErrorMessages();

    @InjectMocks
    private EventLogServiceImpl eventLogService;

    private MockPayload mockPayload;

    private EventPayload eventPayload;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Captor
    private ArgumentCaptor<List<EventLog>> listEventLogCaptor;

    //J-
    private static final String EVENT_BODY_DATA =
            ("{'id':1,"
            + "'code':'mockedcode',"
            + "'more':{'info':'some info'},"
            + "'items':[{'detail':'some detail0'},{'detail':'some detail1'}],"
            + "'active':true"
            + "}").replace('\'', '"');
    //J+

    private String traceId;
    private BunchOfEventsDTO bunchOfEventsDTO;
    private List<EventLog> events;
    private EventLog eventLog;

    @Before
    public void setUp() throws Exception {

        when(tarbelaProperties.getSinkId()).thenReturn(SINK_ID);
        when(tarbelaProperties.getSnapshotBatchSize()).thenReturn(SNAPSHOT_BATCH_SIZE);

        mockPayload = Fixture.mockPayload(1, "mockedcode", true,
                Fixture.mockSubClass("some info"), Fixture.mockSubList(2, "some detail"));

        eventPayload = Fixture.mockEventPayload(mockPayload);

        final Random rand = new Random();
        eventLog = EventLog.builder().id(rand.nextInt()).eventBodyData(EVENT_BODY_DATA)
                 .eventType(PUBLISHER_EVENT_TYPE)
                 .dataType(PUBLISHER_DATA_TYPE)
                 .dataOp(EventDataOperation.CREATE.toString())
                 .status(EventStatus.NEW.name()).flowId("FLOW_ID").errorCount(0).build();

        traceId = "TRACE_ID";
        when(flowIdComponent.getXFlowIdValue()).thenReturn(traceId);

        events = newArrayList(eventLog);

        bunchOfEventsDTO = new BunchOfEventsDTO();
    }

    @Test
    public void testSearchEvents() throws Exception {
        when(eventLogRepository.search(CURSOR, EventStatus.NEW.toString(), LIMIT)).thenReturn(events);

        when(eventLogMapper.mapToDTO(events, EventStatus.NEW.toString(), LIMIT, SINK_ID))
                .thenReturn(bunchOfEventsDTO);

        final BunchOfEventsDTO result = eventLogService.searchEvents(String.valueOf(CURSOR),
                EventStatus.NEW.toString(), LIMIT);
        assertThat(result, is(bunchOfEventsDTO));
    }

    @Test
    public void testSearchEventsWithoutLimit() throws Exception {
        when(eventLogRepository.search(CURSOR, EventStatus.NEW.name(), DEFAULT_LIMIT)).thenReturn(events);
        when(eventLogMapper.mapToDTO(events, EventStatus.NEW.name(), null, SINK_ID))
                .thenReturn(bunchOfEventsDTO);

        final BunchOfEventsDTO result = eventLogService.searchEvents(String.valueOf(CURSOR),
                EventStatus.NEW.toString(), null);

        assertThat(result, is(bunchOfEventsDTO));
    }

    @Test(expected = InvalidCursorException.class)
    public void testSearchEventsWithInvalidCursor() throws Exception {
        eventLogService.searchEvents("a1", EventStatus.NEW.toString(), DEFAULT_LIMIT);
    }

    @Test
    public void testUpdateEvents() {

        when(eventLogRepository.findByIdIn(newArrayList(eventLog.getId()))).thenReturn(newArrayList(
                eventLog));

        final EventUpdateDTO updateEventDTO = new EventUpdateDTO();
        updateEventDTO.setEventId(eventLog.getId().toString());
        updateEventDTO.setDeliveryStatus(EventStatus.SENT.name());

        final BunchOfEventUpdatesDTO updates = new BunchOfEventUpdatesDTO();
        updates.setEvents(newArrayList(updateEventDTO));

        eventLogService.updateEvents(updates);

        verify(eventLogRepository).save(newArrayList(eventLog));
        assertThat(eventLog.getStatus(), is(EventStatus.SENT.name()));
    }

    @Test
    public void testUpdateEventsWithError() {

        when(eventLogRepository.findByIdIn(newArrayList(eventLog.getId()))).thenReturn(newArrayList(
                eventLog));

        final EventUpdateDTO updateEventDTO = new EventUpdateDTO();
        updateEventDTO.setEventId(eventLog.getId().toString());
        updateEventDTO.setDeliveryStatus(EventStatus.ERROR.name());

        final BunchOfEventUpdatesDTO updates = new BunchOfEventUpdatesDTO();
        updates.setEvents(newArrayList(updateEventDTO));

        eventLogService.updateEvents(updates);

        verify(eventLogRepository).save(newArrayList(eventLog));
        assertThat(eventLog.getStatus(), is(EventStatus.ERROR.name()));
        assertThat(eventLog.getErrorCount(), is(1));
    }

    @Test
    public void testUpdateEventsNullEventID() {
        final EventUpdateDTO updateEventDTO = new EventUpdateDTO();
        updateEventDTO.setEventId(null);
        updateEventDTO.setDeliveryStatus(EventStatus.SENT.name());

        final BunchOfEventUpdatesDTO updates = new BunchOfEventUpdatesDTO();
        updates.setEvents(newArrayList(updateEventDTO));

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("events.event_id");

        eventLogService.updateEvents(updates);
    }

    @Test
    public void testUpdateEventsEmptyDeliveryStatus() {
        final EventUpdateDTO updateEventDTO = new EventUpdateDTO();
        updateEventDTO.setEventId(eventLog.getId().toString());
        updateEventDTO.setDeliveryStatus("");

        final BunchOfEventUpdatesDTO updates = new BunchOfEventUpdatesDTO();
        updates.setEvents(newArrayList(updateEventDTO));

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("events.delivery_status");

        eventLogService.updateEvents(updates);
    }

    @Test
    public void testUpdateEventsNullDeliveryStatusAndEmptyEventId() {
        final EventUpdateDTO updateEventDTO = new EventUpdateDTO();
        updateEventDTO.setEventId("");
        updateEventDTO.setDeliveryStatus(null);

        final BunchOfEventUpdatesDTO updates = new BunchOfEventUpdatesDTO();
        updates.setEvents(newArrayList(updateEventDTO));

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("events.event_id");
        expectedException.expectMessage("events.delivery_status");

        eventLogService.updateEvents(updates);
    }

    @Test
    public void testUpdateEventsNonNumericEventId() {
        final EventUpdateDTO updateEventDTO = new EventUpdateDTO();
        final String nonNumericEventId = "abcd";
        updateEventDTO.setEventId(nonNumericEventId);
        updateEventDTO.setDeliveryStatus(EventStatus.SENT.name());

        final BunchOfEventUpdatesDTO updates = new BunchOfEventUpdatesDTO();
        updates.setEvents(newArrayList(updateEventDTO));

        expectedException.expect(InvalidEventIdException.class);
        expectedException.expectMessage(nonNumericEventId);

        eventLogService.updateEvents(updates);
    }

    @Test
    public void testUpdateEventsNonExistingEventId() {

        final Integer eventId = eventLog.getId();
        when(eventLogRepository.findByIdIn(newArrayList(eventId))).thenReturn(emptyList());

        final EventUpdateDTO updateEventDTO = new EventUpdateDTO();
        updateEventDTO.setEventId(eventId.toString());
        updateEventDTO.setDeliveryStatus(EventStatus.SENT.name());

        final BunchOfEventUpdatesDTO updates = new BunchOfEventUpdatesDTO();
        updates.setEvents(newArrayList(updateEventDTO));

        expectedException.expect(UnknownEventIdException.class);
        expectedException.expectMessage(eventId.toString());

        eventLogService.updateEvents(updates);
    }

    @Test
    public void testCreateSnapshotEvents() {

        final List<EventPayload> eventPayloads = Collections.singletonList(eventPayload);

        EventLog eventLog = EventLog.builder().id(123).eventBodyData(EVENT_BODY_DATA)
                .eventType(PUBLISHER_EVENT_TYPE)
                .dataType(PUBLISHER_DATA_TYPE)
                .dataOp(EventDataOperation.SNAPSHOT.toString())
                .status(EventStatus.NEW.name()).flowId("FLOW_ID").errorCount(0).build();

        when(tarbelaSnapshotProvider.getSnapshot(PUBLISHER_EVENT_TYPE)).thenReturn(eventPayloads.stream());
        when(eventLogMapper.createEventLog(any(), any(), any())).thenReturn(eventLog);

        eventLogService.createSnapshotEvents(PUBLISHER_EVENT_TYPE, traceId);

        verify(eventLogRepository).save(listEventLogCaptor.capture());

        assertThat(listEventLogCaptor.getValue().size(), is(1));

        final EventLog snapshotEventLog = listEventLogCaptor.getValue().get(0);

        assertThat(snapshotEventLog.getDataOp(), is(EventDataOperation.SNAPSHOT.toString()));
        assertThat(snapshotEventLog.getStatus(), is(EventStatus.NEW.toString()));
        assertThat(snapshotEventLog.getEventBodyData(), is(EVENT_BODY_DATA));
    }

    @Test
    public void testSnapshotSavedInBatches() {

        final List<MockPayload> mockPayloadList = Fixture.mockPayloadList(5);

        final Stream<EventPayload> eventPayloadsStream = mockPayloadList.stream()
                .map(Fixture::mockEventPayload);

        // when snapshot returns 5 item stream
        when(tarbelaSnapshotProvider.getSnapshot(PUBLISHER_EVENT_TYPE)).thenReturn(eventPayloadsStream);
        // and the size of a batch is 3
        when(tarbelaProperties.getSnapshotBatchSize()).thenReturn(3);

        // create a snapshot
        eventLogService.createSnapshotEvents(PUBLISHER_EVENT_TYPE, traceId);

        // verify that that save got called twice
        verify(eventLogRepository, times(2)).save(listEventLogCaptor.capture());

        // verify that stream was split in two batches
        assertThat(listEventLogCaptor.getAllValues().size(), is(2));
        // verify that size of the first batch is 3
        assertThat(listEventLogCaptor.getAllValues().get(0).size(), is(3));
        // verify that size of the last batch is 2
        assertThat(listEventLogCaptor.getAllValues().get(1).size(), is(2));

    }

}
