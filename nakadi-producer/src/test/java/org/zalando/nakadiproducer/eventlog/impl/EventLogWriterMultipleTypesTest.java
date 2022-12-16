package org.zalando.nakadiproducer.eventlog.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.nakadiproducer.eventlog.CompactionKeyExtractor;
import org.zalando.nakadiproducer.flowid.FlowIdComponent;
import org.zalando.nakadiproducer.util.Fixture;
import org.zalando.nakadiproducer.util.MockPayload;

import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
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
    private FlowIdComponent flowIdComponent;

    @Captor
    private ArgumentCaptor<Collection<EventLog>> eventLogsCapture;

    private EventLogWriterImpl eventLogWriter;

    private static final String TRACE_ID = "TRACE_ID";

    private MockPayload eventPayload1;
    private MockPayload.SubClass eventPayload2;
    private List<MockPayload.SubListItem> eventPayload3;

    @BeforeEach
    public void setUp() {
        Mockito.reset(eventLogRepository, flowIdComponent);

        eventPayload1 = Fixture.mockPayload(1, "mockedcode1", true,
                Fixture.mockSubClass("some info"), Fixture.mockSubList(2, "some detail"));

        eventPayload2 = Fixture.mockSubClass("some info");

        eventPayload3 = Fixture.mockSubList(2, "some detail");

        when(flowIdComponent.getXFlowIdValue()).thenReturn(TRACE_ID);
    }

    @Test
    public void noCompactionExtractors() {
        eventLogWriter = new EventLogWriterImpl(eventLogRepository, new ObjectMapper(),
                flowIdComponent, List.of());

        eventLogWriter.fireCreateEvents(PUBLISHER_EVENT_TYPE, "",
                asList(eventPayload1, eventPayload2, eventPayload3));
        List<String> compactionKeys = getPersistedCompactionKeys();
        assertThat(compactionKeys, contains(nullValue(), nullValue(), nullValue()));
    }

    @Test
    public void oneCompactionExtractor() {
        eventLogWriter = new EventLogWriterImpl(eventLogRepository, new ObjectMapper(),
                flowIdComponent, List.of(CompactionKeyExtractor.of(PUBLISHER_EVENT_TYPE, MockPayload.class, m -> "Hello")));
        eventLogWriter.fireCreateEvents(PUBLISHER_EVENT_TYPE, "",
                asList(eventPayload1, eventPayload2, eventPayload3));
        List<String> compactionKeys = getPersistedCompactionKeys();
        assertThat(compactionKeys, contains(equalTo("Hello"), nullValue(), nullValue()));
    }

    @Test
    public void twoCompactionExtractors() {
        eventLogWriter = new EventLogWriterImpl(eventLogRepository, new ObjectMapper(),
                flowIdComponent,
                List.of(CompactionKeyExtractor.of(PUBLISHER_EVENT_TYPE, MockPayload.class, m -> "Hello"),
                        CompactionKeyExtractor.of(PUBLISHER_EVENT_TYPE, MockPayload.SubClass.class, m -> "World")));
        eventLogWriter.fireCreateEvents(PUBLISHER_EVENT_TYPE, "",
                asList(eventPayload1, eventPayload2, eventPayload3));
        List<String> compactionKeys = getPersistedCompactionKeys();
        assertThat(compactionKeys, contains(equalTo("Hello"), equalTo("World"), nullValue()));
    }

    @Test
    public void threeCompactionExtractors() {
        eventLogWriter = new EventLogWriterImpl(eventLogRepository, new ObjectMapper(),
                flowIdComponent,
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
}
