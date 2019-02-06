package org.zalando.nakadiproducer.transmission.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.zalando.nakadiproducer.eventlog.impl.EventLog;
import org.zalando.nakadiproducer.transmission.impl.EventBatcher.BatchItem;

import java.util.List;
import java.util.function.Consumer;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EventBatcherTest {
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final Consumer<List<BatchItem>> publisher = mock(Consumer.class);
    private EventBatcher eventBatcher = new EventBatcher(objectMapper, publisher);

    @Test
    public void shouldNotPublishEmptyBatches() {
        eventBatcher.finish();

        verify(publisher, never()).accept(any());
    }

    @Test
    public void shouldPublishNonFilledBatchOnFinish() throws JsonProcessingException {
        EventLog eventLogEntry = eventLogEntry(1, "type");
        NakadiEvent nakadiEvent = nakadiEvent("1");

        when(objectMapper.writeValueAsBytes(any())).thenReturn(new byte[500]);

        eventBatcher.pushEvent(eventLogEntry, nakadiEvent);
        verify(publisher, never()).accept(any());

        eventBatcher.finish();
        verify(publisher).accept(eq(singletonList(new BatchItem(eventLogEntry, nakadiEvent))));
    }

    @Test
    public void shouldPublishNonFilledBatchOnEventTypeChange() throws JsonProcessingException {
        EventLog eventLogEntry1 = eventLogEntry(1, "type1");
        EventLog eventLogEntry2 = eventLogEntry(2, "type2");
        NakadiEvent nakadiEvent1 = nakadiEvent("1");
        NakadiEvent nakadiEvent2 = nakadiEvent("2");

        when(objectMapper.writeValueAsBytes(any())).thenReturn(new byte[500]);

        eventBatcher.pushEvent(eventLogEntry1, nakadiEvent1);
        eventBatcher.pushEvent(eventLogEntry2, nakadiEvent2);
        verify(publisher).accept(eq(singletonList(new BatchItem(eventLogEntry1, nakadiEvent1))));
    }

    @Test
    public void shouldPublishFilledBatchOnSubmissionOfNewEvent() throws JsonProcessingException {
        EventLog eventLogEntry1 = eventLogEntry(1, "type1");
        EventLog eventLogEntry2 = eventLogEntry(2, "type1");
        EventLog eventLogEntry3 = eventLogEntry(3, "type1");
        NakadiEvent nakadiEvent1 = nakadiEvent("1");
        NakadiEvent nakadiEvent2 = nakadiEvent("2");
        NakadiEvent nakadiEvent3 = nakadiEvent("3");

        when(objectMapper.writeValueAsBytes(any())).thenReturn(new byte[15000000]);

        // 15 MB batch size
        eventBatcher.pushEvent(eventLogEntry1, nakadiEvent1);
        // 30 MB batch size
        eventBatcher.pushEvent(eventLogEntry2, nakadiEvent2);
        // would be 45MB batch size, wich is more than 80% of 50MB,therefore triggers sumission of the previous two
        eventBatcher.pushEvent(eventLogEntry3, nakadiEvent3);

        verify(publisher)
                .accept(eq(asList(
                    new BatchItem(eventLogEntry1, nakadiEvent1),
                    new BatchItem(eventLogEntry2, nakadiEvent2)
                )));
    }

    @Test
    public void shouldTryPublishEventsIndividuallyWhenTheyExceedBatchThresholdThe() throws JsonProcessingException {
        EventLog eventLogEntry1 = eventLogEntry(1, "type1");
        EventLog eventLogEntry2 = eventLogEntry(2, "type1");
        NakadiEvent nakadiEvent1 = nakadiEvent("1");
        NakadiEvent nakadiEvent2 = nakadiEvent("2");

        when(objectMapper.writeValueAsBytes(any()))
                .thenReturn(new byte[45000000])
                .thenReturn(new byte[450]);

        // 45 MB batch size => will form a batch of it's own
        eventBatcher.pushEvent(eventLogEntry1, nakadiEvent1);
        // ... and be sumitted with the next event added
        eventBatcher.pushEvent(eventLogEntry2, nakadiEvent2);

        verify(publisher).accept(eq(singletonList(new BatchItem(eventLogEntry1, nakadiEvent1))));
    }

    @Test
    public void willGracefullySkipNonSerializableEvents() throws JsonProcessingException {
        EventLog eventLogEntry1 = eventLogEntry(1, "type1");
        EventLog eventLogEntry2 = eventLogEntry(2, "type1");
        NakadiEvent nakadiEvent1 = nakadiEvent("1");
        NakadiEvent nakadiEvent2 = nakadiEvent("2");

        when(objectMapper.writeValueAsBytes(any()))
                .thenThrow(new IllegalStateException())
                .thenReturn(new byte[450]);

        // non serializable
        eventBatcher.pushEvent(eventLogEntry1, nakadiEvent1);
        // serializable
        eventBatcher.pushEvent(eventLogEntry2, nakadiEvent2);
        // and flush it
        eventBatcher.finish();

        verify(publisher).accept(eq(singletonList(new BatchItem(eventLogEntry2, nakadiEvent2))));
    }

    private EventLog eventLogEntry(int id, String type) {
        return new EventLog(id, type, "body", "flow", now(), now(), "me", now());
    }

    private NakadiEvent nakadiEvent(String eid) {
        NakadiMetadata metadata = new NakadiMetadata();
        metadata.setEid(eid);
        NakadiEvent nakadiEvent = new NakadiEvent();
        nakadiEvent.setMetadata(metadata);
        return nakadiEvent;
    }
}