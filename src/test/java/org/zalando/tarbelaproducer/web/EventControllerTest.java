package org.zalando.tarbelaproducer.web;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.mockito.Mockito;
import org.zalando.tarbelaproducer.TarbelaSnapshotProviderNotImplementedException;
import org.zalando.tarbelaproducer.api.event.model.BunchOfEventUpdatesDTO;
import org.zalando.tarbelaproducer.api.event.model.BunchOfEventsDTO;
import org.zalando.tarbelaproducer.api.event.model.BunchofEventsLinksDTO;
import org.zalando.tarbelaproducer.api.event.model.BunchofEventsLinksNextDTO;
import org.zalando.tarbelaproducer.api.event.model.EventDTO;
import org.zalando.tarbelaproducer.persistance.entity.EventStatus;
import org.zalando.tarbelaproducer.service.EventLogService;
import org.zalando.tarbelaproducer.service.exception.InvalidCursorException;
import org.zalando.tarbelaproducer.service.exception.InvalidEventIdException;
import org.zalando.tarbelaproducer.service.exception.UnknownEventIdException;
import org.zalando.tarbelaproducer.service.exception.UnknownEventTypeException;
import org.zalando.tarbelaproducer.util.Fixture;
import org.zalando.tarbelaproducer.service.exception.ValidationException;

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
import static org.mockito.Matchers.matches;
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

    private static final String FLOW_ID_VALUE = "A_FUNKY_TRACER_VALUE";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(flowIdComponent.getXFlowIdKey()).thenReturn("X-Flow-ID");
        when(flowIdComponent.getXFlowIdValue()).thenReturn(FLOW_ID_VALUE);

        final EventController eventController = new EventController(eventLogService, flowIdComponent);
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
                .content(EventController.CONTENT_TYPE_EVENT_LIST))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Flow-ID", not(isEmptyString())))
                .andExpect(header().string("Content-Type", containsString(EventController.CONTENT_TYPE_EVENT_LIST)))
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
                .andExpect(header().string("Content-Type", containsString(EventController.CONTENT_TYPE_PROBLEM)))
                .andExpect(jsonPath("type", is("http://httpstatus.es/422")))
                .andExpect(jsonPath("status", is(422)))
                .andExpect(jsonPath("title", is("Invalid cursor format")))
                .andExpect(jsonPath("detail", is("The provided cursor ("+invalidCursor+") is not numeric.")))
                .andExpect(jsonPath("instance", startsWith("X-Flow-ID")));
    }

    @Test
    public void testPatchEvents() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.patch("/events")
                .contentType(EventController.CONTENT_TYPE_EVENT_LIST_UPDATE)
                .content(mapper.writeValueAsString(new BunchOfEventUpdatesDTO())))
                .andExpect(status().isOk());

        verify(eventLogService, times(1)).updateEvents(any(BunchOfEventUpdatesDTO.class));
    }

    @Test
    public void testPatchEventsInvalidEventId() throws Exception {

        final String invalidEventId = "invalidEventId";

        Mockito.doThrow(new InvalidEventIdException(invalidEventId)).when(eventLogService).updateEvents(any(
                BunchOfEventUpdatesDTO.class));

        mockMvc.perform(MockMvcRequestBuilders.patch("/events")
                .contentType(EventController.CONTENT_TYPE_EVENT_LIST_UPDATE)
                .content(mapper.writeValueAsString(new BunchOfEventUpdatesDTO())))
                .andExpect(status().is(422))
                .andExpect(header().string("Content-Type", containsString(EventController.CONTENT_TYPE_PROBLEM)))
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

        Mockito.doThrow(new UnknownEventIdException(unknownEventID)).when(eventLogService).updateEvents(any(
                BunchOfEventUpdatesDTO.class));

        mockMvc.perform(MockMvcRequestBuilders.patch("/events")
                .contentType(EventController.CONTENT_TYPE_EVENT_LIST_UPDATE)
                .content(mapper.writeValueAsString(new BunchOfEventUpdatesDTO())))
                .andExpect(status().is(422))
                .andExpect(header().string("Content-Type", containsString(EventController.CONTENT_TYPE_PROBLEM)))
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
                .contentType(EventController.CONTENT_TYPE_EVENT_LIST_UPDATE)
                .content(mapper.writeValueAsString(new BunchOfEventUpdatesDTO())))
                .andExpect(status().is(400))
                .andExpect(header().string("Content-Type", containsString(EventController.CONTENT_TYPE_PROBLEM)))
                .andExpect(header().string("X-Flow-ID", not(isEmptyString())))
                .andExpect(jsonPath("type", is("http://httpstatus.es/400")))
                .andExpect(jsonPath("status", is(400)))
                .andExpect(jsonPath("title", is("Validation Error")))
                .andExpect(jsonPath("detail", is("The validation resulted in the following errors: "+validationExceptionMessage)))
                .andExpect(jsonPath("instance", startsWith("X-Flow-ID")));
    }

    @Test
    public void testCreateSnapshotEvents() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.post("/events/snapshots/" + Fixture.PUBLISHER_EVENT_TYPE)).andExpect(status().isCreated());

        verify(eventLogService).createSnapshotEvents(matches(Fixture.PUBLISHER_EVENT_TYPE), any());
    }

    @Test
    public void testSnapshotNotImplemented() throws Exception {

        Mockito.doThrow(new TarbelaSnapshotProviderNotImplementedException()).when(eventLogService).createSnapshotEvents(any(), any());

        mockMvc.perform(MockMvcRequestBuilders.post("/events/snapshots/" + Fixture.PUBLISHER_EVENT_TYPE)
                .contentType(EventController.CONTENT_TYPE_EVENT_LIST_UPDATE)
                .content(mapper.writeValueAsString(new BunchOfEventUpdatesDTO())))
                .andExpect(status().is(501))
                .andExpect(header().string("Content-Type", containsString(EventController.CONTENT_TYPE_PROBLEM)))
                .andExpect(header().string("X-Flow-ID", not(isEmptyString())))
                .andExpect(jsonPath("type", is("http://httpstatus.es/501")))
                .andExpect(jsonPath("status", is(501)))
                .andExpect(jsonPath("title", is("Snapshot not implemented")))
                .andExpect(jsonPath("detail", is("Tarbela Snapshot not implemented by the service")))
                .andExpect(jsonPath("instance", startsWith("X-Flow-ID")));
    }

    @Test
    public void testSnapshotUnknownType() throws Exception {

        String unknownEventType = "unknown.event-type";

        Mockito.doThrow(new UnknownEventTypeException(unknownEventType)).when(eventLogService).createSnapshotEvents(any(), any());

        mockMvc.perform(MockMvcRequestBuilders.post("/events/snapshots/" + unknownEventType)
                .contentType(EventController.CONTENT_TYPE_EVENT_LIST_UPDATE)
                .content(mapper.writeValueAsString(new BunchOfEventUpdatesDTO())))
                .andExpect(status().is(422))
                .andExpect(header().string("Content-Type", containsString(EventController.CONTENT_TYPE_PROBLEM)))
                .andExpect(header().string("X-Flow-ID", not(isEmptyString())))
                .andExpect(jsonPath("type", is("http://httpstatus.es/422")))
                .andExpect(jsonPath("status", is(422)))
                .andExpect(jsonPath("title", is("No event log found")))
                .andExpect(jsonPath("detail", is("No event log found for event type ("+unknownEventType+").")))
                .andExpect(jsonPath("instance", startsWith("X-Flow-ID")));
    }

}
