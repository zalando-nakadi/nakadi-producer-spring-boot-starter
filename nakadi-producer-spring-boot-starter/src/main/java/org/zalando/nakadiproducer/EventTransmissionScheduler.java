package org.zalando.nakadiproducer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zalando.nakadiproducer.transmission.impl.EventTransmitter;

@Component
public class EventTransmissionScheduler {
    private final EventTransmitter eventTransmitter;
    private final boolean scheduledTransmissionEnabled;

    @Autowired
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
