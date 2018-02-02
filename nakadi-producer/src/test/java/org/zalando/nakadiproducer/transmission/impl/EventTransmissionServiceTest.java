package org.zalando.nakadiproducer.transmission.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.zalando.nakadiproducer.eventlog.impl.EventLog;
import org.zalando.nakadiproducer.eventlog.impl.EventLogRepository;
import org.zalando.nakadiproducer.transmission.MockNakadiPublishingClient;
import org.zalando.nakadiproducer.util.Fixture;

import java.util.List;

import static com.jayway.jsonpath.JsonPath.read;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class EventTransmissionServiceTest {

    private EventTransmissionService service;
    private MockNakadiPublishingClient publishingClient;
    private ObjectMapper mapper;
    private EventLogRepository repo;

    @Before
    public void setUp() {
        repo = mock(EventLogRepository.class);
        publishingClient = new MockNakadiPublishingClient();
        mapper = new ObjectMapper();
        service = new EventTransmissionService(repo, publishingClient, mapper);
    }

    @Test
    public void testWithFlowId() throws JsonProcessingException {
        String flowId = "XYZ";
        String payloadString = mapper.writeValueAsString(Fixture.mockPayload(42, "bla"));
        EventLog ev = new EventLog(27, "type", payloadString, flowId, now(), now(), null, now().plus(5, MINUTES));

        service.sendEvent(ev);

        List<String> events = publishingClient.getSentEvents("type");
        assertThat(events, hasSize(1));
        assertThat(read(events.get(0), "$.metadata.flow_id"), is(flowId));
    }

    @Test
    public void testWithoutFlowId() throws JsonProcessingException {
        String payloadString = mapper.writeValueAsString(Fixture.mockPayload(42, "bla"));
        EventLog ev = new EventLog(27, "type", payloadString, null, now(), now(), null, now().plus(5, MINUTES));

        service.sendEvent(ev);

        List<String> events = publishingClient.getSentEvents("type");
        assertThat(events, hasSize(1));
        assertThat(read(events.get(0), "$.metadata"), not(hasKey("flow_id")));
    }

    @Test
    public void shouldNotSendEventsCloseToLockExpiry() throws JsonProcessingException {
        // given an event...
        String payloadString = mapper.writeValueAsString(Fixture.mockPayload(42, "bla"));
        // ... whose lock expires in the next minute
        EventLog ev = new EventLog(27, "type", payloadString, null, now(), now(), null, now().plus(30, SECONDS));

        // when the service is asked to send the event
        service.sendEvent(ev);

        // then event should not have been sent...
        List<String> events = publishingClient.getSentEvents("type");
        assertThat(events, is(empty()));

        // ... and not been deleted
        verifyNoMoreInteractions(repo);
    }

}
