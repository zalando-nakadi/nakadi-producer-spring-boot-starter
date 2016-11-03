package de.zalando.wholesale.tarbelaevents.web;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.zalando.wholesale.tarbelaevents.TarbelaSnapshotProvider;
import de.zalando.wholesale.tarbelaevents.TarbelaSnapshotProviderNotImplemented;
import de.zalando.wholesale.tarbelaevents.api.event.model.BunchOfEventUpdatesDTO;
import de.zalando.wholesale.tarbelaevents.api.event.model.BunchOfEventsDTO;
import de.zalando.wholesale.tarbelaevents.api.event.model.BunchofEventsLinksDTO;
import de.zalando.wholesale.tarbelaevents.api.event.model.BunchofEventsLinksNextDTO;
import de.zalando.wholesale.tarbelaevents.api.event.model.EventDTO;
import de.zalando.wholesale.tarbelaevents.persistance.entity.EventStatus;
import de.zalando.wholesale.tarbelaevents.service.EventLogService;
import de.zalando.wholesale.tarbelaevents.service.exception.InvalidCursorException;
import de.zalando.wholesale.tarbelaevents.service.exception.InvalidEventIdException;
import de.zalando.wholesale.tarbelaevents.service.exception.UnknownEventIdException;
import de.zalando.wholesale.tarbelaevents.service.exception.ValidationException;
import de.zalando.wholesale.tarbelaevents.util.Fixture;
import de.zalando.wholesale.tarbelaevents.util.MockPayload;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.google.common.collect.Lists.newArrayList;
import static de.zalando.wholesale.tarbelaevents.web.EventController.CONTENT_TYPE_EVENT_LIST;
import static de.zalando.wholesale.tarbelaevents.web.EventController.CONTENT_TYPE_EVENT_LIST_UPDATE;
import static de.zalando.wholesale.tarbelaevents.web.EventController.CONTENT_TYPE_PROBLEM;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = { MockServletContext.class }
)
public class EventControllerTest {

    private static final String CURSOR = "1";
    private static final Integer LIMIT = 5;
    private static final String NEXT_LINK = "https://NEXT_LINK";

    @Mock
    private EventLogService eventLogService;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();
    private BunchOfEventsDTO eventsDTO;

    @Mock
    private FlowIdComponent flowIdComponent;

    @Mock
    private TarbelaSnapshotProvider<MockPayload> tarbelaSnapshotProvider;
    private static final String FLOW_ID_VALUE = "A_FUNKY_TRACER_VALUE";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(flowIdComponent.getXFlowIdKey()).thenReturn("X-Flow-ID");
        when(flowIdComponent.getXFlowIdValue()).thenReturn(FLOW_ID_VALUE);
        when(tarbelaSnapshotProvider.getSnapshot()).thenReturn(Fixture.mockPayloadList(6));

        final EventController eventController = new EventController(eventLogService, tarbelaSnapshotProvider, flowIdComponent);
        mockMvc = MockMvcBuilders.standaloneSetup(eventController).setControllerAdvice(new EventExceptionHandlerAdvice(flowIdComponent))
                                 .build();

        final BunchofEventsLinksNextDTO nextDTO = new BunchofEventsLinksNextDTO();
        nextDTO.setHref(NEXT_LINK);

        final BunchofEventsLinksDTO linksDTO = new BunchofEventsLinksDTO();
        linksDTO.setNext(nextDTO);

        eventsDTO = new BunchOfEventsDTO();
        eventsDTO.setLinks(linksDTO);

