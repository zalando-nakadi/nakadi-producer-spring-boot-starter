package org.zalando.nakadiproducer.transmission.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.zalando.fahrschein.EventPublishingException;
import org.zalando.fahrschein.domain.BatchItemResponse;
import org.zalando.nakadiproducer.eventlog.impl.EventLog;
import org.zalando.nakadiproducer.eventlog.impl.EventLogRepository;
import org.zalando.nakadiproducer.transmission.MockNakadiPublishingClient;
import org.zalando.nakadiproducer.util.Fixture;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static com.jayway.jsonpath.JsonPath.read;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class EventTransmissionServiceTest {

    private EventTransmissionService service;
    private MockNakadiPublishingClient publishingClient;
    private ObjectMapper mapper;
    private EventLogRepository repo;

    @Before
    public void setUp() {
        repo = mock(EventLogRepository.class);
        publishingClient = spy(new MockNakadiPublishingClient());
        mapper = spy(new ObjectMapper());
        service = new EventTransmissionService(repo, publishingClient, mapper);
    }

    @Test
    public void testWithFlowId() throws JsonProcessingException {
        String flowId = "XYZ";
        EventLog ev = eventLogEntry(27, "type", now().plus(5, MINUTES));
        ev.setFlowId(flowId);

        service.sendEvents(singletonList(ev));

        List<String> events = publishingClient.getSentEvents("type");
        assertThat(events, hasSize(1));
        assertThat(read(events.get(0), "$.metadata.flow_id"), is(flowId));
    }

    @Test
    public void testWithoutFlowId() throws JsonProcessingException {
        EventLog ev = eventLogEntry(27, "type", now().plus(5, MINUTES));

        service.sendEvents(singletonList(ev));

        List<String> events = publishingClient.getSentEvents("type");
        assertThat(events, hasSize(1));
        assertThat(read(events.get(0), "$.metadata"), not(hasKey("flow_id")));
    }

    @Test
    public void testErrorInPayloadDeserializationIsHandledGracefully() throws IOException {
        String payloadString = mapper.writeValueAsString(Fixture.mockPayload(42, "bla"));
        EventLog ev1 = eventLogEntry(1, "type1", now().plus(5, MINUTES));
        EventLog ev2 = eventLogEntry(2, "type1", now().plus(5, MINUTES));
        EventLog ev3 = eventLogEntry(3, "type2", now().plus(5, MINUTES));

        Mockito.clearInvocations(mapper);


        doReturn(new LinkedHashMap<>())
                .doThrow(new IOException("Failed"))
                .doReturn(new LinkedHashMap<>())
                .when(mapper).readValue(eq(payloadString), anyLinkedHashmapTypeReference());



        service.sendEvents(Arrays.asList(ev1, ev2, ev3));

        // Invalid event is skipped...
        List<String> type1Events = publishingClient.getSentEvents("type1");
        assertThat(type1Events, hasSize(1));
        assertThat(read(type1Events.get(0), "$.metadata.eid"), is("00000000-0000-0000-0000-000000000001"));

        List<String> type2Events = publishingClient.getSentEvents("type2");
        assertThat(type2Events, hasSize(1));
        assertThat(read(type2Events.get(0), "$.metadata.eid"), is("00000000-0000-0000-0000-000000000003"));

        // and only the successful ones have been deleted.
        verify(repo).delete(ev1);
        verify(repo, never()).delete(ev2);
        verify(repo).delete(ev3);
    }

    @Test
    public void testUnknownErrorInTransmissionIsHandledGracefully() throws Exception {
        EventLog ev1 = eventLogEntry(1, "type1", now().plus(5, MINUTES));
        EventLog ev2 = eventLogEntry(2, "type1", now().plus(5, MINUTES));
        EventLog ev3 = eventLogEntry(3, "type2", now().plus(5, MINUTES));

        doThrow(new IllegalStateException("failed"))
                .when(publishingClient).publish(eq("type1"), any());

        doCallRealMethod()
                .when(publishingClient).publish(eq("type2"), any());

        service.sendEvents(Arrays.asList(ev1, ev2, ev3));

        // Broken batch is skipped...
        List<String> type1Events = publishingClient.getSentEvents("type1");
        assertThat(type1Events, hasSize(0));

        List<String> type2Events = publishingClient.getSentEvents("type2");
        assertThat(type2Events, hasSize(1));
        assertThat(read(type2Events.get(0), "$.metadata.eid"), is("00000000-0000-0000-0000-000000000003"));

        // and only the successful ones have been deleted.
        verify(repo, never()).delete(ev1);
        verify(repo, never()).delete(ev2);
        verify(repo).delete(ev3);
    }

    @Test
    public void testEventPublishingExceptionIsHandledGracefully() throws Exception {
        EventLog ev1 = eventLogEntry(1, "type1", now().plus(5, MINUTES));
        EventLog ev2 = eventLogEntry(2, "type1", now().plus(5, MINUTES));
        EventLog ev3 = eventLogEntry(3, "type2", now().plus(5, MINUTES));

        doThrow(new EventPublishingException(new BatchItemResponse[]{
                new BatchItemResponse("00000000-0000-0000-0000-000000000002",
                        BatchItemResponse.PublishingStatus.ABORTED, BatchItemResponse.Step.ENRICHING, "Something went wrong")
        }))
                .when(publishingClient).publish(eq("type1"), any());

        doCallRealMethod()
                .when(publishingClient).publish(eq("type2"), any());

        service.sendEvents(Arrays.asList(ev1, ev2, ev3));

        // In reality, the publishing client will have sent 1 event, but it's hard to fake this behaviour when spying on
        // it, so we check for 0 for now
        List<String> type1Events = publishingClient.getSentEvents("type1");
        assertThat(type1Events, hasSize(0));

        List<String> type2Events = publishingClient.getSentEvents("type2");
        assertThat(type2Events, hasSize(1));
        assertThat(read(type2Events.get(0), "$.metadata.eid"), is("00000000-0000-0000-0000-000000000003"));

        // and only the successful ones have been deleted.
        verify(repo).delete(ev1);
        verify(repo, never()).delete(ev2);
        verify(repo).delete(ev3);
    }

    @Test
    public void testWithMultipleEvents() throws JsonProcessingException {
        EventLog ev1 = eventLogEntry(1, "type1", now().plus(5, MINUTES));
        EventLog ev2 = eventLogEntry(2, "type1", now().plus(5, MINUTES));
        EventLog ev3 = eventLogEntry(3, "type2", now().plus(5, MINUTES));

        service.sendEvents(Arrays.asList(ev1, ev2, ev3));

        List<String> type1Events = publishingClient.getSentEvents("type1");
        assertThat(type1Events, hasSize(2));
        assertThat(read(type1Events.get(0), "$.metadata.eid"), is("00000000-0000-0000-0000-000000000001"));
        assertThat(read(type1Events.get(1), "$.metadata.eid"), is("00000000-0000-0000-0000-000000000002"));

        List<String> type2Events = publishingClient.getSentEvents("type2");
        assertThat(type2Events, hasSize(1));
        assertThat(read(type2Events.get(0), "$.metadata.eid"), is("00000000-0000-0000-0000-000000000003"));
    }

    @Test
    public void shouldNotSendEventsCloseToLockExpiry() throws JsonProcessingException {
        // given an event...
        // ... whose lock expires in the next minute
        EventLog ev = eventLogEntry(27, "type", now().plus(30, SECONDS));

        // when the service is asked to send the event
        service.sendEvents(singletonList(ev));

        // then event should not have been sent...
        List<String> events = publishingClient.getSentEvents("type");
        assertThat(events, is(empty()));

        // ... and not been deleted
        verifyNoMoreInteractions(repo);
    }

    @Test
    public void shouldNotSendEventsWithExpiredLock() throws JsonProcessingException {
        // given an event...
        // ... whose lock expires already expired
        EventLog ev = eventLogEntry(27, "type", now().minus(1, SECONDS));

        // when the service is asked to send the event
        service.sendEvents(singletonList(ev));

        // then event should not have been sent...
        List<String> events = publishingClient.getSentEvents("type");
        assertThat(events, is(empty()));

        // ... and not been deleted
        verifyNoMoreInteractions(repo);
    }

    private EventLog eventLogEntry(int id, String type, Instant lockedUntil) throws JsonProcessingException {
        String payloadString = mapper.writeValueAsString(Fixture.mockPayload(42, "bla"));
        return new EventLog(id, type, payloadString, null, now(), now(), null, lockedUntil, "{}");
    }


    private TypeReference<LinkedHashMap<String, Object>> anyLinkedHashmapTypeReference() {
        return any();
    }

}
