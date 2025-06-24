package org.zalando.nakadiproducer.eventlog.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.nakadiproducer.eventlog.CompactionKeyExtractor;
import org.zalando.nakadiproducer.util.Fixture;
import org.zalando.nakadiproducer.util.MockPayload;

import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.zalando.nakadiproducer.util.Fixture.PUBLISHER_EVENT_TYPE;

/**
 * This tests the cases where we have multiple CompactionKeyExtractors for different types.
 */
@ExtendWith(MockitoExtension.class)
public class EventLogWriterMultipleTypesTest {

    @Mock
    private EventLogRepository eventLogRepository;

    @Mock
    private EventLogBuilder eventLogBuilder;

    @Captor
    private ArgumentCaptor<Collection<EventLog>> eventLogsCapture;

    private EventLogWriterImpl eventLogWriter;

    private MockPayload eventPayload1;
    private MockPayload.SubClass eventPayload2;
    private List<MockPayload.SubListItem> eventPayload3;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        Mockito.reset(eventLogRepository, eventLogBuilder);

        eventPayload1 = Fixture.mockPayload(1, "mockedcode1", true,
                Fixture.mockSubClass("some info"), Fixture.mockSubList(2, "some detail"));

        eventPayload2 = Fixture.mockSubClass("some info");

        eventPayload3 = Fixture.mockSubList(2, "some detail");
    }

    @Test
    public void noCompactionExtractors() {
        mockCreateEventLog(eventPayload1, null);
        mockCreateEventLog(eventPayload2, null);
        mockCreateEventLog(eventPayload3, null);

        eventLogWriter = new EventLogWriterImpl(eventLogRepository, eventLogBuilder, List.of());

        eventLogWriter.fireCreateEvents(PUBLISHER_EVENT_TYPE, "",
                asList(eventPayload1, eventPayload2, eventPayload3));
        List<String> compactionKeys = getPersistedCompactionKeys();
        assertThat(compactionKeys, contains(nullValue(), nullValue(), nullValue()));
    }

    @Test
    public void oneCompactionExtractor() {
        mockCreateEventLog(eventPayload1, "Hello");
        mockCreateEventLog(eventPayload2, null);
        mockCreateEventLog(eventPayload3, null);

        eventLogWriter = new EventLogWriterImpl(eventLogRepository,
            eventLogBuilder, List.of(CompactionKeyExtractor.of(PUBLISHER_EVENT_TYPE, MockPayload.class, m -> "Hello")));
        eventLogWriter.fireCreateEvents(PUBLISHER_EVENT_TYPE, "",
                asList(eventPayload1, eventPayload2, eventPayload3));
        List<String> compactionKeys = getPersistedCompactionKeys();
        assertThat(compactionKeys, contains(equalTo("Hello"), nullValue(), nullValue()));
    }

    @Test
    public void twoCompactionExtractors() {
        mockCreateEventLog(eventPayload1, "Hello");
        mockCreateEventLog(eventPayload2, "World");
        mockCreateEventLog(eventPayload3, null);

        eventLogWriter = new EventLogWriterImpl(eventLogRepository, eventLogBuilder,
                List.of(CompactionKeyExtractor.of(PUBLISHER_EVENT_TYPE, MockPayload.class, m -> "Hello"),
                        CompactionKeyExtractor.of(PUBLISHER_EVENT_TYPE, MockPayload.SubClass.class, m -> "World")));
        eventLogWriter.fireCreateEvents(PUBLISHER_EVENT_TYPE, "",
                asList(eventPayload1, eventPayload2, eventPayload3));
        List<String> compactionKeys = getPersistedCompactionKeys();
        assertThat(compactionKeys, contains(equalTo("Hello"), equalTo("World"), nullValue()));
    }

    @Test
    public void threeCompactionExtractors() {
        mockCreateEventLog(eventPayload1, "Hello");
        mockCreateEventLog(eventPayload2, "World");
        mockCreateEventLog(eventPayload3, "List?");

        eventLogWriter = new EventLogWriterImpl(eventLogRepository, eventLogBuilder,
                List.of(CompactionKeyExtractor.of(PUBLISHER_EVENT_TYPE, MockPayload.class, m -> "Hello"),
                        CompactionKeyExtractor.of(PUBLISHER_EVENT_TYPE, MockPayload.SubClass.class, m -> "World"),
                        CompactionKeyExtractor.of(PUBLISHER_EVENT_TYPE, List.class, m -> "List?")));

        eventLogWriter.fireCreateEvents(PUBLISHER_EVENT_TYPE, "",
                asList(eventPayload1, eventPayload2, eventPayload3));
        List<String> compactionKeys = getPersistedCompactionKeys();
        assertThat(compactionKeys, contains(equalTo("Hello"), equalTo("World"), equalTo("List?")));
    }

    private List<String> getPersistedCompactionKeys() {
        verify(eventLogRepository).persist(eventLogsCapture.capture());
        Collection<EventLog> eventLogs = eventLogsCapture.getValue();
        List<String> compactionKeys = eventLogs.stream().map(el -> el.getCompactionKey()).collect(toList());
        return compactionKeys;
    }

    private void mockCreateEventLog(Object payload, String compactionKey) {
        when(
            eventLogBuilder.buildEventLog(
                eq(PUBLISHER_EVENT_TYPE),
                eq(new DataChangeEventEnvelope(EventDataOperation.CREATE.toString(), "", payload)),
                eq(compactionKey))
        ).thenReturn(buildEventLog(payload, compactionKey));
    }

    @SneakyThrows
    private EventLog buildEventLog(Object payloadString, String compactionKey) {
        return EventLog.builder()
            .eventType("type1")
            .eventBodyData(objectMapper.writeValueAsString(payloadString))
            .flowId("TRACE_ID")
            .compactionKey(compactionKey)
            .eid(UUID.fromString("558a8fe5-330e-4d89-ae6c-d58432b2dde0"))
            .build();
    }
}
