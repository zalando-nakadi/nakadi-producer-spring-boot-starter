package de.zalando.wholesale.tarbelaevents.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.zalando.wholesale.tarbelaevents.TarbelaProperties;
import de.zalando.wholesale.tarbelaevents.TarbelaSnapshotProvider;
import de.zalando.wholesale.tarbelaevents.api.event.model.BunchOfEventUpdatesDTO;
import de.zalando.wholesale.tarbelaevents.api.event.model.BunchOfEventsDTO;
import de.zalando.wholesale.tarbelaevents.api.event.model.EventUpdateDTO;
import de.zalando.wholesale.tarbelaevents.persistance.entity.EventDataOperation;
import de.zalando.wholesale.tarbelaevents.persistance.entity.EventLog;
import de.zalando.wholesale.tarbelaevents.persistance.entity.EventStatus;
import de.zalando.wholesale.tarbelaevents.persistance.repository.EventLogRepository;
import de.zalando.wholesale.tarbelaevents.service.exception.InvalidCursorException;
import de.zalando.wholesale.tarbelaevents.service.exception.InvalidEventIdException;
import de.zalando.wholesale.tarbelaevents.service.exception.UnknownEventIdException;
import de.zalando.wholesale.tarbelaevents.service.exception.ValidationException;
import de.zalando.wholesale.tarbelaevents.util.Fixture;
import de.zalando.wholesale.tarbelaevents.util.MockPayload;
import de.zalando.wholesale.tarbelaevents.web.FlowIdComponent;

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

import static com.google.common.collect.Lists.newArrayList;
import static de.zalando.wholesale.tarbelaevents.service.EventLogServiceImpl.DEFAULT_LIMIT;
import static de.zalando.wholesale.tarbelaevents.util.Fixture.PUBLISHER_DATA_TYPE;
import static de.zalando.wholesale.tarbelaevents.util.Fixture.PUBLISHER_EVENT_TYPE;
import static de.zalando.wholesale.tarbelaevents.util.Fixture.SINK_ID;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
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

        when(tarbelaProperties.getDataType()).thenReturn(PUBLISHER_DATA_TYPE);
        when(tarbelaProperties.getEventType()).thenReturn(PUBLISHER_EVENT_TYPE);
        when(tarbelaProperties.getSinkId()).thenReturn(SINK_ID);
        when(tarbelaProperties.getSnapshotBatchSize()).thenReturn(SNAPSHOT_BATCH_SIZE);

        mockPayload = Fixture.mockPayload(1, "mockedcode", true,
                Fixture.mockSubClass("some info"), Fixture.mockSubList(2, "some detail"));

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
    public void testFireCreateEvent() throws Exception {
        final ArgumentCaptor<EventLog> argumentCaptor = ArgumentCaptor.forClass(EventLog.class);
        eventLogService.fireCreateEvent(mockPayload, traceId);
        verify(eventLogRepository, times(1)).save(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getDataOp(), is(EventDataOperation.CREATE.toString()));
    }

    @Test
    public void testFireUpdateEvent() throws Exception {
        final ArgumentCaptor<EventLog> argumentCaptor = ArgumentCaptor.forClass(EventLog.class);
        eventLogService.fireUpdateEvent(mockPayload, traceId);
        verify(eventLogRepository, times(1)).save(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getDataOp(), is(EventDataOperation.UPDATE.toString()));
    }

    @Test
    public void testCreateWarehouseEventLog() throws Exception {
        final EventLog eventLog = eventLogService.createEventLog(
                EventDataOperation.UPDATE, mockPayload, traceId);
        assertThat(eventLog.getEventBodyData(), is(EVENT_BODY_DATA));
        assertThat(eventLog.getDataOp(), is(EventDataOperation.UPDATE.toString()));
        assertThat(eventLog.getEventType(), is(PUBLISHER_EVENT_TYPE));
        assertThat(eventLog.getDataType(), is(PUBLISHER_DATA_TYPE));
        assertThat(eventLog.getStatus(), is(EventStatus.NEW.toString()));
        assertThat(eventLog.getFlowId(), is(traceId));
    }

    @Test
    public void testSearchEvents() throws Exception {
        when(eventLogRepository.search(CURSOR, EventStatus.NEW.toString(), LIMIT)).thenReturn(events);

        when(eventLogMapper.mapToDTO(events, EventStatus.NEW.toString(), LIMIT, PUBLISHER_EVENT_TYPE, SINK_ID))
                .thenReturn(bunchOfEventsDTO);

        final BunchOfEventsDTO result = eventLogService.searchEvents(String.valueOf(CURSOR),
                EventStatus.NEW.toString(), LIMIT);
        assertThat(result, is(bunchOfEventsDTO));
    }

    @Test
    public void testSearchEventsWithoutLimit() throws Exception {
        when(eventLogRepository.search(CURSOR, EventStatus.NEW.name(), DEFAULT_LIMIT)).thenReturn(events);
        when(eventLogMapper.mapToDTO(events, EventStatus.NEW.name(), null, PUBLISHER_EVENT_TYPE, SINK_ID))
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

        final List<?> mockPayloadList = Collections.singletonList(mockPayload);

        when(tarbelaSnapshotProvider.getSnapshot()).thenReturn(mockPayloadList.stream());

        eventLogService.createSnapshotEvents(traceId);

        verify(eventLogRepository).save(listEventLogCaptor.capture());

        assertThat(listEventLogCaptor.getValue().size(), is(1));

        final EventLog snapshotEventLog = listEventLogCaptor.getValue().get(0);

        assertThat(snapshotEventLog.getDataOp(), is(EventDataOperation.SNAPSHOT.toString()));
        assertThat(snapshotEventLog.getStatus(), is(EventStatus.NEW.toString()));
        assertThat(snapshotEventLog.getEventBodyData(), is(EVENT_BODY_DATA));
    }

    @Test
    public void testSnapshotSavedInBatches() {

        final List<?> mockPayloadList = Fixture.mockPayloadList(5);

        // when snapshot returns 5 item stream
        when(tarbelaSnapshotProvider.getSnapshot()).thenReturn(mockPayloadList.stream());
        // and the size of a batch is 3
        when(tarbelaProperties.getSnapshotBatchSize()).thenReturn(3);

        // create a snapshot
        eventLogService.createSnapshotEvents(traceId);

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
