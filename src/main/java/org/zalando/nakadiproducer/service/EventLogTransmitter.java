package org.zalando.nakadiproducer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zalando.nakadiproducer.NakadiProperties;

@Component
public class EventLogTransmitter {
    @Autowired
    private NakadiProperties nakadiProperties;

    @Autowired
    private EventLogService eventLogService;

    @Scheduled(fixedDelayString = "${nakadi-producer.transmission-polling-delay}")
    protected void sendMessagesPeriodically() {
        if (nakadiProperties.isScheduledTransmissionEnabled()) {
            sendMessages();
        }
    }

    public void sendMessages() {
        eventLogService.lockSomeEvents().forEach(eventLogService::sendEvent);
    }
}
