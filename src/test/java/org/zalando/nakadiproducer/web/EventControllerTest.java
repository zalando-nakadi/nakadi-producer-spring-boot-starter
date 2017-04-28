package org.zalando.nakadiproducer.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zalando.nakadiproducer.SnapshotEventProviderNotImplementedException;
import org.zalando.nakadiproducer.service.EventLogService;
import org.zalando.nakadiproducer.service.exception.UnknownEventTypeException;
import org.zalando.nakadiproducer.util.Fixture;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = { MockServletContext.class }
)
public class EventControllerTest {

    @Mock
    private EventLogService eventLogService;

    private MockMvc mockMvc;

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
    }

    @Test
    public void testCreateSnapshotEvents() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.post("/events/snapshots/" + Fixture.PUBLISHER_EVENT_TYPE)).andExpect(status().isCreated());

        verify(eventLogService).createSnapshotEvents(matches(Fixture.PUBLISHER_EVENT_TYPE), any());
    }

    @Test
    public void testSnapshotNotImplemented() throws Exception {

        Mockito.doThrow(new SnapshotEventProviderNotImplementedException()).when(eventLogService).createSnapshotEvents(any(), any());

        mockMvc.perform(MockMvcRequestBuilders.post("/events/snapshots/" + Fixture.PUBLISHER_EVENT_TYPE))
                .andExpect(status().is(501))
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(header().string("X-Flow-ID", not(isEmptyString())))
                .andExpect(jsonPath("type", is("http://httpstatus.es/501")))
                .andExpect(jsonPath("status", is(501)))
                .andExpect(jsonPath("title", is("Snapshot not implemented")))
                .andExpect(jsonPath("detail", is("Snapshot not implemented by the service")))
                .andExpect(jsonPath("instance", startsWith("X-Flow-ID")));
    }

    @Test
    public void testSnapshotUnknownType() throws Exception {

        String unknownEventType = "unknown.event-type";

        Mockito.doThrow(new UnknownEventTypeException(unknownEventType)).when(eventLogService).createSnapshotEvents(any(), any());

        mockMvc.perform(MockMvcRequestBuilders.post("/events/snapshots/" + unknownEventType))
                .andExpect(status().is(422))
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(header().string("X-Flow-ID", not(isEmptyString())))
                .andExpect(jsonPath("type", is("http://httpstatus.es/422")))
                .andExpect(jsonPath("status", is(422)))
                .andExpect(jsonPath("title", is("No event log found")))
                .andExpect(jsonPath("detail", is("No event log found for event type ("+unknownEventType+").")))
                .andExpect(jsonPath("instance", startsWith("X-Flow-ID")));
    }

}
