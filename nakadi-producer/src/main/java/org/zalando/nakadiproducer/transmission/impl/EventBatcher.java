package org.zalando.nakadiproducer.transmission.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.zalando.nakadiproducer.eventlog.impl.EventLog;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

@Slf4j
public class EventBatcher {

    private static final long NAKADI_BATCH_SIZE_LIMIT_IN_BYTES = 50000000;
    private final ObjectMapper objectMapper;
    private final BiConsumer<List<EventLog>, List<NakadiEvent>> publisher;

    private List<EventLog> rawBatch;
    private List<NakadiEvent> mappedBatch;
    private long aggregatedBatchSize;

    public EventBatcher(ObjectMapper objectMapper, BiConsumer<List<EventLog>, List<NakadiEvent>> publisher) {
        this.objectMapper = objectMapper;
        this.publisher = publisher;

        this.rawBatch = new ArrayList<>();
        this.mappedBatch = new ArrayList<>();
        this.aggregatedBatchSize = 0;
    }

    public void pushEvent(EventLog event, NakadiEvent nakadiEvent) {
        long eventSize;

        try {
            eventSize = objectMapper.writeValueAsBytes(nakadiEvent).length;
        } catch (Exception e) {
            log.error("Could not serialize event {} of type {}, skipping it.", event.getId(), event.getEventType(), e);
            return;
        }


        if (rawBatch.size() > 0 &&
                (hasAnotherEventType(rawBatch, event) || batchWouldBecomeTooBig(aggregatedBatchSize, eventSize))) {
            this.publisher.accept(rawBatch, mappedBatch);

            rawBatch = new ArrayList<>();
            mappedBatch = new ArrayList<>();
            aggregatedBatchSize = 0;
        }

        rawBatch.add(event);
        mappedBatch.add(nakadiEvent);
        aggregatedBatchSize += eventSize;
    }

    public void finish() {
        if (rawBatch.size() > 0) {
            this.publisher.accept(rawBatch, mappedBatch);
        }
    }

    private boolean hasAnotherEventType(List<EventLog> rawBatch, EventLog event) {
        return !event.getEventType().equals(rawBatch.get(0).getEventType());
    }

    private boolean batchWouldBecomeTooBig(long aggregatedBatchSize, long eventSize) {
        return aggregatedBatchSize + eventSize > 0.8 * NAKADI_BATCH_SIZE_LIMIT_IN_BYTES;
    }

}
