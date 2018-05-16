package org.zalando.nakadiproducer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.zalando.nakadiproducer.transmission.impl.EventTransmitter;

public class EventTransmissionScheduler {
    private final EventTransmitter eventTransmitter;
    private final boolean scheduledTransmissionEnabled;

    public EventTransmissionScheduler(EventTransmitter eventTransmitter, @Value("${nakadi-producer.scheduled-transmission-enabled:true}") boolean scheduledTransmissionEnabled) {
        this.eventTransmitter = eventTransmitter;
        this.scheduledTransmissionEnabled = scheduledTransmissionEnabled;
    }

    @Scheduled(fixedDelayString = "${nakadi-producer.transmission-polling-delay:1000}")
    protected void sendEventsIfSchedulingEnabled() {
        if (scheduledTransmissionEnabled) {
            eventTransmitter.sendEvents();
        }
    }
}
