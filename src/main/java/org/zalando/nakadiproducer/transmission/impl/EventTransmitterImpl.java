package org.zalando.nakadiproducer.transmission.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zalando.nakadiproducer.NakadiProperties;
import org.zalando.nakadiproducer.transmission.EventTransmitter;

@Component
public class EventTransmitterImpl implements EventTransmitter {
    @Autowired
    private NakadiProperties nakadiProperties;

    @Autowired
    private EventTransmissionService eventLogService;

    @Scheduled(fixedDelayString = "${nakadi-producer.transmission-polling-delay}")
    protected void sendEventsIfSchedulingEnabled() {
        if (nakadiProperties.isScheduledTransmissionEnabled()) {
            sendEvents();
        }
    }

    @Override
    public void sendEvents() {
        eventLogService.lockSomeEvents().forEach(eventLogService::sendEvent);
    }
}
