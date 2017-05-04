package org.zalando.nakadiproducer.transmission.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zalando.nakadiproducer.transmission.EventTransmitter;

@Component
public class EventTransmitterImpl implements EventTransmitter {
    @Autowired
    private EventTransmissionService eventLogService;

    @Value("${nakadi-producer.scheduled-transmission-enabled:true}")
    private boolean scheduledTransmissionEnabled;

    @Scheduled(fixedDelayString = "${nakadi-producer.transmission-polling-delay:1000}")
    protected void sendEventsIfSchedulingEnabled() {
        if (scheduledTransmissionEnabled) {
            sendEvents();
        }
    }

    @Override
    public void sendEvents() {
        eventLogService.lockSomeEvents().forEach(eventLogService::sendEvent);
    }
}
