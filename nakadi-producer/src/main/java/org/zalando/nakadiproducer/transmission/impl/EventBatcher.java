package org.zalando.nakadiproducer.transmission.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.zalando.nakadiproducer.eventlog.impl.EventLog;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class EventBatcher {

    private static final long NAKADI_BATCH_SIZE_LIMIT_IN_BYTES = 50000000;
    private final ObjectMapper objectMapper;
    private final Consumer<List<BatchItem>> publisher;

    private List<BatchItem> batch;
    private long aggregatedBatchSize;

    public EventBatcher(ObjectMapper objectMapper, Consumer<List<BatchItem>> publisher) {
        this.objectMapper = objectMapper;
        this.publisher = publisher;

        this.batch = new ArrayList<>();
        this.aggregatedBatchSize = 0;
    }

    public void pushEvent(EventLog eventLogEntry, NakadiEvent nakadiEvent) {
        long eventSize;

        try {
            eventSize = objectMapper.writeValueAsBytes(nakadiEvent).length;
        } catch (Exception e) {
            log.error("Could not serialize event {} of type {}, skipping it.", eventLogEntry.getId(), eventLogEntry.getEventType(), e);
            return;
        }


        if (!batch.isEmpty() &&
                (hasAnotherEventType(batch, eventLogEntry) || batchWouldBecomeTooBig(aggregatedBatchSize, eventSize))) {
            this.publisher.accept(batch);

            batch = new ArrayList<>();
            aggregatedBatchSize = 0;
        }

        batch.add(new BatchItem(eventLogEntry, nakadiEvent));
        aggregatedBatchSize += eventSize;
    }

    public void finish() {
        if (!batch.isEmpty()) {
            this.publisher.accept(batch);
        }
    }

    private boolean hasAnotherEventType(List<BatchItem> batch, EventLog event) {
        return !event.getEventType().equals(batch.get(0).getEventLogEntry().getEventType());
    }

    private boolean batchWouldBecomeTooBig(long aggregatedBatchSize, long eventSize) {
        return aggregatedBatchSize + eventSize > 0.8 * NAKADI_BATCH_SIZE_LIMIT_IN_BYTES;
    }

    @AllArgsConstructor
    @Getter
    @EqualsAndHashCode
    @ToString
    protected static class BatchItem {
        EventLog eventLogEntry;
        NakadiEvent nakadiEvent;
    }

}
