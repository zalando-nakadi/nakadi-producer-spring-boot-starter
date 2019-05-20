package org.zalando.nakadiproducer.transmission.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.zalando.fahrschein.EventPublishingException;
import org.zalando.fahrschein.domain.BatchItemResponse;
import org.zalando.nakadiproducer.eventlog.impl.EventLog;
import org.zalando.nakadiproducer.eventlog.impl.EventLogRepository;
import org.zalando.nakadiproducer.transmission.NakadiPublishingClient;
import org.zalando.nakadiproducer.transmission.impl.EventBatcher.BatchItem;

import javax.transaction.Transactional;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.SECONDS;

@Slf4j
public class EventTransmissionService {

    private final EventLogRepository eventLogRepository;
    private final NakadiPublishingClient nakadiPublishingClient;
    private final ObjectMapper objectMapper;
    private final int lockDuration;
    private final int lockDurationBuffer;

    private Clock clock = Clock.systemDefaultZone();

    public EventTransmissionService(EventLogRepository eventLogRepository, NakadiPublishingClient nakadiPublishingClient, ObjectMapper objectMapper,
    int lockDuration, int lockDurationBuffer) {
        this.eventLogRepository = eventLogRepository;
        this.nakadiPublishingClient = nakadiPublishingClient;
        this.objectMapper = objectMapper;
        this.lockDuration = lockDuration;
        this.lockDurationBuffer = lockDurationBuffer;
    }

    @Transactional
    public Collection<EventLog> lockSomeEvents() {
        String lockId = UUID.randomUUID().toString();
        log.debug("Locking events for replication with lockId {} for {} seconds", lockId, lockDuration);
        eventLogRepository.lockSomeMessages(lockId, now(), now().plus(lockDuration, SECONDS));
        return eventLogRepository.findByLockedByAndLockedUntilGreaterThan(lockId, now());
    }

    @Transactional
    public void sendEvents(Collection<EventLog> events) {
        EventBatcher batcher = new EventBatcher(objectMapper, this::publishBatch);

        for (EventLog event : events) {
            if (lockNearlyExpired(event)) {
                // to avoid that two instances process this event, we skip it
                continue;
            }

            NakadiEvent nakadiEvent;

            try {
                nakadiEvent = mapToNakadiEvent(event);
            } catch (Exception e) {
                log.error("Could not serialize event {} of type {}, skipping it.", event.getId(), event.getEventType(), e);
                continue;
            }

            batcher.pushEvent(event, nakadiEvent);
        }

        batcher.finish();
    }

    /**
     * Publishes a list of events.
     * All of the events in this list need to be destined for the same event type.
     */
    private void publishBatch(List<BatchItem> batch) {
        try {
            this.tryToPublishBatch(batch);
        } catch (Exception e) {
            log.error("Could not send {} events of type {}, skipping them.", batch.size(), batch.get(0).getEventLogEntry().getEventType(), e);
        }
    }

    /**
     * Tries to publish a set of events (all of which need to belong to the same event type).
     * The successful ones will be deleted from the database.
     */
    private void tryToPublishBatch(List<BatchItem> batch) throws Exception {
        Stream<EventLog> successfulEvents;
        String eventType = batch.get(0).getEventLogEntry().getEventType();
        try {
            nakadiPublishingClient.publish(
                    eventType,
                    batch.stream()
                            .map(BatchItem::getNakadiEvent)
                            .collect(Collectors.toList())
            );
            successfulEvents = batch.stream().map(BatchItem::getEventLogEntry);
            log.info("Sent {} events of type {}.", batch.size(), eventType);
        } catch (EventPublishingException e) {
            log.error("Exception ", e);
            log.error("{} out of {} events of type {} failed to be sent.", e.getResponses().length, batch.size(), eventType);
            List<String> failedEids = collectEids(e);
            successfulEvents =
                    batch.stream()
                            .map(BatchItem::getEventLogEntry)
                            .filter(rawEvent -> !failedEids.contains(convertToUUID(rawEvent.getId())));
        }

        successfulEvents.forEach(eventLogRepository::delete);
    }

    private List<String> collectEids(EventPublishingException e) {
        return Arrays.stream(e.getResponses()).map(BatchItemResponse::getEid).collect(Collectors.toList());
    }

    private boolean lockNearlyExpired(EventLog eventLog) {
        // since clocks never work exactly synchronous and sending the event also takes some time, we include a safety
        // buffer here. This is still not 100% precise, but since we require events to be consumed idempotent, sending
        // one event twice wont hurt much.
        return now().isAfter(eventLog.getLockedUntil().minus(lockDurationBuffer, SECONDS));
    }

    private NakadiEvent mapToNakadiEvent(final EventLog event) throws IOException {
        final NakadiEvent nakadiEvent = new NakadiEvent();

        final NakadiMetadata metadata = new NakadiMetadata();
        metadata.setEid(convertToUUID(event.getId()));
        metadata.setOccuredAt(event.getCreated());
        metadata.setFlowId(event.getFlowId());
        nakadiEvent.setMetadata(metadata);

        LinkedHashMap<String, Object> payloadDTO = objectMapper.readValue(event.getEventBodyData(), new TypeReference<LinkedHashMap<String, Object>>() { });

        nakadiEvent.setData(payloadDTO);

        return nakadiEvent;
    }

    private Instant now() {
        return clock.instant();
    }

    public void overrideClock(Clock clock) {
        this.clock = clock;
    }

    /**
     * Converts a number in UUID format.
     *
     * <p>For instance 213 will be converted to "00000000-0000-0000-0000-0000000000d5"</p>
     */
    private String convertToUUID(final int number) {
        return new UUID(0, number).toString();
    }

}
