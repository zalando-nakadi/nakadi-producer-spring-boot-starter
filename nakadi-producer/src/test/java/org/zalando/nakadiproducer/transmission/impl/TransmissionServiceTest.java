package org.zalando.nakadiproducer.transmission.impl;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.JsonPath.read;
import static java.time.Instant.now;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.zalando.nakadiproducer.eventlog.impl.EventLog;
import org.zalando.nakadiproducer.eventlog.impl.EventLogRepository;
import org.zalando.nakadiproducer.transmission.MockNakadiPublishingClient;
import org.zalando.nakadiproducer.util.Fixture;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TransmissionServiceTest {

    private EventTransmissionService service;
    private EventLogRepository repo;
    private MockNakadiPublishingClient publishingClient;
    private ObjectMapper mapper;

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
        EventLog ev = new EventLog(27, "type", payloadString, flowId, now(), now(), null, null);

        service.sendEvent(ev);

        List<String> events = publishingClient.getSentEvents("type");
        assertThat(events, hasSize(1));
        assertThat(read(events.get(0), "$.metadata.flow_id"), is(flowId));
    }

    @Test
    public void testWithoutFlowId() throws JsonProcessingException {
        String payloadString = mapper.writeValueAsString(Fixture.mockPayload(42, "bla"));
        EventLog ev = new EventLog(27, "type", payloadString, null, now(), now(), null, null);

        service.sendEvent(ev);

        List<String> events = publishingClient.getSentEvents("type");
        assertThat(events, hasSize(1));
        assertThat(read(events.get(0), "$.metadata.[?]", where("flow_id").exists(true)), is(empty()));
    }

}
