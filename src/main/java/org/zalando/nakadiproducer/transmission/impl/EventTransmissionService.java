package org.zalando.nakadiproducer.transmission.impl;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.singletonList;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zalando.fahrschein.NakadiClient;
import org.zalando.nakadiproducer.eventlog.impl.EventLog;
import org.zalando.nakadiproducer.eventlog.impl.EventLogRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

@Service
@Slf4j
public class EventTransmissionService {

    @Autowired
    private EventLogRepository eventLogRepository;

    @Autowired
    private NakadiClient nakadiClient;

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
            // If it's a business event we use a different payload wrapper
            if (eventLog.getDataOp() == null && eventLog.getDataType() == null) {
                nakadiClient.publish(eventLog.getEventType(), singletonList(mapToNakadiBusinessPayload(eventLog)));
            } else {
                nakadiClient.publish(eventLog.getEventType(), singletonList(mapToNakadiDataChangePayload(eventLog)));
            }
            log.info("Event {} locked by {} was sucessfully transmitted to nakadi", eventLog.getId(), eventLog.getLockedBy());
            eventLogRepository.delete(eventLog);
        } catch (IOException e) {
            log.error("Event {} locked by {} could not be transmitted to nakadi: {}", eventLog.getId(), eventLog.getLockedBy(), e.getMessage());
        }

    }

    public NakadiDataChangeEvent mapToNakadiDataChangePayload(final EventLog event) {
        final NakadiDataChangeEvent nakadiDataChangeEvent = new NakadiDataChangeEvent();

        final NakadiMetadata metadata = new NakadiMetadata();
        metadata.setEid(convertToUUID(event.getId()));
        metadata.setOccuredAt(event.getCreated());
        nakadiDataChangeEvent.setMetadata(metadata);

        nakadiDataChangeEvent.setDataOperation(event.getDataOp());
        nakadiDataChangeEvent.setDataType(event.getDataType());

        HashMap<String, Object> payloadDTO;
        try {
            payloadDTO = objectMapper.readValue(event.getEventBodyData(), Maps.newLinkedHashMap().getClass());
        } catch (IOException e) {
            log.error("An error occurred at JSON deserialization", e);
            throw new UncheckedIOException(e);
        }

        nakadiDataChangeEvent.setData(payloadDTO);

        return nakadiDataChangeEvent;
    }

    public NakadiBusinessEvent mapToNakadiBusinessPayload(final EventLog event) {
        final NakadiBusinessEvent nakadiBusinessEvent = new NakadiBusinessEvent();

        final NakadiMetadata metadata = new NakadiMetadata();
        metadata.setEid(convertToUUID(event.getId()));
        metadata.setOccuredAt(event.getCreated());
        nakadiBusinessEvent.setMetadata(metadata);

        HashMap<String, Object> payloadDTO;
        try {
            payloadDTO = objectMapper.readValue(event.getEventBodyData(), Maps.newLinkedHashMap().getClass());
        } catch (IOException e) {
            log.error("An error occurred at JSON deserialization", e);
            throw new UncheckedIOException(e);
        }

        nakadiBusinessEvent.setData(payloadDTO);

        return nakadiBusinessEvent;
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
