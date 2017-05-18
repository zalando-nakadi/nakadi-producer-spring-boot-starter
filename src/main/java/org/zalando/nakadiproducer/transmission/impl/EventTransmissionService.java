package org.zalando.nakadiproducer.transmission.impl;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.singletonList;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zalando.nakadiproducer.eventlog.impl.EventLog;
import org.zalando.nakadiproducer.eventlog.impl.EventLogRepository;
import org.zalando.nakadiproducer.transmission.NakadiPublishingClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class EventTransmissionService {

    @Autowired
    private EventLogRepository eventLogRepository;

    @Autowired
    private NakadiPublishingClient nakadiPublishingClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public Collection<EventLog> lockSomeEvents() {
        String lockId = UUID.randomUUID().toString();
        log.debug("Locking events for replication with lockId {}", lockId);
        eventLogRepository.lockSomeMessages(lockId, Instant.now(), Instant.now().plus(10, MINUTES));
        return eventLogRepository.findByLockedByAndLockedUntilGreaterThan(lockId, Instant.now());
    }

    @Transactional
    public void sendEvent(EventLog eventLog) {
        try {
            nakadiPublishingClient.publish(eventLog.getEventType(), singletonList(mapToNakadiEvent(eventLog)));
            log.info("Event {} locked by {} was sucessfully transmitted to nakadi", eventLog.getId(), eventLog.getLockedBy());
            eventLogRepository.delete(eventLog);
        } catch (Exception e) {
            log.error("Event {} locked by {} could not be transmitted to nakadi: {}", eventLog.getId(), eventLog.getLockedBy(), e.getMessage());
        }

    }

    public NakadiEvent mapToNakadiEvent(final EventLog event) {
        final NakadiEvent nakadiEvent = new NakadiEvent();

        final NakadiMetadata metadata = new NakadiMetadata();
        metadata.setEid(convertToUUID(event.getId()));
        metadata.setOccuredAt(event.getCreated());
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

    /**
     * Converts a number in UUID format.
     *
     * <p>For instance 213 will be converted to "00000000-0000-0000-0000-0000000000d5"</p>
     */
    private String convertToUUID(final int number) {
        return new UUID(0, number).toString();
    }


}
