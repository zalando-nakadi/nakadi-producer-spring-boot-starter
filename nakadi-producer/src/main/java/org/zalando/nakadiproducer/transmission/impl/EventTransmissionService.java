package org.zalando.nakadiproducer.transmission.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.zalando.nakadiproducer.eventlog.impl.EventLog;
import org.zalando.nakadiproducer.eventlog.impl.EventLogRepository;
import org.zalando.nakadiproducer.transmission.NakadiPublishingClient;

import javax.transaction.Transactional;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.singletonList;

@Slf4j
public class EventTransmissionService {

    private final EventLogRepository eventLogRepository;
    private final NakadiPublishingClient nakadiPublishingClient;
    private final ObjectMapper objectMapper;

    private Clock clock = Clock.systemDefaultZone();

    public EventTransmissionService(EventLogRepository eventLogRepository, NakadiPublishingClient nakadiPublishingClient, ObjectMapper objectMapper) {
        this.eventLogRepository = eventLogRepository;
        this.nakadiPublishingClient = nakadiPublishingClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Collection<EventLog> lockSomeEvents() {
        String lockId = UUID.randomUUID().toString();
        log.debug("Locking events for replication with lockId {}", lockId);
        eventLogRepository.lockSomeMessages(lockId, now(), now().plus(10, MINUTES));
        return eventLogRepository.findByLockedByAndLockedUntilGreaterThan(lockId, now());
    }

    @Transactional
    public void sendEvent(EventLog eventLog) {
        if (lockNearlyExpired(eventLog)) {
            // to avoid that two instances process this event, we skip it
            return;
        }

        try {
            nakadiPublishingClient.publish(eventLog.getEventType(), singletonList(mapToNakadiEvent(eventLog)));
            log.info("Event {} locked by {} was successfully transmitted to nakadi", eventLog.getId(), eventLog.getLockedBy());
            eventLogRepository.delete(eventLog);
        } catch (Exception e) {
            log.error("Event {} locked by {} could not be transmitted to nakadi: {}", eventLog.getId(), eventLog.getLockedBy(), e.toString());
        }

    }

    private boolean lockNearlyExpired(EventLog eventLog) {
        // since clocks never work exactly synchronous and sending the event also takes some time, we include a minute
        // of safety buffer here. This is still not 100% precise, but since we require events to be consumed idempotent,
        // sending one event twice wont hurt much.
        return now().isAfter(eventLog.getLockedUntil().minus(1, MINUTES));
    }

    public NakadiEvent mapToNakadiEvent(final EventLog event) {
        final NakadiEvent nakadiEvent = new NakadiEvent();

        final NakadiMetadata metadata = new NakadiMetadata();
        metadata.setEid(convertToUUID(event.getId()));
        metadata.setOccuredAt(event.getCreated());
        metadata.setFlowId(event.getFlowId());
        nakadiEvent.setMetadata(metadata);

        HashMap<String, Object> payloadDTO;
        try {
            payloadDTO = objectMapper.readValue(event.getEventBodyData(), new TypeReference<LinkedHashMap<String, Object>>() { });
        } catch (IOException e) {
            log.error("An error occurred at JSON deserialization", e);
            throw new UncheckedIOException(e);
        }

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