        eventsDTO.setEvents(newArrayList(new EventDTO(), new EventDTO()));
    }

    @Test
    public void testGetEvents() throws Exception {
        when(eventLogService.searchEvents(eq(CURSOR), eq(EventStatus.NEW.name()), eq(LIMIT))).thenReturn(eventsDTO);

        mockMvc.perform(MockMvcRequestBuilders.get("/events")
                .param("cursor", CURSOR)
                .param("status",  EventStatus.NEW.name())
                .param("limit", LIMIT + "")
                .content(CONTENT_TYPE_EVENT_LIST))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Flow-ID", not(isEmptyString())))
                .andExpect(header().string("Content-Type", containsString(CONTENT_TYPE_EVENT_LIST)))
                .andExpect(jsonPath("_links.next.href", is(NEXT_LINK)))
                .andExpect(jsonPath("events", hasSize(2)));
    }

    @Test
    public void testGetEventsInvalidCursor() throws Exception {
        final String invalidCursor = "ABC";
        when(eventLogService.searchEvents(eq(invalidCursor), anyString(), anyInt()))
                .thenThrow(new InvalidCursorException(invalidCursor));

        mockMvc.perform(MockMvcRequestBuilders.get("/events")
                .param("cursor", invalidCursor))
                .andExpect(status().is(422))
                .andExpect(header().string("Content-Type", containsString(CONTENT_TYPE_PROBLEM)))
                .andExpect(jsonPath("type", is("http://httpstatus.es/422")))
                .andExpect(jsonPath("status", is(422)))
                .andExpect(jsonPath("title", is("Invalid cursor format")))
                .andExpect(jsonPath("detail", is("The provided cursor ("+invalidCursor+") is not numeric.")))
                .andExpect(jsonPath("instance", startsWith("X-Flow-ID")));
    }

    @Test
    public void testPatchEvents() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.patch("/events")
                .contentType(CONTENT_TYPE_EVENT_LIST_UPDATE)
                .content(mapper.writeValueAsString(new BunchOfEventUpdatesDTO())))
                .andExpect(status().isOk());

        verify(eventLogService, times(1)).updateEvents(any(BunchOfEventUpdatesDTO.class));
    }

    @Test
    public void testPatchEventsInvalidEventId() throws Exception {

        final String invalidEventId = "invalidEventId";

        doThrow(new InvalidEventIdException(invalidEventId)).when(eventLogService).updateEvents(any(
                BunchOfEventUpdatesDTO.class));

        mockMvc.perform(MockMvcRequestBuilders.patch("/events")
                .contentType(CONTENT_TYPE_EVENT_LIST_UPDATE)
                .content(mapper.writeValueAsString(new BunchOfEventUpdatesDTO())))
                .andExpect(status().is(422))
                .andExpect(header().string("Content-Type", containsString(CONTENT_TYPE_PROBLEM)))
                .andExpect(header().string("X-Flow-ID", not(isEmptyString())))
                .andExpect(jsonPath("type", is("http://httpstatus.es/422")))
                .andExpect(jsonPath("status", is(422)))
                .andExpect(jsonPath("title", is("Invalid event id format")))
                .andExpect(jsonPath("detail", is("The provided event id ("+invalidEventId+") is not numeric.")))
                .andExpect(jsonPath("instance", startsWith("X-Flow-ID")));
    }

    @Test
    public void testPatchEventsUnknownEventId() throws Exception {

        final Integer unknownEventID = 1234;

        doThrow(new UnknownEventIdException(unknownEventID)).when(eventLogService).updateEvents(any(
                BunchOfEventUpdatesDTO.class));

        mockMvc.perform(MockMvcRequestBuilders.patch("/events")
                .contentType(CONTENT_TYPE_EVENT_LIST_UPDATE)
                .content(mapper.writeValueAsString(new BunchOfEventUpdatesDTO())))
                .andExpect(status().is(422))
                .andExpect(header().string("Content-Type", containsString(CONTENT_TYPE_PROBLEM)))
                .andExpect(header().string("X-Flow-ID", not(isEmptyString())))
                .andExpect(jsonPath("type", is("http://httpstatus.es/422")))
                .andExpect(jsonPath("status", is(422)))
                .andExpect(jsonPath("title", is("No event log found")))
                .andExpect(jsonPath("detail", is("No event log found for event id ("+unknownEventID+").")))
                .andExpect(jsonPath("instance", startsWith("X-Flow-ID")));
    }

    @Test
    public void testPatchEventsMissingField() throws Exception {

        final String validationExceptionMessage = "missing field x.y";

        doThrow(new ValidationException(newArrayList(validationExceptionMessage))).when(eventLogService)
                                                                                  .updateEvents(any(
                                                                                          BunchOfEventUpdatesDTO.class));
        mockMvc.perform(MockMvcRequestBuilders.patch("/events")
                .contentType(CONTENT_TYPE_EVENT_LIST_UPDATE)
                .content(mapper.writeValueAsString(new BunchOfEventUpdatesDTO())))
                .andExpect(status().is(400))
                .andExpect(header().string("Content-Type", containsString(CONTENT_TYPE_PROBLEM)))
                .andExpect(header().string("X-Flow-ID", not(isEmptyString())))
                .andExpect(jsonPath("type", is("http://httpstatus.es/400")))
                .andExpect(jsonPath("status", is(400)))
                .andExpect(jsonPath("title", is("Validation Error")))
                .andExpect(jsonPath("detail", is("The validation resulted in the following errors: "+validationExceptionMessage)))
                .andExpect(jsonPath("instance", startsWith("X-Flow-ID")));
    }

    @Test
    public void testCreateSnapshotEvents() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.post("/events/snapshots")).andExpect(status().isCreated());

        verify(eventLogService).createSnapshotEvents(any(), any());
    }

    @Test
    public void testSnapshotNotImplemented() throws Exception {

        doThrow(new TarbelaSnapshotProviderNotImplemented()).when(tarbelaSnapshotProvider).getSnapshot();

        mockMvc.perform(MockMvcRequestBuilders.post("/events/snapshots")
                .contentType(CONTENT_TYPE_EVENT_LIST_UPDATE)
                .content(mapper.writeValueAsString(new BunchOfEventUpdatesDTO())))
                .andExpect(status().is(501))
                .andExpect(header().string("Content-Type", containsString(CONTENT_TYPE_PROBLEM)))
                .andExpect(header().string("X-Flow-ID", not(isEmptyString())))
                .andExpect(jsonPath("type", is("http://httpstatus.es/501")))
                .andExpect(jsonPath("status", is(501)))
                .andExpect(jsonPath("title", is("Snapshot not implemented")))
                .andExpect(jsonPath("detail", is("Tarbela Snapshot not implemented by the service")))
                .andExpect(jsonPath("instance", startsWith("X-Flow-ID")));
    }

}
